package com.teixugovpn.app.nostr

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda a chave privada Nostr usando EncryptedSharedPreferences (que por
 * baixo dos panos usa o Android Keystore). Isso protege a chave contra
 * outros apps e contra alguém que tenha acesso só ao sistema de arquivos
 * (ex: backup malfeito) -- mas NÃO é backup: se o usuário desinstalar o
 * app ou trocar de aparelho sem ter guardado a chave em outro lugar, a
 * conta é perdida (isso é intencional, ver o fluxo de onboarding).
 */
class SecureKeyStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "teixugo_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun savePrivateKey(privateKeyHex: String) {
        prefs.edit().putString(KEY_PRIVATE, privateKeyHex).apply()
    }

    fun getPrivateKey(): String? = prefs.getString(KEY_PRIVATE, null)

    fun hasIdentity(): Boolean = getPrivateKey() != null

    fun clear() {
        prefs.edit().remove(KEY_PRIVATE).apply()
    }

    companion object {
        private const val KEY_PRIVATE = "nostr_private_key"
    }
}
