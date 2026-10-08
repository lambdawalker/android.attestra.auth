package com.apexfission.android.attestra.auth.identity

import com.apexfission.android.attestra.auth.ui.onboarding.id.IdentityDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IdentityControllerTest {
    private class Store : IdentityCheckpointStore {
        var value: IdentityCheckpoint? = null
        override fun load() = value
        override fun save(checkpoint: IdentityCheckpoint) { value = checkpoint }
        override fun clear() { value = null }
    }
    private open class Api : IdentityGateway {
        var record: IdentityRecord? = null
        var submits = 0
        var loseResponse = false
        var cancel = false
        var failStatus = false
        var submitted: SubmitIdentity? = null
        val original = IdentityDetails(fullName = "Sample Person", dateOfBirth = "1990-01-02", address = "Sample address")
        override suspend fun create(id: String): IdentityRecord = IdentityRecord(id, 1).also { record = it }
        override suspend fun upload(id: String, version: Int, side: DocumentSide, jpeg: ByteArray) { if (cancel) throw CancellationException() }
        override suspend fun extract(id: String, version: Int) = Extraction(true, original)
        override suspend fun submit(id: String, body: SubmitIdentity): IdentityRecord {
            submits++; submitted = body
            val accepted = record!!.copy(accepted = true, decision = IdentityOutcome.PENDING, autoReport = IdentityOutcome.APPROVED, thirdParty = IdentityOutcome.PENDING)
            record = accepted
            if (loseResponse) throw java.io.IOException("Response lost")
            return accepted
        }
        override suspend fun status(id: String): IdentityRecord? {
            if (failStatus) throw java.io.IOException("Offline")
            return record
        }
    }
    private suspend fun reviewed(c: IdentityController) {
        c.startCapture()
        c.captured(DocumentSide.FRONT, byteArrayOf(1))
        c.captured(DocumentSide.BACK, byteArrayOf(2))
    }
    @Test fun acceptedUploadAndAutoReportAreNotApproval() = runBlocking {
        val api = Api(); val c = IdentityController(api, Store())
        reviewed(c); c.submit(api.original)
        val result = c.state.value as IdentityState.Result
        assertEquals(IdentityOutcome.PENDING, result.record.decision)
        assertEquals(IdentityOutcome.APPROVED, result.record.autoReport)
    }
    @Test fun correctionNeverOverwritesExtraction() = runBlocking {
        val api = Api(); val c = IdentityController(api, Store())
        reviewed(c); c.submit(api.original.copy(fullName = "Corrected Name"))
        assertEquals("Sample Person", api.submitted!!.extracted.fullName)
        assertEquals("Corrected Name", api.submitted!!.corrected.fullName)
    }
    @Test fun lostAcceptanceResponseReconcilesWithoutDuplicateSubmission() = runBlocking {
        val api = Api().apply { loseResponse = true }; val c = IdentityController(api, Store())
        reviewed(c); c.submit(api.original)
        assertTrue(c.state.value is IdentityState.SubmissionFailed)
        c.retrySubmission()
        assertEquals(1, api.submits)
        assertTrue(c.state.value is IdentityState.Result)
    }
    @Test fun failedStatusLookupNeverReplaysUnknownSubmission() = runBlocking {
        val api = Api().apply { loseResponse = true }; val c = IdentityController(api, Store())
        reviewed(c); c.submit(api.original); api.failStatus = true
        c.retrySubmission()
        assertEquals(1, api.submits)
        assertEquals(IdentityState.SubmissionFailed, c.state.value)
    }
    @Test fun closeDropsPersonalReviewState() = runBlocking {
        val c = IdentityController(Api(), Store()); reviewed(c)
        assertTrue(c.state.value is IdentityState.Review)
        c.close()
        assertEquals(IdentityState.Start, c.state.value)
    }
    @Test fun restartRestoresAcceptedStatusWithoutImageOrPersonalFields() = runBlocking {
        val api = Api(); val store = Store(); val c = IdentityController(api, store)
        reviewed(c); c.submit(api.original)
        val resumed = IdentityController(api, store); resumed.restore()
        assertTrue(resumed.state.value is IdentityState.Result)
        assertEquals(1, api.submits)
    }
    @Test fun restartOfDraftRequiresRecapture() = runBlocking {
        val api = Api(); val store = Store(); reviewed(IdentityController(api, store))
        val resumed = IdentityController(api, store); resumed.restore()
        assertTrue(resumed.state.value is IdentityState.CaptureProblem)
        assertEquals(0, api.submits)
    }
    @Test fun rejectionWithoutRetryPermissionCannotCreateNewEvidence() = runBlocking {
        val api = Api(); val store = Store(); val c = IdentityController(api, store)
        reviewed(c); c.submit(api.original)
        api.record = api.record!!.copy(decision = IdentityOutcome.REJECTED, canRetry = false)
        c.refresh(); val before = c.state.value
        c.startCapture()
        assertEquals(before, c.state.value)
    }
    @Test fun staleVersionCannotReplaceCurrentEvidence() = runBlocking {
        val api = Api(); val c = IdentityController(api, Store())
        reviewed(c); c.submit(api.original)
        api.record = api.record!!.copy(evidenceVersion = 99, decision = IdentityOutcome.APPROVED)
        c.refresh()
        assertTrue(c.state.value is IdentityState.StatusFailed)
    }
    @Test fun cancellationIsNotPresentedAsNetworkFailure() = runBlocking {
        val api = Api().apply { cancel = true }; val c = IdentityController(api, Store())
        c.startCapture(); c.captured(DocumentSide.FRONT, byteArrayOf(1))
        try { c.captured(DocumentSide.BACK, byteArrayOf(2)); fail("Cancellation must propagate") }
        catch (_: CancellationException) { }
        assertFalse(c.state.value is IdentityState.SubmissionFailed)
    }
    @Test fun resetClosesOldFlowBeforeClearingItsCheckpoint() = runBlocking {
        val store = Store(); val api = Api(); val controller = IdentityController(api, store)
        reviewed(controller); controller.submit(api.original)
        controller.close(); store.clear()
        controller.refresh()
        assertNull(store.value)
        val replacement = IdentityController(api, store)
        replacement.restore()
        assertEquals(IdentityState.Start, replacement.state.value)
    }
    @Test fun noBackCaptureBeforeFront() = runBlocking {
        val api = Api(); val c = IdentityController(api, Store())
        c.startCapture(); c.captured(DocumentSide.BACK, byteArrayOf(2))
        assertEquals(IdentityState.Capture(DocumentSide.FRONT), c.state.value)
        assertNull(api.record)
    }
}
