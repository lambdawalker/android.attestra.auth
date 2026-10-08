package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@kotlinx.serialization.Serializable
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
fun IdentityReviewScreen(
    initialDetails: IdentityDetails, onBack: () -> Unit,
    onSubmit: (IdentityDetails) -> Unit, onRescan: () -> Unit,
    onDetailsChanged: (IdentityDetails) -> Unit = {},
    extractedDetails: IdentityDetails = initialDetails,
    error: String? = null,
) {
    var fullName by remember(initialDetails) { mutableStateOf(initialDetails.fullName) }
    var dateOfBirth by remember(initialDetails) { mutableStateOf(initialDetails.dateOfBirth) }
    var address by remember(initialDetails) { mutableStateOf(initialDetails.address) }
    var documentNumber by remember(initialDetails) { mutableStateOf(initialDetails.documentNumber) }
    var issuingCountry by remember(initialDetails) { mutableStateOf(initialDetails.issuingCountry) }
    var documentType by remember(initialDetails) { mutableStateOf(initialDetails.documentType) }
    var expirationDate by remember(initialDetails) { mutableStateOf(initialDetails.expirationDate) }

    fun notifyChanged() = onDetailsChanged(IdentityDetails(fullName, dateOfBirth, address, documentNumber, issuingCountry, documentType, expirationDate))

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
            if (error != null) BodyText(error)
            if (initialDetails != extractedDetails) BodyText("Your edits will be submitted as corrections. The original extraction is preserved separately.")
            Text("Personal information", fontWeight = FontWeight.SemiBold)
            DetailField("Full legal name", fullName) { fullName = it; notifyChanged() }
            DetailField("Date of birth", dateOfBirth) { dateOfBirth = it; notifyChanged() }
            DetailField("Residential address", address) { address = it; notifyChanged() }
            Text("Document information", fontWeight = FontWeight.SemiBold)
            DetailField("Document number", documentNumber) { documentNumber = it; notifyChanged() }
            DetailField("Issuing country", issuingCountry) { issuingCountry = it; notifyChanged() }
            DetailField("Document type", documentType) { documentType = it; notifyChanged() }
            DetailField("Expiration date", expirationDate) { expirationDate = it; notifyChanged() }
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

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityReviewScreenPreview() {
    AttestraAuthTheme {
        IdentityReviewScreen(IdentityDetails(), {}, {}, {})
    }
}
