package com.apexfission.android.attestra.auth.identity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import com.apexfission.android.attestra.auth.ui.onboarding.common.*

@Composable
fun IdentityIntegrationUnavailable(onExit: () -> Unit) {
    OnboardingFrame("Identity check is not available yet", Icons.Default.Badge, "Optional ID check", onExit,
        primaryLabel = "Return to account", onPrimary = onExit) {
        OnboardingCard { BodyText("You can continue using your account. Identity verification will be available when the service is ready.") }
    }
}
