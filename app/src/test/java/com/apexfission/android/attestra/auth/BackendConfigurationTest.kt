package com.apexfission.android.attestra.auth
import org.junit.Assert.*
import org.junit.Test
class BackendConfigurationTest {
    @Test fun admissionRequiresUnexpiredRuntimeSettings() {
        assertFalse(BackendConfiguration("https://example.com", true).canStartCapture(100))
        val config = BackendConfiguration("https://example.com", true, 200)
        assertTrue(config.canStartCapture(199))
        assertFalse(config.canStartCapture(200))
        assertFalse(config.copy(captureEnabled = false).canStartCapture(100))
    }
}
