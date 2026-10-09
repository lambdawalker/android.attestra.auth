package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import java.security.MessageDigest

class SecureCaptureCheckpointStore(private val storage: SecureAuthStorage, environment: String, subject: String) : CaptureCheckpointStore {
    init { require(environment.isNotBlank() && subject.isNotBlank()) }
    private val namespace = MessageDigest.getInstance("SHA-256").digest("$environment\u0000$subject".toByteArray()).joinToString("") { "%02x".format(it) }
    override fun load() = storage.captureCheckpoint(namespace)
    override fun save(value: CaptureCheckpoint) = storage.saveCaptureCheckpoint(namespace, value)
    override fun clear() = storage.clearCaptureCheckpoint(namespace)
}
