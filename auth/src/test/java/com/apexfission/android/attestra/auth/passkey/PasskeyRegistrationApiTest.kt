package com.apexfission.android.attestra.auth.passkey

import com.apexfission.android.attestra.auth.email.EmailHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import io.ktor.serialization.kotlinx.json.json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasskeyRegistrationApiTest {
    @Test fun optionsAndCompletionUseBearerAndCredentialObject() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            assertEquals("Bearer test.access-token", request.headers[HttpHeaders.Authorization])
            paths += request.url.encodedPath
            when (paths.size) {
                1 -> respond("""{"creation_options":{"challenge":"abc","rp":{"id":"attestrabond.com"}}}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> {
                    respond("""{"registered":true}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        }
        val client = HttpClient(engine) { install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json(EmailHttpClient.json) } }
        val api = PasskeyRegistrationApi("https://example.com", client)
        assertTrue(api.options("test.access-token").contains("attestrabond.com"))
        api.complete("test.access-token", """{"id":"key","type":"public-key","response":{}}""")
        assertEquals(listOf("/passkeys/options", "/passkeys/complete"), paths)
        client.close()
    }

    @Test fun expiredTokenDoesNotComplete() = runBlocking {
        val client = HttpClient(MockEngine { respond("""{"error":"sign_in_required"}""", HttpStatusCode.Unauthorized) })
        val error = runCatching { PasskeyRegistrationApi("https://example.com", client).options("expired") }.exceptionOrNull()
        assertTrue(error is PasskeyApiException && error.signInRequired)
        client.close()
    }
}
