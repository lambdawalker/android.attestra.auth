package com.apexfission.android.attestra.auth.ui.onboarding.email

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme
import com.apexfission.android.attestra.auth.ui.theme.AttestraOnSurfaceVariant

@Composable
fun EmailAttemptLimitScreen(
    email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit,
    canRequestEmail: Boolean, retryAfter: String? = null,
) {
    OnboardingFrame(
        title = "Too many code attempts", icon = Icons.Default.LockClock, step = "2/5", onBack = onBack,
        primaryLabel = "Request a new email", primaryEnabled = canRequestEmail, onPrimary = onRequestEmail,
        secondaryLabel = "Change email address", onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            Text("This code can no longer be used", fontWeight = FontWeight.SemiBold)
            BodyText("Request another email when available. Open its latest link and use its new code together.")
            Text(email, color = AttestraOnSurfaceVariant)
            if (!canRequestEmail) BodyText(retryAfter ?: "Please try again later.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailAttemptLimitScreenPreview() {
    AttestraAuthTheme {
        EmailAttemptLimitScreen("name@example.invalid", {}, {}, {}, true)
    }
}
