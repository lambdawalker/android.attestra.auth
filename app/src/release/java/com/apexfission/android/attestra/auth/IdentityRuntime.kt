package com.apexfission.android.attestra.auth

import androidx.compose.runtime.Composable
import com.apexfission.android.attestra.auth.identity.IdentityIntegrationUnavailable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

internal const val HAS_IDENTITY_DEMO = false

/**
 * Screen that provides a mock UI for testing identity capture without a backend.
 * (Unavailable in release builds).
 */
@Composable internal fun IdentityDemo(onExit: () -> Unit) = IdentityIntegrationUnavailable(onExit)

/**
 * Screen that provides a mock UI for capturing an identity directly.
 * (Falls back to LiveCaptureFlow in release builds).
 */
@Composable internal fun DirectIdentityCapture(onExit: () -> Unit) = LiveCaptureFlow(onExit)

/**
 * Screen that handles the main identity capture flow for an account.
 */
@Composable internal fun AccountIdentityFlow(passkeyAdded: Boolean, acceptedPolicy: com.apexfission.android.attestra.auth.capture.CapturePolicy?, onExit: () -> Unit) = LiveCaptureFlow(onExit, acceptedPolicy)

@Preview
@Composable
private fun IdentityDemoPreview() {
    MaterialTheme {
        IdentityDemo(onExit = {})
    }
}
