package com.apexfission.android.attestra.auth.ui.onboarding.email

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailSessionRecoveryScreen(onBack: () -> Unit, onSignIn: () -> Unit) {
    OnboardingFrame(
        title = "Email verified", icon = Icons.Default.MarkEmailRead, step = "2/5", onBack = onBack,
        primaryLabel = "Sign in to continue", onPrimary = onSignIn,
    ) {
        OnboardingCard {
            BodyText("Your email was confirmed, but this device could not finish signing in.")
            BodyText("Sign in with an email code to continue to passkey setup. You do not need to confirm your email again.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailSessionRecoveryScreenPreview() {
    AttestraAuthTheme {
        EmailSessionRecoveryScreen({}, {})
    }
}
