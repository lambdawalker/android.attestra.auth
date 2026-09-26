package com.apexfission.android.attestra.auth.ui.onboarding.email

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailWaitScreen(
    email: String, onBack: () -> Unit, onResend: () -> Unit, onChangeEmail: () -> Unit,
    canResend: Boolean = true, resendHelp: String? = null, onSignIn: (() -> Unit)? = null,
) {
    OnboardingFrame(
        title = "Check your email", icon = Icons.Default.Visibility, step = "2/5", onBack = onBack,
        primaryLabel = "Resend email", onPrimary = onResend, primaryEnabled = canResend,
        secondaryLabel = if (onSignIn != null) "Already verified? Sign in" else "Change email address", onSecondary = { if (onSignIn != null) onSignIn() else onChangeEmail() },
    ) {
        OnboardingCard {
            BodyText("If a message arrives, open its confirmation link for:")
            if (email.isNotBlank()) Text(email, fontWeight = FontWeight.SemiBold)
            BodyText("Keep the six-digit code in that email available in case it is requested.")
            if (resendHelp != null) BodyText(resendHelp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailWaitScreenPreview() {
    AttestraAuthTheme {
        EmailWaitScreen("name@example.invalid", {}, {}, {})
    }
}
