package com.apexfission.android.attestra.auth

import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.email.EmailHttpClient
import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import com.apexfission.android.attestra.auth.identity.*
import com.apexfission.android.attestra.identitycapture.IdentityCamera
import com.apexfission.android.attestra.identitymock.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.security.MessageDigest

internal const val HAS_IDENTITY_DEMO = true

@Composable
internal fun IdentityDemo(onExit: () -> Unit) = MockIdentityFlow("offline-demo", false, onExit)

@Composable
internal fun AccountIdentityFlow(passkeyAdded: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    val subject = remember {
        runCatching {
            val session = requireNotNull(SecureAuthStorage(context).session())
            val payload = Base64.decode(session.idToken.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val claims = EmailHttpClient.json.parseToJsonElement(payload.decodeToString()).jsonObject
            val sub = requireNotNull(claims["sub"]?.jsonPrimitive?.content).also { require(it.isNotBlank()) }
            "${claims["iss"]?.jsonPrimitive?.content.orEmpty()}:$sub"
        }.getOrNull()
    }
    if (subject == null) {
        Column(Modifier.padding(24.dp)) {
            Text("Sign in again to access your identity check.")
            Button(onClick = onExit) { Text("Return") }
        }
    } else MockIdentityFlow(subject, passkeyAdded, onExit)
}

@Composable
private fun MockIdentityFlow(subject: String, passkeyAdded: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    val storage = remember(context) { SecureAuthStorage(context) }
    val store = remember(subject) { SecureIdentityCheckpointStore(storage, MockIdentityServer.ORIGIN, subject) }
    val hash = remember(subject) { MessageDigest.getInstance("SHA-256").digest(subject.toByteArray()).joinToString("") { "%02x".format(it) } }
    val server = remember(subject) {
        MockIdentityServer(object : MockStateStore {
            private val file = File(context.noBackupFilesDir, "identity-mock-$hash.json")
            override fun read(): String? = if (file.exists()) file.readText() else null
            override fun write(value: String) {
                val temporary = File(file.parentFile, file.name + ".tmp")
                temporary.writeText(value)
                check(temporary.renameTo(file))
            }
        })
    }
    var selected by rememberSaveable(subject) { mutableStateOf(server.scenario.name) }
    var started by rememberSaveable(subject) { mutableStateOf(store.load() != null) }
    var generation by remember { mutableIntStateOf(0) }
    SideEffect { if (server.scenario.name != selected) server.scenario = MockScenario.valueOf(selected) }
    val client = remember(server) { server.client() }
    DisposableEffect(client) { onDispose { client.close() } }
    val controller = remember(subject, server, generation) {
        IdentityController(IdentityApi(MockIdentityServer.ORIGIN, client) { MockIdentityServer.TOKEN }, store)
    }
    // Prevent screenshots/task-switcher previews from retaining document images and corrections.
    val window = (context as? ComponentActivity)?.window
    DisposableEffect(window) {
        val alreadySecure = (window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) ?: 0) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                Text("LOCAL MOCK • Sample cards only. OCR and identity decisions are simulated; images stay on this device.", style = MaterialTheme.typography.labelMedium)
                if (started) TextButton(onClick = {
                    controller.close()
                    store.clear()
                    started = false
                    generation++
                }) { Text("New sample check") }
            }
        }
        Box(Modifier.weight(1f)) {
            if (!started) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Test identity verification", style = MaterialTheme.typography.headlineSmall)
                Text("Choose a simulated service outcome, then capture the front and back of a sample card.")
                MockScenario.entries.forEach { scenario ->
                    OutlinedButton(onClick = { selected = scenario.name }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selected == scenario.name) "✓ ${scenario.label}" else scenario.label)
                    }
                }
                Button(onClick = { server.scenario = MockScenario.valueOf(selected); started = true }) { Text("Start sample check") }
                TextButton(onClick = onExit) { Text("Return") }
            } else {
                IdentityHost(controller, passkeyAdded, onExit, isMock = true) { side, captured, error, cancel ->
                    IdentityCamera(side, captured, error, cancel)
                }
            }
        }
    }
}
