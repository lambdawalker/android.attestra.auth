package com.apexfission.android.auth.email

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailVerificationApiTest {
    @Test fun signupSendsOnlyChallengeAndReceivesOpaqueRequestId() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("https://api.example.test/signup", request.url.toString())
            val body = (request.body as TextContent).text
            assertTrue(body.contains("code_challenge"))
            assertTrue(body.contains("\"code_challenge_method\":\"S256\""))
            assertFalse(body.contains("token_a"))
            respond("""{"request_id":"request-1"}""", HttpStatusCode.Accepted, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(EmailHttpClient.json) } }
        val api = EmailVerificationApi("https://api.example.test", client)
        assertEquals("request-1", api.signup("person@example.test", "challenge"))
        client.close()
    }

    @Test fun originalDeviceCodeSendsAPlusCWithoutLinkToken() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("https://api.example.test/confirm", request.url.toString())
            val body = (request.body as TextContent).text
            assertTrue(body.contains("\"token_a\":\"local-a\""))
            assertTrue(body.contains("\"token_c\":\"000123\""))
            assertFalse(body.contains("token_b"))
            respond(
                """{"access_token":"access","id_token":"id","refresh_token":"refresh","expires_in":3600,"token_type":"Bearer"}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { install(ContentNegotiation) { json(EmailHttpClient.json) } }
        val api = EmailVerificationApi("https://api.example.test", client)
        assertEquals("access", api.confirmLocalCode("request-1", "local-a", "000123").accessToken)
        client.close()
    }

    @Test fun wrongCodeReturnsRemainingAttemptsWithoutLeakingResponse() = runBlocking {
        val client = HttpClient(MockEngine {
            respond("""{"error":"incorrect_code","attempts_remaining":2}""", HttpStatusCode.UnprocessableEntity, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(EmailHttpClient.json) } }
        val api = EmailVerificationApi("https://api.example.test", client)
        try {
            api.confirmCode("request-1", "link-b", "000123")
            throw AssertionError("Expected incorrect code")
        } catch (e: EmailApiError) {
            assertEquals(EmailApiError.Kind.INCORRECT_CODE, e.kind)
            assertEquals(2, e.attemptsRemaining)
        }
        client.close()
    }
}
