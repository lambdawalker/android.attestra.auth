package com.apexfission.android.attestra.auth.discovery

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

const val DEFAULT_INDEX_URL = "https://index.attestrabond.com/v1/environments"

@Serializable data class EnvironmentFeatures(val idCapture: Boolean = false)
@Serializable data class EnvironmentConfiguration(
    val apiUrl: String,
    val awsRegion: String,
    val cognitoUserPoolId: String,
    val cognitoClientId: String,
    val features: EnvironmentFeatures = EnvironmentFeatures(),
)
@Serializable data class EnvironmentEntry(
    val id: String,
    val configuration: EnvironmentConfiguration,
    val configHash: String,
    val revision: Long,
    val updatedAt: String,
)
@Serializable private data class IndexDocument(val schemaVersion: Int, val environments: List<EnvironmentEntry>)
@Serializable private data class CachedIndex(val indexUrl: String, val environment: String, val savedAt: Long, val body: String)

/** Public configuration only. Implementations must keep each index/environment key isolated. */
interface EnvironmentCache {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}
data class ResolvedEnvironment(val entry: EnvironmentEntry, val fromCache: Boolean)
class EnvironmentDiscoveryException(message: String) : Exception(message)

fun environmentIndexClient() = HttpClient(CIO) {
    followRedirects = false
    expectSuccess = false
    install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
}

/** Resolve once before constructing authenticated clients; never switch hosts mid-session. */
class EnvironmentIndex(
    private val client: HttpClient,
    private val cache: EnvironmentCache,
    private val now: () -> Long = System::currentTimeMillis,
    private val maxCacheAgeMillis: Long = 24 * 60 * 60 * 1000L,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolve(indexUrl: String = DEFAULT_INDEX_URL, environment: String): ResolvedEnvironment {
        requireHttps(indexUrl)
        require(environment.matches(Regex("[a-z][a-z0-9-]{0,31}"))) { "Invalid environment name" }
        require(maxCacheAgeMillis >= 0)
        val key = "$indexUrl|$environment"
        val response = try { client.get(indexUrl) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return fallback(key, indexUrl, environment) }
        if (response.status.value == 429 || response.status.value in 500..599) return fallback(key, indexUrl, environment)
        if (response.status.value != 200) {
            cache.remove(key)
            throw EnvironmentDiscoveryException("Environment index rejected the request (${response.status.value}).")
        }
        val body = try { response.bodyAsText() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return fallback(key, indexUrl, environment) }
        val entry = try { parse(body, environment) } catch (e: Exception) {
            cache.remove(key)
            throw EnvironmentDiscoveryException("Environment is missing or its published configuration is invalid.")
        }
        // Cache failure must not prevent using a valid online response.
        runCatching { cache.write(key, json.encodeToString(CachedIndex(indexUrl, environment, now(), body))) }
        return ResolvedEnvironment(entry, false)
    }

    private fun fallback(key: String, indexUrl: String, environment: String): ResolvedEnvironment {
        val cached = runCatching {
            val saved = json.decodeFromString<CachedIndex>(requireNotNull(cache.read(key)))
            require(saved.indexUrl == indexUrl && saved.environment == environment)
            require(now() - saved.savedAt in 0..maxCacheAgeMillis)
            parse(saved.body, environment)
        }.getOrNull() ?: throw EnvironmentDiscoveryException("Environment index is unavailable and there is no recent valid cached configuration. Retry when connected.")
        return ResolvedEnvironment(cached, true)
    }

    private fun parse(body: String, environment: String): EnvironmentEntry {
        require(body.length <= 1_048_576)
        val document = json.decodeFromString<IndexDocument>(body)
        require(document.schemaVersion == 1)
        require(document.environments.map { it.id }.distinct().size == document.environments.size)
        val entry = document.environments.single { it.id == environment }
        require(entry.revision > 0 && entry.configHash.matches(Regex("[a-f0-9]{64}")))
        val c = entry.configuration
        requireHttps(c.apiUrl)
        require(c.awsRegion.matches(Regex("[a-z]{2}(?:-[a-z]+)+-[0-9]+")))
        require(c.cognitoUserPoolId.startsWith(c.awsRegion + "_") && c.cognitoUserPoolId.substringAfter('_').isNotBlank())
        require(c.cognitoClientId.matches(Regex("[a-zA-Z0-9]+")))
        return entry
    }
}

private fun requireHttps(url: String) {
    val uri = URI(url)
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) {
        "Configuration endpoints must use HTTPS without credentials, queries or fragments"
    }
}
