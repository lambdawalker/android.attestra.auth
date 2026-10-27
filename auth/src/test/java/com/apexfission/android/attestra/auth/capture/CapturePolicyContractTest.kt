package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.email.EmailHttpClient
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class CapturePolicyContractTest {
    @Test fun legacyRecordWithoutPolicyRemainsManageable() {
        val body = """{"capture_id":"${"a".repeat(32)}","evidence_version":1,"revision":1,"policy_version":"capture-v1","document_type":"passport","state":"uploading","expires_at":9999999999}"""
        for (value in listOf(body, body.dropLast(1) + ",\"policy\":null}")) {
            val record = EmailHttpClient.json.decodeFromString<CaptureRecord>(value)
            assertEquals("uploading", record.state)
            assertNull(record.policy)
        }
    }
    @Test fun checkpointRetainsReviewedIdentityAndWireField() {
        val request = CreateCapture("stable-operation-key", "passport", "capture-sha256-reviewed")
        val saved = EmailHttpClient.json.encodeToString(CaptureCheckpoint(create = request))
        assertTrue(saved.contains("expected_policy_version"))
        assertEquals(request, EmailHttpClient.json.decodeFromString<CaptureCheckpoint>(saved).create)
    }
}
