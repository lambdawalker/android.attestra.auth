package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentityStartScreen(
    onBack: () -> Unit, onStartCapture: () -> Unit, onSkip: () -> Unit,
    passkeyAdded: Boolean,
) {
    OnboardingFrame(
        title = "Confirm your ID", icon = Icons.Default.Badge, step = "Optional ID check", onBack = onBack,
        primaryLabel = "Scan government ID", onPrimary = onStartCapture,
        secondaryLabel = "Skip for now · Go to dashboard", onSecondary = onSkip,
    ) {
        OnboardingCard {
            BodyText("You can start an identity check now or return to it later.")
            StatusRow("Email verified", "Your email was confirmed.")
            StatusRow(
                if (passkeyAdded) "Passkey added" else "Passkey not added",
                if (passkeyAdded) "Passkey registration completed." else "You can add a passkey later.",
                complete = passkeyAdded,
            )
            BodyText("The document capture flow will open in the selected ID capture provider.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityStartScreenPreview() {
    AttestraAuthTheme {
        IdentityStartScreen({}, {}, {}, false)
    }
}
