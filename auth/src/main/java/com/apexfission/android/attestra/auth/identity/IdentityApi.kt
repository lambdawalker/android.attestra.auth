package com.apexfission.android.attestra.auth.identity

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import java.util.UUID

class IdentityApiException(val status: Int) : Exception("Identity request failed ($status)")

/** Draft contract. Inject a mock engine until the Go identity service is deployed. No body logging. */
class IdentityApi(baseUrl: String, private val client: HttpClient, private val token: () -> String) : IdentityGateway {
    private val root = baseUrl.trimEnd('/').also {
        val url = Url(it)
        require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank() && url.user.isNullOrEmpty() && url.password.isNullOrEmpty())
        require(url.encodedPath in listOf("", "/") && url.parameters.isEmpty() && url.fragment.isEmpty())
    } + "/identity/v1/submissions"

    private fun path(id: String): String {
        require(UUID.fromString(id).toString() == id) { "Submission ID must be a canonical UUID" }
        return "$root/$id"
    }
    private fun HttpRequestBuilder.authorize() {
        val access = token()
        if (access.isBlank()) throw IdentityApiException(401)
        bearerAuth(access)
    }
    private suspend fun HttpResponse.checked(vararg expected: Int): HttpResponse {
        if (status.value !in expected) throw IdentityApiException(status.value)
        return this
    }
    override suspend fun create(id: String): IdentityRecord = client.put(path(id)) {
        authorize(); contentType(ContentType.Application.Json); setBody("{}")
    }.checked(200, 201).body()

    override suspend fun upload(id: String, version: Int, side: DocumentSide, jpeg: ByteArray) {
        require(jpeg.isNotEmpty() && jpeg.size <= MAX_JPEG_BYTES)
        client.put("${path(id)}/assets/${side.name.lowercase()}") {
            authorize(); parameter("evidence_version", version)
            contentType(ContentType.Image.JPEG); setBody(jpeg)
        }.checked(204)
    }
    override suspend fun extract(id: String, version: Int): Extraction = client.post("${path(id)}/extraction") {
        authorize(); contentType(ContentType.Application.Json); setBody(EvidenceVersion(version))
    }.checked(200).body()

    override suspend fun submit(id: String, body: SubmitIdentity): IdentityRecord = client.post("${path(id)}/submit") {
        authorize(); header("Idempotency-Key", id)
        contentType(ContentType.Application.Json); setBody(body)
    }.checked(200, 202).body()

    override suspend fun status(id: String): IdentityRecord? {
        val response = client.get(path(id)) { authorize() }
        return if (response.status.value == 404) null else response.checked(200).body()
    }
    companion object { const val MAX_JPEG_BYTES = 4 * 1024 * 1024 }
}
