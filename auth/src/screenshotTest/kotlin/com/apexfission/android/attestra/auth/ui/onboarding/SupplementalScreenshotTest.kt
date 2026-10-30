package com.apexfission.android.attestra.auth.ui.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme
import com.apexfission.android.attestra.auth.ui.onboarding.common.*
import com.apexfission.android.attestra.auth.ui.onboarding.id.*
import com.apexfission.android.attestra.auth.identity.IdentityIntegrationUnavailable
import com.apexfission.android.attestra.auth.identity.DocumentSide
import com.apexfission.android.attestra.auth.identity.capture.CaptureConfirmation
import com.apexfission.android.attestra.auth.identity.capture.NoCameraContent
import java.io.ByteArrayOutputStream

/** Preview-only sample pixels, not an identity document or a manually redrawn screen. */
private fun sampleJpeg(): ByteArray {
    val bitmap = Bitmap.createBitmap(600, 360, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).apply {
        drawColor(Color.LTGRAY)
        drawText("SYNTHETIC SAMPLE", 35f, 180f, Paint().apply { color = Color.DKGRAY; textSize = 38f })
    }
    return try { ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); out.toByteArray() } }
    finally { bitmap.recycle() }
}

class SupplementalScreenshotTest {
    @PreviewTest @DocumentationPreview @Composable
    fun components() { AttestraAuthTheme {
        OnboardingFrame("Components", Icons.Default.Shield, "Fixture", {}, primaryLabel = "Continue", primaryEnabled = false) {
            OnboardingCard { BodyText("Synthetic component example"); StatusRow("Ready", "Completed state"); StatusRow("Pending", "Incomplete state", false); WaitingIndicator("Waiting for the service") }
        }
    } }
    @PreviewTest @DocumentationPreview @Composable
    fun catalog() { AttestraAuthTheme { OnboardingCatalog(OnboardingBusinessActions()) } }
    @PreviewTest @DocumentationPreview @Composable
    fun unavailable() { AttestraAuthTheme { IdentityIntegrationUnavailable({}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun noCamera() { AttestraAuthTheme { NoCameraContent({}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun legacyFrontReview() { val jpeg = remember { sampleJpeg() }; AttestraAuthTheme { CaptureConfirmation(DocumentSide.FRONT, jpeg, {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun legacyBackReview() { val jpeg = remember { sampleJpeg() }; AttestraAuthTheme { CaptureConfirmation(DocumentSide.BACK, jpeg, {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun legacyIdentityReview() { AttestraAuthTheme { IdentityReviewScreen(IdentityDetails(), {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun legacyIdentitySuccess() { AttestraAuthTheme { IdentitySuccessScreen({}, {}, passkeyAdded = true) } }
    @PreviewTest @DocumentationPreview @Composable
    fun legacyIdentityUnsuccessful() { AttestraAuthTheme { IdentityUnsuccessfulScreen({}, {}, {}, canRetry = true) } }
    @PreviewTest
    @Preview(name = "Narrow large text", widthDp = 320, heightDp = 740, fontScale = 1.3f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, locale = "en")
    @Composable
    fun narrowLargeTextError() { AttestraAuthTheme { com.apexfission.android.attestra.auth.ui.onboarding.email.EmailCodeScreen("name@example.invalid", {}, {}, {}, error = "Incorrect code. Check your email and try again.", attemptsRemaining = 2) } }
}
