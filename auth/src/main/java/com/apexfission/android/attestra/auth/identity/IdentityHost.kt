package com.apexfission.android.attestra.auth.identity

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.*
import com.apexfission.android.attestra.auth.ui.onboarding.common.*
import com.apexfission.android.attestra.auth.ui.onboarding.id.*
import kotlinx.coroutines.launch

/** Host owns controller lifetime. The camera slot transfers ownership of each JPEG to onCaptured. */
@Composable
fun IdentityHost(
    controller: IdentityController,
    passkeyAdded: Boolean,
    onExit: () -> Unit,
    isMock: Boolean = false,
    startCaptureOnEntry: Boolean = false,
    capture: @Composable (DocumentSide, (ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit,
) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller) {
        if (startCaptureOnEntry) controller.openCapture() else controller.restore()
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    BackHandler(onBack = onExit)
    val start = { scope.launch { controller.startCapture() }; Unit }
    val refresh = { scope.launch { controller.refresh() }; Unit }
    when (val current = state) {
        IdentityState.Start -> IdentityStartScreen(onExit, start, onExit, passkeyAdded, showAccountStatus = !isMock)
        is IdentityState.Capture -> key(current.side) {
            capture(current.side, { bytes ->
                // UNDISPATCHED enters controller ownership before the composition can be cancelled.
                scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { controller.captured(current.side, bytes) }
            }, { scope.launch { controller.captureProblem() } }, {
                scope.launch { controller.captureProblem("Capture was cancelled. Retry whenever you are ready.") }
            })
        }
        is IdentityState.CaptureProblem -> MessageScreen("Scan not completed", current.message, "Capture again", start, onExit)
        IdentityState.Unreadable -> IdentityDocumentUnreadableScreen(onExit, start, onExit)
        IdentityState.Reading -> OnboardingLoadingScreen(LoadingTask.READ_DOCUMENT, onExit)
        IdentityState.Submitting -> OnboardingLoadingScreen(LoadingTask.SUBMIT_ID, onExit)
        IdentityState.Checking -> OnboardingLoadingScreen(LoadingTask.CHECK_IDENTITY, onExit, onExit)
        is IdentityState.Review -> IdentityReviewScreen(
            initialDetails = current.corrected, onBack = onExit,
            onSubmit = { scope.launch { controller.submit(it) } }, onRescan = start,
            onDetailsChanged = controller::edit, extractedDetails = current.extracted, error = current.error,
        )
        IdentityState.SubmissionFailed -> IdentitySubmissionFailedScreen(onExit, { scope.launch { controller.retrySubmission() } }, onExit)
        IdentityState.StatusFailed -> MessageScreen("Could not check identity status", "Your previous submission has not been replaced. Check again when your connection is available.", "Check status", refresh, onExit)
        IdentityState.SignInRequired -> MessageScreen("Sign in to continue", "Your session can no longer access this check. Return to your account and sign in again.", "Return to account", onExit, onExit)
        is IdentityState.Result -> IdentityStatusScreen(current.record, isMock, refresh, start, onExit)
    }
}

@Composable
private fun MessageScreen(title: String, text: String, action: String, onAction: () -> Unit, onExit: () -> Unit) {
    OnboardingFrame(title, Icons.Default.Badge, "ID check", onExit, action, onAction,
        secondaryLabel = "Return to account", onSecondary = onExit) { OnboardingCard { BodyText(text) } }
}

@Composable
fun IdentityStatusScreen(record: IdentityRecord, isMock: Boolean, onRefresh: () -> Unit, onRetry: () -> Unit, onExit: () -> Unit) {
    val approved = record.decision == IdentityOutcome.APPROVED
    val pending = record.decision in listOf(IdentityOutcome.NOT_STARTED, IdentityOutcome.PENDING)
    val title = when {
        approved && isMock -> "Sample approved result"
        approved -> "Identity check complete"
        pending -> "Your identity check is pending"
        record.decision == IdentityOutcome.INCONCLUSIVE -> "More review is needed"
        record.decision == IdentityOutcome.ERROR -> "Identity service needs another try"
        else -> "Identity check unsuccessful"
    }
    OnboardingFrame(
        title, Icons.Default.Badge, if (isMock) "Mock result" else "ID check", onExit,
        primaryLabel = if (approved) "Return to account" else "Check status",
        onPrimary = if (approved) onExit else onRefresh,
        secondaryLabel = if (record.canRetry) "Capture a new document" else if (!approved) "Return to account" else null,
        onSecondary = if (record.canRetry) onRetry else onExit,
    ) {
        OnboardingCard {
            if (isMock) BodyText("This is a simulated result. No identity has been verified.")
            if (pending) BodyText("Your submission was accepted. You can leave now and check its status later.")
            if (!pending && !approved) BodyText(if (record.canRetry) "You may capture a new document and try again." else "Another capture is not currently allowed. Return to your account; contact your service’s support team if you need help.")
            BodyText("Automated report: ${record.autoReport.displayName()}")
            BodyText("Independent provider: ${record.thirdParty.displayName()}")
            BodyText("Identity decision: ${record.decision.displayName()}")
        }
    }
}
private fun IdentityOutcome.displayName() = name.lowercase().replace('_', ' ')
