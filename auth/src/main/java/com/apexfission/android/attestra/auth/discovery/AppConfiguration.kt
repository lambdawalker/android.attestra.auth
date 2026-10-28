package com.apexfission.android.attestra.auth.discovery

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonNull
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant

@Serializable data class RuntimeFeatures(val idCapture: Boolean)
@Serializable data class RuntimeCapturePolicy(val documentType: String, val jurisdiction: String, val purpose: String)
@Serializable data class RuntimeConfiguration(
    val schemaVersion: Int, val environment: String, val revision: Long,
    val updatedAt: String, val features: RuntimeFeatures, val capture: RuntimeCapturePolicy,
    val onboarding: OnboardingPolicy? = null,
) {
    val effectiveOnboarding: OnboardingPolicy get() = onboarding ?: OnboardingPolicy.legacy(features.idCapture)
}
@Serializable private data class CachedRuntime(val savedAt: Long, val body: String)
enum class RuntimeSource { LIVE, CACHED, UNAVAILABLE }
data class AppConfigurationSnapshot(val document: RuntimeConfiguration? = null, val source: RuntimeSource = RuntimeSource.UNAVAILABLE, val validUntilMillis: Long? = null) {
    val captureEnabled: Boolean get() = document?.features?.idCapture == true
}

/** Public runtime settings only: no bearer token, host switching, or authentication reset. */
class AppConfiguration(
    private val client: HttpClient,
    private val cache: EnvironmentCache,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun resolve(apiUrl: String, environment: String): AppConfigurationSnapshot {
        val uri = URI(apiUrl)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null)
        require(uri.path in listOf("", "/") && environment.matches(Regex("[a-z][a-z0-9-]{0,31}")))
        val endpoint = apiUrl.trimEnd('/') + "/v1/app-config"
        val key = "runtime|$endpoint|$environment"
        return try {
            client.prepareGet(endpoint).execute { response ->
                when {
                    response.status.value == 429 || response.status.value in 500..599 -> fallback(key, environment)
                    response.status.value != 200 -> invalid(key)
                    else -> {
                        val channel = response.bodyAsChannel()
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (true) {
                            val count = channel.readAvailable(buffer, 0, buffer.size)
                            if (count < 0) break
                            if (output.size() + count > MAX_BYTES) { channel.cancel(null); return@execute invalid(key) }
                            output.write(buffer, 0, count)
                        }
                        val body = output.toString("UTF-8")
                        val document = runCatching { parse(body, environment) }.getOrNull() ?: return@execute invalid(key)
                        val savedAt = now()
                        runCatching { cache.write(key, json.encodeToString(CachedRuntime(savedAt, body))) }
                        AppConfigurationSnapshot(document, RuntimeSource.LIVE, savedAt + MAX_AGE_MILLIS)
                    }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { fallback(key, environment) }
    }
    private fun invalid(key: String): AppConfigurationSnapshot {
        runCatching { cache.remove(key) }
        return AppConfigurationSnapshot()
    }
    private fun fallback(key: String, environment: String): AppConfigurationSnapshot = runCatching {
        val saved = json.decodeFromString<CachedRuntime>(requireNotNull(cache.read(key)))
        require(now() - saved.savedAt in 0 until MAX_AGE_MILLIS)
        AppConfigurationSnapshot(parse(saved.body, environment), RuntimeSource.CACHED, saved.savedAt + MAX_AGE_MILLIS)
    }.getOrElse { invalid(key) }
    private fun parse(body: String, environment: String): RuntimeConfiguration {
        require(body.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val objectValue = json.parseToJsonElement(body).jsonObject
        require("onboarding" !in objectValue || objectValue["onboarding"] != JsonNull)
        objectValue["onboarding"]?.jsonObject?.get("passwordPolicy")?.jsonObject?.let { rules ->
            require(rules.keys.containsAll(listOf("minLength", "maxLength", "lowercase", "uppercase", "numbers", "symbols")))
        }
        return json.decodeFromString<RuntimeConfiguration>(body).also {
            it.effectiveOnboarding.validate()
            if (it.onboarding != null) require(it.features.idCapture == (it.onboarding.idCapture != Requirement.DISABLED))
            require(it.schemaVersion == 1 && it.environment == environment && it.revision >= 0)
            require(it.updatedAt.isNotBlank() || it.revision == 0L)
            if (it.updatedAt.isNotBlank()) Instant.parse(it.updatedAt)
            require(it.capture.documentType.matches(Regex("[a-z][a-z0-9_]{0,63}")))
            for (text in listOf(it.capture.jurisdiction, it.capture.purpose)) {
                require(text.toByteArray(Charsets.UTF_8).size <= 1024 && text.none(Char::isISOControl))
            }
            if (it.features.idCapture) require(it.capture.documentType != "sample_card" && it.capture.purpose.isNotBlank())
        }
    }
    companion object { const val MAX_AGE_MILLIS = 15 * 60 * 1000L; const val MAX_BYTES = 65_536 }
}
