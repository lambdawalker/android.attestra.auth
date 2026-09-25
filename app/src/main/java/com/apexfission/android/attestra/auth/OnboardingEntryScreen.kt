package com.apexfission.android.attestra.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingEntryScreen(
    hasBackend: Boolean,
    onStart: () -> Unit,
    onCatalog: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Attestra onboarding", style = MaterialTheme.typography.headlineMedium)
            Text("Choose a flow to test.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Live onboarding", style = MaterialTheme.typography.titleLarge)
                    Text("Request an email, confirm it, and continue to passkey setup. Email links open this flow directly.")
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text("Start onboarding")
                    }
                    if (!hasBackend) {
                        Text(
                            "Backend URL missing. Set attestraApiBaseUrl in Gradle to test email confirmation.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("UI catalog", style = MaterialTheme.typography.titleLarge)
                    Text("Preview each onboarding screen without calling the backend.")
                    OutlinedButton(onClick = onCatalog, modifier = Modifier.fillMaxWidth()) {
                        Text("Open UI catalog")
                    }
                }
            }
        }
    }
}

@Composable
internal fun BackendNotConfiguredScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Backend not configured", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Build the app with attestraApiBaseUrl set to the Go stack's apiUrl to test live onboarding.",
            modifier = Modifier.padding(vertical = 20.dp),
        )
        OutlinedButton(onClick = onBack) { Text("Back to start") }
    }
}
