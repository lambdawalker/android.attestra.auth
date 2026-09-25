package com.apexfission.android.attestra.auth.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

data class IdentityDetails(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val documentNumber: String = "",
    val issuingCountry: String = "",
    val documentType: String = "",
    val expirationDate: String = "",
)

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

@Composable
fun IdentityReviewScreen(
    initialDetails: IdentityDetails, onBack: () -> Unit,
    onSubmit: (IdentityDetails) -> Unit, onRescan: () -> Unit,
) {
    var fullName by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.fullName) }
    var dateOfBirth by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.dateOfBirth) }
    var address by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.address) }
    var documentNumber by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.documentNumber) }
    var issuingCountry by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.issuingCountry) }
    var documentType by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.documentType) }
    var expirationDate by rememberSaveable(initialDetails) { mutableStateOf(initialDetails.expirationDate) }

    OnboardingFrame(
        title = "Review ID details", icon = Icons.Default.Badge, step = "ID check", onBack = onBack,
        primaryLabel = "Submit details for review",
        onPrimary = {
            onSubmit(
                IdentityDetails(fullName, dateOfBirth, address, documentNumber, issuingCountry, documentType, expirationDate)
            )
        },
        secondaryLabel = "Rescan document", onSecondary = onRescan,
    ) {
        OnboardingCard {
            BodyText("Review the details read from your document. Correct mistakes before submitting them for an identity check.")
            Text("Personal information", fontWeight = FontWeight.SemiBold)
            DetailField("Full legal name", fullName) { fullName = it }
            DetailField("Date of birth", dateOfBirth) { dateOfBirth = it }
            DetailField("Residential address", address) { address = it }
            Text("Document information", fontWeight = FontWeight.SemiBold)
            DetailField("Document number", documentNumber) { documentNumber = it }
            DetailField("Issuing country", issuingCountry) { issuingCountry = it }
            DetailField("Document type", documentType) { documentType = it }
            DetailField("Expiration date", expirationDate) { expirationDate = it }
            BodyText("Correcting extracted text does not verify your identity. A separate result follows submission.")
        }
    }
}

@Composable
private fun DetailField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = label != "Residential address",
    )
}

@Composable
fun IdentitySubmissionFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDashboard: () -> Unit) {
    OnboardingFrame(
        title = "Could not submit ID details", icon = Icons.Default.ErrorOutline, step = "ID check", onBack = onBack,
        primaryLabel = "Try again", onPrimary = onRetry,
        secondaryLabel = "Go to dashboard", onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText("We could not confirm that your details were submitted. Check your connection and try again.")
            BodyText("Check the submission status before sending the same details again.")
        }
    }
}

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

// The design submodule does not yet contain a final unsuccessful identity mockup.
// This uses the shared error layout until that separate result design is supplied.
@Composable
fun IdentityUnsuccessfulScreen(
    onBack: () -> Unit, onDashboard: () -> Unit, onRetry: () -> Unit,
    canRetry: Boolean, explanation: String? = null,
) {
    OnboardingFrame(
        title = "We couldn’t complete your identity check", icon = Icons.Default.WarningAmber,
        step = "ID check", onBack = onBack,
        primaryLabel = if (canRetry) "Try again" else "Go to dashboard",
        onPrimary = if (canRetry) onRetry else onDashboard,
        secondaryLabel = if (canRetry) "Go to dashboard" else null,
        onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText(explanation ?: "Your identity check could not be completed. You can return to your account.")
            BodyText("Your account remains available. Try again only if another attempt is allowed.")
        }
    }
}
