package com.apexfission.android.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.AppRegistration
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Widgets
import com.apexfission.android.democommon.CardTitle
import com.apexfission.android.democommon.DemoCard
import com.apexfission.android.democommon.DemoScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

/**
 * Screen that acts as the main entry point for testing and launching the onboarding flows.
 */
@Composable
internal fun OnboardingEntryScreen(
    hasBackend: Boolean,
    resetting: Boolean,
    resetMessage: String?,
    onDeleteLocalUser: () -> Unit,
    onStart: () -> Unit,
    onCatalog: () -> Unit,
    onCardCapture: () -> Unit,
    applicationId: String = "",
    assetLinksUrl: String = "",
    appLinkDiagnosticState: AppLinkDiagnosticState = AppLinkDiagnosticState.Checking,
    onRecheckAppLinks: () -> Unit = {},
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    DemoScreen(
        title = "Attestra test menu",
        welcomeNote = "Choose a flow to test. Your saved progress stays on this device until you reset it.",
        eyebrow = "AUTHENTICATION",
    ) {
        item(key = "onboarding") {
            DemoCard {
                CardTitle(title = "Onboarding", icon = Icons.Outlined.AppRegistration)
                Text(
                    "In development · Start or resume email confirmation, passkey setup, and ID capture.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onStart,
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Open onboarding") }
                if (!hasBackend) Text(
                    "Environment settings are not ready. Open onboarding to check or retry. The UI catalog remains available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "ui_catalog") {
            DemoCard {
                CardTitle(title = "UI catalog", icon = Icons.Outlined.Widgets)
                Text(
                    "Preview authentication screens without calling the backend. The live ID capture entry requires a configured backend and sign-in.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onCatalog,
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Open UI catalog") }
            }
        }
        item(key = "local_card_capture") {
            DemoCard {
                CardTitle(title = "Card capture", icon = Icons.Outlined.PhotoCamera)
                Text(
                    "Try front/back capture, review, and retakes on this device. No sign-in or backend is needed. Images are discarded after confirmation or exit.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onCardCapture,
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Open card capture demo") }
            }
        }
        if (applicationId.isNotBlank()) {
            item(key = "app_link_diagnostics") {
                val comparison = (appLinkDiagnosticState as? AppLinkDiagnosticState.Ready)?.comparison
                val unavailable = appLinkDiagnosticState as? AppLinkDiagnosticState.Failed
                val checking = appLinkDiagnosticState == AppLinkDiagnosticState.Checking
                val passes = comparison?.passesAppLinks == true
                val statusColor = when {
                    checking -> MaterialTheme.colorScheme.primary
                    passes -> Color(0xFF2E7D32)
                    else -> Color(0xFFC62828)
                }
                val statusLabel = when {
                    checking -> "CHECKING"
                    passes -> "PASS"
                    else -> "FAIL"
                }
                val installedFingerprints = comparison?.installedFingerprints
                    ?: unavailable?.installedFingerprints.orEmpty()
                val publishedFingerprints = comparison?.publishedFingerprints.orEmpty()
                val summary = when {
                    checking -> "Checking the installed APK against the live website association…"
                    comparison != null -> comparison.appLinkSummary()
                    else -> unavailable?.message ?: "The App Link association could not be checked."
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = statusColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = statusColor.copy(alpha = 0.18f),
                            ) {
                                Icon(
                                    imageVector = when {
                                        checking -> Icons.Outlined.Fingerprint
                                        passes -> Icons.Outlined.CheckCircle
                                        else -> Icons.Outlined.ErrorOutline
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "App Link verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Installed build vs live assetlinks.json",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = statusColor,
                            ) {
                                Text(
                                    statusLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checking) MaterialTheme.colorScheme.onSurface else statusColor,
                        )

                        HorizontalDivider(color = statusColor.copy(alpha = 0.25f))

                        Text("Package", style = MaterialTheme.typography.labelMedium)
                        SelectionContainer {
                            Text(
                                applicationId,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }

                        Text("Installed SHA-256", style = MaterialTheme.typography.labelMedium)
                        if (installedFingerprints.isEmpty()) {
                            Text(
                                if (checking) "Reading installed certificate…" else "Unavailable",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    installedFingerprints.forEach { fingerprint ->
                                        Text(
                                            fingerprint,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }

                        Text("Published SHA-256", style = MaterialTheme.typography.labelMedium)
                        if (publishedFingerprints.isEmpty()) {
                            Text(
                                if (checking) "Loading assetlinks.json…" else "No matching package fingerprint found",
                                color = if (!checking) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    publishedFingerprints.forEach { fingerprint ->
                                        Text(
                                            fingerprint,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }

                        if (comparison != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val fingerprintColor = if (comparison.fingerprintMatch) Color(0xFF2E7D32) else Color(0xFFC62828)
                                Text(
                                    if (comparison.fingerprintMatch) "✓ Certificate match" else "✕ Certificate mismatch",
                                    modifier = Modifier.weight(1f),
                                    color = fingerprintColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val relationColor = if (comparison.appLinks) Color(0xFF2E7D32) else Color(0xFFC62828)
                                Text(
                                    if (comparison.appLinks) "✓ App Links relation" else "✕ App Links relation",
                                    modifier = Modifier.weight(1f),
                                    color = relationColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                if (comparison.passkeys) "✓ Passkey relation is also published" else "✕ Passkey relation is not published",
                                color = if (comparison.passkeys) Color(0xFF2E7D32) else Color(0xFFC62828),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (assetLinksUrl.isNotBlank()) {
                            Text("Live association URL", style = MaterialTheme.typography.labelMedium)
                            SelectionContainer {
                                Text(
                                    assetLinksUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRecheckAppLinks,
                            enabled = !checking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(if (checking) "Checking…" else "Recheck")
                        }
                    }
                }
            }
        }
        item(key = "account_actions") {
            DemoCard {
                CardTitle(title = "Account actions", icon = Icons.Outlined.DeleteOutline)
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text("Sign in · Not implemented")
                }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text("Log out · Not implemented")
                }
                Text(
                    "Local reset clears saved sign-in, onboarding progress, and identity test data. It does not delete a server account or a passkey in your credential manager.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text(if (resetting) "Deleting local data…" else "Delete local user") }
                resetMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
    if (confirmDelete && !resetting) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete local user?") },
        text = { Text("Clear this app’s saved session, email, onboarding progress, and identity test data? Your server account and credential-manager passkeys will remain.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteLocalUser() }, shape = MaterialTheme.shapes.small) { Text("Delete local user") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }, shape = MaterialTheme.shapes.small) { Text("Cancel") } },
    )
}

@Preview
@Composable
private fun OnboardingEntryScreenPreview() {
    MaterialTheme {
        OnboardingEntryScreen(
            hasBackend = true,
            resetting = false,
            resetMessage = null,
            onDeleteLocalUser = {},
            onStart = {},
            onCatalog = {},
            onCardCapture = {}
        )
    }
}

/**
 * Screen that displays the status of the environment configuration.
 */
@Composable
internal fun EnvironmentStatusScreen(loading: Boolean, message: String?, onRetry: () -> Unit, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (loading) "Loading environment…" else "Environment unavailable", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(message ?: "Retrieving the selected environment’s public configuration.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
            if (!loading) Button(onClick = onRetry) { Text("Retry") }
            OutlinedButton(onClick = onBack) { Text("Back to test menu") }
        }
    }
}

@Preview
@Composable
private fun EnvironmentStatusScreenPreview() {
    MaterialTheme {
        EnvironmentStatusScreen(
            loading = false,
            message = "Configuration loaded",
            onRetry = {},
            onBack = {}
        )
    }
}
