package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.email.EmailHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

object CaptureClients {
    fun api() = HttpClient(CIO) {
        followRedirects = false
        expectSuccess = false
        install(ContentNegotiation) { json(EmailHttpClient.json) }
        install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000 }
    }
    // Dedicated transport: no auth plugin, cookies, logging, or redirects to another host.
    fun uploads() = HttpClient(CIO) {
        followRedirects = false
        expectSuccess = false
        install(HttpTimeout) { requestTimeoutMillis = 60_000; connectTimeoutMillis = 10_000 }
    }
}
class CaptureApi(baseUrl: String, private val client: HttpClient, private val uploads: HttpClient, private val token: () -> String) : CaptureGateway {
    private val root = baseUrl.trimEnd('/').also {
        val url = Url(it)
        require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank() && url.user.isNullOrEmpty() && url.password.isNullOrEmpty())
        require(url.encodedPath in listOf("", "/") && url.parameters.isEmpty() && url.fragment.isEmpty())
    } + "/onboarding/id"
    private fun path(id: String): String { require(id.matches(Regex("[a-f0-9]{32}"))); return "$root/captures/$id" }
    private fun HttpRequestBuilder.authorize() { val access = token(); if (access.isBlank()) throw CaptureException(401); bearerAuth(access) }
    @Serializable private data class Failure(val error: String = "request_failed")
    private suspend fun HttpResponse.checked(): HttpResponse {
        if (status.value !in 200..299) throw CaptureException(status.value, runCatching { body<Failure>().error }.getOrDefault("request_failed"))
        return this
    }
    override suspend fun policy(): CapturePolicy = client.get("$root/document-policy") { authorize() }.checked().body()
    override suspend fun current(): CaptureRecord? = client.get("$root/status") { authorize() }.checked().body<CaptureStatus>().capture
    override suspend fun create(request: CreateCapture): CaptureRecord = client.post("$root/captures") { authorize(); contentType(ContentType.Application.Json); setBody(request) }.checked().body()
    override suspend fun get(id: String): CaptureRecord = client.get(path(id)) { authorize() }.checked().body()
    override suspend fun register(id: String, request: RegisterUpload): UploadInstructions = client.post("${path(id)}/uploads") { authorize(); contentType(ContentType.Application.Json); setBody(request) }.checked().body()
    override suspend fun put(instructions: UploadInstructions, jpeg: ByteArray) {
        require(jpeg.size in 4..MAX_JPEG_BYTES)
        val url = Url(instructions.url)
        require(url.protocol == URLProtocol.HTTPS && url.user.isNullOrEmpty() && url.password.isNullOrEmpty() && url.port == 443)
        require(url.host.matches(Regex("[a-z0-9-]+\\.s3\\.[a-z0-9-]+\\.amazonaws\\.com")))
        require(instructions.headers.keys.map(String::lowercase).toSet() == setOf("content-type", "x-amz-checksum-sha256", "x-amz-server-side-encryption"))
        val response = uploads.put(instructions.url) {
            instructions.headers.forEach { (key, value) -> header(key, value) }
            setBody(jpeg)
        }
        if (response.status.value !in 200..299) throw CaptureException(response.status.value, "upload_failed")
    }
    override suspend fun finalize(id: String, request: FinalizeCapture): CaptureRecord = client.post("${path(id)}/finalize") { authorize(); contentType(ContentType.Application.Json); setBody(request) }.checked().body()
    override suspend fun retry(id: String, key: String): CaptureRecord = client.post("${path(id)}/retry-finalization") { authorize(); contentType(ContentType.Application.Json); setBody(OperationKey(key)) }.checked().body()
    override suspend fun cancel(id: String): CaptureRecord = client.post("${path(id)}/cancel") { authorize(); contentType(ContentType.Application.Json); setBody("{}") }.checked().body()
    companion object { const val MAX_JPEG_BYTES = 4 * 1024 * 1024 }
}
