package com.apexfission.android.attestra.auth.capture

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable internal fun CardReviewForm(
    ui: CaptureUi,
    onEdit: (CardData) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val data = ui.draft ?: return
    Text("Review your card details", style = MaterialTheme.typography.headlineSmall)
    Text("Check each value against your card and correct any mistakes. Leave missing or unreadable information blank. Keep dates as printed.")
    if (ui.record?.reviewed != null && !ui.reviewDirty) Text("Your reviewed details are saved.", style = MaterialTheme.typography.titleMedium)
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
    if (!data.isValid()) Text("A field is too long. Shorten it before saving.", color = MaterialTheme.colorScheme.error)
    Button(onClick = onSave, enabled = !ui.busy && data.isValid() && !data.isEmpty() && (ui.reviewDirty || ui.record?.reviewed == null)) { Text("Save reviewed details") }
    if (ui.reviewDirty) {
        Text("Unsaved edits are kept only while this screen is open.")
        TextButton(onClick = onReset, enabled = !ui.busy) { Text("Discard edits and use latest details") }
    }
}

@Composable private fun CardField(label: String, value: String, busy: Boolean, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, enabled = !busy,
        modifier = Modifier.fillMaxWidth(), singleLine = !multiline, maxLines = if (multiline) 4 else 1)
}
