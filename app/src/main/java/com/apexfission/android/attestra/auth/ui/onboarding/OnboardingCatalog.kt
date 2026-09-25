package com.apexfission.android.attestra.auth.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.ui.theme.AttestraSurfaceContainerLow
import com.apexfission.android.attestra.auth.ui.theme.AttestraSurface

/**
 * UI-only integration seam. A future host supplies real implementations that own
 * requests, authentication/session state, passkey APIs, deep links and the ID plugin.
 * None of these methods should infer a successful result from a button tap.
 */
class OnboardingBusinessActions {
    fun requestEmail(email: String) { /* TODO: POST /signup and retain local signup proof. */ }
    fun resendEmail() { /* TODO: POST /resend and honor the server retry window. */ }
    fun confirmWithCode(code: String) { /* TODO: submit B+C for the opened link. */ }
    fun confirmWithLocalProof() { /* TODO: submit A+B after link routing. */ }
    fun createPasskey() { /* TODO: fetch options and invoke platform credential UI. */ }
    fun finishPasskeyRegistration() { /* TODO: send credential to /passkeys/complete. */ }
    fun checkPasskeySupport() { /* TODO: query current platform capability. */ }
    fun openIdentityCapture() { /* TODO: launch the selected ID capture plugin. */ }
    fun submitIdentityDetails(details: IdentityDetails) { /* TODO: submit reviewed data once. */ }
    fun reconcileIdentitySubmission() { /* TODO: query status before retrying uncertain submission. */ }
    fun retryIdentityCheck() { /* TODO: ask provider for a new permitted attempt. */ }
    fun recoverEmailSignIn() { /* TODO: resume email OTP login when session issuance fails. */ }
}

private data class CatalogItem(val key: String, val title: String)

private val catalogItems = listOf(
    CatalogItem("email_start", "Email verification - start"),
    CatalogItem("email_wait", "Email verification - wait"),
    CatalogItem("email_code", "Email verification - manual code input"),
    CatalogItem("email_wrong", "Email verification - incorrect code"),
    CatalogItem("email_limit", "Email verification - attempt limit"),
    CatalogItem("email_link", "Email verification - unusable link"),
    CatalogItem("email_recovery", "Email verification - session recovery"),
    CatalogItem("passkey_start", "Passkey setup - start"),
    CatalogItem("passkey_failed", "Passkey setup - failed"),
    CatalogItem("passkey_unsupported", "Passkey setup - unsupported device"),
    CatalogItem("identity_start", "Identity verification - start"),
    CatalogItem("identity_review", "Identity verification - review details"),
    CatalogItem("identity_unreadable", "Identity verification - unreadable document"),
    CatalogItem("identity_submission", "Identity verification - submission failed"),
    CatalogItem("identity_success", "Identity verification - success"),
    CatalogItem("identity_unsuccessful", "Identity verification - unsuccessful result (pending design)"),
) + LoadingTask.entries.map { CatalogItem("loading_${it.name}", "Processing - ${it.title}") }

/** A screen gallery until the onboarding backend, link router and platform integrations exist. */
@Composable
fun OnboardingCatalog(actions: OnboardingBusinessActions) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val back = { selected = null }
    val sampleEmail = "name@example.invalid"
    if (selected == null) {
        Column(
            Modifier.fillMaxSize().background(AttestraSurface).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Onboarding screen gallery", style = MaterialTheme.typography.headlineSmall)
            BodyText("Select a UI state. Actions call empty business hooks; no account, passkey, or ID status changes here.")
            catalogItems.forEach { item ->
                Card(
                    Modifier.fillMaxWidth().clickable { selected = item.key },
                    colors = CardDefaults.cardColors(containerColor = AttestraSurfaceContainerLow),
                ) {
                    Text(item.title, modifier = Modifier.padding(18.dp))
                }
            }
        }
        return
    }
    when (selected) {
        "email_start" -> EmailStartScreen(back, actions::requestEmail)
        "email_wait" -> EmailWaitScreen(sampleEmail, back, actions::resendEmail) { selected = "email_start" }
        "email_code" -> EmailCodeScreen(sampleEmail, back, actions::confirmWithCode, actions::resendEmail)
        "email_wrong" -> EmailCodeScreen(
            sampleEmail, back, actions::confirmWithCode, actions::resendEmail,
            error = "That code didn’t match. Check the six-digit code in your email and try again.",
            attemptsRemaining = 2,
        )
        "email_limit" -> EmailAttemptLimitScreen(sampleEmail, back, actions::resendEmail, { selected = "email_start" }, canRequestEmail = true)
        "email_link" -> EmailLinkUnusableScreen(sampleEmail, back, actions::resendEmail) { selected = "email_start" }
        "email_recovery" -> EmailSessionRecoveryScreen(back, actions::recoverEmailSignIn)
        "passkey_start" -> PasskeyStartScreen(back, actions::createPasskey, back)
        "passkey_failed" -> PasskeyFailedScreen(back, actions::createPasskey, back, cancelled = false)
        "passkey_unsupported" -> PasskeyUnsupportedScreen(back, back, actions::checkPasskeySupport)
        "identity_start" -> IdentityStartScreen(back, actions::openIdentityCapture, back, passkeyAdded = false)
        "identity_review" -> IdentityReviewScreen(IdentityDetails(), back, actions::submitIdentityDetails, actions::openIdentityCapture)
        "identity_unreadable" -> IdentityDocumentUnreadableScreen(back, actions::openIdentityCapture, back)
        "identity_submission" -> IdentitySubmissionFailedScreen(back, actions::reconcileIdentitySubmission, back)
        "identity_success" -> IdentitySuccessScreen(back, back, passkeyAdded = true)
        "identity_unsuccessful" -> IdentityUnsuccessfulScreen(back, back, actions::retryIdentityCheck, canRetry = true)
        else -> {
            val task = LoadingTask.entries.firstOrNull { "loading_${it.name}" == selected }
            if (task != null) OnboardingLoadingScreen(task, back, onLeave = back)
        }
    }
}
