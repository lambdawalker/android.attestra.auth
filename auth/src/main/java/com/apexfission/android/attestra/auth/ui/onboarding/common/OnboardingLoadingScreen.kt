package com.apexfission.android.attestra.auth.ui.onboarding.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

enum class LoadingTask(val title: String, val description: String, val step: String) {
    REQUEST_EMAIL("Sending your confirmation email", "We are processing your request.", "1/5"),
    CONFIRM_EMAIL("Verifying your email", "We are confirming your email and preparing your session.", "2/5"),
    RESEND_EMAIL("Requesting another email", "We are processing your request.", "2/5"),
    SIGN_IN("Signing you in", "We are checking your account and preparing your session.", "Account"),
    OPEN_PASSKEY_MANAGER("Opening your passkey manager", "Follow your device or password manager to create a passkey.", "3/5"),
    SAVE_PASSKEY("Saving your passkey", "We are adding the passkey to your account.", "3/5"),
    READ_DOCUMENT("Reading your document", "We are extracting details for you to review.", "ID check"),
    SUBMIT_ID("Submitting your ID details", "We are sending your reviewed details for an identity check.", "ID check"),
    CHECK_IDENTITY("Checking your identity", "Your details have been submitted. We will show the result when available.", "ID check"),
}

@Composable
fun OnboardingLoadingScreen(task: LoadingTask, onBack: () -> Unit, onLeave: (() -> Unit)? = null) {
    OnboardingFrame(
        title = task.title, icon = Icons.Default.Autorenew, step = task.step, onBack = onBack,
        secondaryLabel = if (task == LoadingTask.CHECK_IDENTITY) "Go to dashboard" else null,
        onSecondary = { onLeave?.invoke() },
    ) {
        OnboardingCard {
            WaitingIndicator(task.description)
            BodyText("This may take a moment. We will update the screen when it finishes.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun OnboardingLoadingScreenPreview() {
    AttestraAuthTheme {
        OnboardingLoadingScreen(LoadingTask.CONFIRM_EMAIL, {})
    }
}
