package com.apexfission.android.attestra.auth.capture

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class DisclosureMessagesTest {
    @Test fun selectsLanguageAndFallsBackToEnglish() {
        val messages = DisclosureMessages("English", "Español", "Français")
        assertEquals("Español", messages.text("es-MX"))
        assertEquals("Français", messages.text("fr-CA"))
        assertEquals("en", messages.language("de"))
        assertEquals("English", messages.text("de"))
        assertEquals("en", DisclosureMessages("English").language("es"))
    }
    @Test fun languageAndOriginSurviveWireRoundTrip() {
        val intent = CreateCapture("operation_12345678", "id_card", "policy-v1", "es")
        assertEquals(intent, Json.decodeFromString<CreateCapture>(Json.encodeToString(intent)))
        val review = ReviewCapture(2, CardData(issuingCountry = "US"), "MX")
        assertEquals(review, Json.decodeFromString<ReviewCapture>(Json.encodeToString(review)))
        assertEquals("", Json.decodeFromString<ReviewedCard>("""{"data":{},"saved_at":1}""").originCountry)
    }
}
