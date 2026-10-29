package com.apexfission.android.attestra.auth.capture

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier

/**
 * Component of the identity review screen containing the editable form fields for card details.
 */
@Composable internal fun CardReviewForm(
    ui: CaptureUi,
    onEdit: (CardData) -> Unit,
    onReset: () -> Unit,
    onOrigin: (String) -> Unit,
    onSave: () -> Unit,
) {
    val data = ui.draft ?: return
    Text("Review your card details", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    Text("Check each value against your card and correct any mistakes. Leave missing or unreadable information blank. Keep dates as printed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (ui.record?.reviewed != null && !ui.reviewDirty) Text("Your reviewed details are saved.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    CardField("Full name", data.fullName, ui.busy) { onEdit(data.copy(fullName = it)) }
    CardField("Given names", data.givenNames, ui.busy) { onEdit(data.copy(givenNames = it)) }
    CardField("Surnames", data.surnames, ui.busy) { onEdit(data.copy(surnames = it)) }
    CardField("Date of birth", data.birthDate, ui.busy) { onEdit(data.copy(birthDate = it)) }
    CardField("Document number", data.documentNumber, ui.busy) { onEdit(data.copy(documentNumber = it)) }
    CardField("Personal number (e.g. CURP)", data.personalNumber, ui.busy) { onEdit(data.copy(personalNumber = it)) }
    CardField("Address", data.address, ui.busy, multiline = true) { onEdit(data.copy(address = it)) }
    CardField("Issue date", data.issueDate, ui.busy) { onEdit(data.copy(issueDate = it)) }
    CardField("Expiry date", data.expiryDate, ui.busy) { onEdit(data.copy(expiryDate = it)) }
    CardField("Issuing country", data.issuingCountry, ui.busy) { onEdit(data.copy(issuingCountry = it)) }
    var countriesOpen by remember { mutableStateOf(false) }
    val countries = mapOf("CA" to "Canada", "US" to "United States", "MX" to "Mexico", "GT" to "Guatemala", "BZ" to "Belize")
    Text("Country of origin", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Text("Select your country of origin. It may differ from the country that issued your ID.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box {
        OutlinedButton(onClick = { countriesOpen = true }, enabled = !ui.busy) { Text(countries[ui.originCountry] ?: "Select country") }
        DropdownMenu(expanded = countriesOpen, onDismissRequest = { countriesOpen = false }) {
            countries.forEach { (code, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onOrigin(code); countriesOpen = false }) }
        }
    }
    if (!data.isValid()) Text("A field is too long. Shorten it before saving.", color = MaterialTheme.colorScheme.error)
    Button(onClick = onSave, enabled = !ui.busy && data.isValid() && !data.isEmpty() && ui.originCountry in countries.keys) { Text("Save and continue") }
    if (ui.reviewDirty) {
        Text("Unsaved edits are kept only while this screen is open.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onReset, enabled = !ui.busy) { Text("Discard edits and use latest details") }
    }
}

/**
 * Component of the review form representing a single input field.
 */
@Composable private fun CardField(label: String, value: String, busy: Boolean, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, enabled = !busy,
        modifier = Modifier.fillMaxWidth(), singleLine = !multiline, maxLines = if (multiline) 4 else 1)
}

@Preview
@Composable
private fun CardReviewFormPreview() {
    MaterialTheme {
        CardReviewForm(
            ui = CaptureUi(),
            onEdit = {},
            onReset = {},
            onOrigin = {},
            onSave = {}
        )
    }
}
