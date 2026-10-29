package com.apexfission.android.attestra.auth.identity.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.identity.DocumentSide
import com.apexfission.android.attestra.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/**
 * Screen used during ID capture to confirm a taken photo.
 */
@Composable
internal fun CaptureConfirmation(side: DocumentSide, jpeg: ByteArray, onAccept: () -> Unit, onRetake: () -> Unit, onCancel: () -> Unit) {
    val bitmap = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) }
    DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
    OnboardingFrame(
        title = if (side == DocumentSide.FRONT) "Review the front" else "Review the back",
        icon = Icons.Default.PhotoCamera, step = "ID capture", onBack = onCancel,
        primaryLabel = "Use this photo", onPrimary = onAccept, primaryEnabled = bitmap != null,
        secondaryLabel = "Retake photo", onSecondary = onRetake,
    ) {
        OnboardingCard {
            BodyText("Check that this is the ${side.name.lowercase()} side and that all text and corners are visible.")
            if (bitmap != null) Image(bitmap.asImageBitmap(), "Captured ${side.name.lowercase()} of ID", Modifier.fillMaxWidth().heightIn(max = 320.dp))
        }
    }
}

@Preview
@Composable
private fun CaptureConfirmationPreview() {
    MaterialTheme {
        CaptureConfirmation(
            side = DocumentSide.FRONT,
            jpeg = ByteArray(0),
            onAccept = {},
            onRetake = {},
            onCancel = {}
        )
    }
}
