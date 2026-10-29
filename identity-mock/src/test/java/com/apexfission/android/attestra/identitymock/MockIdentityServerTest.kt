package com.apexfission.android.attestra.identitymock

import com.apexfission.android.attestra.auth.capture.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class MockCaptureServerTest {
    private class Memory : MockStateStore { var value: String? = null; override fun read() = value; override fun write(value: String) { this.value = value } }
    @Test fun uploadAndFinalizeUseSeparateTransportsAndSurviveRestart() = runBlocking {
        val store = Memory(); val server = MockCaptureServer(store)
        val client = server.client(); val uploads = server.client()
        try {
            val api = CaptureApi(MockCaptureServer.ORIGIN, client, uploads) { MockCaptureServer.TOKEN }
            var r = api.create(CreateCapture("create-operation-123", "sample_card"))
            val jpeg = byteArrayOf(1, 2, 3, 4)
            val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(jpeg))
            for (side in listOf("front", "back")) {
                val instruction = api.register(r.id, RegisterUpload("upload-operation-$side", r.revision, side, hash, jpeg.size.toLong()))
                api.put(instruction, jpeg); r = instruction.capture
            }
            server.scenario = MockScenario.RESPONSE_LOST
            assertTrue(runCatching { api.finalize(r.id, FinalizeCapture("finalize-operation-123", r.revision, r.selected)) }.isFailure)
            val resumedServer = MockCaptureServer(store); val resumedClient = resumedServer.client()
            try {
                val resumed = CaptureApi(MockCaptureServer.ORIGIN, resumedClient, uploads) { MockCaptureServer.TOKEN }
                assertEquals("finalizing", resumed.current()?.state); assertEquals("ready", resumed.current()?.state)
                val extracted = requireNotNull(resumed.current())
                assertEquals("000123", extracted.extraction?.data?.documentNumber)
                val reviewed = resumed.review(extracted.id, ReviewCapture(extracted.revision, CardData(fullName = "Private edited value")))
                assertEquals("Private edited value", reviewed.reviewed?.data?.fullName)
                assertFalse(requireNotNull(store.value).contains("Private edited value"))
                assertFalse(requireNotNull(store.value).contains("signed"))
            } finally { resumedClient.close() }
        } finally { client.close(); uploads.close() }
    }
    @Test fun failedExtractionCanBeReviewedThroughHttpWithoutRecapture() = runBlocking {
        val store = Memory(); val server = MockCaptureServer(store)
        server.scenario = MockScenario.EXTRACTION_FAILED
        val client = server.client(); val uploads = server.client()
        try {
            val api = CaptureApi(MockCaptureServer.ORIGIN, client, uploads) { MockCaptureServer.TOKEN }
            var r = api.create(CreateCapture("manual-create-operation", "sample_card"))
            val jpeg = byteArrayOf(1, 2, 3, 4)
            val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(jpeg))
            for (side in listOf("front", "back")) {
                val instruction = api.register(r.id, RegisterUpload("manual-upload-$side", r.revision, side, hash, jpeg.size.toLong()))
                api.put(instruction, jpeg); r = instruction.capture
            }
            api.finalize(r.id, FinalizeCapture("manual-finalize-operation", r.revision, r.selected))
            repeat(3) { r = requireNotNull(api.current()) }
            assertEquals("failed", r.extraction?.status)
            val saved = api.review(r.id, ReviewCapture(r.revision, CardData(fullName = "Manually entered"), "MX"))
            assertEquals("Manually entered", saved.reviewed?.data?.fullName)
            assertEquals("failed", saved.extraction?.status)
            assertFalse(requireNotNull(store.value).contains("Manually entered"))
        } finally { client.close(); uploads.close() }
    }

}
