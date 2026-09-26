package com.apexfission.android.attestra.auth.email

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

data class VerificationLink(val requestId: String, val tokenB: String) {
    fun isValid(): Boolean = requestId.matches(TOKEN) && tokenB.matches(TOKEN)
    private companion object { val TOKEN = Regex("[A-Za-z0-9_-]{43}") }
}

sealed interface EmailScreen {
    data class Start(val error: String? = null) : EmailScreen
    data class Wait(val email: String, val requestId: String, val help: String? = null) : EmailScreen
    data class Code(val email: String, val link: VerificationLink, val error: String? = null, val attemptsRemaining: Int? = null) : EmailScreen
    data class Limit(val email: String, val requestId: String) : EmailScreen
    data class Unusable(val email: String, val requestId: String?) : EmailScreen
    data object Recovery : EmailScreen
    data object Verified : EmailScreen
    data class Loading(val task: LoadingStep) : EmailScreen
}

enum class LoadingStep { REQUEST_EMAIL, CONFIRM_EMAIL, RESEND_EMAIL }

/** No navigation change claims success until the backend returns tokens and they are stored. */
class EmailFlowController(
    private val api: EmailVerificationGateway,
    private val storage: AuthStorage,
    private val proofs: ProofGenerator = ProofGenerator(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    var screen: EmailScreen by mutableStateOf(
        storage.pending()?.takeIf { now() - it.createdAtMillis < 60 * 60_000L }
            ?.let { EmailScreen.Wait(it.email, it.requestId) } ?: EmailScreen.Start(),
    )
        private set

    private val requestLock = Mutex()
    var lastEmailAtMillis: Long = storage.pending()?.createdAtMillis ?: 0L
        private set

    fun canResend(): Boolean = now() - lastEmailAtMillis >= 60_000L

    suspend fun start(email: String) = exclusive {
        Log.d("EmailVerification", "Requesting verification email")
        screen = EmailScreen.Loading(LoadingStep.REQUEST_EMAIL)
        try {
            val proof = proofs.create()
            val requestId = api.signup(email, proof.challenge)
            require(VerificationLink(requestId, "A".repeat(43)).isValid())
            val pending = PendingEmail(email, requestId, proof.tokenA, now())
            storage.savePending(pending)
            lastEmailAtMillis = now()
            screen = EmailScreen.Wait(email, requestId)
            Log.d("EmailVerification", "Verification email requested; waiting for link")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("EmailVerification", "Email request failed", error)
            screen = EmailScreen.Start("We couldn't request an email. Check your connection and try again.")
        }
    }

    suspend fun openLink(link: VerificationLink) = exclusive {
        val pending = storage.pending()?.takeIf { it.requestId == link.requestId && now() - it.createdAtMillis < 60 * 60_000L }
        val email = pending?.email.orEmpty()
        Log.d("EmailVerification", "Opened verification link request_id=${link.requestId} local_proof=${pending != null} valid_format=${link.isValid()}")
        if (!link.isValid()) {
            Log.w("EmailVerification", "Verification link has invalid proof format")
            screen = EmailScreen.Unusable(email, link.requestId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{43}")) })
            return@exclusive
        }
        if (pending == null) {
            Log.d("EmailVerification", "Verification link opened without matching local proof; requesting manual code")
            screen = EmailScreen.Code(email, link)
            return@exclusive
        }
        Log.d("EmailVerification", "Verification link matched local proof; confirming automatically")
        screen = EmailScreen.Loading(LoadingStep.CONFIRM_EMAIL)
        try {
            complete(link.requestId, api.confirmLocal(link.requestId, link.tokenB, pending.tokenA))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: EmailApiError) {
            Log.e("EmailVerification", "Automatic confirmation rejected request_id=${link.requestId} kind=${error.kind}", error)
            screen = when (error.kind) {
                EmailApiError.Kind.SIGN_IN_REQUIRED -> EmailScreen.Recovery
                EmailApiError.Kind.LINK_UNUSABLE -> EmailScreen.Code(email, link, "Automatic verification didn't work. Enter the code from your email.")
                else -> EmailScreen.Code(email, link, "We couldn't verify automatically. Enter the code from your email or try again.")
            }
        } catch (error: Exception) {
            Log.e("EmailVerification", "Automatic confirmation failed request_id=${link.requestId}", error)
            screen = EmailScreen.Code(email, link, "We couldn't verify automatically. Enter the code from your email or try again.")
        }
    }

    suspend fun confirmCode(code: String) = exclusive {
        val current = screen as? EmailScreen.Code ?: return@exclusive
        if (!code.matches(Regex("[0-9]{6}"))) {
            Log.d("EmailVerification", "Manual code rejected by local format validation")
            screen = current.copy(error = "Enter the six-digit code in your email.")
            return@exclusive
        }
        Log.d("EmailVerification", "Submitting manual confirmation code")
        screen = EmailScreen.Loading(LoadingStep.CONFIRM_EMAIL)
        try {
            complete(current.link.requestId, api.confirmCode(current.link.requestId, current.link.tokenB, code))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: EmailApiError) {
            Log.e("EmailVerification", "Manual confirmation rejected request_id=${current.link.requestId} kind=${error.kind}", error)
            screen = when (error.kind) {
                EmailApiError.Kind.INCORRECT_CODE -> current.copy(error = "That code didn't match. Try the latest email.", attemptsRemaining = error.attemptsRemaining)
                EmailApiError.Kind.ATTEMPT_LIMIT -> EmailScreen.Limit(current.email, current.link.requestId)
                EmailApiError.Kind.LINK_UNUSABLE -> EmailScreen.Unusable(current.email, current.link.requestId)
                EmailApiError.Kind.SIGN_IN_REQUIRED -> EmailScreen.Recovery
                else -> current.copy(error = "We couldn't verify your email. Please try again.")
            }
        } catch (error: Exception) {
            Log.e("EmailVerification", "Manual confirmation failed", error)
            screen = current.copy(error = "Check your connection and try again.")
        }
    }

    suspend fun resend() = exclusive {
        val previous = screen
        val (email, id) = when (previous) {
            is EmailScreen.Wait -> previous.email to previous.requestId
            is EmailScreen.Code -> previous.email to previous.link.requestId
            is EmailScreen.Limit -> previous.email to previous.requestId
            is EmailScreen.Unusable -> previous.email to previous.requestId
            else -> return@exclusive
        }
        if (id == null || !canResend()) {
            Log.d("EmailVerification", "Resend skipped: request missing or cooldown active")
            return@exclusive
        }
        Log.d("EmailVerification", "Requesting another verification email")
        screen = EmailScreen.Loading(LoadingStep.RESEND_EMAIL)
        try {
            api.resend(id)
            lastEmailAtMillis = now()
            screen = EmailScreen.Wait(email, id, "If a new email arrives, open its latest link.")
            Log.d("EmailVerification", "Verification email resent")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("EmailVerification", "Resend failed", error)
            screen = EmailScreen.Wait(email, id, "We couldn't request another email. Please try again.")
        }
    }

    fun changeEmail() {
        Log.d("EmailVerification", "Clearing pending email and returning to start")
        storage.clearPending()
        screen = EmailScreen.Start()
    }

    private fun complete(requestId: String, session: AuthSession) {
        try {
            Log.d("EmailVerification", "Confirm returned session request_id=$requestId; saving encrypted session")
            storage.saveSession(session)
            Log.d("EmailVerification", "Encrypted session saved request_id=$requestId")
            if (storage.pending()?.requestId == requestId) storage.clearPending()
            screen = EmailScreen.Verified
            Log.d("EmailVerification", "Email verified; session stored")
        } catch (error: Exception) {
            Log.e("EmailVerification", "Email confirmed but session handling failed request_id=$requestId", error)
            // The server already consumed the proof. Retrying it cannot recover tokens.
            screen = EmailScreen.Recovery
        }
    }

    private suspend inline fun exclusive(crossinline action: suspend () -> Unit) {
        if (!requestLock.tryLock()) {
            Log.d("EmailVerification", "Ignoring action while another email request is running")
            return
        }
        try { action() } finally { requestLock.unlock() }
    }
}
