package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun CardDetailsSavedScreen(onDone: () -> Unit, onReview: () -> Unit) {
    OnboardingFrame(
        title = "Details saved", icon = Icons.Default.TaskAlt, step = "Card details", onBack = onDone,
        primaryLabel = "Done", onPrimary = onDone,
        secondaryLabel = "Review details", onSecondary = onReview,
    ) {
        OnboardingCard {
            BodyText("Your card details have been saved. You can now continue to your account.")
            BodyText("Saving these details does not verify your identity.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CardDetailsSavedScreenPreview() {
    AttestraAuthTheme { CardDetailsSavedScreen({}, {}) }
}
