package com.apexfission.android.attestra.auth.ui.onboarding.passkey

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasskeyStartScreen(onBack: () -> Unit, onCreatePasskey: () -> Unit, onDoLater: () -> Unit) {
    OnboardingFrame(
        title = "Protect your account with a passkey", icon = Icons.Default.Key,
        step = "3/5", onBack = onBack,
        primaryLabel = "Create passkey", onPrimary = onCreatePasskey,
        secondaryLabel = "Do this later", onSecondary = onDoLater,
    ) {
        OnboardingCard {
            BodyText("Start passkey setup and follow the instructions from your device or password manager.")
            StatusRow("Faster sign-in", "Use a passkey on a supported device.")
            StatusRow("No password to remember", "You can choose where your passkey is saved.")
            BodyText("We will mark the passkey as added only when registration completes.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyStartScreenPreview() {
    AttestraAuthTheme {
        PasskeyStartScreen({}, {}, {})
    }
}
