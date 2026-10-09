package com.apexfission.android.attestra.auth.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.identity.DocumentSide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    camera: @Composable (DocumentSide, (ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit,
) {
    val ui by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller) {
        controller.restore()
        if (startCaptureOnEntry) {
            if (controller.state.value.record == null) controller.start()
            if (controller.state.value.record?.state == "uploading") controller.camera(DocumentSide.FRONT)
        }
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    // Bounded foreground polling; Refresh remains available after this window.
    LaunchedEffect(ui.record?.id, ui.record?.state) {
        if (ui.record?.state == "finalizing") repeat(12) { delay(5000); controller.restore() }
    }
    BackHandler { controller.close(); onExit() }
    val side = ui.camera
    if (side != null) {
        camera(side, { bytes -> scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { controller.captured(side, bytes) } }, controller::cameraFailed, controller::cameraCancelled)
        return
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Capture your ID", style = MaterialTheme.typography.headlineMedium)
        Text("Capture both sides, check the photos, and upload them securely. This step saves your document; it does not verify your identity.")
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val record = ui.record
        when (record?.state) {
            null -> {
                if (ui.policy?.enabled == true) {
                    Text("Document: ${ui.policy?.documentType} (${ui.policy?.jurisdiction}). JPEG images, up to ${ui.policy?.maxBytes?.div(1024 * 1024)} MiB per side.")
                    Text("${ui.policy?.purpose}. Uploaded photos are retained for up to ${ui.policy?.retentionDays} days after capture completes. You can cancel to request deletion.")
                    Button(enabled = !ui.busy, onClick = { scope.launch { controller.start() } }) { Text("Start capture") }
                } else Text("Document capture is not enabled for this service yet.")
            }
            "uploading" -> {
                Text("Photos stay in memory until uploaded. After leaving this screen, unfinished photos may need to be retaken.")
                DocumentSide.entries.forEach { side ->
                    val uploaded = side.name.lowercase() in ui.uploaded
                    OutlinedButton(enabled = !ui.busy, onClick = { controller.camera(side) }) { Text("${if (uploaded) "Retake" else "Capture"} ${side.name.lowercase()}") }
                }
                if (ui.message != null) OutlinedButton(enabled = !ui.busy, onClick = { scope.launch { controller.upload() } }) { Text("Retry upload") }
                Button(enabled = !ui.busy && ui.uploaded.containsAll(listOf("front", "back")), onClick = { scope.launch { controller.finalizeCapture() } }) { Text("Finish capture") }
            }
            "finalizing" -> Text("Checking the uploaded files. You can leave and return while this finishes.")
            "ready" -> { Text("Document captured", style = MaterialTheme.typography.headlineSmall); Text("Both sides are saved. Reading document details will be a separate step.") }
            "failed" -> { Text("File checks are temporarily unavailable."); Button(enabled = !ui.busy, onClick = { scope.launch { controller.retry() } }) { Text("Retry file checks") } }
            "requires_recapture", "expired", "cancelled" -> { Text("Please capture your document again."); Button(enabled = !ui.busy, onClick = { scope.launch { controller.start() } }) { Text("Start again") } }
            else -> Text("This capture state needs a newer app. Your document has not been marked captured here.")
        }
        if (!ui.signInRequired) TextButton(enabled = !ui.busy, onClick = { scope.launch { controller.restore() } }) { Text("Refresh status") }
        if (record != null && record.state !in setOf("expired", "cancelled")) TextButton(enabled = !ui.busy, onClick = { scope.launch { controller.cancel() } }) { Text("Cancel capture and delete photos") }
        TextButton(onClick = { controller.close(); onExit() }) { Text(if (ui.signInRequired) "Return to sign in" else "Continue later") }
    }
}
