package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.identity.DocumentSide
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CaptureControllerTest {
    private class Store : CaptureCheckpointStore {
        var value: CaptureCheckpoint? = null
        override fun load() = value
        override fun save(value: CaptureCheckpoint) { this.value = value }
        override fun clear() { value = null }
    }
    private class Gateway(val store: Store) : CaptureGateway {
        var record: CaptureRecord? = null
        var creates = 0
        var failCreate = false
        var failPut = false
        var failFinalize = false
        var lastUploadKey: String? = null
        var policyValue = CapturePolicy(true, "capture-v1", "sample_card", listOf("front", "back"), "image/jpeg", 4194304)
        var policyFailure = false
        var onCurrent: () -> Unit = {}
        var onCreate: () -> Unit = {}
        var createCalls = 0
        override suspend fun policy(): CapturePolicy { if (policyFailure) throw CaptureException(503); return policyValue }
        override suspend fun current(): CaptureRecord? { onCurrent(); return record }
        override suspend fun create(request: CreateCapture): CaptureRecord {
            createCalls++
            onCreate()
            if (record == null && request.expectedPolicyVersion != policyValue.version) throw CaptureException(409, "policy_changed")
            assertEquals(request, store.value?.create)
            if (record == null) { creates++; record = CaptureRecord("a".repeat(32), 1, 1, "capture-v1", "sample_card", "uploading", 9999999999) }
            if (failCreate) { failCreate = false; throw CaptureException(503) }
            return requireNotNull(record)
        }
        override suspend fun get(id: String) = requireNotNull(record)
        override suspend fun register(id: String, request: RegisterUpload): UploadInstructions {
            val r = requireNotNull(record)
            if (lastUploadKey != request.key) record = r.copy(revision = r.revision + 1, selected = r.selected + (request.slot to request.key))
            lastUploadKey = request.key
            return UploadInstructions(requireNotNull(record), request.key, "https://example.invalid", emptyMap(), 300)
        }
        override suspend fun put(instructions: UploadInstructions, jpeg: ByteArray) { if (failPut) { failPut = false; throw CaptureException(503) } }
        override suspend fun finalize(id: String, request: FinalizeCapture): CaptureRecord {
            assertEquals(request, store.value?.finalize)
            record = requireNotNull(record).copy(state = "finalizing")
            if (failFinalize) { failFinalize = false; throw CaptureException(503) }
            return requireNotNull(record)
        }
        override suspend fun retry(id: String, key: String) = requireNotNull(record).copy(state = "finalizing").also { record = it }
        var failReview = false
        override suspend fun review(id: String, request: ReviewCapture): CaptureRecord {
            val r = requireNotNull(record)
            if (request.revision != r.revision && r.reviewed?.data != request.data) throw CaptureException(409)
            record = r.copy(revision = r.revision + 1, reviewed = ReviewedCard(request.data, 123, request.originCountry))
            if (failReview) { failReview = false; throw CaptureException(503) }
            return requireNotNull(record)
        }
        override suspend fun cancel(id: String) = requireNotNull(record).copy(state = "cancelled").also { record = it }
    }
    @Test fun pairedCaptureRetriesBothSidesAndWipesImages() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start(); c.camera(DocumentSide.FRONT)
        val front = byteArrayOf(1, 2, 3, 4); val back = byteArrayOf(5, 6, 7, 8)
        api.failPut = true
        c.capturedBoth(front, back)
        assertFalse(back.all { it == 0.toByte() })
        c.upload()
        assertEquals(setOf("front", "back"), c.state.value.uploaded)
        assertTrue(front.all { it == 0.toByte() }); assertTrue(back.all { it == 0.toByte() })
    }
    @Test fun closeWipesBothPendingSides() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start(); c.camera(DocumentSide.FRONT); api.failPut = true
        val front = byteArrayOf(1, 2, 3, 4); val back = byteArrayOf(5, 6, 7, 8)
        c.capturedBoth(front, back); c.close()
        assertTrue(front.all { it == 0.toByte() }); assertTrue(back.all { it == 0.toByte() })
    }
    @Test fun createResponseLossRecoversWithoutDuplicate() = runBlocking {
        val store = Store(); val api = Gateway(store); api.failCreate = true
        CaptureController(api, store).apply { restore(); start() }
        val resumed = CaptureController(api, store); resumed.restore()
        assertEquals(1, api.creates); assertEquals("uploading", resumed.state.value.record?.state)
    }
    @Test fun retryUsesSameUploadAndWipesBytesAfterSuccess() = runBlocking {
        val store = Store(); val api = Gateway(store); val controller = CaptureController(api, store)
        controller.restore(); controller.start(); controller.camera(DocumentSide.FRONT); api.failPut = true
        val bytes = byteArrayOf(1, 2, 3, 4); controller.captured(DocumentSide.FRONT, bytes)
        val key = api.lastUploadKey; assertFalse(bytes.all { it == 0.toByte() })
        controller.upload(); assertEquals(key, api.lastUploadKey); assertTrue(bytes.all { it == 0.toByte() }); assertEquals(setOf("front"), controller.state.value.uploaded)
    }
    @Test fun lostFinalizeReconcilesAndNeverClaimsIdentityApproval() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.restore(); c.start()
        for (side in DocumentSide.entries) { c.camera(side); c.captured(side, byteArrayOf(1, 2, 3, 4)) }
        api.failFinalize = true; c.finalizeCapture(); c.close()
        val resumed = CaptureController(api, store); resumed.restore(); assertEquals("finalizing", resumed.state.value.record?.state)
        api.record = api.record?.copy(state = "ready"); resumed.restore(); assertEquals("ready", resumed.state.value.record?.state)
    }
    @Test fun closeDiscardsFailedUploadAndIgnoresLateCameraCallback() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.restore(); c.start(); c.camera(DocumentSide.FRONT)
        api.failPut = true; val bytes = byteArrayOf(1, 2, 3, 4); c.captured(DocumentSide.FRONT, bytes); c.close()
        assertTrue(bytes.all { it == 0.toByte() }); val late = byteArrayOf(5, 6, 7, 8); c.captured(DocumentSide.FRONT, late); assertTrue(late.all { it == 0.toByte() })
    }
    @Test fun refreshPreservesSuccessButFailedRetakeInvalidatesIt() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.restore(); c.start()
        c.camera(DocumentSide.FRONT); c.captured(DocumentSide.FRONT, byteArrayOf(1, 2, 3, 4))
        c.restore(); assertEquals(setOf("front"), c.state.value.uploaded)
        c.camera(DocumentSide.FRONT); api.failPut = true; c.captured(DocumentSide.FRONT, byteArrayOf(5, 6, 7, 8))
        assertTrue(c.state.value.uploaded.isEmpty()); c.upload(); assertEquals(setOf("front"), c.state.value.uploaded)
    }
    @Test fun discoveringAnotherCaptureDiscardsOldPendingImage() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.restore(); c.start()
        c.camera(DocumentSide.FRONT); api.failPut = true
        val bytes = byteArrayOf(1, 2, 3, 4); c.captured(DocumentSide.FRONT, bytes)
        api.record = api.record?.copy(id = "b".repeat(32), revision = 1, selected = emptyMap())
        c.restore(); assertTrue(bytes.all { it == 0.toByte() }); assertTrue(c.state.value.uploaded.isEmpty())
    }
    @Test fun checksumUsesStandardPaddedBase64() { assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", checksum(byteArrayOf())) }
    @Test fun changedDisclosureNeedsSecondExplicitStart() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore()
        api.policyValue = api.policyValue.copy(version = "changed", purpose = "Different purpose")
        c.start(); assertEquals(0, api.creates)
        assertEquals("Different purpose", c.state.value.policy?.purpose)
        c.start(); assertEquals(1, api.creates)
    }
    @Test fun serverPolicyRaceClearsIntentAndRequiresReview() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore()
        api.onCreate = { api.policyValue = api.policyValue.copy(version = "new", purpose = "New purpose") }
        c.start(); assertEquals(0, api.creates); assertNull(store.value?.create)
        assertEquals("New purpose", c.state.value.policy?.purpose)
        api.onCreate = {}
        c.start(); assertEquals(1, api.creates)
    }
    @Test fun disabledCaptureStillAllowsStatusAndCancellationDuringPolicyOutage() = runBlocking {
        val store = Store(); val api = Gateway(store); var enabled = true
        val c = CaptureController(api, store) { enabled }
        c.restore(); c.start(); enabled = false; api.policyFailure = true
        c.restore(); assertNotNull(c.state.value.record)
        c.cancel(); assertEquals("cancelled", api.record?.state)
        c.start(); assertEquals(1, api.creates)
    }
    @Test fun expirationDuringRequestPreventsNewCreate() = runBlocking {
        val store = Store(); val api = Gateway(store); var enabled = true
        val c = CaptureController(api, store) { enabled }
        c.restore(); api.onCurrent = { enabled = false }
        c.start(); assertEquals(0, api.createCalls)
    }
    @Test fun disabledRestorePreservesUncertainCreateWithoutReplayingIt() = runBlocking {
        val store = Store(); store.value = CaptureCheckpoint(create = CreateCapture("uncertain-key", "sample_card", "capture-v1"))
        val api = Gateway(store); val c = CaptureController(api, store) { false }
        c.restore(); assertEquals(0, api.createCalls); assertNotNull(store.value?.create)
    }
    @Test fun policyIdentitySurvivesLostResponseAndChangedPolicy() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); api.failCreate = true; c.start()
        val intent = store.value?.create
        assertEquals("capture-v1", intent?.expectedPolicyVersion)
        api.policyValue = api.policyValue.copy(version = "new")
        val resumed = CaptureController(api, store); resumed.restore()
        assertEquals(1, api.creates); assertEquals("capture-v1", resumed.state.value.record?.policyVersion)
    }

    @Test fun extractedFieldsAreEditableAndRefreshPreservesDraft() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start()
        val extracted = CardData(fullName = "Sample Person", documentNumber = "000123")
        api.record = api.record?.copy(state = "ready", extraction = CardExtraction("complete", extracted))
        c.restore(); assertEquals(extracted, c.state.value.draft)
        c.editCard(extracted.copy(fullName = "Corrected Person"))
        c.restore(); assertEquals("Corrected Person", c.state.value.draft?.fullName)
        c.editOrigin("MX"); api.failReview = true; c.saveReview()
        assertNotNull(c.state.value.message)
        c.restore(); assertEquals("Corrected Person", c.state.value.record?.reviewed?.data?.fullName)
        assertEquals("Sample Person", c.state.value.record?.extraction?.data?.fullName)
        assertFalse(c.state.value.reviewDirty)
        assertFalse(store.value.toString().contains("Corrected Person"))
    }
    @Test fun cancellationAndCloseClearCardDataFromState() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start()
        api.record = api.record?.copy(state = "ready", extraction = CardExtraction("complete", CardData(fullName = "Sample")))
        c.restore(); assertNotNull(c.state.value.draft)
        c.cancel(); assertNull(c.state.value.draft)
        c.close(); assertNull(c.state.value.record)
    }

    @Test fun continueOpensCameraWithoutExtraStart() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.continueToCapture()
        assertEquals(1, api.creates)
        assertEquals(DocumentSide.FRONT, c.state.value.camera)
    }
    @Test fun completedCaptureGoesStraightToReviewOnReentry() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start()
        api.record = api.record?.copy(state = "ready", extraction = CardExtraction("complete", CardData(fullName = "Sample")))
        val resumed = CaptureController(api, store); resumed.restore(); resumed.continueToCapture()
        assertNull(resumed.state.value.camera); assertEquals("Sample", resumed.state.value.draft?.fullName)
        assertEquals(1, api.creates)
    }
    @Test fun confirmedPairAutomaticallyFinalizes() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.continueToCapture()
        c.capturedBoth(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8))
        assertEquals("finalizing", c.state.value.record?.state)
        assertNotNull(store.value?.finalize)
    }
    @Test fun retryPairUploadsAndFinalizesWithoutAnotherButton() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.continueToCapture(); api.failPut = true
        c.capturedBoth(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8))
        assertEquals("uploading", c.state.value.record?.state)
        c.uploadAndContinue()
        assertEquals("finalizing", c.state.value.record?.state)
    }
    @Test fun failedExtractionStartsEmptyAndManualEditsCanBeSaved() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start()
        api.record = api.record?.copy(state = "ready", extraction = CardExtraction("failed", CardData(fullName = "Untrusted partial result")))
        c.restore(); assertEquals(CardData(), c.state.value.draft)
        c.editCard(CardData(fullName = "Manual Person")); c.editOrigin("MX")
        c.restore(); assertEquals("Manual Person", c.state.value.draft?.fullName)
        c.saveReview(); assertNull(c.state.value.message)
        assertEquals("Manual Person", c.state.value.record?.reviewed?.data?.fullName)
        assertEquals("failed", c.state.value.record?.extraction?.status)
        c.editCard(CardData(fullName = "Unsaved")); c.resetReview()
        assertEquals("Manual Person", c.state.value.draft?.fullName)
    }
    @Test fun pendingOrMissingExtractionDoesNotOfferManualDraft() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start()
        for (extraction in listOf(null, CardExtraction("pending"))) {
            api.record = api.record?.copy(state = "ready", extraction = extraction)
            c.restore(); assertNull(c.state.value.draft)
        }
    }
    @Test fun policyRaceStopsAutomaticCameraEntry() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); api.onCreate = { api.policyValue = api.policyValue.copy(version = "new") }
        c.continueToCapture()
        assertNull(c.state.value.camera); assertNotNull(c.state.value.message)
    }

    @Test fun selectedPairResumesThroughServerValidationWithoutCamera() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.start()
        for (side in DocumentSide.entries) { c.camera(side); c.captured(side, byteArrayOf(1, 2, 3, 4)) }
        val resumed = CaptureController(api, store); resumed.restore(); resumed.continueToCapture()
        assertEquals("finalizing", resumed.state.value.record?.state)
        assertNull(resumed.state.value.camera)
    }
    @Test fun lostAutomaticFinalizeResponseResumesWithoutRecapture() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.continueToCapture(); api.failFinalize = true
        c.capturedBoth(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8))
        assertNotNull(c.state.value.message)
        val resumed = CaptureController(api, store); resumed.restore(); resumed.continueToCapture()
        assertEquals("finalizing", resumed.state.value.record?.state)
        assertNull(resumed.state.value.camera); assertEquals(1, api.creates)
    }

    @Test fun policyAcceptedOnContinueScreenMustMatchEvenAfterRestore() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        val accepted = api.policyValue
        api.policyValue = api.policyValue.copy(version = "new-policy", purpose = "New disclosure")
        c.restore(); c.continueToCapture(reviewedPolicy = accepted)
        assertEquals(0, api.creates); assertNull(c.state.value.camera)
        assertNotNull(c.state.value.message)
        // After explicitly accepting the refreshed disclosure, the camera may open.
        c.continueToCapture(reviewedPolicy = c.state.value.policy)
        assertEquals(1, api.creates); assertEquals(DocumentSide.FRONT, c.state.value.camera)
    }
    @Test fun noAcceptedPolicyCannotAutomaticallyCreateButCanResumeReview() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store)
        c.restore(); c.continueToCapture(reviewedPolicy = null)
        assertEquals(0, api.creates)
        c.start()
        api.record = api.record?.copy(state = "ready", extraction = CardExtraction("failed"))
        c.restore(); c.continueToCapture(reviewedPolicy = null)
        assertEquals(CardData(), c.state.value.draft); assertNull(c.state.value.camera)
    }

}
