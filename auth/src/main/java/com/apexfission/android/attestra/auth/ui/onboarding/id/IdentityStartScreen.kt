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
    showAccountStatus: Boolean = true,
) {
    OnboardingFrame(
        title = "Capture your ID", icon = Icons.Default.Badge, step = "Optional ID capture", onBack = onBack,
        primaryLabel = "Continue to capture", onPrimary = onStartCapture,
        secondaryLabel = "Skip for now · Go to dashboard", onSecondary = onSkip,
    ) {
        OnboardingCard {
            BodyText("You can save document photos now or return later. Capturing a document does not verify your identity.")
            if (showAccountStatus) {
            StatusRow("Email verified", "Your email was confirmed.")
            StatusRow(
                if (passkeyAdded) "Passkey added" else "Passkey not added",
                if (passkeyAdded) "Passkey registration completed." else "You can add a passkey later.",
                complete = passkeyAdded,
            )
            }
            BodyText("Use a two-sided card. We will ask for camera access, capture each side, and let you review the photos before uploading.")
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
