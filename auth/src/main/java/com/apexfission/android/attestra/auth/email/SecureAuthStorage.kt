package com.apexfission.android.attestra.auth.email

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable

@Serializable data class PendingEmail(val email: String, val requestId: String, val tokenA: String, val createdAtMillis: Long)

interface AuthStorage {
    fun pending(): PendingEmail?
    fun savePending(value: PendingEmail)
    fun clearPending()
    fun saveSession(session: AuthSession)
    fun session(): AuthSession?
}

/** Per-install Keystore key; encrypted preferences are excluded from both backup types. */
class SecureAuthStorage(context: Context) : AuthStorage {
    private val prefs = context.applicationContext.getSharedPreferences("attestra_auth_private", Context.MODE_PRIVATE)
    private val json = EmailHttpClient.json
    private val key: SecretKey by lazy {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("attestra_auth_v1", null) as? SecretKey) ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore",
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    "attestra_auth_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    override fun pending(): PendingEmail? = read("pending") { json.decodeFromString<PendingEmail>(it) }
    override fun savePending(value: PendingEmail) = write("pending", json.encodeToString(value))
    override fun clearPending() { prefs.edit().remove("pending").commit() }
    override fun saveSession(session: AuthSession) = write("session", json.encodeToString(session))
    override fun session(): AuthSession? = read("session") { json.decodeFromString<AuthSession>(it) }

    private fun write(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val bytes = cipher.iv + encrypted
        check(prefs.edit().putString(name, Base64.encodeToString(bytes, Base64.NO_WRAP)).commit())
    }

    private fun <T> read(name: String, decode: (String) -> T): T? {
        val stored = prefs.getString(name, null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            decode(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
        } catch (_: Exception) {
            prefs.edit().remove(name).commit()
            null
        }
    }
}
