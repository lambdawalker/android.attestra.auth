package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentityDocumentUnreadableScreen(onBack: () -> Unit, onRetake: () -> Unit, onDashboard: () -> Unit) {
    OnboardingFrame(
        title = "Document images need another try", icon = Icons.Default.PhotoCamera,
        step = "ID check", onBack = onBack,
        primaryLabel = "Try again", onPrimary = onRetake,
        secondaryLabel = "Go to dashboard", onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText("We couldn’t read the document images clearly. Retake them with good lighting and all details visible.")
            BodyText("Check for glare, missing corners, and blurry text. The capture provider will handle the retake.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityDocumentUnreadableScreenPreview() {
    AttestraAuthTheme {
        IdentityDocumentUnreadableScreen({}, {}, {})
    }
}
