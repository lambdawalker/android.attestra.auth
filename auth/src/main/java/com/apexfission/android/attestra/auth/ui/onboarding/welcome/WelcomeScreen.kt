package com.apexfission.android.attestra.auth.ui.onboarding.welcome

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/** Landing screen for a signed-in user who deferred the optional ID check. */
@Composable
fun WelcomeScreen(passkeyAdded: Boolean, onCheckId: () -> Unit) {
    OnboardingFrame(
        title = "Welcome to Attestra",
        icon = Icons.Default.VerifiedUser,
        step = "Your account",
        onBack = onCheckId,
        primaryLabel = "Open ID check / status",
        onPrimary = onCheckId,
    ) {
        OnboardingCard {
            BodyText("Your account is ready. You can return to your identity check whenever you're ready.")
            StatusRow("Email verified", "You can use your email to sign in.")
            StatusRow(
                if (passkeyAdded) "Passkey added" else "Passkey not added",
                if (passkeyAdded) "You can sign in with your passkey." else "You can add a passkey later.",
                complete = passkeyAdded,
            )
            StatusRow("Identity check", "Open your check to start, resume, or retrieve its latest result.", complete = false)
            BodyText("Identity verification is optional for now. Some future actions may require it.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun WelcomeScreenPreview() {
    AttestraAuthTheme { WelcomeScreen(passkeyAdded = true, onCheckId = {}) }
}
