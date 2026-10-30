package com.apexfission.android.attestra.auth

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Preview(name = "Light", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "Dark", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "en")
annotation class AppDocumentationPreview

class AppScreenshotTest {
    @PreviewTest @AppDocumentationPreview @Composable
    fun menu() { AttestraAuthTheme { OnboardingEntryScreen(true, false, null, {}, {}, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun resetting() { AttestraAuthTheme { OnboardingEntryScreen(false, true, "Clearing local data", {}, {}, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun configurationLoading() { AttestraAuthTheme { EnvironmentStatusScreen(true, null, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun configurationFailure() { AttestraAuthTheme { EnvironmentStatusScreen(false, "Could not load environment settings. Retry when connected.", {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun signInRequired() { AttestraAuthTheme { CaptureSignInRequired({}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun mockOptions() { AttestraAuthTheme { MockCaptureContent(false, com.apexfission.android.attestra.identitymock.MockScenario.READY, {}, {}, {}, {}, {}) } }
}
