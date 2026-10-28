package com.apexfission.android.attestra.auth.capture

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class CapturePolicy(
    val enabled: Boolean,
    @SerialName("policy_version") val version: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("required_slots") val slots: List<String>,
    @SerialName("content_type") val contentType: String,
    @SerialName("max_bytes") val maxBytes: Long,
    val purpose: String = "Save document photos for onboarding",
    val jurisdiction: String = "sample",
    @SerialName("retention_days") val retentionDays: Int = 7,
)
@Serializable data class CardData(
    @SerialName("full_name") val fullName: String = "",
    @SerialName("given_names") val givenNames: String = "",
    val surnames: String = "",
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("document_number") val documentNumber: String = "",
    @SerialName("personal_number") val personalNumber: String = "",
    val address: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("expiry_date") val expiryDate: String = "",
    @SerialName("issuing_country") val issuingCountry: String = "",
) {
    fun isValid(): Boolean = listOf(fullName, givenNames, surnames, birthDate, documentNumber,
        personalNumber, address, issueDate, expiryDate, issuingCountry).all {
        it.toByteArray(Charsets.UTF_8).size <= 512 && '\u0000' !in it && '\r' !in it
    }
    fun isEmpty(): Boolean = this == CardData()
}
@Serializable data class CardExtraction(val status: String, val data: CardData = CardData(), val error: String = "")
@Serializable data class ReviewedCard(val data: CardData, @SerialName("saved_at") val savedAt: Long)
@Serializable data class ReviewCapture(@SerialName("expected_revision") val revision: Long, val data: CardData)
@Serializable data class CaptureRecord(
    @SerialName("capture_id") val id: String,
    @SerialName("evidence_version") val evidenceVersion: Long,
    val revision: Long,
    @SerialName("policy_version") val policyVersion: String,
    @SerialName("document_type") val documentType: String,
    val state: String,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("selected_uploads") val selected: Map<String, String> = emptyMap(),
    val error: String = "",
    val policy: CapturePolicy? = null,
    val extraction: CardExtraction? = null,
    val reviewed: ReviewedCard? = null,
)
@Serializable data class CreateCapture(@SerialName("operation_key") val key: String, @SerialName("document_type") val documentType: String,
    @SerialName("expected_policy_version") val expectedPolicyVersion: String = "")
@Serializable data class RegisterUpload(
    @SerialName("operation_key") val key: String,
    @SerialName("expected_revision") val revision: Long,
    val slot: String,
    val sha256: String,
    val size: Long,
)
@Serializable data class UploadInstructions(
    val capture: CaptureRecord,
    @SerialName("upload_id") val uploadId: String,
    val url: String,
    val headers: Map<String, String>,
    @SerialName("expires_in") val expiresIn: Int,
)
@Serializable data class FinalizeCapture(
    @SerialName("operation_key") val key: String,
    @SerialName("expected_revision") val revision: Long,
    @SerialName("upload_ids") val uploads: Map<String, String>,
)
@Serializable data class OperationKey(@SerialName("operation_key") val key: String)
@Serializable data class CaptureStatus(val capture: CaptureRecord? = null)
/** Only identifiers and mutation intents survive process death. Never image bytes or signed URLs. */
@Serializable data class CaptureCheckpoint(
    val create: CreateCapture? = null,
    val id: String? = null,
    val finalize: FinalizeCapture? = null,
    val retry: String? = null,
)
interface CaptureCheckpointStore {
    fun load(): CaptureCheckpoint?
    fun save(value: CaptureCheckpoint)
    fun clear()
}
interface CaptureGateway {
    suspend fun policy(): CapturePolicy
    suspend fun current(): CaptureRecord?
    suspend fun create(request: CreateCapture): CaptureRecord
    suspend fun get(id: String): CaptureRecord
    suspend fun register(id: String, request: RegisterUpload): UploadInstructions
    suspend fun put(instructions: UploadInstructions, jpeg: ByteArray)
    suspend fun finalize(id: String, request: FinalizeCapture): CaptureRecord
    suspend fun retry(id: String, key: String): CaptureRecord
    suspend fun review(id: String, request: ReviewCapture): CaptureRecord
    suspend fun cancel(id: String): CaptureRecord
}
class CaptureException(val status: Int, val code: String = "request_failed") : Exception("Capture request failed ($status)")
