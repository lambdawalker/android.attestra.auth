package com.apexfission.android.attestra.auth

internal data class BackendConfiguration(
    val apiUrl: String,
    val captureEnabled: Boolean = false,
    val runtimeValidUntilMillis: Long? = null,
    val onboarding: com.apexfission.android.attestra.auth.discovery.OnboardingPolicy? = null,
) {
    fun onboardingPolicy(now: Long = System.currentTimeMillis()) = onboarding.takeIf { runtimeValidUntilMillis?.let { now < it } == true }
    fun canStartCapture(now: Long = System.currentTimeMillis()): Boolean =
        captureEnabled && runtimeValidUntilMillis?.let { now < it } == true
}
