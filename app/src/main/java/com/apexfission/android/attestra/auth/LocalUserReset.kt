package com.apexfission.android.attestra.auth

import java.io.File

/** Local test reset only: no remote account deletion or credential-manager changes. */
internal fun resetLocalUser(mockDirectory: File, clearAuth: () -> Unit) {
    clearAuth()
    val files = checkNotNull(mockDirectory.listFiles()) { "Could not list identity test data" }
    files.filter {
        it.name.startsWith("identity-mock-") && (it.name.endsWith(".json") || it.name.endsWith(".json.tmp"))
    }.forEach { file -> check(file.delete() || !file.exists()) { "Could not clear identity test data" } }
}
