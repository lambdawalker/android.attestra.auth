package com.apexfission.android.attestra.auth.ui.onboarding.passkey

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasskeyFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDoLater: () -> Unit, cancelled: Boolean) {
    OnboardingFrame(
        title = "Passkey setup incomplete", icon = Icons.Default.SyncProblem,
        step = "3/5", onBack = onBack,
        primaryLabel = "Try again", onPrimary = onRetry,
        secondaryLabel = "Do this later", onSecondary = onDoLater,
    ) {
        OnboardingCard {
            BodyText(if (cancelled) "Passkey setup wasn’t finished." else "We couldn’t add your passkey. Please try again.")
            BodyText("You can add a passkey later. Your email remains confirmed.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyFailedScreenPreview() {
    AttestraAuthTheme {
        PasskeyFailedScreen({}, {}, {}, false)
    }
}
