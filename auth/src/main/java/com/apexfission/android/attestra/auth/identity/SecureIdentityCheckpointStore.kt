package com.apexfission.android.attestra.auth.identity

import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import java.security.MessageDigest

/** Namespace includes the service/environment and authenticated subject, never an email or access token. */
class SecureIdentityCheckpointStore(
    private val storage: SecureAuthStorage,
    environment: String,
    subject: String,
) : IdentityCheckpointStore {
    private val namespace = MessageDigest.getInstance("SHA-256")
        .digest("$environment\u0000$subject".toByteArray()).joinToString("") { "%02x".format(it) }
    init { require(environment.isNotBlank() && subject.isNotBlank()) }
    override fun load() = storage.identityCheckpoint(namespace)
    override fun save(checkpoint: IdentityCheckpoint) = storage.saveIdentityCheckpoint(namespace, checkpoint)
    override fun clear() = storage.clearIdentityCheckpoint(namespace)
}
