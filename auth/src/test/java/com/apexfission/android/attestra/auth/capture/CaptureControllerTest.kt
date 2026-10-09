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
        override suspend fun policy() = CapturePolicy(true, "capture-v1", "sample_card", listOf("front", "back"), "image/jpeg", 4194304)
        override suspend fun current() = record
        override suspend fun create(request: CreateCapture): CaptureRecord {
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
        override suspend fun cancel(id: String) = requireNotNull(record).copy(state = "cancelled").also { record = it }
    }
    @Test fun createResponseLossRecoversWithoutDuplicate() = runBlocking {
        val store = Store(); val api = Gateway(store); api.failCreate = true
        CaptureController(api, store).start()
        val resumed = CaptureController(api, store); resumed.restore()
        assertEquals(1, api.creates); assertEquals("uploading", resumed.state.value.record?.state)
    }
    @Test fun retryUsesSameUploadAndWipesBytesAfterSuccess() = runBlocking {
        val store = Store(); val api = Gateway(store); val controller = CaptureController(api, store)
        controller.start(); controller.camera(DocumentSide.FRONT); api.failPut = true
        val bytes = byteArrayOf(1, 2, 3, 4); controller.captured(DocumentSide.FRONT, bytes)
        val key = api.lastUploadKey; assertFalse(bytes.all { it == 0.toByte() })
        controller.upload(); assertEquals(key, api.lastUploadKey); assertTrue(bytes.all { it == 0.toByte() }); assertEquals(setOf("front"), controller.state.value.uploaded)
    }
    @Test fun lostFinalizeReconcilesAndNeverClaimsIdentityApproval() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.start()
        for (side in DocumentSide.entries) { c.camera(side); c.captured(side, byteArrayOf(1, 2, 3, 4)) }
        api.failFinalize = true; c.finalizeCapture(); c.close()
        val resumed = CaptureController(api, store); resumed.restore(); assertEquals("finalizing", resumed.state.value.record?.state)
        api.record = api.record?.copy(state = "ready"); resumed.restore(); assertEquals("ready", resumed.state.value.record?.state)
    }
    @Test fun closeDiscardsFailedUploadAndIgnoresLateCameraCallback() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.start(); c.camera(DocumentSide.FRONT)
        api.failPut = true; val bytes = byteArrayOf(1, 2, 3, 4); c.captured(DocumentSide.FRONT, bytes); c.close()
        assertTrue(bytes.all { it == 0.toByte() }); val late = byteArrayOf(5, 6, 7, 8); c.captured(DocumentSide.FRONT, late); assertTrue(late.all { it == 0.toByte() })
    }
    @Test fun refreshPreservesSuccessButFailedRetakeInvalidatesIt() = runBlocking {
        val store = Store(); val api = Gateway(store); val c = CaptureController(api, store); c.start()
        c.camera(DocumentSide.FRONT); c.captured(DocumentSide.FRONT, byteArrayOf(1, 2, 3, 4))
        c.restore(); assertEquals(setOf("front"), c.state.value.uploaded)
        c.camera(DocumentSide.FRONT); api.failPut = true; c.captured(DocumentSide.FRONT, byteArrayOf(5, 6, 7, 8))
        assertTrue(c.state.value.uploaded.isEmpty()); c.upload(); assertEquals(setOf("front"), c.state.value.uploaded)
    }
    @Test fun checksumUsesStandardPaddedBase64() { assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", checksum(byteArrayOf())) }
}
