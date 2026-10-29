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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apexfission.android.attestra.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.attestra.auth.identity.DocumentSide
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme
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
    var initializing by remember(controller) { mutableStateOf(true) }
    var refreshGeneration by remember(controller) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(controller) {
        try {
            controller.restore()
            if (startCaptureOnEntry && controller.state.value.message == null) {
                controller.continueToCapture(language, allowNewCapture)
            }
        } finally { initializing = false }
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    // Foreground-only, bounded-rate polling keeps long extraction jobs moving without another tap.
    LaunchedEffect(controller, ui.record?.id, ui.record?.state, ui.record?.extraction?.status, foreground, refreshGeneration) {
        if (foreground && ui.record.isProcessingCard()) {
            var attempts = 0
            while (controller.state.value.record.isProcessingCard() && !controller.state.value.signInRequired) {
                delay(if (attempts++ < 36) 5000L else 15000L)
                controller.restore()
            }
        }
    }
    BackHandler { controller.close(); onExit() }

    CaptureHostContent(
        ui = ui,
        onExit = onExit,
        initializing = initializing,
        allowNewCapture = allowNewCapture,
        onStart = { lang -> scope.launch { controller.continueToCapture(lang, allowNewCapture) } },
        onCamera = controller::camera,
        onCaptured = { front, back -> scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { controller.capturedBoth(front, back) } },
        onCameraFailed = controller::cameraFailed,
        onCameraCancelled = { controller.cameraCancelled(); onExit() },
        onUpload = { scope.launch { controller.uploadAndContinue() } },
        onFinalizeCapture = { scope.launch { controller.finalizeCapture(resumeSelectedUploads = true) } },
        onRetry = { scope.launch { controller.retry() } },
        onEditCard = controller::editCard,
        onResetReview = controller::resetReview,
        onEditOrigin = controller::editOrigin,
        onSaveReview = { scope.launch { controller.saveReview() } },
        onRestore = { refreshGeneration++; scope.launch { controller.restore() } },
        onCancel = { scope.launch { controller.cancel() } },
        onClose = controller::close,
        camera = camera,
    )
}

@Composable
internal fun CaptureHostContent(
    ui: CaptureUi,
    onExit: () -> Unit,
    initializing: Boolean = false,
    allowNewCapture: Boolean = true,
    onStart: (String) -> Unit = {},
    onCamera: (DocumentSide) -> Unit = {},
    onCaptured: (ByteArray, ByteArray) -> Unit = { _, _ -> },
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
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val language = LocalConfiguration.current.locales[0].language
    val destination = captureDestination(ui, initializing)
    if (destination == CaptureDestination.CAMERA) {
        camera(onCaptured, onCameraFailed, onCameraCancelled)
        return
    }
    if (destination == CaptureDestination.LOADING) {
        val task = when {
            initializing -> LoadingTask.PREPARE_CAPTURE
            ui.record?.state == "uploading" -> LoadingTask.UPLOAD_DOCUMENT
            else -> LoadingTask.READ_DOCUMENT
        }
        OnboardingLoadingScreen(task, onBack = { onClose(); onExit() })
        return
    }
    if (destination == CaptureDestination.REVIEW) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (ui.record?.extraction?.status == "failed" && ui.record.reviewed == null) {
                    Text("We could not read your card. Enter your details below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ui.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                CardReviewForm(ui, onEditCard, onResetReview, onEditOrigin, onSaveReview)
                if (ui.message != null) TextButton(enabled = !ui.busy, onClick = onRestore) { Text("Refresh status") }
                TextButton(enabled = !ui.busy, onClick = onCancel) { Text("Cancel and delete card data") }
                TextButton(onClick = { onClose(); onExit() }) { Text("Continue later") }
            }
        }
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
                        Button(enabled = !ui.busy, onClick = { onStart(language) }) { Text("Continue to capture") }
                    } else Text("Document capture is not enabled for this service yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "uploading" -> {
                    Text("Photos stay in memory until uploaded. After leaving this screen, unfinished photos may need to be retaken.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(enabled = !ui.busy, onClick = { onCamera(DocumentSide.FRONT) }) {
                        Text(if (ui.uploaded.isEmpty()) "Capture both sides" else "Retake both sides")
                    }
                    if (ui.message != null) OutlinedButton(enabled = !ui.busy, onClick = onUpload) { Text("Retry upload") }
                    if (ui.message != null && record.selected.keys.containsAll(listOf("front", "back"))) {
                        TextButton(enabled = !ui.busy, onClick = onFinalizeCapture) { Text("Retry processing") }
                    }
                }
                "finalizing" -> Text("Checking the uploaded files. You can leave and return while this finishes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                "ready" -> {
                    when (record.extraction?.status) {
                        "pending" -> { Text("Reading your card details", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground); CircularProgressIndicator(); Text("You can leave and return while this finishes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        "failed", "complete" -> Text("Refresh to load your review form.")
                        null -> Text("Waiting for your card details. Check again or return later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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



