package com.apexfission.android.attestra.auth.email

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Base64
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.apexfission.android.attestra.auth.passkey.ReturnAuthApi
import com.apexfission.android.attestra.auth.passkey.SignInChallenge
import com.apexfission.android.attestra.auth.ui.onboarding.signin.ReturnSignInScreen
import com.apexfission.android.attestra.auth.ui.onboarding.signin.ReturnEmailCodeScreen
import androidx.compose.runtime.mutableStateOf
import com.apexfission.android.attestra.auth.passkey.AndroidPasskeyManager
import com.apexfission.android.attestra.auth.passkey.PasskeyRegistrationApi
import com.apexfission.android.attestra.auth.passkey.PasskeyApiException
import com.apexfission.android.attestra.auth.passkey.PasskeyCancelled
import com.apexfission.android.attestra.auth.passkey.PasskeyUnsupported
import com.apexfission.android.attestra.auth.ui.onboarding.passkey.PasskeyFailedScreen
import com.apexfission.android.attestra.auth.ui.onboarding.passkey.PasskeyUnsupportedScreen
import com.apexfission.android.attestra.auth.ui.onboarding.id.IdentityStartScreen
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

private fun emailFromSession(session: AuthSession?): String? = runCatching {
    val payload = session!!.idToken.split('.')[1]
    val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    EmailHttpClient.json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject["email"]?.jsonPrimitive?.content
}.getOrNull()

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
    onPasskeyDeferred: () -> Unit,
) {
    val activityContext: Context = LocalContext.current
    val context: Context = activityContext.applicationContext
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
    val passkeyApi = remember(apiBaseUrl, client) { PasskeyRegistrationApi(apiBaseUrl, client) }
    val passkeyManager = remember(activityContext) { AndroidPasskeyManager(activityContext) }
    val returnApi = remember(apiBaseUrl, client) { ReturnAuthApi(apiBaseUrl, client) }
    var passkeyScreen by remember { mutableStateOf("start") }
    var passkeyAdded by remember { mutableStateOf(false) }
    var returnMode by remember { mutableStateOf("") }
    var returnEmail by remember { mutableStateOf(storage.email() ?: storage.pending()?.email ?: emailFromSession(storage.session()).orEmpty()) }
    var returnError by remember { mutableStateOf<String?>(null) }
    var signInChallenge by remember { mutableStateOf<SignInChallenge?>(null) }
    var authenticated by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(storage.session() != null) }
    val saveSignedIn: suspend (AuthSession) -> Unit = { result ->
        storage.saveSession(result)
        storage.saveEmail(returnEmail)
        val status = returnApi.status(result.accessToken)
        authenticated = true
        returnMode = ""
        passkeyScreen = if (status.passkeyRegistered) "identity" else "start"
        passkeyAdded = status.passkeyRegistered
        Log.d("EmailDebugX", "Sign-in completed passkey_registered=${status.passkeyRegistered}")
    }
    LaunchedEffect(controller.screen) {
        if (controller.screen == EmailScreen.Verified && !authenticated) {
            restoring = true
            val stored = storage.session()
            try {
                if (stored != null) {
                    val fresh = if (stored.refreshToken.isNotBlank()) returnApi.refresh(stored.refreshToken)
                        .let { if (it.refreshToken.isBlank()) it.copy(refreshToken = stored.refreshToken) else it } else stored
                    storage.saveSession(fresh)
                    val status = returnApi.status(fresh.accessToken)
                    passkeyAdded = status.passkeyRegistered
                    passkeyScreen = if (status.passkeyRegistered) "identity" else "start"
                    authenticated = true
                    Log.d("EmailDebugX", "Session restored passkey_registered=${status.passkeyRegistered}")
                }
            } catch (error: Exception) {
                Log.e("EmailDebugX", "Could not restore session", error)
                storage.clearSession()
                returnMode = "choice"
                returnError = "Sign in to continue."
            } finally { restoring = false }
        }
    }
    val startSignIn: (String, Boolean) -> Unit = { address, usePasskey ->
        returnEmail = address
        returnError = null
        returnMode = "loading"
        scope.launch {
            try {
                val challenge = returnApi.start(address, usePasskey)
                signInChallenge = challenge
                if (usePasskey) {
                    val credential = passkeyManager.signIn(challenge.options)
                    saveSignedIn(returnApi.finishPasskey(address, challenge.session, credential))
                } else returnMode = "code"
            } catch (error: Exception) {
                Log.e("EmailDebugX", "Sign-in start failed passkey=$usePasskey", error)
                returnError = if (usePasskey) "Passkey sign-in did not finish. Try again or use email." else "Could not send a sign-in code. Try again."
                returnMode = "choice"
            }
        }
    }
    val finishEmail: (String) -> Unit = { code ->
        val challenge = signInChallenge
        if (challenge != null) {
            returnMode = "loading"
            scope.launch {
                try { saveSignedIn(returnApi.finishEmail(returnEmail, challenge.session, code)) }
                catch (error: Exception) {
                    Log.e("EmailDebugX", "Email sign-in failed", error)
                    returnError = "That code did not work. Try again or request a new one."
                    returnMode = "code"
                }
            }
        }
    }
    val createPasskey: () -> Unit = {
        if (passkeyScreen != "opening" && passkeyScreen != "saving") {
            scope.launch {
                val token = storage.session()?.accessToken
                if (token.isNullOrBlank()) {
                    Log.e("EmailDebugX", "Passkey setup requires email sign-in")
                    passkeyScreen = "recovery"
                } else {
                    passkeyScreen = "opening"
                    try {
                        val options = passkeyApi.options(token)
                        val credential = passkeyManager.create(options)
                        passkeyScreen = "saving"
                        passkeyApi.complete(token, credential)
                        passkeyAdded = true
                        passkeyScreen = "identity"
                    } catch (error: PasskeyCancelled) {
                        passkeyScreen = "cancelled"
                    } catch (error: PasskeyUnsupported) {
                        passkeyScreen = "unsupported"
                    } catch (error: PasskeyApiException) {
                        passkeyScreen = if (error.signInRequired) "recovery" else "failed"
                    } catch (error: Exception) {
                        Log.e("EmailDebugX", "Passkey registration failed", error)
                        passkeyScreen = "failed"
                    }
                }
            }
        }
    }
    val screen = if (authenticated) EmailScreen.Verified else controller.screen
    LaunchedEffect(screen) {
        while (screen is EmailScreen.Wait || screen is EmailScreen.Code || screen is EmailScreen.Limit || screen is EmailScreen.Unusable) {
            clock = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val canResend = clock >= controller.lastEmailAtMillis + 60_000
    val resend = { scope.launch { controller.resend() }; Unit }
    val changeEmail = { controller.changeEmail() }
    if (returnMode == "choice") {
        ReturnSignInScreen(returnEmail, returnError, { startSignIn(it, true) }, { startSignIn(it, false) }, { returnMode = "" })
    } else if (returnMode == "code") {
        ReturnEmailCodeScreen(returnEmail, returnError, finishEmail, { returnMode = "choice" })
    } else if (returnMode == "loading" || (screen == EmailScreen.Verified && restoring)) {
        OnboardingLoadingScreen(LoadingTask.SIGN_IN, onBack = {})
    } else when (screen) {
        is EmailScreen.Start -> EmailStartScreen(changeEmail, { scope.launch { controller.start(it) } }, screen.error, onExistingAccount = { returnMode = "choice" })
        is EmailScreen.Wait -> EmailWaitScreen(
            screen.email, changeEmail, resend, changeEmail,
            canResend = canResend, resendHelp = screen.help ?: if (canResend) null else "You can request another email in a minute.",
            onSignIn = { returnEmail = screen.email; returnMode = "choice" },
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
        EmailScreen.Recovery -> EmailSessionRecoveryScreen(changeEmail, { returnMode = "choice" })
        EmailScreen.Verified -> when (passkeyScreen) {
            "opening" -> OnboardingLoadingScreen(LoadingTask.OPEN_PASSKEY_MANAGER, onBack = {})
            "saving" -> OnboardingLoadingScreen(LoadingTask.SAVE_PASSKEY, onBack = {})
            "failed" -> PasskeyFailedScreen({ passkeyScreen = "start" }, createPasskey, { passkeyScreen = "identity" }, false)
            "cancelled" -> PasskeyFailedScreen({ passkeyScreen = "start" }, createPasskey, { passkeyScreen = "identity" }, true)
            "unsupported" -> PasskeyUnsupportedScreen({ passkeyScreen = "start" }, { passkeyScreen = "identity" }, createPasskey)
            "recovery" -> EmailSessionRecoveryScreen({ passkeyScreen = "start" }, { returnMode = "choice" })
            "identity" -> IdentityStartScreen({ passkeyScreen = "start" }, onPasskeyDeferred, onPasskeyDeferred, passkeyAdded)
            else -> PasskeyStartScreen(onPasskeyDeferred, createPasskey, { passkeyScreen = "identity" })
        }
        is EmailScreen.Loading -> OnboardingLoadingScreen(
            when (screen.task) {
                LoadingStep.REQUEST_EMAIL -> LoadingTask.REQUEST_EMAIL
                LoadingStep.CONFIRM_EMAIL -> LoadingTask.CONFIRM_EMAIL
                LoadingStep.RESEND_EMAIL -> LoadingTask.RESEND_EMAIL
            }, onBack = {},
        )
    }
}
