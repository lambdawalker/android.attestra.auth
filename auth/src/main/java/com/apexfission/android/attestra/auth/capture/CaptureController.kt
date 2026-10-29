package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.identity.DocumentSide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.security.MessageDigest
import java.util.UUID

// java.util.Base64 requires API 26; this encoding keeps the auth library on API 24.
internal fun checksum(data: ByteArray): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val bytes = MessageDigest.getInstance("SHA-256").digest(data)
    return buildString {
        for (i in bytes.indices step 3) {
            val a = bytes[i].toInt() and 255
            val b = bytes.getOrNull(i + 1)?.toInt()?.and(255) ?: 0
            val c = bytes.getOrNull(i + 2)?.toInt()?.and(255) ?: 0
            append(alphabet[a shr 2]); append(alphabet[((a and 3) shl 4) or (b shr 4)])
            append(if (i + 1 < bytes.size) alphabet[((b and 15) shl 2) or (c shr 6)] else '=')
            append(if (i + 2 < bytes.size) alphabet[c and 63] else '=')
        }
    }
}
data class CaptureUi(
    val policy: CapturePolicy? = null,
    val record: CaptureRecord? = null,
    val busy: Boolean = false,
    val camera: DocumentSide? = null,
    val uploaded: Set<String> = emptySet(),
    val message: String? = null,
    val signInRequired: Boolean = false,
    val draft: CardData? = null,
    val draftRevision: Long? = null,
    val reviewDirty: Boolean = false,
    val originCountry: String = "",
)
/** A stable checkpoint precedes each create/finalize/retry request. GET reconciles uncertain responses. */
class CaptureController(private val api: CaptureGateway, private val store: CaptureCheckpointStore, private val mayCreate: () -> Boolean = { true }) {
    private val mutable = MutableStateFlow(CaptureUi())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private var checkpoint = store.load() ?: CaptureCheckpoint()
    private var closed = false
    private var pending: Pair<RegisterUpload, ByteArray>? = null
    private var pendingBack: ByteArray? = null
    private var acknowledged: Map<String, String> = emptyMap()
    private fun key() = UUID.randomUUID().toString()
    private fun show(value: CaptureUi) { if (!closed) mutable.value = value }
    private fun save(value: CaptureCheckpoint) { check(!closed); store.save(value); checkpoint = value }
    private suspend fun operation(block: suspend () -> Unit) {
        if (closed || !mutex.tryLock()) return
        show(state.value.copy(busy = true, message = null, camera = null))
        try { block() }
        catch (cancel: CancellationException) { throw cancel }
        catch (e: Exception) {
            if (!closed) show(state.value.copy(
                signInRequired = e is CaptureException && e.status == 401,
                message = when {
                    e is CaptureException && e.status == 401 -> "Sign in again to resume your capture."
                    e is CaptureException && e.status == 429 -> "Capture limit reached. Please try again tomorrow."
                    e is CaptureException && e.status == 422 -> "The image could not be accepted. Retake both sides."
                    e is CaptureException && e.status == 409 && state.value.draft != null -> "The saved details changed. Refresh, then discard your edits to load the latest details before editing again."
                    e is CaptureException && e.status == 409 -> "Capture changed or an upload is incomplete. Refresh before continuing."
                    state.value.draft != null -> "We could not save or load the card details. Your edits are still here. Refresh and try again."
                    else -> "We could not finish this step. Refresh to recover, or retry the upload."
                },
            ))
        } finally { if (!closed) show(state.value.copy(busy = false)); mutex.unlock() }
    }
    private fun record(r: CaptureRecord) {
        if (closed) throw CancellationException("Capture closed")
        require(r.id.matches(Regex("[a-f0-9]{32}")) && r.evidenceVersion > 0)
        if (state.value.record?.id != r.id) discard()
        save(checkpoint.copy(id = r.id, create = null))
        acknowledged = if (state.value.record?.id == r.id) acknowledged.filter { (slot, id) -> r.selected[slot] == id } else emptyMap()
        val old = state.value
        val canReview = r.state == "ready" && r.extraction?.status == "complete"
        val savedDraft = old.draft != null && r.reviewed?.data == old.draft && r.reviewed?.originCountry == old.originCountry
        val keepDraft = canReview && old.record?.id == r.id && old.reviewDirty && !savedDraft
        show(old.copy(
            record = r, policy = r.policy ?: old.policy, uploaded = acknowledged.keys,
            draft = if (!canReview) null else if (keepDraft) old.draft else r.reviewed?.data ?: r.extraction?.data,
            draftRevision = if (!canReview) null else if (keepDraft) old.draftRevision else r.revision,
            reviewDirty = keepDraft,
            originCountry = if (keepDraft) old.originCountry else if (canReview) r.reviewed?.originCountry.orEmpty() else "",
        ))
        if (r.state != "uploading") discard()
    }
    suspend fun restore() = operation {
        // A settings outage must not prevent viewing or deleting an existing capture.
        val policy = try { api.policy() }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { null }
        show(state.value.copy(policy = policy, signInRequired = false))
        val create = checkpoint.create
        val current = api.current()
        val r = if (create != null && mayCreate() && policy?.enabled == true && create.expectedPolicyVersion.isNotBlank()) {
            createOrReview(create)
        } else current
        if (r != null) {
            if (checkpoint.id != r.id) save(CaptureCheckpoint(id = r.id))
            record(r)
            checkpoint.finalize?.let { if (r.state == "uploading") record(api.finalize(r.id, it)) }
            checkpoint.retry?.let { if (r.state == "failed" || r.extraction?.status == "failed") record(api.retry(r.id, it)); save(checkpoint.copy(retry = null)) }
        } else if (checkpoint.create == null) {
            discard(); acknowledged = emptyMap(); store.clear(); checkpoint = CaptureCheckpoint()
            show(state.value.copy(record = null, uploaded = emptySet(), draft = null, draftRevision = null, reviewDirty = false, originCountry = ""))
        }
    }
    private suspend fun createOrReview(request: CreateCapture): CaptureRecord? = try {
        api.create(request)
    } catch (e: CaptureException) {
        if (e.status != 409 || e.code != "policy_changed") throw e
        // Server definitively rejected creation: a new intent needs new consent.
        save(checkpoint.copy(create = null))
        val policy = api.policy()
        show(state.value.copy(policy = policy, message = "The capture policy changed. Review the updated details, then select Start capture again."))
        null
    }
    suspend fun start(language: String = "en") = operation {
        if (!mayCreate()) { show(state.value.copy(message = "New ID capture is disabled until app settings are refreshed.")); return@operation }
        val reviewed = state.value.policy
        val policy = api.policy()
        show(state.value.copy(policy = policy))
        require(policy.enabled && policy.slots == listOf("front", "back") && policy.contentType == "image/jpeg")
        val existing = api.current()
        if (existing != null && existing.state in setOf("uploading", "finalizing", "ready", "failed")) { record(existing); return@operation }
        val pending = checkpoint.create
        if (pending == null && (reviewed == null || reviewed != policy)) {
            show(state.value.copy(message = "Review the current capture policy, then select Start capture again."))
            return@operation
        }
        if (!mayCreate()) { show(state.value.copy(message = "New ID capture is disabled until app settings are refreshed.")); return@operation }
        val request = pending?.takeIf { it.expectedPolicyVersion.isNotBlank() }
            ?: CreateCapture(key(), policy.documentType, policy.version, policy.purposes.language(language))
        if (pending != null && pending.expectedPolicyVersion.isBlank()) {
            save(checkpoint.copy(create = null))
            show(state.value.copy(message = "Review the current capture policy, then select Start capture again."))
            return@operation
        }
        save(CaptureCheckpoint(create = request))
        createOrReview(request)?.let { record(it); show(state.value.copy(uploaded = emptySet())) }
    }
    fun camera(side: DocumentSide) {
        if (closed || state.value.busy || state.value.record?.state != "uploading") return
        discard(); save(checkpoint.copy(finalize = null)); show(state.value.copy(camera = side, message = null))
    }
    fun cameraCancelled() { show(state.value.copy(camera = null)) }
    fun cameraFailed() { show(state.value.copy(camera = null, message = "Could not capture a clear image. Try again.")) }
    suspend fun captured(side: DocumentSide, bytes: ByteArray) {
        if (closed || state.value.busy || state.value.camera != side) { bytes.fill(0); return }
        val r = state.value.record ?: run { bytes.fill(0); return }
        if (bytes.size !in 4..CaptureApi.MAX_JPEG_BYTES) { bytes.fill(0); cameraFailed(); return }
        discard()
        pending = RegisterUpload(key(), r.revision, side.name.lowercase(), checksum(bytes), bytes.size.toLong()) to bytes
        upload()
    }
    /** Takes ownership of both reviewed JPEGs, retaining unfinished uploads for Retry upload. */
    suspend fun capturedBoth(front: ByteArray, back: ByteArray) {
        val ui = state.value
        if (closed || ui.busy || ui.camera == null || ui.record?.state != "uploading" ||
            front.size !in 4..CaptureApi.MAX_JPEG_BYTES || back.size !in 4..CaptureApi.MAX_JPEG_BYTES) {
            front.fill(0); back.fill(0); cameraFailed(); return
        }
        discard()
        pendingBack = back
        val r = requireNotNull(ui.record)
        pending = RegisterUpload(key(), r.revision, "front", checksum(front), front.size.toLong()) to front
        upload()
    }
    suspend fun upload() = operation {
        while (pending != null) {
            val (request, bytes) = requireNotNull(pending)
            val r = requireNotNull(state.value.record)
            val instructions = api.register(r.id, request)
            require(instructions.capture.id == r.id)
            record(instructions.capture)
            api.put(instructions, bytes)
            acknowledged = acknowledged + (request.slot to instructions.uploadId)
            show(state.value.copy(uploaded = acknowledged.keys))
            bytes.fill(0)
            pending = null
            val back = pendingBack
            pendingBack = null
            if (back != null) {
                val current = requireNotNull(state.value.record)
                pending = RegisterUpload(key(), current.revision, "back", checksum(back), back.size.toLong()) to back
            }
        }
    }
    suspend fun finalizeCapture() = operation {
        val r = api.get(requireNotNull(checkpoint.id)); record(r)
        if (r.state != "uploading") return@operation
        require(state.value.uploaded.containsAll(listOf("front", "back")) || checkpoint.finalize != null)
        val request = checkpoint.finalize ?: FinalizeCapture(key(), r.revision, r.selected.toMap())
        save(checkpoint.copy(finalize = request))
        record(api.finalize(r.id, request))
    }
    suspend fun retry() = operation {
        val r = api.get(requireNotNull(checkpoint.id)); record(r)
        if (r.state != "failed" && !(r.state == "ready" && r.extraction?.status == "failed")) return@operation
        val operationKey = checkpoint.retry ?: key()
        save(checkpoint.copy(retry = operationKey))
        record(api.retry(r.id, operationKey))
        save(checkpoint.copy(retry = null))
    }
    suspend fun cancel() = operation {
        val id = checkpoint.id ?: return@operation
        record(api.cancel(id)); discard(); acknowledged = emptyMap(); store.clear(); checkpoint = CaptureCheckpoint()
        show(state.value.copy(record = null, uploaded = emptySet(), draft = null, draftRevision = null, reviewDirty = false, originCountry = ""))
    }
    fun editCard(data: CardData) {
        val ui = state.value
        if (closed || ui.busy || ui.draft == null) return
        show(ui.copy(draft = data, reviewDirty = true, message = null))
    }
    fun resetReview() {
        val ui = state.value
        val r = ui.record ?: return
        if (closed || ui.busy || r.state != "ready" || r.extraction?.status != "complete") return
        show(ui.copy(draft = r.reviewed?.data ?: r.extraction.data, draftRevision = r.revision, reviewDirty = false, originCountry = r.reviewed?.originCountry.orEmpty(), message = null))
    }
    fun editOrigin(country: String) {
        val ui = state.value
        if (closed || ui.busy || ui.draft == null || country !in listOf("CA", "US", "MX", "GT", "BZ")) return
        show(ui.copy(originCountry = country, reviewDirty = true, message = null))
    }
    suspend fun saveReview() = operation {
        val ui = state.value
        val r = requireNotNull(ui.record)
        val data = requireNotNull(ui.draft)
        if (!data.isValid() || data.isEmpty() || ui.originCountry !in listOf("CA", "US", "MX", "GT", "BZ")) {
            show(ui.copy(message = "Enter the visible card details and select your country of origin. Each field must be at most 512 bytes."))
            return@operation
        }
        require(r.state == "ready" && r.extraction?.status == "complete")
        record(api.review(r.id, ReviewCapture(requireNotNull(ui.draftRevision), data, ui.originCountry)))
    }
    private fun discard() { pending?.second?.fill(0); pending = null; pendingBack?.fill(0); pendingBack = null }
    fun close() { closed = true; discard(); mutable.value = CaptureUi(signInRequired = mutable.value.signInRequired) }
}
