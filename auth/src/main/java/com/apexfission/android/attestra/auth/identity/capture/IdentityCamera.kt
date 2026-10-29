package com.apexfission.android.attestra.auth.identity.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apexfission.android.attestra.auth.capture.CaptureApi
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverviewMode
import com.apexfission.android.carddetector.ui.capture.CardCaptureConfig
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import java.io.ByteArrayOutputStream
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/** 
 * Screen that manages camera permissions and the ID document capture workflow.
 * Permission-gated front/back capture. Side labels guide users; they do not verify identity. 
 */
@Composable
fun IdentityCamera(onCaptured: (ByteArray, ByteArray) -> Unit, onError: () -> Unit, onCancel: () -> Unit) {
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
    ) {
        TwoSidedCardCapture(
            instanceKey = "identity-document",
            modelPath = IdentityCardModel.path,
            classLabels = IdentityCardModel.classes,
            cardClasses = IdentityCardModel.cardClasses,
            config = CardCaptureConfig(autoCaptureEnabled = true),
            onBack = onCancel,
            onComplete = { result ->
                var front: ByteArray? = null
                var back: ByteArray? = null
                try {
                    front = encodeJpeg(result.front.bitmap)
                    back = encodeJpeg(result.back.bitmap)
                } catch (_: Exception) {
                    front?.fill(0); back?.fill(0)
                    onError()
                } finally {
                    result.front.bitmap.recycle()
                    result.back.bitmap.recycle()
                }
                if (front != null && back != null) onCaptured(front, back)
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

@Preview
@Composable
private fun IdentityCameraPreview() {
    MaterialTheme {
        IdentityCamera(
            onCaptured = { _, _ -> },
            onError = {},
            onCancel = {}
        )
    }
}
