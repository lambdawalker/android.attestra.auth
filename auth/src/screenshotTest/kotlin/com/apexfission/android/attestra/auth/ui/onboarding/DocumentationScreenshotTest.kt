package com.apexfission.android.attestra.auth.ui.onboarding

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme
import com.apexfission.android.attestra.auth.ui.onboarding.email.*
import com.apexfission.android.attestra.auth.ui.onboarding.id.*
import com.apexfission.android.attestra.auth.ui.onboarding.passkey.*
import com.apexfission.android.attestra.auth.ui.onboarding.password.PasswordScreen
import com.apexfission.android.attestra.auth.ui.onboarding.signin.*
import com.apexfission.android.attestra.auth.ui.onboarding.welcome.WelcomeScreen
import com.apexfission.android.attestra.auth.ui.onboarding.common.*
import com.apexfission.android.attestra.auth.capture.*

@Preview(name = "Light", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "Dark", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "en")
annotation class DocumentationPreview

/** Synthetic, offline fixtures: no controller, account storage, camera or network. */
class DocumentationScreenshotTest {
    @PreviewTest @DocumentationPreview @Composable
    fun emailStart() { AttestraAuthTheme { EmailStartScreen({}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun emailWait() { AttestraAuthTheme { EmailWaitScreen("name@example.invalid", {}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun emailLimit() { AttestraAuthTheme { EmailAttemptLimitScreen("name@example.invalid", {}, {}, {}, canRequestEmail = true) } }

    @PreviewTest @DocumentationPreview @Composable
    fun emailLinkUnusable() { AttestraAuthTheme { EmailLinkUnusableScreen("name@example.invalid", {}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun emailRecovery() { AttestraAuthTheme { EmailSessionRecoveryScreen({}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun passkeyStart() { AttestraAuthTheme { PasskeyStartScreen({}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun passkeyFailed() { AttestraAuthTheme { PasskeyFailedScreen({}, {}, {}, cancelled = false) } }

    @PreviewTest @DocumentationPreview @Composable
    fun passkeyUnsupported() { AttestraAuthTheme { PasskeyUnsupportedScreen({}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun identityStart() { AttestraAuthTheme { IdentityStartScreen({}, {}, {}, passkeyAdded = false) } }

    @PreviewTest @DocumentationPreview @Composable
    fun identityUnreadable() { AttestraAuthTheme { IdentityDocumentUnreadableScreen({}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun identitySubmissionFailed() { AttestraAuthTheme { IdentitySubmissionFailedScreen({}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun detailsSaved() { AttestraAuthTheme { CardDetailsSavedScreen({}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun welcome() { AttestraAuthTheme { WelcomeScreen(passkeyAdded = false, onCheckId = {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun password() { AttestraAuthTheme { PasswordScreen("", {}, {}, {}, error = null) } }

    @PreviewTest @DocumentationPreview @Composable
    fun returnSignIn() { AttestraAuthTheme { ReturnSignInScreen("name@example.invalid", null, {}, {}, {}, onPassword = {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun returnCode() { AttestraAuthTheme { ReturnEmailCodeScreen("name@example.invalid", null, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun prepareCapture() { AttestraAuthTheme { OnboardingLoadingScreen(LoadingTask.PREPARE_CAPTURE, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun uploadDocument() { AttestraAuthTheme { OnboardingLoadingScreen(LoadingTask.UPLOAD_DOCUMENT, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun readDocument() { AttestraAuthTheme { OnboardingLoadingScreen(LoadingTask.READ_DOCUMENT, {}) } }

    private fun reviewUi(failed: Boolean): CaptureUi {
        val extraction = if (failed) CardExtraction("failed") else CardExtraction("complete", CardData(fullName = "Sample Person", documentNumber = "DEMO-001"))
        val record = CaptureRecord("a".repeat(32), 1, 1, "sample-policy", "id_card", "ready", 9999999999, extraction = extraction)
        return CaptureUi(record = record, draft = extraction.data, draftRevision = 1, originCountry = if (failed) "" else "MX")
    }
    @PreviewTest @DocumentationPreview @Composable
    fun extractedReview() { AttestraAuthTheme { CaptureHostContent(ui = reviewUi(false), onExit = {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun manualReview() { AttestraAuthTheme { CaptureHostContent(ui = reviewUi(true), onExit = {}) } }
}
