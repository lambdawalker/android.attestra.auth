package com.apexfission.android.attestra.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.capture.*
import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import com.apexfission.android.attestra.auth.identity.capture.IdentityCamera
import com.apexfission.android.attestra.identitymock.*
import java.io.File

internal const val HAS_IDENTITY_DEMO = true

/**
 * Screen that handles the main identity capture flow for an account.
 */
@Composable internal fun AccountIdentityFlow(passkeyAdded: Boolean, acceptedPolicy: com.apexfission.android.attestra.auth.capture.CapturePolicy?, onExit: () -> Unit) = LiveCaptureFlow(onExit, acceptedPolicy)

/**
 * Screen that provides a mock UI for testing identity capture without a backend.
 */
@Composable internal fun IdentityDemo(onExit: () -> Unit) = MockCaptureFlow(onExit, true)

/**
 * Screen that provides a mock UI for capturing an identity directly.
 */
@Composable internal fun DirectIdentityCapture(onExit: () -> Unit) = MockCaptureFlow(onExit, false)

/**
 * Screen component that hosts the mocked capture workflow.
 */
@Composable private fun MockCaptureFlow(onExit: () -> Unit, showTestOptions: Boolean) {
    CapturePrivacy()
    val context = LocalContext.current
    val storage = remember { SecureAuthStorage(context) }
    val store = remember { SecureCaptureCheckpointStore(storage, MockCaptureServer.ORIGIN, "offline-sample") }
    val server = remember {
        MockCaptureServer(object : MockStateStore {
            private val file = File(context.noBackupFilesDir, "identity-mock-capture.json")
            override fun read() = if (file.exists()) file.readText() else null
            override fun write(value: String) {
                val temp = File(file.parentFile, file.name + ".tmp")
                temp.writeText(value); check(temp.renameTo(file))
            }
        })
    }
    var started by remember { mutableStateOf(!showTestOptions || store.load() != null) }
    var selected by remember { mutableStateOf(server.scenario) }
    var generation by remember { mutableIntStateOf(0) }
    val client = remember(server) { server.client() }
    val uploads = remember(server) { server.client() }
    DisposableEffect(client, uploads) { onDispose { client.close(); uploads.close() } }
    val controller = remember(generation) { CaptureController(CaptureApi(MockCaptureServer.ORIGIN, client, uploads) { MockCaptureServer.TOKEN }, store) }
    MockCaptureContent(started, selected, { selected = it },
        { server.scenario = selected; started = true },
        { controller.close(); store.clear(); server.reset(); started = false; generation++ }, onExit) {
        CaptureHost(controller, onExit, startCaptureOnEntry = !showTestOptions) { captured, error, cancel -> IdentityCamera(captured, error, cancel) }
    }
}

/** Pure debug demo shell; injected capture content never starts services in previews. */
@Composable internal fun MockCaptureContent(
    started: Boolean, selected: MockScenario, onSelect: (MockScenario) -> Unit,
    onStart: () -> Unit, onReset: () -> Unit, onExit: () -> Unit,
    capture: @Composable () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                Text("LOCAL CAPTURE MOCK • Sample cards only. Uploads and file checks are simulated on this device.", style = MaterialTheme.typography.labelMedium)
                if (started) TextButton(onClick = { onReset() }) { Text("New sample capture") }
            }
        }
        Box(Modifier.weight(1f)) {
            if (!started) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Test document capture", style = MaterialTheme.typography.headlineSmall)
                MockScenario.entries.forEach { scenario ->
                    OutlinedButton(onClick = { onSelect(scenario) }, modifier = Modifier.fillMaxWidth()) { Text(if (selected == scenario) "✓ ${scenario.label}" else scenario.label) }
                }
                Button(onClick = { onStart() }) { Text("Start sample capture") }
                TextButton(onClick = onExit) { Text("Return") }
            } else capture()
        }
    }
}
    }
