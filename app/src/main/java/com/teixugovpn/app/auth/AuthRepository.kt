package com.teixugovpn.app.auth

import com.teixugovpn.app.network.ApiClient
import com.teixugovpn.app.network.dto.NostrEventDto
import com.teixugovpn.app.network.dto.VerifyRequest
import com.teixugovpn.app.nostr.NostrCrypto
import com.teixugovpn.app.nostr.SecureKeyStorage

sealed class AuthResult {
    data class Success(val pubkey: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthRepository(private val keyStorage: SecureKeyStorage) {

    fun hasLocalIdentity(): Boolean = keyStorage.hasIdentity()

    fun createNewIdentity(): String {
        val kp = NostrCrypto.generateKeypair()
        keyStorage.savePrivateKey(kp.privateKeyHex)
        return kp.privateKeyHex
    }

    fun restoreIdentity(privateKeyHex: String): Boolean {
        val normalized = privateKeyHex.trim().lowercase()
        if (!normalized.matches(Regex("^[0-9a-f]{64}$"))) {
            return false
        }
        keyStorage.savePrivateKey(normalized)
        return true
    }

    fun forgetLocalIdentity() {
        keyStorage.clear()
    }

    suspend fun login(): AuthResult {
        val privateKeyHex = keyStorage.getPrivateKey()
            ?: return AuthResult.Failure("Nenhuma chave encontrada neste dispositivo.")

        val api = ApiClient.service

        val challengeRes = try {
            api.getChallenge()
        } catch (e: Exception) {
            return AuthResult.Failure("Erro de rede ao obter desafio: ${e.message ?: "sem detalhes"}")
        }

        val challengeBody = challengeRes.body()
        if (!challengeRes.isSuccessful || challengeBody?.success != true || challengeBody.nonce == null) {
            return AuthResult.Failure(
                challengeBody?.error ?: "Nao foi possivel obter o desafio de login. HTTP ${challengeRes.code()}."
            )
        }

        val event = try {
            NostrCrypto.signChallenge(privateKeyHex, challengeBody.nonce)
        } catch (e: Exception) {
            return AuthResult.Failure("Erro ao assinar desafio: ${e.message ?: "sem detalhes"}")
        }

        val eventDto = NostrEventDto(
            id = event.id,
            pubkey = event.pubkey,
            created_at = event.createdAt,
            kind = event.kind,
            tags = event.tags,
            content = event.content,
            sig = event.sig,
        )

        val verifyRes = try {
            api.verifyEvent(VerifyRequest(eventDto))
        } catch (e: Exception) {
            return AuthResult.Failure("Erro de rede ao verificar assinatura: ${e.message ?: "sem detalhes"}")
        }

        val verifyBody = verifyRes.body()
        if (!verifyRes.isSuccessful || verifyBody?.success != true) {
            return AuthResult.Failure(verifyBody?.error ?: "Falha ao verificar assinatura. HTTP ${verifyRes.code()}.")
        }

        return AuthResult.Success(verifyBody.pubkey ?: event.pubkey)
    }

    suspend fun logout() {
        try {
            ApiClient.service.logout()
        } finally {
            ApiClient.clearSession()
        }
    }
}
