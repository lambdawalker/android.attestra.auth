package com.apexfission.android.attestra.auth.email

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.apexfission.android.attestra.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.attestra.auth.ui.onboarding.email.EmailAttemptLimitScreen
import com.apexfission.android.attestra.auth.ui.onboarding.email.EmailCodeScreen
import com.apexfission.android.attestra.auth.ui.onboarding.email.EmailLinkUnusableScreen
import com.apexfission.android.attestra.auth.ui.onboarding.email.EmailSessionRecoveryScreen
import com.apexfission.android.attestra.auth.ui.onboarding.email.EmailStartScreen
import com.apexfission.android.attestra.auth.ui.onboarding.email.EmailWaitScreen
import com.apexfission.android.attestra.auth.ui.onboarding.passkey.PasskeyStartScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Rejects URLs from other origins even if another app explicitly sends an intent to this activity. */
fun parseVerificationLink(uri: Uri?, expectedHost: String): VerificationLink? {
    if (uri?.scheme != "https" || uri.host != expectedHost || uri.path != "/verify-email" || (uri.port != -1 && uri.port != 443)) return null
    return VerificationLink(uri.getQueryParameter("request_id").orEmpty(), uri.getQueryParameter("b").orEmpty())
}

@Composable
fun EmailOnboardingHost(
    apiBaseUrl: String,
    link: VerificationLink?,
    onLinkConsumed: () -> Unit,
    onSignInRequired: () -> Unit,
    onPasskeyRequested: () -> Unit,
    onPasskeyDeferred: () -> Unit,
) {
    val context: Context = LocalContext.current.applicationContext
    val storage = remember(context) { SecureAuthStorage(context) }
    val client = remember(apiBaseUrl) { EmailHttpClient.create() }
    DisposableEffect(client) { onDispose { client.close() } }
    val controller = remember(apiBaseUrl, storage, client) {
        EmailFlowController(EmailVerificationApi(apiBaseUrl, client), storage)
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller, link) {
        if (link != null) {
            onLinkConsumed()
            // Clearing the activity intent changes the effect key. Keep the request
            // in the composable scope so that recomposition does not cancel it.
            scope.launch { controller.openLink(link) }
        }
    }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val screen = controller.screen
    LaunchedEffect(screen) {
        while (screen is EmailScreen.Wait || screen is EmailScreen.Code || screen is EmailScreen.Limit || screen is EmailScreen.Unusable) {
            clock = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val canResend = clock >= controller.lastEmailAtMillis + 60_000
    val resend = { scope.launch { controller.resend() }; Unit }
    val changeEmail = { controller.changeEmail() }
    when (screen) {
        is EmailScreen.Start -> EmailStartScreen(changeEmail, { scope.launch { controller.start(it) } }, screen.error)
        is EmailScreen.Wait -> EmailWaitScreen(
            screen.email, changeEmail, resend, changeEmail,
            canResend = canResend, resendHelp = screen.help ?: if (canResend) null else "You can request another email in a minute.",
        )
        is EmailScreen.Code -> EmailCodeScreen(
            screen.email, changeEmail, { scope.launch { controller.confirmCode(it) } }, resend,
            error = screen.error, attemptsRemaining = screen.attemptsRemaining, canResend = canResend,
        )
        is EmailScreen.Limit -> EmailAttemptLimitScreen(
            screen.email, changeEmail, resend, changeEmail, canRequestEmail = canResend,
            retryAfter = if (canResend) null else "Wait a minute before requesting another email.",
        )
        is EmailScreen.Unusable -> EmailLinkUnusableScreen(
            screen.email, changeEmail, resend, changeEmail, canRequestEmail = screen.requestId != null && canResend,
        )
        EmailScreen.Recovery -> EmailSessionRecoveryScreen(changeEmail, onSignInRequired)
        EmailScreen.Verified -> PasskeyStartScreen(onPasskeyDeferred, onPasskeyRequested, onPasskeyDeferred)
        is EmailScreen.Loading -> OnboardingLoadingScreen(
            when (screen.task) {
                LoadingStep.REQUEST_EMAIL -> LoadingTask.REQUEST_EMAIL
                LoadingStep.CONFIRM_EMAIL -> LoadingTask.CONFIRM_EMAIL
                LoadingStep.RESEND_EMAIL -> LoadingTask.RESEND_EMAIL
            }, onBack = {},
        )
    }
}
