package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalConfiguration
import com.apexfission.android.attestra.auth.capture.CapturePolicy
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that introduces the identity capture process and displays the privacy policy before starting.
 */
@Composable
fun IdentityStartScreen(
    onBack: () -> Unit, onStartCapture: () -> Unit, onSkip: () -> Unit,
    passkeyAdded: Boolean,
    showAccountStatus: Boolean = true,
    required: Boolean = false,
    capturePolicy: CapturePolicy? = null,
    policyLoading: Boolean = false,
    policyError: String? = null,
    requireDisclosure: Boolean = false,
    onRetryPolicy: () -> Unit = {},
) {
    val language = LocalConfiguration.current.locales[0].language
    val canContinue = !requireDisclosure || (!policyLoading && policyError == null && capturePolicy?.enabled == true)

    OnboardingFrame(
        title = "Capture your ID", icon = Icons.Default.Badge, step = if (required) "Required ID capture" else "Optional ID capture", onBack = onBack,
        primaryLabel = "Continue to capture", onPrimary = onStartCapture, primaryEnabled = canContinue,
        secondaryLabel = if (required) null else "Skip for now", onSecondary = onSkip,
    ) {
        OnboardingCard {
            BodyText(if (required) "Save document photos to finish setup. Capturing a document does not verify your identity." else "You can save document photos now or return later. Capturing a document does not verify your identity.")
            if (showAccountStatus) {
            StatusRow("Email verified", "Your email was confirmed.")
            StatusRow(
                if (passkeyAdded) "Passkey added" else "Passkey not added",
                if (passkeyAdded) "Passkey registration completed." else "You can add a passkey later.",
                complete = passkeyAdded,
            )
            }
            if (requireDisclosure) {
                when {
                    policyLoading || (capturePolicy == null && policyError == null) -> BodyText("Loading capture information…")
                    policyError != null -> {
                        Text(policyError, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetryPolicy) { Text("Retry") }
                    }
                    capturePolicy != null -> {
                        BodyText(capturePolicy.purposes.text(language).ifBlank { capturePolicy.purpose })
                        BodyText("Uploaded photos are retained for up to ${capturePolicy.retentionDays} days after capture completes. You can cancel to request deletion.")
                        if (!capturePolicy.enabled) BodyText("Document capture is not enabled for this service yet.")
                    }
                }
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
