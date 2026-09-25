package com.apexfission.android.attestra.auth.email

import java.security.MessageDigest
import java.security.SecureRandom

data class EmailProof(val tokenA: String, val challenge: String)

/** A stays on this device; only its S256 challenge is sent during signup. */
class ProofGenerator(private val random: (ByteArray) -> Unit = SecureRandom()::nextBytes) {
    fun create(): EmailProof {
        val secret = ByteArray(32).also(random)
        return EmailProof(
            tokenA = secret.base64Url(),
            challenge = MessageDigest.getInstance("SHA-256").digest(secret).base64Url(),
        )
    }
}

private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

/** Base64url without padding; works on Android API 24 without java.util.Base64. */
internal fun ByteArray.base64Url(): String = buildString((size * 4 + 2) / 3) {
    var i = 0
    while (i < size) {
        val a = this@base64Url[i++].toInt() and 255
        val b = if (i < size) this@base64Url[i++].toInt() and 255 else -1
        val c = if (i < size) this@base64Url[i++].toInt() and 255 else -1
        append(ALPHABET[a ushr 2])
        append(ALPHABET[((a and 3) shl 4) or (if (b < 0) 0 else b ushr 4)])
        if (b >= 0) append(ALPHABET[((b and 15) shl 2) or (if (c < 0) 0 else c ushr 6)])
        if (c >= 0) append(ALPHABET[c and 63])
    }
}
