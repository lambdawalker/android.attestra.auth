package com.apexfission.android.attestra.auth.ui.onboarding.id

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

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

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityReviewScreenPreview() {
    AttestraAuthTheme {
        IdentityReviewScreen(IdentityDetails(), {}, {}, {})
    }
}
