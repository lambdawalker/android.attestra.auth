package com.apexfission.android.attestra.auth.identity

import com.apexfission.android.attestra.auth.ui.onboarding.id.IdentityDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable enum class DocumentSide { @SerialName("front") FRONT, @SerialName("back") BACK }
@Serializable enum class IdentityOutcome {
    @SerialName("not_started") NOT_STARTED,
    @SerialName("pending") PENDING,
    @SerialName("approved") APPROVED,
    @SerialName("rejected") REJECTED,
    @SerialName("inconclusive") INCONCLUSIVE,
    @SerialName("error") ERROR,
}

/** The server owns the policy decision; clients must not derive it from either track. */
@Serializable data class IdentityRecord(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("evidence_version") val evidenceVersion: Int,
    val accepted: Boolean = false,
    val decision: IdentityOutcome = IdentityOutcome.NOT_STARTED,
    val autoReport: IdentityOutcome = IdentityOutcome.NOT_STARTED,
    val thirdParty: IdentityOutcome = IdentityOutcome.NOT_STARTED,
    @SerialName("can_retry") val canRetry: Boolean = false,
)
@Serializable data class Extraction(val readable: Boolean, val extracted: IdentityDetails = IdentityDetails())
@Serializable data class EvidenceVersion(@SerialName("evidence_version") val evidenceVersion: Int)
@Serializable data class SubmitIdentity(
    @SerialName("evidence_version") val evidenceVersion: Int,
    val extracted: IdentityDetails,
    val corrected: IdentityDetails,
)

/** Only resumable identifiers are persisted. Images and editable personal fields stay in memory. */
@Serializable data class IdentityCheckpoint(val submissionId: String, val evidenceVersion: Int = 0)
interface IdentityCheckpointStore {
    fun load(): IdentityCheckpoint?
    fun save(checkpoint: IdentityCheckpoint)
    fun clear()
}
interface IdentityGateway {
    suspend fun create(id: String): IdentityRecord
    suspend fun upload(id: String, version: Int, side: DocumentSide, jpeg: ByteArray)
    suspend fun extract(id: String, version: Int): Extraction
    suspend fun submit(id: String, body: SubmitIdentity): IdentityRecord
    suspend fun status(id: String): IdentityRecord?
}

sealed interface IdentityState {
    data object Start : IdentityState
    data class Capture(val side: DocumentSide) : IdentityState
    data class CaptureProblem(val message: String) : IdentityState
    data object Reading : IdentityState
    data object Unreadable : IdentityState
    data class Review(val extracted: IdentityDetails, val corrected: IdentityDetails, val error: String? = null) : IdentityState
    data object Submitting : IdentityState
    data object Checking : IdentityState
    data object SubmissionFailed : IdentityState
    data object StatusFailed : IdentityState
    data object SignInRequired : IdentityState
    data class Result(val record: IdentityRecord) : IdentityState
}
