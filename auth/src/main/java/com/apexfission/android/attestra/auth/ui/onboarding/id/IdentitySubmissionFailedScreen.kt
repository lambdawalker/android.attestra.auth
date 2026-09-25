package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentitySubmissionFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDashboard: () -> Unit) {
    OnboardingFrame(
        title = "Could not submit ID details", icon = Icons.Default.ErrorOutline, step = "ID check", onBack = onBack,
        primaryLabel = "Try again", onPrimary = onRetry,
        secondaryLabel = "Go to dashboard", onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText("We could not confirm that your details were submitted. Check your connection and try again.")
            BodyText("Check the submission status before sending the same details again.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentitySubmissionFailedScreenPreview() {
    AttestraAuthTheme {
        IdentitySubmissionFailedScreen({}, {}, {})
    }
}
