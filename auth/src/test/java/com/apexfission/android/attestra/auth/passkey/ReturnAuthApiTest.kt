package com.apexfission.android.attestra.auth.passkey

import com.apexfission.android.attestra.auth.email.EmailHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import io.ktor.http.content.TextContent
import org.junit.Test

class ReturnAuthApiTest {
    @Test fun passwordRequestsUseDedicatedEndpointsAndBearerOnlyForEnrollment() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            val body = (request.body as TextContent).text
            when (request.url.encodedPath) {
                "/auth/password/start" -> {
                    assertFalse(body.contains("password"))
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    respond("""{"session":"challenge"}""")
                }
                "/auth/password/complete" -> {
                    assertTrue(body.contains("\"password\":\"example-secret\""))
                    assertTrue(body.contains("\"session\":\"challenge\""))
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    respond("""{"access_token":"access","id_token":"id","refresh_token":"refresh","expires_in":3600,"token_type":"Bearer"}""")
                }
                "/auth/password/set" -> {
                    assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                    assertEquals("""{"password":"example-secret"}""", body)
                    respond("""{"password_registered":true}""")
                }
                else -> error("Unexpected request")
            }
        }) { install(ContentNegotiation) { json(EmailHttpClient.json) } }
        try {
            val api = ReturnAuthApi("https://example.com", client)
            assertEquals("challenge", api.startPassword("person@example.test").session)
            assertEquals("access", api.finishPassword("person@example.test", "challenge", "example-secret").accessToken)
            assertTrue(api.setPassword("access", "example-secret").passwordRegistered)
        } finally { client.close() }
    }
    @Test fun legacyStatusCannotAssertNewCompletionFacts() {
        val status = EmailHttpClient.json.decodeFromString<AccountStatus>("""{"passkey_registered":true}""")
        assertTrue(status.passkeyRegistered)
        assertFalse(status.passwordRegistered)
        assertFalse(status.idCaptured)
        assertFalse(status.onboardingComplete)
    }
    @Test fun passkeyChallengeAndAuthenticatedStatus() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/passkey/start" -> respond("""{"session":"challenge","options":"{}"}""", HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"))
                "/auth/status" -> {
                    assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                    respond("""{"passkey_registered":true}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> error("Unexpected request")
            }
        }) { install(ContentNegotiation) { json(EmailHttpClient.json) } }
        val api = ReturnAuthApi("https://example.com", client)
        assertEquals("challenge", api.start("person@example.test", true).session)
        assertTrue(api.status("access").passkeyRegistered)
        client.close()
    }
}
