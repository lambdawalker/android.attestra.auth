package com.apexfission.android.attestra.auth.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.identity.DocumentSide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    allowNewCapture: Boolean = true,
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit,
) {
    val ui by controller.state.collectAsState()
    val language = LocalConfiguration.current.locales[0].language
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller) {
        controller.restore()
        if (startCaptureOnEntry && allowNewCapture) {
            if (controller.state.value.record == null) controller.start(language)
            if (controller.state.value.record?.state == "uploading") controller.camera(DocumentSide.FRONT)
        }
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    // Bounded foreground polling; Refresh remains available after this window.
    LaunchedEffect(ui.record?.id, ui.record?.state, ui.record?.extraction?.status) {
        if (ui.record?.state == "finalizing" || (ui.record?.state == "ready" && ui.record?.extraction?.status == "pending")) repeat(36) { delay(5000); controller.restore() }
    }
    BackHandler { controller.close(); onExit() }
    val side = ui.camera
    if (side != null) {
        camera({ front, back -> scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { controller.capturedBoth(front, back) } }, controller::cameraFailed, controller::cameraCancelled)
        return
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Capture your ID", style = MaterialTheme.typography.headlineMedium)
        Text("Capture both sides, check the photos, and upload them securely. We read the card details so you can review them. This does not verify your identity.")
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val record = ui.record
        if (record == null || record.state in setOf("requires_recapture", "expired", "cancelled")) {
            ui.policy?.let { policy ->
                Text("ID card · JPEG images, up to ${policy.maxBytes / (1024 * 1024)} MiB per side.")
                val disclosureLanguage = ui.record?.disclosureLanguage?.takeIf { it.isNotBlank() } ?: language
                Text(policy.purposes.text(disclosureLanguage).ifBlank { policy.purpose })
                Text("Uploaded photos are retained for up to ${policy.retentionDays} days after capture completes. You can cancel to request deletion.")
            }
        }
        when (record?.state) {
            null -> {
                if (allowNewCapture && ui.policy?.enabled == true) {
                    Button(enabled = !ui.busy, onClick = { scope.launch { controller.start(language) } }) { Text("Start capture") }
                } else Text("Document capture is not enabled for this service yet.")
            }
            "uploading" -> {
                Text("Photos stay in memory until uploaded. After leaving this screen, unfinished photos may need to be retaken.")
                OutlinedButton(enabled = !ui.busy, onClick = { controller.camera(DocumentSide.FRONT) }) {
                    Text(if (ui.uploaded.isEmpty()) "Capture both sides" else "Retake both sides")
                }
                if (ui.message != null) OutlinedButton(enabled = !ui.busy, onClick = { scope.launch { controller.upload() } }) { Text("Retry upload") }
                Button(enabled = !ui.busy && ui.uploaded.containsAll(listOf("front", "back")), onClick = { scope.launch { controller.finalizeCapture() } }) { Text("Finish capture") }
            }
            "finalizing" -> Text("Checking the uploaded files. You can leave and return while this finishes.")
            "ready" -> {
                when (record.extraction?.status) {
                    "pending" -> { Text("Reading your card details", style = MaterialTheme.typography.headlineSmall); CircularProgressIndicator(); Text("You can leave and return while this finishes.") }
                    "failed" -> { Text("We could not read the card details. Your photos are saved."); Button(enabled = !ui.busy, onClick = { scope.launch { controller.retry() } }) { Text("Retry reading card") } }
                    "complete" -> CardReviewForm(ui, controller::editCard, controller::resetReview, controller::editOrigin) { scope.launch { controller.saveReview() } }
                    null -> Text("Both sides are saved. This older capture has no extracted details. Start a new capture after cancelling it to read the card.")
                    else -> Text("Update the app to view this extraction status.")
                }
            }
            "failed" -> { Text("File checks are temporarily unavailable."); Button(enabled = !ui.busy, onClick = { scope.launch { controller.retry() } }) { Text("Retry file checks") } }
            "requires_recapture", "expired", "cancelled" -> { Text("Please capture your document again."); Button(enabled = !ui.busy && allowNewCapture && ui.policy?.enabled == true, onClick = { scope.launch { controller.start(language) } }) { Text("Start again") } }
            else -> Text("This capture state needs a newer app. Your document has not been marked captured here.")
        }
        if (!ui.signInRequired) TextButton(enabled = !ui.busy, onClick = { scope.launch { controller.restore() } }) { Text("Refresh status") }
        if (record != null && record.state !in setOf("expired", "cancelled")) TextButton(enabled = !ui.busy, onClick = { scope.launch { controller.cancel() } }) { Text("Cancel and delete card data") }
        TextButton(onClick = { controller.close(); onExit() }) { Text(if (ui.signInRequired) "Return to sign in" else "Continue later") }
    }
}
