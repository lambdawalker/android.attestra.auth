package com.apexfission.android.attestra.identitymock

import com.apexfission.android.attestra.auth.capture.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

interface MockStateStore { fun read(): String?; fun write(value: String) }
enum class MockScenario(val label: String) {
    READY("Capture ready"), PENDING("File checks remain pending"), INVALID_IMAGE("Recapture required"),
    UPLOAD_FAILURE("Upload fails once"), RESPONSE_LOST("Finalize response lost"), FAILED("File checks fail once"),
}
@Serializable private data class SavedUpload(val request: RegisterUpload, val id: String, val uploaded: Boolean = false)
@Serializable private data class Snapshot(
    val record: CaptureRecord? = null,
    val create: CreateCapture? = null,
    val uploads: Map<String, SavedUpload> = emptyMap(),
    val finalize: FinalizeCapture? = null,
    val polls: Int = 0,
    val failureSent: Boolean = false,
    val scenario: MockScenario = MockScenario.READY,
)
private data class Reply(val status: Int, val body: String)
/** In-process HTTP mock. Persists identifiers/coarse state/checksums, never image bytes or signed URLs. */
class MockCaptureServer(private val store: MockStateStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private var snapshot = store.read()?.let { runCatching { json.decodeFromString<Snapshot>(it) }.getOrNull() } ?: Snapshot()
    var scenario: MockScenario
        get() = snapshot.scenario
        set(value) { snapshot = snapshot.copy(scenario = value); persist() }
    private fun persist() { store.write(json.encodeToString(snapshot)) }
    fun reset() { snapshot = Snapshot(scenario = scenario); persist() }
    fun client() = HttpClient(MockEngine { request ->
        val result = mutex.withLock { handle(request) }
        respond(result.body, HttpStatusCode.fromValue(result.status), headersOf(HttpHeaders.ContentType, "application/json"))
    }) { followRedirects = false; expectSuccess = false; install(ContentNegotiation) { json(this@MockCaptureServer.json) } }
    private fun error(status: Int, code: String) = Reply(status, "{\"error\":\"$code\"}")
    private fun response(record: CaptureRecord, status: Int = 200) = Reply(status, json.encodeToString(record))
    private fun bytes(request: HttpRequestData) = (request.body as OutgoingContent.ByteArrayContent).bytes()
    private inline fun <reified T> body(request: HttpRequestData): T = json.decodeFromString(bytes(request).decodeToString())
    private fun id() = UUID.randomUUID().toString().replace("-", "")
    private fun advance(): CaptureRecord? {
        var r = snapshot.record ?: return null
        if (r.state == "finalizing") {
            val polls = snapshot.polls + 1
            if (polls >= 2 && scenario != MockScenario.PENDING) r = r.copy(revision = r.revision + 1, state = when {
                scenario == MockScenario.INVALID_IMAGE -> "requires_recapture"
                scenario == MockScenario.FAILED && !snapshot.failureSent -> "failed"
                else -> "ready"
            })
            snapshot = snapshot.copy(record = r, polls = polls); persist()
        }
        return r
    }
    private fun handle(request: HttpRequestData): Reply {
        if (request.url.host == "capture-mock.s3.us-east-1.amazonaws.com") {
            if (request.headers[HttpHeaders.Authorization] != null) return error(400, "bearer_leaked")
            val upload = snapshot.uploads.values.firstOrNull { it.id == request.url.encodedPath.trim('/') } ?: return error(404, "not_found")
            if (scenario == MockScenario.UPLOAD_FAILURE && !snapshot.failureSent) { snapshot = snapshot.copy(failureSent = true); persist(); return error(503, "upload_failed") }
            val b = bytes(request)
            val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(b))
            if (b.size.toLong() != upload.request.size || hash != upload.request.sha256 || request.headers["x-amz-checksum-sha256"] != hash) return error(400, "bad_checksum")
            snapshot = snapshot.copy(uploads = snapshot.uploads + (upload.request.key to upload.copy(uploaded = true))); persist()
            return Reply(200, "")
        }
        if (request.url.host != "capture.mock.invalid") return error(404, "not_found")
        if (request.headers[HttpHeaders.Authorization] != "Bearer $TOKEN") return error(401, "sign_in_required")
        val path = request.url.encodedPath.removePrefix("/onboarding/id/")
        if (path == "document-policy") return Reply(200, json.encodeToString(CapturePolicy(true, "capture-v1", "sample_card", listOf("front", "back"), "image/jpeg", 4194304)))
        if (path == "status") return Reply(200, json.encodeToString(CaptureStatus(advance())))
        if (path == "captures" && request.method == HttpMethod.Post) {
            val create = body<CreateCapture>(request)
            snapshot.record?.let { if (snapshot.create == create) return response(it); if (it.state in setOf("uploading", "finalizing")) return error(409, "revision_conflict") }
            val r = CaptureRecord(id(), (snapshot.record?.evidenceVersion ?: 0) + 1, 1, "capture-v1", create.documentType, "uploading", System.currentTimeMillis() / 1000 + 86400)
            snapshot = Snapshot(record = r, create = create, scenario = scenario); persist(); return response(r)
        }
        val parts = path.split('/')
        val r = snapshot.record ?: return error(404, "not_found")
        if (parts.size !in 2..3 || parts[0] != "captures" || parts[1] != r.id) return error(404, "not_found")
        if (parts.size == 2 && request.method == HttpMethod.Get) return response(requireNotNull(advance()))
        if (request.method != HttpMethod.Post) return error(404, "not_found")
        when (parts.last()) {
            "uploads" -> {
                val input = body<RegisterUpload>(request)
                if (r.state != "uploading") return error(409, "revision_conflict")
                var upload = snapshot.uploads[input.key]
                if (upload == null) {
                    if (r.revision != input.revision) return error(409, "revision_conflict")
                    upload = SavedUpload(input, id())
                    snapshot = snapshot.copy(record = r.copy(revision = r.revision + 1, selected = r.selected + (input.slot to upload.id)), uploads = snapshot.uploads + (input.key to upload))
                    persist()
                } else if (upload.request != input || r.selected[input.slot] != upload.id) return error(409, "revision_conflict")
                return Reply(200, json.encodeToString(UploadInstructions(requireNotNull(snapshot.record), upload.id, "https://capture-mock.s3.us-east-1.amazonaws.com/${upload.id}", mapOf("Content-Type" to "image/jpeg", "x-amz-checksum-sha256" to input.sha256, "x-amz-server-side-encryption" to "AES256"), 300)))
            }
            "finalize" -> {
                val input = body<FinalizeCapture>(request)
                if (snapshot.finalize == input) return response(r, 202)
                if (r.state != "uploading" || r.revision != input.revision || r.selected != input.uploads) return error(409, "revision_conflict")
                if (input.uploads.keys != setOf("front", "back") || input.uploads.values.any { id -> snapshot.uploads.values.none { it.id == id && it.uploaded } }) return error(409, "uploads_incomplete")
                val next = r.copy(state = "finalizing", revision = r.revision + 1)
                snapshot = snapshot.copy(record = next, finalize = input); persist()
                if (scenario == MockScenario.RESPONSE_LOST && !snapshot.failureSent) { snapshot = snapshot.copy(failureSent = true); persist(); throw IOException("Simulated response loss") }
                return response(next, 202)
            }
            "retry-finalization" -> { if (r.state != "failed") return response(r); snapshot = snapshot.copy(record = r.copy(state = "finalizing", revision = r.revision + 1), polls = 0, failureSent = true); persist(); return response(requireNotNull(snapshot.record), 202) }
            "cancel" -> { snapshot = snapshot.copy(record = r.copy(state = "cancelled", revision = r.revision + 1), uploads = emptyMap()); persist(); return response(requireNotNull(snapshot.record)) }
        }
        return error(404, "not_found")
    }
    companion object { const val ORIGIN = "https://capture.mock.invalid"; const val TOKEN = "mock-capture-only" }
}
