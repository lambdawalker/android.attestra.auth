package com.apexfission.android.attestra.auth.identity.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.apexfission.android.attestra.auth.identity.DocumentSide
import com.apexfission.android.attestra.auth.capture.CaptureApi
import com.apexfission.android.carddetector.domain.ModelCatalog
import com.apexfission.android.carddetector.tfmodel.cardClasses
import com.apexfission.android.carddetector.tfmodel.classes
import com.apexfission.android.carddetector.tfmodel.modelPath
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorLite
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverviewMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/** Real camera capture. The side is explicitly selected; detector classes do not establish front/back or identity. */
@Composable
fun IdentityCamera(side: DocumentSide, onCaptured: (ByteArray) -> Unit, onError: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
        Column { Text("This device has no camera."); Button(onClick = onCancel) { Text("Return") } }
        return
    }
    HandlePermissions(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        permissions = listOf(PermissionDescription(Manifest.permission.CAMERA) {
            DefaultPermissionPage("Camera", Icons.Default.PhotoCamera, title = "Scan both sides of your ID",
                body = "Allow the camera to capture the front and back. You can cancel and continue later.")
        }),
        overviewMode = PermissionOverviewMode.Hide,
        onBack = onCancel, onNotNow = onCancel,
    ) { key(side) { CameraSession(side, onCaptured, onError, onCancel) } }
}

@Composable
private fun CameraSession(side: DocumentSide, onCaptured: (ByteArray) -> Unit, onError: () -> Unit, onCancel: () -> Unit) {
    // Dispose the detector's native resources and retained crop when moving between sides/retakes.
    val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    val active = remember { AtomicBoolean(true) }
    val delivering = remember { AtomicBoolean(false) }
    val main = remember { Handler(Looper.getMainLooper()) }
    val captured by rememberUpdatedState(onCaptured)
    val failed by rememberUpdatedState(onError)
    var cameraActive by remember { mutableStateOf(true) }
    var confirmation by remember { mutableStateOf<ByteArray?>(null) }
    DisposableEffect(owner) {
        onDispose { active.set(false); confirmation?.fill(0); owner.viewModelStore.clear() }
    }
    val bytes = confirmation
    if (bytes != null) {
        CaptureConfirmation(side, bytes, onAccept = {
            confirmation = null // ownership moves to the host
            captured(bytes)
        }, onRetake = {
            bytes.fill(0); confirmation = null
            owner.viewModelStore.clear(); delivering.set(false); cameraActive = true
        }, onCancel = onCancel)
    } else if (cameraActive) CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        CardDetectorLite(
            instanceKey = "identity-${side.name.lowercase()}",
            modelPath = ModelCatalog.TfLite.modelPath,
            classLabels = ModelCatalog.TfLite.classes, cardClasses = ModelCatalog.TfLite.cardClasses,
            cameraPreset = CameraPreset.Default,
            onCardDetection = { _, bitmap -> bitmap.recycle() },
            onCapture = { _, bitmap ->
                var output: ByteArray? = null
                try {
                    if (active.get() && delivering.compareAndSet(false, true)) {
                        output = encodeJpeg(bitmap)
                        val encoded = output
                        main.post {
                            if (active.get()) { cameraActive = false; confirmation = encoded }
                            else encoded.fill(0)
                        }
                        output = null
                    }
                } catch (_: Exception) {
                    main.post { if (active.get()) failed() }
                } finally { output?.fill(0); bitmap.recycle() }
            },
            onBack = onCancel,
            controlOverlay = {
                IdCaptureOverlay(config = IdCaptureOverlayConfig(
                    title = if (side == DocumentSide.FRONT) "Capture ID front · 1 of 2" else "Capture ID back · 2 of 2",
                    instructionTitle = if (side == DocumentSide.FRONT) "Show the front of your ID" else "Turn your ID over",
                    instructionSubTitle = "Keep all corners visible, avoid glare, and tap the shutter. Review the photo next.",
                ))
            },
        )
    }
}

private fun encodeJpeg(bitmap: Bitmap): ByteArray {
    val maxEdge = 2048
    val scale = minOf(1f, maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height))
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
    try {
        return ByteArrayOutputStream().use { out ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, out))
            out.toByteArray().also { require(it.size <= CaptureApi.MAX_JPEG_BYTES) }
        }
    } finally { if (scaled !== bitmap) scaled.recycle() }
}
