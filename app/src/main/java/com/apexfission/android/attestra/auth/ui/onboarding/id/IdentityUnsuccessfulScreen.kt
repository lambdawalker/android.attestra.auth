package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentityUnsuccessfulScreen(
    onBack: () -> Unit, onDashboard: () -> Unit, onRetry: () -> Unit,
    canRetry: Boolean, explanation: String? = null,
) {
    OnboardingFrame(
        title = "We couldn’t complete your identity check", icon = Icons.Default.WarningAmber,
        step = "ID check", onBack = onBack,
        primaryLabel = if (canRetry) "Try again" else "Go to dashboard",
        onPrimary = if (canRetry) onRetry else onDashboard,
        secondaryLabel = if (canRetry) "Go to dashboard" else null,
        onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText(explanation ?: "Your identity check could not be completed. You can return to your account.")
            BodyText("Your account remains available. Try again only if another attempt is allowed.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityUnsuccessfulScreenPreview() {
    AttestraAuthTheme {
        IdentityUnsuccessfulScreen({}, {}, {}, true)
    }
}
