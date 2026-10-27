package com.apexfission.android.attestra.auth

internal data class BackendConfiguration(
    val apiUrl: String,
    val captureEnabled: Boolean = false,
    val runtimeValidUntilMillis: Long? = null,
) {
    fun canStartCapture(now: Long = System.currentTimeMillis()): Boolean =
        captureEnabled && runtimeValidUntilMillis?.let { now < it } == true
}
