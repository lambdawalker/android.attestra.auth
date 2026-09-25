package com.apexfission.android.attestra.auth.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

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

@Composable
fun PasskeyFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDoLater: () -> Unit, cancelled: Boolean) {
    OnboardingFrame(
        title = "Passkey setup incomplete", icon = Icons.Default.SyncProblem,
        step = "3/5", onBack = onBack,
        primaryLabel = "Try again", onPrimary = onRetry,
        secondaryLabel = "Do this later", onSecondary = onDoLater,
    ) {
        OnboardingCard {
            BodyText(if (cancelled) "Passkey setup wasn’t finished." else "We couldn’t add your passkey. Please try again.")
            BodyText("You can add a passkey later. Your email remains confirmed.")
        }
    }
}

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
