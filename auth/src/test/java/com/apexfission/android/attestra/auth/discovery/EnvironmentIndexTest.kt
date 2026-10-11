package com.apexfission.android.attestra.auth.discovery

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EnvironmentIndexTest {
    private class Cache : EnvironmentCache {
        val values = mutableMapOf<String, String>()
        override fun read(key: String) = values[key]
        override fun write(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }
    private val doc = """{"schemaVersion":1,"environments":[{"id":"qa","configuration":{"apiUrl":"https://qa.api.attestrabond.com","awsRegion":"us-east-2","cognitoUserPoolId":"us-east-2_pool","cognitoClientId":"client","features":{"idCapture":true}},"configHash":"${"a".repeat(64)}","revision":2,"updatedAt":"2026-10-11T00:00:00Z"}]}"""

    @Test fun resolvesBackendContractAndScopesCache() = runBlocking {
        val cache = Cache()
        var offline = false
        val client = HttpClient(MockEngine { request ->
            assertEquals("/v1/environments", request.url.encodedPath)
            assertNull(request.headers["Authorization"])
            if (offline) throw java.io.IOException("offline")
            respond(doc)
        })
        try {
            val resolver = EnvironmentIndex(client, cache, { 100 })
            val result = resolver.resolve(environment = "qa")
            assertEquals("https://qa.api.attestrabond.com", result.entry.configuration.apiUrl)
            assertTrue(result.entry.configuration.features.idCapture)
            assertFalse(result.fromCache)
            offline = true
            assertTrue(resolver.resolve(environment = "qa").fromCache)
            fails { resolver.resolve(environment = "dev") }
            fails { resolver.resolve("https://other.example/v1/environments", "qa") }
        } finally { client.close() }
    }

    @Test fun staleAndFutureCachesAreRejected() = runBlocking {
        val cache = Cache()
        var offline = false
        var now = 100L
        val client = HttpClient(MockEngine { if (offline) throw java.io.IOException(); respond(doc) })
        try {
            val resolver = EnvironmentIndex(client, cache, { now }, 50)
            resolver.resolve(environment = "qa")
            offline = true
            now = 151
            fails { resolver.resolve(environment = "qa") }
            now = 99
            fails { resolver.resolve(environment = "qa") }
        } finally { client.close() }
    }

    @Test fun removalAndInvalidDocumentsInvalidateCachedEnvironment() = runBlocking {
        for (invalid in listOf("{}", doc.replace("https://qa.api", "http://qa.api"),
            doc.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
            """{"schemaVersion":1,"environments":[]}""")) {
            val cache = Cache()
            var body = doc
            val client = HttpClient(MockEngine { respond(body) })
            try {
                val resolver = EnvironmentIndex(client, cache)
                resolver.resolve(environment = "qa")
                body = invalid
                fails { resolver.resolve(environment = "qa") }
                assertTrue(cache.values.isEmpty())
            } finally { client.close() }
        }
    }

    @Test fun onlyTransientHttpFailuresUseCache() = runBlocking {
        val cache = Cache()
        var status = HttpStatusCode.OK
        val client = HttpClient(MockEngine { respond(doc, status) })
        try {
            val resolver = EnvironmentIndex(client, cache)
            resolver.resolve(environment = "qa")
            status = HttpStatusCode.ServiceUnavailable
            assertTrue(resolver.resolve(environment = "qa").fromCache)
            status = HttpStatusCode.Forbidden
            fails { resolver.resolve(environment = "qa") }
            assertTrue(cache.values.isEmpty())
        } finally { client.close() }
    }

    @Test fun cancellationIsNotHiddenByCache() = runBlocking {
        val client = HttpClient(MockEngine { throw CancellationException("cancelled") })
        try {
            try {
                EnvironmentIndex(client, Cache()).resolve(environment = "qa")
                fail("Expected cancellation")
            } catch (_: CancellationException) { }
        } finally { client.close() }
    }

    private suspend fun fails(block: suspend () -> Unit) {
        try { block(); fail("Expected discovery failure") }
        catch (_: EnvironmentDiscoveryException) { }
    }
}
