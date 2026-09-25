package com.apexfission.android.attestra.auth.ui.onboarding.email

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailLinkUnusableScreen(email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit, canRequestEmail: Boolean = true) {
    OnboardingFrame(
        title = "This confirmation link can’t be used", icon = Icons.Default.LinkOff, step = "2/5", onBack = onBack,
        primaryLabel = "Request a new email", onPrimary = onRequestEmail, primaryEnabled = canRequestEmail,
        secondaryLabel = "Change email address", onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            BodyText("This link may have expired or been replaced. Request another email and open its latest link.")
            if (email.isNotBlank()) Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BodyText("The old link and code cannot be reused.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailLinkUnusableScreenPreview() {
    AttestraAuthTheme {
        EmailLinkUnusableScreen("name@example.invalid", {}, {}, {})
    }
}
