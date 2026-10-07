package com.teixugovpn.app.nostr

import fr.acinq.secp256k1.Secp256k1
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Identidade Nostr do usuário: um simples par de chaves secp256k1/Schnorr
 * (BIP-340), gerado no próprio dispositivo. Não existe servidor de "conta"
 * tradicional -- essa chave inteira É a conta.
 *
 * Mesma lógica usada no site (assets/nostr-bundle.js) e no verificador
 * (verifier/server.js), pra garantir que os eventos assinados aqui batem
 * com o que o backend espera.
 *
 * ATENÇÃO -- NÃO TESTADO: não tenho como rodar o Gradle/compilar este
 * projeto neste ambiente (sem acesso ao Google/Maven Central daqui), então
 * não pude confirmar 100% os nomes exatos de método da secp256k1-kmp
 * (`secKeyVerify`, `pubkeyCreate`, `signSchnorr`) nem o tamanho exato que
 * `pubkeyCreate` devolve. Eles seguem a convenção usual dessa lib (e do
 * app Amethyst, que a usa do mesmo jeito), mas ao compilar pela primeira
 * vez, se o compilador reclamar de algum desses nomes, me mande o erro
 * exato que eu ajusto na hora.
 */
data class NostrKeypair(
    val privateKeyHex: String,
    val publicKeyHex: String,
)

/**
 * Representa um evento Nostr simples (NIP-01), usado só para autenticação
 * pontual (kind 22242) -- nunca é publicado em nenhum relay de verdade.
 */
data class NostrEvent(
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>> = emptyList(),
    val content: String,
    var id: String = "",
    var sig: String = "",
)

object NostrCrypto {
    private val secp256k1 = Secp256k1.get()

    fun generateKeypair(): NostrKeypair {
        val random = SecureRandom()
        var privKeyBytes: ByteArray
        // gera até achar uma chave válida (praticamente sempre na primeira tentativa)
        do {
            privKeyBytes = ByteArray(32).also { random.nextBytes(it) }
        } while (!secp256k1.secKeyVerify(privKeyBytes))

        val pubKeyBytes = secp256k1.pubkeyCreate(privKeyBytes)
        val xOnlyPubKey = toXOnly(pubKeyBytes)

        return NostrKeypair(
            privateKeyHex = privKeyBytes.toHex(),
            publicKeyHex = xOnlyPubKey.toHex(),
        )
    }

    fun getPublicKey(privateKeyHex: String): String {
        val privKeyBytes = privateKeyHex.hexToBytes()
        val pubKeyBytes = secp256k1.pubkeyCreate(privKeyBytes)
        return toXOnly(pubKeyBytes).toHex()
    }

    /**
     * Cria e assina um evento de desafio de login. `content` deve ser
     * exatamente o nonce recebido de /api/auth/challenge.php.
     */
    fun signChallenge(privateKeyHex: String, nonce: String): NostrEvent {
        val pubkey = getPublicKey(privateKeyHex)
        val event = NostrEvent(
            pubkey = pubkey,
            createdAt = System.currentTimeMillis() / 1000,
            kind = 22242,
            tags = emptyList(),
            content = nonce,
        )

        val id = computeEventId(event)
        event.id = id.toHex()

        val privKeyBytes = privateKeyHex.hexToBytes()
        val auxRand = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val sig = secp256k1.signSchnorr(id, privKeyBytes, auxRand)
        event.sig = sig.toHex()

        return event
    }

    /**
     * Serialização canônica do NIP-01: [0, pubkey, created_at, kind, tags, content]
     * -- precisa bater byte a byte com o que o PHP/JS calculam, senão o id
     * (e portanto a assinatura) não confere no verificador.
     */
    private fun computeEventId(event: NostrEvent): ByteArray {
        val tagsJson = event.tags.joinToString(",", prefix = "[", postfix = "]") { tag ->
            tag.joinToString(",", prefix = "[", postfix = "]") { "\"${it}\"" }
        }
        val serialized = "[0,\"${event.pubkey}\",${event.createdAt},${event.kind},$tagsJson,\"${escapeJson(event.content)}\"]"
        return MessageDigest.getInstance("SHA-256").digest(serialized.toByteArray(Charsets.UTF_8))
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    /**
     * A chave pública do Nostr é "x-only" (32 bytes) -- descarta o byte de
     * paridade / componente Y, dependendo do formato que a lib devolver.
     *
     * ATENÇÃO: não tenho como compilar este projeto neste ambiente pra
     * confirmar o tamanho exato que `pubkeyCreate` devolve na versão
     * instalada da secp256k1-kmp (pode ser 65 bytes com prefixo 0x04+X+Y,
     * ou 64 bytes crus X+Y). Ao compilar pela primeira vez, adicione um
     * log temporário de `fullPubKey.size` e confirme que bate com um dos
     * casos abaixo -- se não bater com nenhum, ajuste aqui.
     */
    private fun toXOnly(fullPubKey: ByteArray): ByteArray = when (fullPubKey.size) {
        65 -> fullPubKey.copyOfRange(1, 33)  // 0x04 + X(32) + Y(32) -> pega só X
        64 -> fullPubKey.copyOfRange(0, 32)  // X(32) + Y(32) sem prefixo -> pega só X
        32 -> fullPubKey                      // já veio x-only
        else -> throw IllegalStateException(
            "Tamanho de chave pública inesperado: ${fullPubKey.size} bytes. " +
            "Confira a versão da lib secp256k1-kmp e ajuste toXOnly()."
        )
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun String.hexToBytes(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()
