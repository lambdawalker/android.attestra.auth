package com.apexfission.android.attestra.auth.identity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import com.apexfission.android.attestra.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/**
 * Screen that informs the user that the ID capture integration is currently unavailable.
 */
@Composable
fun IdentityIntegrationUnavailable(onExit: () -> Unit) {
    OnboardingFrame("Identity check is not available yet", Icons.Default.Badge, "Optional ID check", onExit,
        primaryLabel = "Return to account", onPrimary = onExit) {
        OnboardingCard { BodyText("You can continue using your account. Identity verification will be available when the service is ready.") }
    }
}

@Preview
@Composable
private fun IdentityIntegrationUnavailablePreview() {
    MaterialTheme {
        IdentityIntegrationUnavailable(onExit = {})
    }
}
