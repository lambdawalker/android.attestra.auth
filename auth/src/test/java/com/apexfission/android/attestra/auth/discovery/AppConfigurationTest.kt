package com.apexfission.android.attestra.auth.discovery

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AppConfigurationTest {
    private class Cache : EnvironmentCache {
        val values = mutableMapOf<String, String>()
        override fun read(key: String) = values[key]
        override fun write(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }
    private val doc = """{"schemaVersion":1,"environment":"qa","revision":1,"updatedAt":"2026-10-27T00:00:00Z","features":{"idCapture":true},"capture":{"documentType":"driver_license","jurisdiction":"US","purpose":"Onboarding"}}"""
    private val url = "https://qa.api.attestrabond.com"
    @Test fun refreshAndTransientFallbackAreBoundedAndEnvironmentScoped() = runBlocking {
        var now = 100L
        var offline = false
        val client = HttpClient(MockEngine { request ->
            assertEquals("/v1/app-config", request.url.encodedPath)
            assertNull(request.headers["Authorization"])
            if (offline) throw java.io.IOException()
            respond(doc)
        })
        try {
            val resolver = AppConfiguration(client, Cache(), { now })
            assertTrue(resolver.resolve(url, "qa").captureEnabled)
            offline = true
            assertEquals(RuntimeSource.CACHED, resolver.resolve(url, "qa").source)
            assertFalse(resolver.resolve(url, "dev").captureEnabled)
            assertFalse(resolver.resolve("https://other.example", "qa").captureEnabled)
            now += AppConfiguration.MAX_AGE_MILLIS
            assertFalse(resolver.resolve(url, "qa").captureEnabled)
        } finally { client.close() }
    }
    @Test fun invalidOrOversizeDocumentRevokesCachedEnablement() = runBlocking {
        for (bad in listOf("{}", doc.replace("\"schemaVersion\":1", "\"schemaVersion\":2"), doc.replace("driver_license", "sample_card"), doc.replace("\"qa\"", "\"dev\""), " ".repeat(65_537))) {
            var body = doc
            val cache = Cache()
            val client = HttpClient(MockEngine { respond(body) })
            try {
                val resolver = AppConfiguration(client, cache)
                assertTrue(resolver.resolve(url, "qa").captureEnabled)
                body = bad
                assertFalse(resolver.resolve(url, "qa").captureEnabled)
                assertTrue(cache.values.isEmpty())
            } finally { client.close() }
        }
    }
    @Test fun genericCaptureAllowsEmptyJurisdictionAndLegacyPolicyIsExplicit() = runBlocking {
        val client = HttpClient(MockEngine { respond(doc.replace("\"US\"", "\"\"")) })
        try {
            val policy = AppConfiguration(client, Cache()).resolve(url, "qa").document!!.effectiveOnboarding
            assertEquals(Requirement.OPTIONAL, policy.idCapture)
            assertEquals(Requirement.REQUIRED, policy.passkey)
            assertEquals(Requirement.DISABLED, policy.password)
        } finally { client.close() }
    }
    @Test fun malformedOnboardingCannotFallBackToLegacy() = runBlocking {
        for (policy in listOf("null", "{}", "{\"idCapture\":\"sometimes\"}")) {
            val client = HttpClient(MockEngine { respond(doc.dropLast(1) + ",\"onboarding\":" + policy + "}") })
            try { assertEquals(RuntimeSource.UNAVAILABLE, AppConfiguration(client, Cache()).resolve(url, "qa").source) }
            finally { client.close() }
        }
    }
    @Test fun explicitPolicyValidatesAllFieldsAndCompatibilityFlag() = runBlocking {
        val policy = """{"idCapture":"required","passkey":"optional","password":"required","passwordPlacement":"with_email","passwordPolicy":{"minLength":12,"maxLength":128,"lowercase":"allowed","uppercase":"required","numbers":"disallowed","symbols":"allowed"}}"""
        val valid = doc.dropLast(1) + ",\"onboarding\":" + policy + "}"
        val invalid = listOf(valid.replace("with_email", "hidden"), valid.replace("\"idCapture\":true", "\"idCapture\":false"),
            valid.replace("\"minLength\":12,", ""), valid.replace("\"minLength\":12", "\"minLength\":7"),
            valid.replace("\"uppercase\":\"required\"", "\"uppercase\":\"unknown\""))
        for (body in listOf(valid) + invalid) {
            val client = HttpClient(MockEngine { respond(body) })
            try {
                val snapshot = AppConfiguration(client, Cache()).resolve(url, "qa")
                assertEquals(if (body == valid) RuntimeSource.LIVE else RuntimeSource.UNAVAILABLE, snapshot.source)
            } finally { client.close() }
        }
    }
    @Test fun onlyTransientStatusesUseCache() = runBlocking {
        var status = HttpStatusCode.OK
        val client = HttpClient(MockEngine { respond(doc, status) })
        try {
            val resolver = AppConfiguration(client, Cache())
            resolver.resolve(url, "qa")
            status = HttpStatusCode.ServiceUnavailable
            assertTrue(resolver.resolve(url, "qa").captureEnabled)
            status = HttpStatusCode.Forbidden
            assertFalse(resolver.resolve(url, "qa").captureEnabled)
        } finally { client.close() }
    }
    @Test fun futureCacheFailsClosed() = runBlocking {
        var now = 100L
        var offline = false
        val client = HttpClient(MockEngine { if (offline) throw java.io.IOException(); respond(doc) })
        try {
            val resolver = AppConfiguration(client, Cache(), { now })
            resolver.resolve(url, "qa")
            now = 99; offline = true
            assertFalse(resolver.resolve(url, "qa").captureEnabled)
        } finally { client.close() }
    }
}
