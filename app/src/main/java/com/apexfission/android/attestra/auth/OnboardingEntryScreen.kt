package com.apexfission.android.attestra.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingEntryScreen(
    hasBackend: Boolean,
    resetting: Boolean,
    resetMessage: String?,
    onDeleteLocalUser: () -> Unit,
    onStart: () -> Unit,
    onCatalog: () -> Unit,
    onIdentityDemo: (() -> Unit)? = null,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Attestra test menu", style = MaterialTheme.typography.headlineMedium)
        Text("Choose a flow to test. Your saved progress stays on this device until you reset it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Onboarding", style = MaterialTheme.typography.titleLarge)
                Text("In development · Start or resume email confirmation, passkey setup, and ID capture.")
                Button(onClick = onStart, enabled = !resetting, modifier = Modifier.fillMaxWidth()) { Text("Open onboarding") }
                if (!hasBackend) Text("Environment settings are not ready. Open onboarding to check or retry. The UI catalog and local capture demo remain available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Account actions", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Sign in · Not implemented") }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Log out · Not implemented") }
                Text("Local reset clears saved sign-in, onboarding progress, and identity test data. It does not delete a server account or a passkey in your credential manager.")
                OutlinedButton(onClick = { confirmDelete = true }, enabled = !resetting, modifier = Modifier.fillMaxWidth()) {
                    Text(if (resetting) "Deleting local data…" else "Delete local user")
                }
                resetMessage?.let { Text(it) }
            }
        }
        if (onIdentityDemo != null) Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ID capture", style = MaterialTheme.typography.titleLarge)
                Text("Use a sample card to test capture, uploads and file-check recovery. This does not verify a real identity.")
                OutlinedButton(onClick = onIdentityDemo, enabled = !resetting, modifier = Modifier.fillMaxWidth()) { Text("Test ID capture") }
            }
        }
        OutlinedButton(onClick = onCatalog, enabled = !resetting, modifier = Modifier.fillMaxWidth()) { Text("Open UI catalog") }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete local user?") },
        text = { Text("Clear this app’s saved session, email, onboarding progress, and identity test data? Your server account and credential-manager passkeys will remain.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteLocalUser() }) { Text("Delete local user") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

@Composable
internal fun EnvironmentStatusScreen(loading: Boolean, message: String?, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (loading) "Loading environment…" else "Environment unavailable", style = MaterialTheme.typography.headlineMedium)
        Text(message ?: "Retrieving the selected environment’s public configuration.", modifier = Modifier.padding(vertical = 20.dp))
        if (!loading) Button(onClick = onRetry) { Text("Retry") }
        OutlinedButton(onClick = onBack) { Text("Back to test menu") }
    }
}
