package com.apexfission.android.attestra.identitymock

import com.apexfission.android.attestra.auth.identity.*
import com.apexfission.android.attestra.auth.ui.onboarding.id.IdentityDetails
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

interface MockStateStore { fun read(): String?; fun write(value: String) }
enum class MockScenario(val label: String) {
    APPROVED("Approved"), PENDING("Stays pending"), REJECTED("Rejected · no retry"),
    INCONCLUSIVE("Inconclusive · retry allowed"), UNREADABLE("Unreadable document"),
    SUBMISSION_FAILURE("Submission fails once"), RESPONSE_LOST("Accepted · response lost"),
}
@Serializable private data class StoredSubmission(
    val record: IdentityRecord,
    val scenario: MockScenario,
    val uploads: Set<DocumentSide> = emptySet(),
    val extracted: Boolean = false,
    val submissionHash: String? = null,
    val polls: Int = 0,
    val failureSent: Boolean = false,
)
@Serializable private data class MockSnapshot(
    val scenario: MockScenario = MockScenario.APPROVED,
    val records: Map<String, StoredSubmission> = emptyMap(),
)
private data class Reply(val status: Int, val body: String = "")

/** Local transport, not a listening server. Only identifiers/coarse state/hashes reach the store. */
class MockIdentityServer(private val store: MockStateStore, initialScenario: MockScenario? = null) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val restored = store.read()?.let { json.decodeFromString<MockSnapshot>(it) } ?: MockSnapshot()
    private var records: Map<String, StoredSubmission> = restored.records
    var scenario: MockScenario = initialScenario ?: restored.scenario
        set(value) { synchronized(this) { field = value; persist() } }
    private fun persist() { store.write(json.encodeToString(MockSnapshot(scenario, records))) }

    fun client() = HttpClient(MockEngine { request ->
        delay(300)
        val result = mutex.withLock { handle(request) }
        respond(result.body, HttpStatusCode.fromValue(result.status), headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        expectSuccess = false
        install(ContentNegotiation) { json(this@MockIdentityServer.json) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    }
    private fun save(id: String, value: StoredSubmission) {
        records = records + (id to value)
        persist()
    }
    private fun response(status: Int, value: IdentityRecord) = Reply(status, json.encodeToString(value))
    private fun error(status: Int) = Reply(status, "{\"error\":\"mock_request_failed\"}")
    @Synchronized
    private fun handle(request: HttpRequestData): Reply {
        if (request.url.host != "identity.mock.invalid") return error(404)
        if (request.headers[HttpHeaders.Authorization] != "Bearer $TOKEN") return error(401)
        val parts = request.url.encodedPath.trim('/').split('/')
        if (parts.take(3) != listOf("identity", "v1", "submissions") || parts.size < 4) return error(404)
        val id = parts[3]
        if (runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false).not()) return error(400)
        val old = records[id]
        val verb = request.method
        if (parts.size == 4 && verb == HttpMethod.Put) {
            if (old != null) return response(200, old.record)
            val next = IdentityRecord(id, (records.values.maxOfOrNull { it.record.evidenceVersion } ?: 0) + 1)
            save(id, StoredSubmission(next, scenario))
            return response(201, next)
        }
        if (old == null) return error(404)
        if (parts.size == 4 && verb == HttpMethod.Get) {
            if (!old.record.accepted) return response(200, old.record)
            val polls = old.polls + 1
            val outcome = if (polls < 2) IdentityOutcome.PENDING else when (old.scenario) {
                MockScenario.PENDING -> IdentityOutcome.PENDING
                MockScenario.REJECTED -> IdentityOutcome.REJECTED
                MockScenario.INCONCLUSIVE -> IdentityOutcome.INCONCLUSIVE
                else -> IdentityOutcome.APPROVED
            }
            val record = old.record.copy(
                decision = outcome, autoReport = IdentityOutcome.APPROVED, thirdParty = outcome,
                canRetry = outcome == IdentityOutcome.INCONCLUSIVE,
            )
            save(id, old.copy(record = record, polls = polls))
            return response(200, record)
        }
        val bytes = (request.body as? OutgoingContent.ByteArrayContent)?.bytes() ?: return error(400)
        if (parts.size == 6 && parts[4] == "assets" && verb == HttpMethod.Put) {
            if (old.record.accepted) return error(409)
            if (request.url.parameters["evidence_version"]?.toIntOrNull() != old.record.evidenceVersion) return error(409)
            val side = DocumentSide.entries.firstOrNull { it.name.lowercase() == parts[5] } ?: return error(400)
            if (bytes.size !in 4..IdentityApi.MAX_JPEG_BYTES || bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte()) return error(422)
            // Bytes are inspected then discarded. No document image is written to the mock store.
            save(id, old.copy(uploads = old.uploads + side))
            return Reply(204)
        }
        if (parts.size != 5 || verb != HttpMethod.Post) return error(404)
        if (parts[4] == "extraction") {
            val body = runCatching { json.decodeFromString<EvidenceVersion>(bytes.decodeToString()) }.getOrNull() ?: return error(400)
            if (old.record.accepted || body.evidenceVersion != old.record.evidenceVersion) return error(409)
            if (old.uploads.size != 2) return error(422)
            save(id, old.copy(extracted = true))
            return Reply(200, json.encodeToString(Extraction(old.scenario != MockScenario.UNREADABLE, fixture)))
        }
        if (parts[4] != "submit") return error(404)
        val body = runCatching { json.decodeFromString<SubmitIdentity>(bytes.decodeToString()) }.getOrNull() ?: return error(400)
        if (body.evidenceVersion != old.record.evidenceVersion || request.headers["Idempotency-Key"] != id) return error(409)
        if (old.uploads.size != 2 || !old.extracted || old.scenario == MockScenario.UNREADABLE || body.extracted != fixture) return error(422)
        if (body.corrected.fullName.isBlank() || body.corrected.dateOfBirth.isBlank() || body.corrected.address.isBlank()) return error(422)
        val hash = MessageDigest.getInstance("SHA-256").digest(json.encodeToString(body).toByteArray()).joinToString("") { "%02x".format(it) }
        if (old.record.accepted) return if (old.submissionHash == hash) response(200, old.record) else error(409)
        if (old.scenario == MockScenario.SUBMISSION_FAILURE && !old.failureSent) {
            save(id, old.copy(failureSent = true)); return error(503)
        }
        val record = old.record.copy(accepted = true, decision = IdentityOutcome.PENDING, autoReport = IdentityOutcome.PENDING, thirdParty = IdentityOutcome.PENDING)
        save(id, old.copy(record = record, submissionHash = hash, failureSent = true))
        if (old.scenario == MockScenario.RESPONSE_LOST && !old.failureSent) throw IOException("Simulated response loss")
        return response(202, record)
    }
    companion object {
        const val ORIGIN = "https://identity.mock.invalid"
        const val TOKEN = "local-mock-session"
        private val fixture = IdentityDetails(
            fullName = "SAMPLE PERSON", dateOfBirth = "1990-01-02", address = "123 Example Street, Sample City",
            documentNumber = "SAMPLE-0001", issuingCountry = "TEST", documentType = "Sample two-sided ID", expirationDate = "2030-01-01",
        )
    }
}
