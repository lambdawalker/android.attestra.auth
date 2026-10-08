package com.apexfission.android.attestra.auth.identity

import com.apexfission.android.attestra.auth.ui.onboarding.id.IdentityDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

/** One foreground operation at a time; never auto-retry a submission whose acceptance is unknown. */
class IdentityController(private val api: IdentityGateway, private val store: IdentityCheckpointStore) {
    private val mutableState = MutableStateFlow<IdentityState>(IdentityState.Start)
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var checkpoint = store.load()
    private var front: ByteArray? = null
    private var review: IdentityState.Review? = null
    private var submission: SubmitIdentity? = null
    private var lastRecord: IdentityRecord? = null
    private var closed = false

    private fun show(value: IdentityState) { if (!closed) mutableState.value = value }
    private suspend fun operation(failure: IdentityState, block: suspend () -> Unit) {
        if (closed || !mutex.tryLock()) return
        try { block() }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) {
            show(if (error is IdentityApiException && error.status in listOf(401, 403)) IdentityState.SignInRequired else failure)
        } finally { mutex.unlock() }
    }
    private fun checked(record: IdentityRecord): IdentityRecord {
        if (closed) throw CancellationException("Identity flow closed")
        val cp = requireNotNull(checkpoint)
        require(record.submissionId == cp.submissionId && record.evidenceVersion > 0)
        require(cp.evidenceVersion == 0 || record.evidenceVersion == cp.evidenceVersion)
        require(record.accepted || record.decision == IdentityOutcome.NOT_STARTED)
        if (cp.evidenceVersion == 0) save(IdentityCheckpoint(record.submissionId, record.evidenceVersion))
        lastRecord = record
        return record
    }
    private fun save(value: IdentityCheckpoint) { if (!closed) { store.save(value); checkpoint = value } }
    private fun discardImages() { front?.fill(0); front = null }

    suspend fun restore() = operation(IdentityState.StatusFailed) {
        val cp = checkpoint ?: return@operation
        show(IdentityState.Checking)
        val record = api.status(cp.submissionId)?.let(::checked)
        if (record?.accepted == true) show(IdentityState.Result(record))
        else show(IdentityState.CaptureProblem("Your unfinished scan was discarded. Capture both sides again."))
    }
    /** Entered after the account screen's explicit Scan action; reconcile an existing check first. */
    suspend fun openCapture() {
        restore()
        if (state.value == IdentityState.Start) startCapture()
    }
    suspend fun startCapture() = operation(IdentityState.CaptureProblem("Could not start the camera. Try again.")) {
        // A result can allow recapture only through explicit server policy. Pending checks cannot be replaced.
        val record = lastRecord
        if (record?.accepted == true && !record.canRetry) return@operation
        if (state.value in listOf(IdentityState.SubmissionFailed, IdentityState.StatusFailed, IdentityState.SignInRequired)) return@operation
        discardImages(); review = null; submission = null
        checkpoint = null; lastRecord = null; store.clear()
        show(IdentityState.Capture(DocumentSide.FRONT))
    }
    suspend fun captureProblem(message: String = "Capture did not finish. You can retry or return later.") =
        operation(IdentityState.CaptureProblem(message)) { discardImages(); show(IdentityState.CaptureProblem(message)) }

    /** Ownership of jpeg transfers here; it is wiped after upload or discard. Late/duplicate deliveries are wiped too. */
    suspend fun captured(side: DocumentSide, jpeg: ByteArray) {
        var retained = false
        try {
            operation(IdentityState.CaptureProblem("The document could not be processed. Capture both sides again.")) {
                if (state.value != IdentityState.Capture(side)) return@operation
                require(jpeg.isNotEmpty() && jpeg.size <= IdentityApi.MAX_JPEG_BYTES)
                if (side == DocumentSide.FRONT) {
                    front = jpeg; retained = true; show(IdentityState.Capture(DocumentSide.BACK))
                } else {
                    val first = requireNotNull(front)
                    show(IdentityState.Reading)
                    val id = UUID.randomUUID().toString()
                    // Save before the first request so loss of a create response can still be reconciled.
                    save(IdentityCheckpoint(id))
                    try {
                        val created = checked(api.create(id))
                        api.upload(id, created.evidenceVersion, DocumentSide.FRONT, first)
                        api.upload(id, created.evidenceVersion, DocumentSide.BACK, jpeg)
                        val extraction = api.extract(id, created.evidenceVersion)
                        if (closed) throw CancellationException("Identity flow closed")
                        if (extraction.readable) {
                            val value = IdentityState.Review(extraction.extracted, extraction.extracted)
                            review = value; show(value)
                        } else show(IdentityState.Unreadable)
                    } finally { discardImages() }
                }
            }
        } finally { if (!retained) jpeg.fill(0) }
    }
    fun edit(details: IdentityDetails) {
        if (closed || state.value !is IdentityState.Review) return
        review = review?.copy(corrected = details, error = null)
        review?.let(::show)
    }
    suspend fun submit(details: IdentityDetails) = operation(IdentityState.SubmissionFailed) {
        val current = review ?: return@operation
        if (state.value !is IdentityState.Review) return@operation
        if (details.fullName.isBlank() || details.dateOfBirth.isBlank() || details.address.isBlank()) {
            review = current.copy(corrected = details, error = "Enter your name, date of birth, and address.")
            show(review!!); return@operation
        }
        val cp = requireNotNull(checkpoint)
        submission = SubmitIdentity(cp.evidenceVersion, current.extracted, details)
        show(IdentityState.Submitting)
        val accepted = checked(api.submit(cp.submissionId, submission!!))
        require(accepted.accepted)
        review = null; submission = null
        show(IdentityState.Result(accepted))
    }
    suspend fun retrySubmission() = operation(IdentityState.SubmissionFailed) {
        if (state.value != IdentityState.SubmissionFailed) return@operation
        val cp = requireNotNull(checkpoint)
        show(IdentityState.Checking)
        // If status fails, do NOT send again. Only an explicit draft permits the same immutable retry body.
        val existing = api.status(cp.submissionId)?.let(::checked)
        if (existing?.accepted == true) {
            review = null; submission = null; show(IdentityState.Result(existing))
        } else if (existing != null && submission != null) {
            show(IdentityState.Submitting)
            val accepted = checked(api.submit(cp.submissionId, submission!!))
            require(accepted.accepted)
            review = null; submission = null; show(IdentityState.Result(accepted))
        } else show(IdentityState.CaptureProblem("This draft is no longer available. Capture both sides again."))
    }
    suspend fun refresh() = operation(IdentityState.StatusFailed) {
        val cp = checkpoint ?: return@operation
        show(IdentityState.Checking)
        val record = api.status(cp.submissionId)?.let(::checked)
        if (record?.accepted == true) show(IdentityState.Result(record))
        else if (submission != null) show(IdentityState.SubmissionFailed)
        else show(IdentityState.CaptureProblem("This unfinished scan needs to be captured again."))
    }
    fun close() { closed = true; discardImages(); review = null; submission = null; lastRecord = null; mutableState.value = IdentityState.Start }
}
