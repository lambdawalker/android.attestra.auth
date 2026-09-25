package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentitySuccessScreen(onBack: () -> Unit, onDashboard: () -> Unit, passkeyAdded: Boolean) {
    OnboardingFrame(
        title = "Identity check complete", icon = Icons.Default.TaskAlt, step = "ID check", onBack = onBack,
        primaryLabel = "Go to dashboard", onPrimary = onDashboard,
    ) {
        OnboardingCard {
            BodyText("Your identity check is complete. You can now continue to your account.")
            StatusRow("Email verified", "Confirmed on this account.")
            if (passkeyAdded) StatusRow("Passkey added", "Added to your account.")
            StatusRow("Identity check complete", "Approved result received from the identity service.")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentitySuccessScreenPreview() {
    AttestraAuthTheme {
        IdentitySuccessScreen({}, {}, true)
    }
}
