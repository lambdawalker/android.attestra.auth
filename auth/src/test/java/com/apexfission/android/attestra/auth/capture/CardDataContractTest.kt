package com.apexfission.android.attestra.auth.capture

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CardDataContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    @Test fun statusDecodesExtractionAndPreservesLeadingZeros() {
        val r = json.decodeFromString<CaptureRecord>("""{
            "capture_id":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","evidence_version":1,"revision":8,
            "policy_version":"v1","document_type":"card","state":"ready","expires_at":9999999999,
            "extraction":{"status":"complete","data":{"full_name":"Ana García","document_number":"000123"}},
            "reviewed":{"data":{"full_name":"Ana María García","document_number":"000123"},"saved_at":123}
        }""")
        assertEquals("000123", r.extraction?.data?.documentNumber)
        assertEquals("", r.extraction?.data?.birthDate)
        assertEquals("Ana María García", r.reviewed?.data?.fullName)
    }
    @Test fun limitsCountUtf8BytesNotCharacters() {
        assertTrue(CardData(fullName = "é".repeat(256)).isValid())
        assertFalse(CardData(fullName = "é".repeat(257)).isValid())
        assertFalse(CardData(documentNumber = "a\u0000b").isValid())
    }
}
