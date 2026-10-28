package com.apexfission.android.attestra.auth
import org.junit.Assert.*
import org.junit.Test
class BackendConfigurationTest {
    @Test fun onboardingPolicyIsUnavailableAtExpiry() {
        val policy = com.apexfission.android.attestra.auth.discovery.OnboardingPolicy.legacy(true)
        val config = BackendConfiguration("https://example.com", true, 200, policy)
        assertEquals(policy, config.onboardingPolicy(199))
        assertNull(config.onboardingPolicy(200))
        assertNull(config.copy(runtimeValidUntilMillis = null).onboardingPolicy(100))
    }
    @Test fun admissionRequiresUnexpiredRuntimeSettings() {
        assertFalse(BackendConfiguration("https://example.com", true).canStartCapture(100))
        val config = BackendConfiguration("https://example.com", true, 200)
        assertTrue(config.canStartCapture(199))
        assertFalse(config.canStartCapture(200))
        assertFalse(config.copy(captureEnabled = false).canStartCapture(100))
    }
}
