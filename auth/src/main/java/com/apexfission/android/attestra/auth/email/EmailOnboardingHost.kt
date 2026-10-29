package com.apexfission.android.attestra.auth.email

import androidx.activity.compose.BackHandler
import com.apexfission.android.attestra.auth.capture.CaptureApi
import com.apexfission.android.attestra.auth.capture.CapturePolicy
import com.apexfission.android.attestra.auth.discovery.*
import com.apexfission.android.attestra.auth.passkey.AccountStatus
import com.apexfission.android.attestra.auth.passkey.SignInError
import com.apexfission.android.attestra.auth.ui.onboarding.password.PasswordField
import com.apexfission.android.attestra.auth.ui.onboarding.password.PasswordScreen
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import kotlinx.coroutines.CancellationException
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

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

/**
 * Screen that hosts the complete email onboarding and sign-in flow.
 * Orchestrates password, passkey, and ID capture steps based on the provided onboarding policy.
 */
@Composable
fun EmailOnboardingHost(
    apiBaseUrl: String,
    link: VerificationLink?,
    onLinkConsumed: () -> Unit,
    onPasskeyDeferred: () -> Unit,
    onboarding: OnboardingPolicy? = null,
    onRetryConfiguration: () -> Unit = {},
    identityContent: @Composable (Boolean, CapturePolicy?, () -> Unit) -> Unit = { _, _, exit ->
        com.apexfission.android.attestra.auth.identity.IdentityIntegrationUnavailable(exit)
    },
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
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val passkeyApi = remember(apiBaseUrl, client) { PasskeyRegistrationApi(apiBaseUrl, client) }
    val passkeyManager = remember(activityContext) { AndroidPasskeyManager(activityContext) }
    val returnApi = remember(apiBaseUrl, client) { ReturnAuthApi(apiBaseUrl, client) }
    var passkeyScreen by remember(apiBaseUrl) { mutableStateOf("refresh") }
    var capturePolicy by remember(apiBaseUrl) { mutableStateOf<CapturePolicy?>(null) }
    var acceptedCapturePolicy by remember(apiBaseUrl) { mutableStateOf<CapturePolicy?>(null) }
    var capturePolicyLoading by remember(apiBaseUrl) { mutableStateOf(false) }
    var capturePolicyError by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var capturePolicyRefresh by remember(apiBaseUrl) { mutableLongStateOf(0L) }
    LaunchedEffect(passkeyScreen, apiBaseUrl, capturePolicyRefresh) {
        if (passkeyScreen != "identity") return@LaunchedEffect
        capturePolicy = null
        acceptedCapturePolicy = null
        capturePolicyLoading = true
        capturePolicyError = null
        try {
            // Only policy GET uses this instance; image uploads use LiveCaptureFlow's dedicated transport.
            capturePolicy = CaptureApi(apiBaseUrl, client, client) { storage.session()?.accessToken.orEmpty() }.policy()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            capturePolicyError = "We could not load the capture information. Try again or return to sign in."
        } finally { capturePolicyLoading = false }
    }
    var status by remember(apiBaseUrl) { mutableStateOf<AccountStatus?>(null) }
    var skipped by remember(apiBaseUrl) { mutableStateOf(emptySet<OnboardingStep>()) }
    // Intentionally not rememberSaveable: never serialize credentials into saved state.
    var password by remember(apiBaseUrl) { mutableStateOf("") }
    var passwordEmail by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var passwordRequestId by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var passwordError by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var returnMode by remember(apiBaseUrl) { mutableStateOf("") }
    var returnEmail by remember(apiBaseUrl) { mutableStateOf(storage.email() ?: storage.pending()?.email ?: emailFromSession(storage.session()).orEmpty()) }
    var returnError by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var signInChallenge by remember(apiBaseUrl) { mutableStateOf<SignInChallenge?>(null) }
    var authenticated by remember(apiBaseUrl) { mutableStateOf(false) }
    var restoring by remember(apiBaseUrl) { mutableStateOf(storage.session() != null) }
    val currentPolicy by rememberUpdatedState(onboarding)
    fun clearPassword() { password = ""; passwordEmail = null; passwordRequestId = null }
    val exit = { clearPassword(); onPasskeyDeferred() }
    LaunchedEffect(controller, link) {
        if (link != null) {
            if (link.requestId != passwordRequestId) { clearPassword(); skipped = emptySet() }
            status = null
            authenticated = false
            returnMode = ""
            onLinkConsumed()
            scope.launch { controller.openLink(link) }
        }
    }
    BackHandler { exit() }
    fun route() {
        passkeyScreen = when (currentPolicy?.next(status ?: AccountStatus(), skipped)) {
            OnboardingStep.PASSWORD -> "password"
            OnboardingStep.PASSKEY -> "start"
            OnboardingStep.ID_CAPTURE -> "identity"
            OnboardingStep.COMPLETE -> "welcome"
            else -> "refresh"
        }
    }
    val refreshStatus: suspend () -> Unit = {
        passkeyScreen = "refreshing"
        try {
            val session = requireNotNull(storage.session())
            status = try { returnApi.status(session.accessToken) }
            catch (expired: SignInError) {
                if (!expired.expired || session.refreshToken.isBlank()) throw expired
                val refreshed = returnApi.refresh(session.refreshToken).let {
                    if (it.refreshToken.isBlank()) it.copy(refreshToken = session.refreshToken) else it
                }
                storage.saveSession(refreshed)
                returnApi.status(refreshed.accessToken)
            }
            route()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: SignInError) {
            if (error.expired) {
                clearPassword()
                status = null
                storage.clearSession()
                authenticated = false
                returnError = "Sign in to continue setup."
                returnMode = "choice"
            }
            passkeyScreen = "refresh"
        }
        catch (_: Exception) { passkeyScreen = "refresh" }
    }
    val skip: (OnboardingStep) -> Unit = { step ->
        val requirement = when (step) {
            OnboardingStep.PASSWORD -> onboarding?.password
            OnboardingStep.PASSKEY -> onboarding?.passkey
            OnboardingStep.ID_CAPTURE -> onboarding?.idCapture
            else -> null
        }
        if (requirement == Requirement.OPTIONAL) {
            skipped = skipped + step
            if (step == OnboardingStep.PASSWORD) clearPassword()
            scope.launch { refreshStatus() }
        }
    }
    LaunchedEffect(onboarding) {
        if (onboarding == null || onboarding.password == Requirement.DISABLED) clearPassword()
        if (status != null) refreshStatus()
    }
    val saveSignedIn: suspend (AuthSession) -> Unit = { result ->
        clearPassword()
        skipped = emptySet()
        storage.saveSession(result)
        storage.saveEmail(returnEmail)
        authenticated = true
        returnMode = ""
        refreshStatus()
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
                    authenticated = true
                    refreshStatus()
                    val policy = currentPolicy
                    val matches = pendingPasswordMatches(passwordEmail, passwordRequestId,
                        emailFromSession(fresh), controller.verifiedRequestId)
                    if (!matches || status?.passwordRegistered == true || policy?.passwordPlacement != PasswordPlacement.WITH_EMAIL) clearPassword()
                    if (matches && password.isNotEmpty() && policy?.passwordPlacement == PasswordPlacement.WITH_EMAIL &&
                        policy.password != Requirement.DISABLED && status != null && status?.passwordRegistered == false &&
                        policy.passwordPolicy.problems(password).isEmpty()) {
                        val entered = password
                        clearPassword()
                        passkeyScreen = "saving"
                        try {
                            check(returnApi.setPassword(requireNotNull(storage.session()).accessToken, entered).passwordRegistered)
                            refreshStatus()
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) {
                            passwordError = "Could not save your password. Enter it again to retry."
                            passkeyScreen = "password"
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                Log.e("EmailDebugX", "Could not restore session", error)
                clearPassword()
                storage.clearSession()
                returnMode = "choice"
                returnError = "Sign in to continue."
            } finally { restoring = false }
        }
    }
    val startSignIn: (String, Boolean) -> Unit = { address, usePasskey ->
        clearPassword()
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
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
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
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    Log.e("EmailDebugX", "Email sign-in failed", error)
                    returnError = "That code did not work. Try again or request a new one."
                    returnMode = "code"
                }
            }
        }
    }
    val createPasskey: () -> Unit = {
        if (currentPolicy != null && currentPolicy?.passkey != Requirement.DISABLED && passkeyScreen != "opening" && passkeyScreen != "saving") {
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
                        refreshStatus()
                                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: PasskeyCancelled) {
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
    val changeEmail = { clearPassword(); skipped = emptySet(); passwordError = null; controller.changeEmail() }
    val passwordSignIn: () -> Unit = {
        val entered = password
        clearPassword()
        returnMode = "loading"
        scope.launch {
            try {
                val challenge = returnApi.startPassword(returnEmail)
                saveSignedIn(returnApi.finishPassword(returnEmail, challenge.session, entered))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { returnError = "Password sign-in did not finish. Try again or use an email code."; returnMode = "password" }
        }
    }
    val savePassword: () -> Unit = {
        if (onboarding != null && onboarding.password != Requirement.DISABLED && onboarding.passwordPolicy.problems(password).isEmpty()) {
            val entered = password
            clearPassword()
            passwordError = null
            passkeyScreen = "saving"
            scope.launch {
                try {
                    check(returnApi.setPassword(requireNotNull(storage.session()).accessToken, entered).passwordRegistered)
                    refreshStatus()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { passwordError = "Could not save your password. Check the requirements and try again."; passkeyScreen = "password" }
            }
        }
    }
    if (returnMode == "choice") {
        ReturnSignInScreen(returnEmail, returnError, { startSignIn(it, true) }, { startSignIn(it, false) }, { clearPassword(); returnMode = "" },
            onPassword = { returnEmail = it; clearPassword(); returnError = null; returnMode = "password" })
    } else if (returnMode == "password") {
        PasswordScreen(password, { password = it }, passwordSignIn, { clearPassword(); returnMode = "choice" }, returnError, signIn = true)
    } else if (returnMode == "code") {
        ReturnEmailCodeScreen(returnEmail, returnError, finishEmail, { returnMode = "choice" })
    } else if (returnMode == "loading" || (screen == EmailScreen.Verified && restoring)) {
        OnboardingLoadingScreen(LoadingTask.SIGN_IN, onBack = {})
    } else if (onboarding == null) {
        OnboardingFrame("App settings unavailable", Icons.Default.Lock, "Setup", exit,
            primaryLabel = "Retry settings", onPrimary = onRetryConfiguration,
            secondaryLabel = "Sign in to an existing account", onSecondary = { clearPassword(); returnMode = "choice" }) {
            BodyText("Connect to load current setup requirements before continuing.")
        }
    } else when (screen) {
        is EmailScreen.Start -> EmailStartScreen(exit, { address ->
            passwordEmail = address
            skipped = if (onboarding.passwordPlacement == PasswordPlacement.WITH_EMAIL && onboarding.password == Requirement.OPTIONAL && password.isEmpty()) setOf(OnboardingStep.PASSWORD) else emptySet()
            scope.launch {
                controller.start(address)
                passwordRequestId = (controller.screen as? EmailScreen.Wait)?.requestId
                if (passwordRequestId == null) clearPassword()
            }
        }, screen.error,
            onExistingAccount = { clearPassword(); returnMode = "choice" },
            onEmailChanged = { clearPassword() },
            passwordValid = onboarding.passwordPlacement != PasswordPlacement.WITH_EMAIL ||
                (password.isEmpty() && onboarding.password == Requirement.OPTIONAL) || onboarding.passwordPolicy.problems(password).isEmpty(),
            passwordContent = if (onboarding.passwordPlacement == PasswordPlacement.WITH_EMAIL) {
                { PasswordField(password, { password = it }, onboarding.passwordPolicy, onboarding.password == Requirement.OPTIONAL) }
            } else null)
        is EmailScreen.Wait -> EmailWaitScreen(
            screen.email, changeEmail, resend, changeEmail,
            canResend = canResend, resendHelp = screen.help ?: if (canResend) null else "You can request another email in a minute.",
            onSignIn = { clearPassword(); returnEmail = screen.email; returnMode = "choice" },
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
            "refreshing", "opening", "saving" -> OnboardingLoadingScreen(LoadingTask.SIGN_IN, onBack = {})
            "password" -> PasswordScreen(password, { password = it }, savePassword, exit, passwordError,
                onboarding.passwordPolicy, onboarding.password == Requirement.OPTIONAL, { skip(OnboardingStep.PASSWORD) })
            "failed", "cancelled" -> PasskeyFailedScreen(exit, createPasskey, { skip(OnboardingStep.PASSKEY) },
                passkeyScreen == "cancelled", required = onboarding.passkey == Requirement.REQUIRED)
            "unsupported" -> PasskeyUnsupportedScreen(exit, { skip(OnboardingStep.PASSKEY) }, createPasskey,
                required = onboarding.passkey == Requirement.REQUIRED)
            "recovery" -> EmailSessionRecoveryScreen(exit, { clearPassword(); returnMode = "choice" })
            "identity" -> IdentityStartScreen(exit, {
                capturePolicy?.let { acceptedCapturePolicy = it; passkeyScreen = "identity_flow" }
            }, { skip(OnboardingStep.ID_CAPTURE) },
                passkeyAdded = status?.passkeyRegistered == true, required = onboarding.idCapture == Requirement.REQUIRED,
                capturePolicy = capturePolicy, policyLoading = capturePolicyLoading, policyError = capturePolicyError,
                requireDisclosure = true, onRetryPolicy = { capturePolicyRefresh++ })
            "identity_flow" -> identityContent(status?.passkeyRegistered == true, acceptedCapturePolicy) {
                // Both successful capture and cancellation only trigger a server read.
                if (storage.session() == null) {
                    clearPassword()
                    status = null
                    authenticated = false
                    returnMode = "choice"
                    returnError = "Sign in to resume your capture."
                } else scope.launch { refreshStatus() }
            }
            "welcome" -> OnboardingFrame("Welcome to Attestra", Icons.Default.Lock, "Your account", exit,
                primaryLabel = if (status?.idCaptured == true) "Review ID details" else "Refresh account status",
                onPrimary = { if (status?.idCaptured == true) { acceptedCapturePolicy = null; passkeyScreen = "identity_flow" } else scope.launch { refreshStatus() } }) {
                BodyText("Your account setup is complete.")
                if (status?.idCaptured == true) BodyText("Document captured. This does not assert identity verification.")
            }
            "start" -> PasskeyStartScreen(exit, createPasskey, { skip(OnboardingStep.PASSKEY) }, required = onboarding.passkey == Requirement.REQUIRED)
            else -> OnboardingFrame("Check account setup", Icons.Default.Lock, "Account", exit,
                primaryLabel = "Retry account status", onPrimary = { scope.launch { refreshStatus() } },
                secondaryLabel = "Reload settings", onSecondary = onRetryConfiguration) {
                BodyText("We could not confirm that setup is complete. Retry to get the latest server status.")
            }
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

@Preview
@Composable
private fun EmailOnboardingHostPreview() {
    MaterialTheme {
        // Complex state makes this hard to fully mock in a simple preview without a mock environment
    }
}
