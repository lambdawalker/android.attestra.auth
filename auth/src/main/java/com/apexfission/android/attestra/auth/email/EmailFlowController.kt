package com.apexfission.android.attestra.auth.email

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
        screen = EmailScreen.Loading(LoadingStep.REQUEST_EMAIL)
        try {
            val proof = proofs.create()
            val requestId = api.signup(email, proof.challenge)
            require(VerificationLink(requestId, "A".repeat(43)).isValid())
            val pending = PendingEmail(email, requestId, proof.tokenA, now())
            storage.savePending(pending)
            lastEmailAtMillis = now()
            screen = EmailScreen.Wait(email, requestId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            screen = EmailScreen.Start("We couldn't request an email. Check your connection and try again.")
        }
    }

    suspend fun openLink(link: VerificationLink) = exclusive {
        val pending = storage.pending()?.takeIf { it.requestId == link.requestId && now() - it.createdAtMillis < 60 * 60_000L }
        val email = pending?.email.orEmpty()
        if (!link.isValid()) {
            screen = EmailScreen.Unusable(email, link.requestId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{43}")) })
            return@exclusive
        }
        if (pending == null) {
            screen = EmailScreen.Code(email, link)
            return@exclusive
        }
        screen = EmailScreen.Loading(LoadingStep.CONFIRM_EMAIL)
        try {
            complete(link.requestId, api.confirmLocal(link.requestId, link.tokenB, pending.tokenA))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: EmailApiError) {
            screen = when (error.kind) {
                EmailApiError.Kind.SIGN_IN_REQUIRED -> EmailScreen.Recovery
                EmailApiError.Kind.LINK_UNUSABLE -> EmailScreen.Code(email, link, "Automatic verification didn't work. Enter the code from your email.")
                else -> EmailScreen.Code(email, link, "We couldn't verify automatically. Enter the code from your email or try again.")
            }
        } catch (_: Exception) {
            screen = EmailScreen.Code(email, link, "We couldn't verify automatically. Enter the code from your email or try again.")
        }
    }

    suspend fun confirmCode(code: String) = exclusive {
        val current = screen as? EmailScreen.Code ?: return@exclusive
        if (!code.matches(Regex("[0-9]{6}"))) {
            screen = current.copy(error = "Enter the six-digit code in your email.")
            return@exclusive
        }
        screen = EmailScreen.Loading(LoadingStep.CONFIRM_EMAIL)
        try {
            complete(current.link.requestId, api.confirmCode(current.link.requestId, current.link.tokenB, code))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: EmailApiError) {
            screen = when (error.kind) {
                EmailApiError.Kind.INCORRECT_CODE -> current.copy(error = "That code didn't match. Try the latest email.", attemptsRemaining = error.attemptsRemaining)
                EmailApiError.Kind.ATTEMPT_LIMIT -> EmailScreen.Limit(current.email, current.link.requestId)
                EmailApiError.Kind.LINK_UNUSABLE -> EmailScreen.Unusable(current.email, current.link.requestId)
                EmailApiError.Kind.SIGN_IN_REQUIRED -> EmailScreen.Recovery
                else -> current.copy(error = "We couldn't verify your email. Please try again.")
            }
        } catch (_: Exception) {
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
        if (id == null || !canResend()) return@exclusive
        screen = EmailScreen.Loading(LoadingStep.RESEND_EMAIL)
        try {
            api.resend(id)
            lastEmailAtMillis = now()
            screen = EmailScreen.Wait(email, id, "If a new email arrives, open its latest link.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            screen = EmailScreen.Wait(email, id, "We couldn't request another email. Please try again.")
        }
    }

    fun changeEmail() {
        storage.clearPending()
        screen = EmailScreen.Start()
    }

    private fun complete(requestId: String, session: AuthSession) {
        try {
            storage.saveSession(session)
            if (storage.pending()?.requestId == requestId) storage.clearPending()
            screen = EmailScreen.Verified
        } catch (_: Exception) {
            // The server already consumed the proof. Retrying it cannot recover tokens.
            screen = EmailScreen.Recovery
        }
    }

    private suspend inline fun exclusive(crossinline action: suspend () -> Unit) {
        if (!requestLock.tryLock()) return
        try { action() } finally { requestLock.unlock() }
    }
}
