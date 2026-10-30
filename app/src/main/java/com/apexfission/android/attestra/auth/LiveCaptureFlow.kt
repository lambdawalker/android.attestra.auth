package com.apexfission.android.attestra.auth

import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.capture.*
import com.apexfission.android.attestra.auth.email.EmailHttpClient
import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import com.apexfission.android.attestra.auth.identity.capture.IdentityCamera
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Screen component that applies FLAG_SECURE to the window to prevent screenshots during capture.
 */
@Composable internal fun CapturePrivacy() {
    val window = (LocalContext.current as? ComponentActivity)?.window
    DisposableEffect(window) {
        val wasSecure = (window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) ?: 0) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/**
 * Screen that orchestrates the live identity capture workflow by communicating with the real backend.
 */
@Composable internal fun LiveCaptureFlow(onExit: () -> Unit, acceptedPolicy: CapturePolicy? = null) {
    val config = LocalBackendConfiguration.current
    val currentConfiguration = rememberUpdatedState(config)
    CapturePrivacy()
    val context = LocalContext.current
    val storage = remember(context) { SecureAuthStorage(context) }
    // Unverified claims are only a local cache namespace. The server verifies the access token.
    fun subject(): String? = runCatching {
        val token = requireNotNull(storage.session()).idToken
        val bytes = Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val claims = EmailHttpClient.json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val sub = requireNotNull(claims["sub"]?.jsonPrimitive?.content).also { require(it.isNotBlank()) }
        "${claims["iss"]?.jsonPrimitive?.content}:$sub"
    }.getOrNull()
    val account = remember(storage) { subject() }
    if (account == null || config.apiUrl.isBlank()) {
        CaptureSignInRequired(onExit)
        return
    }
    val client = remember { CaptureClients.api() }
    val uploads = remember { CaptureClients.uploads() }
    DisposableEffect(client, uploads) { onDispose { client.close(); uploads.close() } }
    val controller = remember(account) {
        CaptureController(
            CaptureApi(config.apiUrl, client, uploads) {
                if (subject() != account) "" else storage.session()?.accessToken.orEmpty()
            },
            SecureCaptureCheckpointStore(storage, config.apiUrl, account),
            mayCreate = { currentConfiguration.value.canStartCapture() },
        ).also { controller -> controller.diagnostic = { android.util.Log.i("AttestraCapture", it) } }
    }
    CaptureHost(controller, {
        if (controller.state.value.signInRequired) storage.clearSession()
        onExit()
    }, startCaptureOnEntry = true, acceptedPolicy = acceptedPolicy, requireReviewedPolicy = true, allowNewCapture = config.canStartCapture()) { captured, error, cancel -> IdentityCamera(captured, error, cancel) }
}

/** Pure authentication-required content shared by runtime and screenshot fixtures. */
@Composable internal fun CaptureSignInRequired(onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Column(Modifier.padding(24.dp)) { Text("Sign in to the configured service to capture your ID."); Button(onClick = onExit) { Text("Return") } }
    }
}
