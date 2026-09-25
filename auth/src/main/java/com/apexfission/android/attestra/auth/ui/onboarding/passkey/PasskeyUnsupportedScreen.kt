package com.apexfission.android.attestra.auth.ui.onboarding.passkey

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasskeyUnsupportedScreen(onBack: () -> Unit, onDoLater: () -> Unit, onCheckAgain: () -> Unit) {
    OnboardingFrame(
        title = "Passkey setup isn’t available here", icon = Icons.Default.KeyOff,
        step = "3/5", onBack = onBack,
        primaryLabel = "Do this later", onPrimary = onDoLater,
        secondaryLabel = "Check again", onSecondary = onCheckAgain,
    ) {
        OnboardingCard {
            BodyText("Try a supported device or password manager. You can add a passkey later.")
            BodyText("Check again if this device’s passkey support has changed.")
            Text("You can use email sign-in while no passkey is registered.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyUnsupportedScreenPreview() {
    AttestraAuthTheme {
        PasskeyUnsupportedScreen({}, {}, {})
    }
}
