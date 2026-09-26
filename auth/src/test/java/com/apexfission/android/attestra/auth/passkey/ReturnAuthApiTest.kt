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
import org.junit.Test

class ReturnAuthApiTest {
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
