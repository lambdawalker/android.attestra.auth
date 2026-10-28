package com.apexfission.android.attestra.auth.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.identity.DocumentSide
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    allowNewCapture: Boolean = true,
    camera: @Composable (DocumentSide, (ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit,
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

    CaptureHostContent(
        ui = ui,
        onExit = onExit,
        allowNewCapture = allowNewCapture,
        onStart = { lang -> scope.launch { controller.start(lang) } },
        onCamera = controller::camera,
        onCaptured = { side, bytes -> scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { controller.captured(side, bytes) } },
        onCameraFailed = controller::cameraFailed,
        onCameraCancelled = controller::cameraCancelled,
        onUpload = { scope.launch { controller.upload() } },
        onFinalizeCapture = { scope.launch { controller.finalizeCapture() } },
        onRetry = { scope.launch { controller.retry() } },
        onEditCard = controller::editCard,
        onResetReview = controller::resetReview,
        onEditOrigin = controller::editOrigin,
        onSaveReview = { scope.launch { controller.saveReview() } },
        onRestore = { scope.launch { controller.restore() } },
        onCancel = { scope.launch { controller.cancel() } },
        onClose = controller::close,
        camera = camera,
    )
}

@Composable
internal fun CaptureHostContent(
    ui: CaptureUi,
    onExit: () -> Unit,
    allowNewCapture: Boolean = true,
    onStart: (String) -> Unit = {},
    onCamera: (DocumentSide) -> Unit = {},
    onCaptured: (DocumentSide, ByteArray) -> Unit = { _, _ -> },
    onCameraFailed: () -> Unit = {},
    onCameraCancelled: () -> Unit = {},
    onUpload: () -> Unit = {},
    onFinalizeCapture: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditCard: (CardData) -> Unit = {},
    onResetReview: () -> Unit = {},
    onEditOrigin: (String) -> Unit = {},
    onSaveReview: () -> Unit = {},
    onRestore: () -> Unit = {},
    onCancel: () -> Unit = {},
    onClose: () -> Unit = {},
    camera: @Composable (DocumentSide, (ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit = { _, _, _, _ -> },
) {
    val language = LocalConfiguration.current.locales[0].language
    val side = ui.camera
    if (side != null) {
        camera(side, { bytes -> onCaptured(side, bytes) }, onCameraFailed, onCameraCancelled)
        return
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Capture your ID", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Capture both sides, check the photos, and upload them securely. We read the card details so you can review them. This does not verify your identity.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val record = ui.record
            if (record == null || record.state in setOf("requires_recapture", "expired", "cancelled")) {
                ui.policy?.let { policy ->
                    Text("ID card · JPEG images, up to ${policy.maxBytes / (1024 * 1024)} MiB per side.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val disclosureLanguage = ui.record?.disclosureLanguage?.takeIf { it.isNotBlank() } ?: language
                    Text(policy.purposes.text(disclosureLanguage).ifBlank { policy.purpose }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Uploaded photos are retained for up to ${policy.retentionDays} days after capture completes. You can cancel to request deletion.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (record?.state) {
                null -> {
                    if (allowNewCapture && ui.policy?.enabled == true) {
                        Button(enabled = !ui.busy, onClick = { onStart(language) }) { Text("Start capture") }
                    } else Text("Document capture is not enabled for this service yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "uploading" -> {
                    Text("Photos stay in memory until uploaded. After leaving this screen, unfinished photos may need to be retaken.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DocumentSide.entries.forEach { side ->
                        val uploaded = side.name.lowercase() in ui.uploaded
                        OutlinedButton(enabled = !ui.busy, onClick = { onCamera(side) }) { Text("${if (uploaded) "Retake" else "Capture"} ${side.name.lowercase()}") }
                    }
                    if (ui.message != null) OutlinedButton(enabled = !ui.busy, onClick = onUpload) { Text("Retry upload") }
                    Button(enabled = !ui.busy && ui.uploaded.containsAll(listOf("front", "back")), onClick = onFinalizeCapture) { Text("Finish capture") }
                }
                "finalizing" -> Text("Checking the uploaded files. You can leave and return while this finishes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                "ready" -> {
                    when (record.extraction?.status) {
                        "pending" -> { Text("Reading your card details", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground); CircularProgressIndicator(); Text("You can leave and return while this finishes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        "failed" -> { Text("We could not read the card details. Your photos are saved.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(enabled = !ui.busy, onClick = onRetry) { Text("Retry reading card") } }
                        "complete" -> CardReviewForm(ui, onEditCard, onResetReview, onEditOrigin, onSaveReview)
                        null -> Text("Both sides are saved. This older capture has no extracted details. Start a new capture after cancelling it to read the card.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Text("Update the app to view this extraction status.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                "failed" -> { Text("File checks are temporarily unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(enabled = !ui.busy, onClick = onRetry) { Text("Retry file checks") } }
                "requires_recapture", "expired", "cancelled" -> { Text("Please capture your document again.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(enabled = !ui.busy && allowNewCapture && ui.policy?.enabled == true, onClick = { onStart(language) }) { Text("Start again") } }
                else -> Text("This capture state needs a newer app. Your document has not been marked captured here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!ui.signInRequired) TextButton(enabled = !ui.busy, onClick = onRestore) { Text("Refresh status") }
            if (record != null && record.state !in setOf("expired", "cancelled")) TextButton(enabled = !ui.busy, onClick = onCancel) { Text("Cancel and delete card data") }
            TextButton(onClick = { onClose(); onExit() }) { Text(if (ui.signInRequired) "Return to sign in" else "Continue later") }
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CaptureHostDarkPreview() {
    AttestraAuthTheme(darkTheme = true) {
        CaptureHostContent(
            ui = CaptureUi(
                policy = CapturePolicy(
                    enabled = true,
                    version = "capture-v1",
                    documentType = "id_card",
                    slots = listOf("front", "back"),
                    contentType = "image/jpeg",
                    maxBytes = 4194304L,
                    retentionDays = 7,
                ),
                record = CaptureRecord(
                    id = "sample_capture_id",
                    evidenceVersion = 1,
                    revision = 1,
                    policyVersion = "capture-v1",
                    documentType = "id_card",
                    state = "uploading",
                    expiresAt = 0,
                ),
                uploaded = setOf("front"),
            ),
            onExit = {},
        )
    }
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
fun CaptureHostLightPreview() {
    AttestraAuthTheme(darkTheme = false) {
        CaptureHostContent(
            ui = CaptureUi(
                policy = CapturePolicy(
                    enabled = true,
                    version = "capture-v1",
                    documentType = "id_card",
                    slots = listOf("front", "back"),
                    contentType = "image/jpeg",
                    maxBytes = 4194304L,
                    retentionDays = 7,
                ),
                record = CaptureRecord(
                    id = "sample_capture_id",
                    evidenceVersion = 1,
                    revision = 1,
                    policyVersion = "capture-v1",
                    documentType = "id_card",
                    state = "uploading",
                    expiresAt = 0,
                ),
                uploaded = setOf("front"),
            ),
            onExit = {},
        )
    }
}



