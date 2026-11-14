# Source declaration reference

Generated from the current development sources. See [API contracts](api.md) for supported entry points, ownership, and behavior. These excerpts are a source reference, not a binary compatibility report.

## `auth/src/main/java/com/apexfission/android/auth/capture/CaptureApi.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/capture/CaptureApi.kt)

```kotlin
package com.apexfission.android.auth.capture
import com.apexfission.android.auth.email.EmailHttpClient
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
    fun api()
    fun uploads()
}
class CaptureApi(baseUrl: String, private val client: HttpClient, private val uploads: HttpClient, private val token: () -> String) : CaptureGateway {
    override suspend fun policy(): CapturePolicy
    override suspend fun current(): CaptureRecord?
    override suspend fun create(request: CreateCapture): CaptureRecord
    override suspend fun get(id: String): CaptureRecord
    override suspend fun register(id: String, request: RegisterUpload): UploadInstructions
    override suspend fun put(instructions: UploadInstructions, jpeg: ByteArray)
    override suspend fun confirmFront(id: String, uploadId: String): CaptureRecord?
    override suspend fun finalize(id: String, request: FinalizeCapture): CaptureRecord
    override suspend fun retry(id: String, key: String): CaptureRecord
    override suspend fun review(id: String, request: ReviewCapture): CaptureRecord
    override suspend fun retake(id: String, request: RetakeCapture): CaptureRecord
    override suspend fun cancel(id: String): CaptureRecord
    companion object {
        const val MAX_JPEG_BYTES = 4 * 1024 * 1024
    }
}
```

## `auth/src/main/java/com/apexfission/android/auth/capture/CaptureController.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/capture/CaptureController.kt)

```kotlin
package com.apexfission.android.auth.capture
import com.apexfission.android.auth.identity.DocumentSide
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.security.MessageDigest
import java.util.UUID

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
class CaptureController(private val api: CaptureGateway, private val store: CaptureCheckpointStore, private val mayCreate: () -> Boolean = { true }) {
    val state = mutable.asStateFlow()
    suspend fun restore()
    suspend fun start(language: String = "en", reviewedPolicy: CapturePolicy? = state.value.policy)
    suspend fun continueToCapture(language: String = "en", allowNewCapture: Boolean = true, reviewedPolicy: CapturePolicy? = state.value.policy)
    suspend fun uploadAndContinue()
    fun camera(side: DocumentSide)
    fun cameraCancelled()
    fun cameraFailed()
    suspend fun captured(side: DocumentSide, bytes: ByteArray)
    suspend fun capturedBoth(front: ByteArray, back: ByteArray)
    suspend fun upload()
    suspend fun finalizeCapture(resumeSelectedUploads: Boolean = false)
    suspend fun retry()
    suspend fun cancel()
    fun editCard(data: CardData)
    fun resetReview()
    fun editOrigin(country: String)
    suspend fun saveReview()
    fun close()
}
```

## `auth/src/main/java/com/apexfission/android/auth/capture/CaptureHost.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/capture/CaptureHost.kt)

```kotlin
package com.apexfission.android.auth.capture
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apexfission.android.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.auth.identity.DocumentSide
import com.apexfission.android.auth.ui.onboarding.id.CardDetailsSavedScreen
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme
import kotlinx.coroutines.launch

@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    acceptedPolicy: CapturePolicy? = null,
    requireReviewedPolicy: Boolean = false,
    allowNewCapture: Boolean = true,
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit,
)
@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    acceptedPolicy: CapturePolicy? = null,
    requireReviewedPolicy: Boolean = false,
    allowNewCapture: Boolean = true,
    earlyCamera: (@Composable (CaptureCameraEvents) -> Unit)?,
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
)
@Preview(name = "Dark Theme", showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CaptureHostDarkPreview()
@Preview(name = "Light Theme", showBackground = true)
@Composable
fun CaptureHostLightPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/capture/CaptureModels.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/capture/CaptureModels.kt)

```kotlin
package com.apexfission.android.auth.capture
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

@Serializable data class DisclosureMessages(val en: String = "", val es: String = "", val fr: String = "") {
    fun language(requested: String): String
    fun text(requested: String): String
}
@Serializable data class CapturePolicy(
    val enabled: Boolean,
    @SerialName("policy_version") val version: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("required_slots") val slots: List<String>,
    @SerialName("content_type") val contentType: String,
    @SerialName("max_bytes") val maxBytes: Long,
    val purpose: String = "Scan your ID to provide your identity details. You’ll review and confirm the information afterward.",
    val purposes: DisclosureMessages = DisclosureMessages(),
    val jurisdiction: String = "",
    @SerialName("retention_days") val retentionDays: Int = 7,
    @SerialName("stitched_pair_supported") val stitchedPairSupported: Boolean = false,
)
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class CardData(
    @SerialName("full_name") val fullName: String = "",
    @SerialName("given_names") val givenNames: String = "",
    val surnames: String = "",
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("document_number") val documentNumber: String = "",
    @SerialName("issued_identifier") @JsonNames("personal_number") val personalNumber: String = "",
    val address: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("expiry_date") val expiryDate: String = "",
    @SerialName("issuing_country") val issuingCountry: String = "",
    @SerialName("document_type") val documentType: String = "",
    @SerialName("document_name") val documentName: String = "",
    @SerialName("issuing_region") val issuingRegion: String = "",
    @SerialName("issuing_city") val issuingCity: String = "",
    @SerialName("issuing_authority") val issuingAuthority: String = "",
) {
    fun isValid(): Boolean
    fun isEmpty(): Boolean
}
@Serializable data class CardExtraction(val status: String, val data: CardData = CardData(), val error: String = "", val retrying: Boolean = false)
@Serializable data class ReviewedCard(val data: CardData, @SerialName("saved_at") val savedAt: Long, @SerialName("origin_country") val originCountry: String = "")
@Serializable data class ReviewCapture(@SerialName("expected_revision") val revision: Long, val data: CardData, @SerialName("origin_country") val originCountry: String = "")
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
    @SerialName("disclosure_language") val disclosureLanguage: String = "",
    @SerialName("image_revision") val imageRevision: Long? = null,
)
@Serializable data class CreateCapture(@SerialName("operation_key") val key: String, @SerialName("document_type") val documentType: String,
    @SerialName("expected_policy_version") val expectedPolicyVersion: String = "",
    @SerialName("disclosure_language") val disclosureLanguage: String = "en")
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
@Serializable data class RetakeCapture(@SerialName("operation_key") val key: String, @SerialName("expected_revision") val revision: Long)
@Serializable data class OperationKey(@SerialName("operation_key") val key: String)
@Serializable data class CaptureStatus(val capture: CaptureRecord? = null)
@Serializable data class CaptureCheckpoint(
    val create: CreateCapture? = null,
    val id: String? = null,
    val finalize: FinalizeCapture? = null,
    val retry: String? = null,
    val retake: RetakeCapture? = null,
    val awaitingImageConfirmation: Boolean = false,
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
    suspend fun confirmFront(id: String, uploadId: String): CaptureRecord?
    suspend fun finalize(id: String, request: FinalizeCapture): CaptureRecord
    suspend fun retry(id: String, key: String): CaptureRecord
    suspend fun review(id: String, request: ReviewCapture): CaptureRecord
    suspend fun retake(id: String, request: RetakeCapture): CaptureRecord
    suspend fun cancel(id: String): CaptureRecord
}
class CaptureException(val status: Int, val code: String = "request_failed") : Exception("Capture request failed ($status)")
```

## `auth/src/main/java/com/apexfission/android/auth/capture/EarlyCaptureSession.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/capture/EarlyCaptureSession.kt)

```kotlin
package com.apexfission.android.auth.capture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EarlyCaptureSession(private val controller: CaptureController, private val scope: CoroutineScope) {
    fun frontReady(front: ByteArray)
    fun imagesReady(front: ByteArray, back: ByteArray, stitched: Boolean = false): Job
    fun invalidated()
    fun confirmed()
    fun close()
}
class CaptureCameraEvents(
    val onImagesReady: (ByteArray, ByteArray) -> Unit,
    val onImagesInvalidated: () -> Unit,
    val onConfirmed: () -> Unit,
    val onError: () -> Unit,
    val onCancel: () -> Unit,
    val stitchedPair: Boolean = false,
    val onFrontReady: (ByteArray) -> Unit = { it.fill(0) },
)
```

## `auth/src/main/java/com/apexfission/android/auth/capture/SecureCaptureCheckpointStore.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/capture/SecureCaptureCheckpointStore.kt)

```kotlin
package com.apexfission.android.auth.capture
import com.apexfission.android.auth.email.SecureAuthStorage
import java.security.MessageDigest

class SecureCaptureCheckpointStore(private val storage: SecureAuthStorage, environment: String, subject: String) : CaptureCheckpointStore {
    override fun load()
    override fun save(value: CaptureCheckpoint)
    override fun clear()
}
```

## `auth/src/main/java/com/apexfission/android/auth/discovery/AppConfiguration.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/discovery/AppConfiguration.kt)

```kotlin
package com.apexfission.android.auth.discovery
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
    val effectiveOnboarding: OnboardingPolicy
        get()
}
enum class RuntimeSource {
    LIVE,
    CACHED,
    UNAVAILABLE,
}
data class AppConfigurationSnapshot(val document: RuntimeConfiguration? = null, val source: RuntimeSource = RuntimeSource.UNAVAILABLE, val validUntilMillis: Long? = null) {
    val captureEnabled: Boolean
        get()
}
class AppConfiguration(
    private val client: HttpClient,
    private val cache: EnvironmentCache,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun resolve(apiUrl: String, environment: String): AppConfigurationSnapshot
    companion object {
        const val MAX_AGE_MILLIS = 15 * 60 * 1000L
        const val MAX_BYTES = 65_536
    }
}
```

## `auth/src/main/java/com/apexfission/android/auth/discovery/EnvironmentIndex.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/discovery/EnvironmentIndex.kt)

```kotlin
package com.apexfission.android.auth.discovery
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
interface EnvironmentCache {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}
data class ResolvedEnvironment(val entry: EnvironmentEntry, val fromCache: Boolean)
class EnvironmentDiscoveryException(message: String) : Exception(message)
fun environmentIndexClient()
class EnvironmentIndex(
    private val client: HttpClient,
    private val cache: EnvironmentCache,
    private val now: () -> Long = System::currentTimeMillis,
    private val maxCacheAgeMillis: Long = 24 * 60 * 60 * 1000L,
) {
    suspend fun resolve(indexUrl: String = DEFAULT_INDEX_URL, environment: String): ResolvedEnvironment
}
```

## `auth/src/main/java/com/apexfission/android/auth/discovery/OnboardingPolicy.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/discovery/OnboardingPolicy.kt)

```kotlin
package com.apexfission.android.auth.discovery
import com.apexfission.android.auth.passkey.AccountStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable enum class Requirement {
    @SerialName("required") REQUIRED,
    @SerialName("optional") OPTIONAL,
    @SerialName("disabled") DISABLED,
}
@Serializable enum class PasswordPlacement {
    @SerialName("with_email") WITH_EMAIL,
    @SerialName("after_verification") AFTER_VERIFICATION,
    @SerialName("hidden") HIDDEN,
}
@Serializable enum class CharacterRule {
    @SerialName("allowed") ALLOWED,
    @SerialName("required") REQUIRED,
    @SerialName("disallowed") DISALLOWED,
}
enum class OnboardingStep {
    PASSWORD,
    PASSKEY,
    ID_CAPTURE,
    REFRESH,
    COMPLETE,
}
@Serializable data class PasswordPolicy(
    val minLength: Int = 12, val maxLength: Int = 128,
    val lowercase: CharacterRule = CharacterRule.ALLOWED, val uppercase: CharacterRule = CharacterRule.ALLOWED,
    val numbers: CharacterRule = CharacterRule.ALLOWED, val symbols: CharacterRule = CharacterRule.ALLOWED,
) {
    fun validate()
    fun requirements(): List<String>
    fun problems(value: String): List<String>
    fun strength(value: String): String
}
@Serializable data class OnboardingPolicy(
    val idCapture: Requirement, val passkey: Requirement, val password: Requirement,
    val passwordPlacement: PasswordPlacement, val passwordPolicy: PasswordPolicy,
) {
    fun validate()
    fun next(status: AccountStatus, skipped: Set<OnboardingStep> = emptySet()): OnboardingStep
    companion object {
        fun legacy(capture: Boolean)
    }
}
```


## `auth/src/main/java/com/apexfission/android/auth/email/EmailFlowController.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/email/EmailFlowController.kt)

```kotlin
package com.apexfission.android.auth.email
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

data class VerificationLink(val requestId: String, val tokenB: String) {
    fun isValid(): Boolean
}
sealed interface EmailScreen {
    data class Start(val error: String? = null) : EmailScreen
    data class Wait(val email: String, val requestId: String, val help: String? = null) : EmailScreen
    data class Code(
        val email: String,
        val requestId: String,
        val tokenB: String? = null,
        val error: String? = null,
        val attemptsRemaining: Int? = null,
    ) : EmailScreen
    data class Limit(val email: String, val requestId: String) : EmailScreen
    data class Unusable(val email: String, val requestId: String?) : EmailScreen
    data object Recovery : EmailScreen
    data object Verified : EmailScreen
    data class Loading(val task: LoadingStep) : EmailScreen
}
enum class LoadingStep {
    REQUEST_EMAIL,
    CONFIRM_EMAIL,
    RESEND_EMAIL,
}
class EmailFlowController(
    private val api: EmailVerificationGateway,
    private val storage: AuthStorage,
    private val proofs: ProofGenerator = ProofGenerator(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    var screen: EmailScreen
        private set
    var verifiedRequestId: String?
        private set
    var lastEmailAtMillis: Long
        private set
    fun canResend(): Boolean
    suspend fun start(email: String)
    suspend fun openLink(link: VerificationLink)
    fun enterCode()
    suspend fun confirmCode(code: String)
    suspend fun resend()
    fun changeEmail()
}
```

## auth/src/main/java/com/apexfission/android/auth/email/EmailOnboardingHost.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/email/EmailOnboardingHost.kt)

```kotlin
package com.apexfission.android.auth.email
import com.apexfission.android.auth.library.R
import androidx.activity.compose.BackHandler
import com.apexfission.android.auth.capture.CaptureApi
import com.apexfission.android.auth.capture.CapturePolicy
import com.apexfission.android.auth.discovery.*
import com.apexfission.android.auth.passkey.AccountStatus
import com.apexfission.android.auth.passkey.SignInError
import com.apexfission.android.auth.ui.onboarding.password.PasswordField
import com.apexfission.android.auth.ui.onboarding.password.PasswordScreen
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import kotlinx.coroutines.CancellationException
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Base64
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.apexfission.android.auth.passkey.ReturnAuthApi
import com.apexfission.android.auth.passkey.SignInChallenge
import com.apexfission.android.auth.ui.onboarding.signin.ReturnSignInScreen
import com.apexfission.android.auth.ui.onboarding.signin.ReturnEmailCodeScreen
import androidx.compose.runtime.mutableStateOf
import com.apexfission.android.auth.passkey.AndroidPasskeyManager
import com.apexfission.android.auth.passkey.PasskeyRegistrationApi
import com.apexfission.android.auth.passkey.PasskeyApiException
import com.apexfission.android.auth.passkey.PasskeyCancelled
import com.apexfission.android.auth.passkey.PasskeyUnsupported
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyFailedScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyUnsupportedScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentityStartScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.apexfission.android.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailAttemptLimitScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailCodeScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailLinkUnusableScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailSessionRecoveryScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailStartScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailWaitScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyStartScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

fun parseVerificationLink(uri: Uri?, expectedHost: String): VerificationLink?
@Composable
fun EmailOnboardingHost(
    apiBaseUrl: String,
    link: VerificationLink?,
    onLinkConsumed: () -> Unit,
    onPasskeyDeferred: () -> Unit,
    onboarding: OnboardingPolicy? = null,
    onRetryConfiguration: () -> Unit = {},
    identityContent: @Composable (Boolean, CapturePolicy?, () -> Unit) -> Unit = { _, _, exit ->
        com.apexfission.android.auth.identity.IdentityIntegrationUnavailable(exit)
    },
)
```


## `auth/src/main/java/com/apexfission/android/auth/email/EmailVerificationApi.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/email/EmailVerificationApi.kt)

```kotlin
package com.apexfission.android.auth.email
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)
class EmailApiError(val kind: Kind, val attemptsRemaining: Int? = null) : Exception(kind.name) {
    enum class Kind {
        INVALID_REQUEST,
        INCORRECT_CODE,
        ATTEMPT_LIMIT,
        LINK_UNUSABLE,
        SIGN_IN_REQUIRED,
        IN_PROGRESS,
        UNAVAILABLE,
    }
}
object EmailHttpClient {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    fun create(): HttpClient
}
interface EmailVerificationGateway {
    suspend fun signup(email: String, challenge: String): String
    suspend fun resend(requestId: String)
    suspend fun confirmLocal(requestId: String, tokenB: String, tokenA: String): AuthSession
    suspend fun confirmLocalCode(requestId: String, tokenA: String, tokenC: String): AuthSession
    suspend fun confirmCode(requestId: String, tokenB: String, tokenC: String): AuthSession
}
class EmailVerificationApi(baseUrl: String, private val client: HttpClient) : EmailVerificationGateway {
    override suspend fun signup(email: String, challenge: String): String
    override suspend fun resend(requestId: String)
    override suspend fun confirmLocal(requestId: String, tokenB: String, tokenA: String): AuthSession
    override suspend fun confirmLocalCode(requestId: String, tokenA: String, tokenC: String): AuthSession
    override suspend fun confirmCode(requestId: String, tokenB: String, tokenC: String): AuthSession
}
```

## auth/src/main/java/com/apexfission/android/auth/email/ProofGenerator.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/email/ProofGenerator.kt)

```kotlin
package com.apexfission.android.auth.email
import java.security.MessageDigest
import java.security.SecureRandom

data class EmailProof(val tokenA: String, val challenge: String)
class ProofGenerator(private val random: (ByteArray) -> Unit = SecureRandom()::nextBytes) {
    fun create(): EmailProof
}
```

## `auth/src/main/java/com/apexfission/android/auth/email/SecureAuthStorage.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/email/SecureAuthStorage.kt)

```kotlin
package com.apexfission.android.auth.email
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable

@Serializable data class PendingEmail(val email: String, val requestId: String, val tokenA: String, val createdAtMillis: Long)
interface AuthStorage {
    fun pending(): PendingEmail?
    fun savePending(value: PendingEmail)
    fun clearPending()
    fun saveSession(session: AuthSession)
    fun session(): AuthSession?
    fun email(): String?
    fun saveEmail(email: String)
    fun clearSession()
    fun idCheckDeferred(): Boolean
    fun setIdCheckDeferred(deferred: Boolean)
}
class SecureAuthStorage(context: Context) : AuthStorage {
    override fun pending(): PendingEmail?
    override fun savePending(value: PendingEmail)
    override fun clearPending()
    override fun saveSession(session: AuthSession)
    override fun session(): AuthSession?
    override fun email(): String?
    override fun saveEmail(email: String)
    override fun idCheckDeferred(): Boolean
    override fun setIdCheckDeferred(deferred: Boolean)
    override fun clearSession()
    fun captureCheckpoint(namespace: String): com.apexfission.android.auth.capture.CaptureCheckpoint?
    fun saveCaptureCheckpoint(namespace: String, value: com.apexfission.android.auth.capture.CaptureCheckpoint)
    fun clearCaptureCheckpoint(namespace: String)
    fun clearLocalUser()
}
```

## `auth/src/main/java/com/apexfission/android/auth/identity/IdentityIntegrationUnavailable.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/identity/IdentityIntegrationUnavailable.kt)

```kotlin
package com.apexfission.android.auth.identity
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import com.apexfission.android.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

@Composable
fun IdentityIntegrationUnavailable(onExit: () -> Unit)
```

## `auth/src/main/java/com/apexfission/android/auth/identity/IdentityModels.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/identity/IdentityModels.kt)

```kotlin
package com.apexfission.android.auth.identity

enum class DocumentSide {
    FRONT,
    BACK,
}
```

## `auth/src/main/java/com/apexfission/android/auth/identity/capture/IdentityCamera.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/identity/capture/IdentityCamera.kt)

```kotlin
package com.apexfission.android.auth.identity.capture
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverviewMode
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.CardCaptureConfig
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*

@Composable
fun IdentityCamera(onCaptured: (ByteArray, ByteArray) -> Unit, onError: () -> Unit, onCancel: () -> Unit)
@Composable
fun IdentityCamera(
    onCaptured: (ByteArray, ByteArray) -> Unit,
    onError: () -> Unit,
    onCancel: () -> Unit,
    onImagesReady: ((ByteArray, ByteArray) -> Unit)?,
    onImagesInvalidated: () -> Unit = {},
    onConfirmed: () -> Unit = {},
)
@Composable
fun IdentityCamera(
    onCaptured: (ByteArray, ByteArray) -> Unit,
    onError: () -> Unit,
    onCancel: () -> Unit,
    onImagesReady: ((ByteArray, ByteArray) -> Unit)?,
    onImagesInvalidated: () -> Unit,
    onConfirmed: () -> Unit,
    stitchedPair: Boolean,
    onFrontReady: (ByteArray) -> Unit = { it.fill(0) },
)
```

## `auth/src/main/java/com/apexfission/android/auth/passkey/PasskeyRegistration.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/passkey/PasskeyRegistration.kt)

```kotlin
package com.apexfission.android.auth.passkey
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import com.apexfission.android.auth.email.EmailHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

class PasskeyApiException(val signInRequired: Boolean) : Exception(if (signInRequired) "sign_in_required" else "passkey_request_failed")
class PasskeyCancelled : Exception()
class PasskeyUnsupported : Exception()
class PasskeyRegistrationApi(baseUrl: String, private val client: HttpClient) {
    suspend fun options(accessToken: String): String
    suspend fun complete(accessToken: String, registrationJson: String)
}
class AndroidPasskeyManager(private val context: Context) {
    suspend fun signIn(options: String): String
    suspend fun create(options: String): String
}
```

## `auth/src/main/java/com/apexfission/android/auth/passkey/ReturnAuthApi.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/passkey/ReturnAuthApi.kt)

```kotlin
package com.apexfission.android.auth.passkey
import android.util.Log
import com.apexfission.android.auth.email.AuthSession
import com.apexfission.android.auth.email.EmailHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable data class SignInChallenge(val session: String, val options: String = "")
@Serializable data class AccountStatus(
    @SerialName("passkey_registered") val passkeyRegistered: Boolean = false,
    @SerialName("password_registered") val passwordRegistered: Boolean = false,
    @SerialName("id_captured") val idCaptured: Boolean = false,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean = false,
)
@Serializable data class PasswordRegistered(@SerialName("password_registered") val passwordRegistered: Boolean)
class SignInError(val expired: Boolean = false) : Exception(if (expired) "sign_in_required" else "authentication_failed")
class ReturnAuthApi(baseUrl: String, private val client: HttpClient) {
    suspend fun start(email: String, passkey: Boolean): SignInChallenge
    suspend fun finishEmail(email: String, session: String, code: String): AuthSession
    suspend fun finishPasskey(email: String, session: String, credential: String): AuthSession
    suspend fun startPassword(email: String): SignInChallenge
    suspend fun finishPassword(email: String, session: String, password: String): AuthSession
    suspend fun setPassword(access: String, password: String): PasswordRegistered
    suspend fun refresh(refresh: String): AuthSession
    suspend fun status(access: String): AccountStatus
}
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/OnboardingCatalog.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/OnboardingCatalog.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailAttemptLimitScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailCodeScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailLinkUnusableScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailSessionRecoveryScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailStartScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailWaitScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentityDetails
import com.apexfission.android.auth.ui.onboarding.id.IdentityDocumentUnreadableScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentityReviewScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentityStartScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentitySubmissionFailedScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentitySuccessScreen
import com.apexfission.android.auth.ui.onboarding.id.IdentityUnsuccessfulScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyFailedScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyStartScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyUnsupportedScreen
import com.apexfission.android.auth.ui.onboarding.welcome.WelcomeScreen
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

class OnboardingBusinessActions(private val onOpenIdentityCapture: () -> Unit = {}) {
    fun requestEmail(email: String)
    fun resendEmail()
    fun confirmWithCode(code: String)
    fun confirmWithLocalProof()
    fun createPasskey()
    fun finishPasskeyRegistration()
    fun checkPasskeySupport()
    fun openIdentityCapture()
    fun submitIdentityDetails(details: IdentityDetails)
    fun reconcileIdentitySubmission()
    fun retryIdentityCheck()
    fun recoverEmailSignIn()
}
@Composable
fun OnboardingCatalog(actions: OnboardingBusinessActions)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun OnboardingCatalogPreview()
```


## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/common/OnboardingComponents.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/common/OnboardingComponents.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.common
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFrame(
    title: String,
    icon: ImageVector,
    step: String,
    onBack: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    secondaryEnabled: Boolean = true,
    content: @Composable () -> Unit,
)
@Composable
fun OnboardingCard(content: @Composable () -> Unit)
@Composable
fun BodyText(text: String)
@Composable
fun StatusRow(label: String, detail: String, complete: Boolean = true)
@Composable
fun WaitingIndicator(description: String)
```

## auth/src/main/java/com/apexfission/android/auth/ui/onboarding/common/OnboardingLoadingScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/common/OnboardingLoadingScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.common
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

enum class LoadingTask(val title: String, val description: String, val step: String) {
    REQUEST_EMAIL("Sending your confirmation email", "We are processing your request.", "1/5"),
    CONFIRM_EMAIL("Verifying your email", "We are confirming your email and preparing your session.", "2/5"),
    RESEND_EMAIL("Requesting another email", "We are processing your request.", "2/5"),
    SIGN_IN("Signing you in", "We are checking your account and preparing your session.", "Account"),
    OPEN_PASSKEY_MANAGER("Opening your passkey manager", "Follow your device or password manager to create a passkey.", "3/5"),
    SAVE_PASSKEY("Saving your passkey", "We are adding the passkey to your account.", "3/5"),
    PREPARE_CAPTURE("Preparing your camera", "We are checking your saved capture.", "ID capture"),
    UPLOAD_DOCUMENT("Saving your photos", "We are uploading both sides of your card.", "ID capture"),
    READ_DOCUMENT("Reading your document", "We are extracting details for you to review. You can leave this screen and return to check progress.", "ID check"),
    SUBMIT_ID("Submitting your ID details", "We are sending your reviewed details for an identity check.", "ID check"),
    CHECK_IDENTITY("Checking your identity", "Your details have been submitted. We will show the result when available.", "ID check"),
}
@Composable
fun OnboardingLoadingScreen(task: LoadingTask, onBack: () -> Unit, onLeave: (() -> Unit)? = null)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun OnboardingLoadingScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailAttemptLimitScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailAttemptLimitScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.email
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailAttemptLimitScreen(
    email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit,
    canRequestEmail: Boolean, retryAfter: String? = null,
)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailAttemptLimitScreenPreview()
```


## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailCodeScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailCodeScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.email
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.EmailOneTimeCodeField
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailCodeScreen(
    email: String,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    error: String? = null,
    attemptsRemaining: Int? = null,
    canResend: Boolean = true,
)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailCodeScreenPreview()
```

## auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailLinkUnusableScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailLinkUnusableScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.email
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailLinkUnusableScreen(email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit, canRequestEmail: Boolean = true)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailLinkUnusableScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailSessionRecoveryScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailSessionRecoveryScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.email
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailSessionRecoveryScreen(onBack: () -> Unit, onSignIn: () -> Unit)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailSessionRecoveryScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailStartScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailStartScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.email
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailStartScreen(onBack: () -> Unit, onContinue: (String) -> Unit, error: String? = null, onExistingAccount: (() -> Unit)? = null,
    passwordContent: (@Composable () -> Unit)? = null, passwordValid: Boolean = true,
    onEmailChanged: () -> Unit = {})
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailStartScreenPreview()
```


## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailWaitScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/email/EmailWaitScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.email
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailWaitScreen(
    email: String,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onEnterCode: () -> Unit,
    canResend: Boolean = true,
    resendHelp: String? = null,
    onSignIn: (() -> Unit)? = null,
)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailWaitScreenPreview()
```

## auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/CardDetailsSavedScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/CardDetailsSavedScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun CardDetailsSavedScreen(onDone: () -> Unit, onReview: () -> Unit)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CardDetailsSavedScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityDocumentUnreadableScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityDocumentUnreadableScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentityDocumentUnreadableScreen(onBack: () -> Unit, onRetake: () -> Unit, onDashboard: () -> Unit)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityDocumentUnreadableScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityReviewScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityReviewScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.capture.DocumentTypeField
import com.apexfission.android.auth.capture.IssuingJurisdiction
import com.apexfission.android.auth.capture.IssuingJurisdictionField
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@kotlinx.serialization.Serializable
data class IdentityDetails(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val documentNumber: String = "",
    val issuingCountry: String = "",
    val documentType: String = "",
    val expirationDate: String = "",
    val issuingRegion: String = "",
    val issuingCity: String = "",
    val documentName: String = "",
    val issuingAuthority: String = "",
)
@Composable
fun IdentityReviewScreen(
    initialDetails: IdentityDetails, onBack: () -> Unit,
    onSubmit: (IdentityDetails) -> Unit, onRescan: () -> Unit,
    onDetailsChanged: (IdentityDetails) -> Unit = {},
    extractedDetails: IdentityDetails = initialDetails,
    error: String? = null,
)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityReviewScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityStartScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityStartScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalConfiguration
import com.apexfission.android.auth.capture.CapturePolicy
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentityStartScreen(
    onBack: () -> Unit, onStartCapture: () -> Unit, onSkip: () -> Unit,
    passkeyAdded: Boolean,
    showAccountStatus: Boolean = true,
    required: Boolean = false,
    capturePolicy: CapturePolicy? = null,
    policyLoading: Boolean = false,
    policyError: String? = null,
    requireDisclosure: Boolean = false,
    onRetryPolicy: () -> Unit = {},
)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityStartScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentitySubmissionFailedScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentitySubmissionFailedScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentitySubmissionFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDashboard: () -> Unit)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentitySubmissionFailedScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentitySuccessScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentitySuccessScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentitySuccessScreen(onBack: () -> Unit, onDashboard: () -> Unit, passkeyAdded: Boolean)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentitySuccessScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityUnsuccessfulScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/id/IdentityUnsuccessfulScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.id
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun IdentityUnsuccessfulScreen(
    onBack: () -> Unit, onDashboard: () -> Unit, onRetry: () -> Unit,
    canRetry: Boolean, explanation: String? = null,
)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityUnsuccessfulScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/passkey/PasskeyFailedScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/passkey/PasskeyFailedScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.passkey
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasskeyFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDoLater: () -> Unit, cancelled: Boolean, required: Boolean = false)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyFailedScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/passkey/PasskeyStartScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/passkey/PasskeyStartScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.passkey
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasskeyStartScreen(onBack: () -> Unit, onCreatePasskey: () -> Unit, onDoLater: () -> Unit, required: Boolean = false)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyStartScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/passkey/PasskeyUnsupportedScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/passkey/PasskeyUnsupportedScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.passkey
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasskeyUnsupportedScreen(onBack: () -> Unit, onDoLater: () -> Unit, onCheckAgain: () -> Unit, required: Boolean = false)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyUnsupportedScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/password/PasswordScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/password/PasswordScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.password
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.apexfission.android.auth.discovery.PasswordPolicy
import com.apexfission.android.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, policy: PasswordPolicy? = null, optional: Boolean = false)
@Composable
fun PasswordScreen(value: String, onChange: (String) -> Unit, onSubmit: () -> Unit,
    onBack: () -> Unit, error: String?, policy: PasswordPolicy? = null,
    optional: Boolean = false, onSkip: () -> Unit = {}, signIn: Boolean = false)
```


## `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/signin/ReturnSignInScreens.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/signin/ReturnSignInScreens.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.signin
import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.EmailOneTimeCodeField
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun ReturnSignInScreen(
    initialEmail: String,
    error: String?,
    onPasskey: (String) -> Unit,
    onEmail: (String) -> Unit,
    onBack: () -> Unit,
    onPassword: ((String) -> Unit)? = null,
)
@Composable
fun ReturnEmailCodeScreen(
    email: String,
    error: String?,
    onVerify: (String) -> Unit,
    onBack: () -> Unit,
)
```

## auth/src/main/java/com/apexfission/android/auth/ui/onboarding/welcome/WelcomeScreen.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/onboarding/welcome/WelcomeScreen.kt)

```kotlin
package com.apexfission.android.auth.ui.onboarding.welcome
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Composable
fun WelcomeScreen(passkeyAdded: Boolean, onCheckId: () -> Unit)
@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun WelcomeScreenPreview()
```

## `auth/src/main/java/com/apexfission/android/auth/ui/theme/Color.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/theme/Color.kt)

```kotlin
package com.apexfission.android.auth.ui.theme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val AttestraBackground = Color(0xFF0F131B)
val AttestraSurface = Color(0xFF0F131B)
val AttestraOnSurface = Color(0xFFDFE2ED)
val AttestraOnSurfaceVariant = Color(0xFFC2C6D6)
val AttestraPrimary = Color(0xFFADC6FF)
val AttestraPrimaryContainer = Color(0xFF4D8EFF)
val AttestraOnPrimary = Color(0xFF002E6A)
val AttestraOnPrimaryContainer = Color(0xFF001B3E)
val AttestraSecondary = Color(0xFF4EDEA3)
val AttestraOnSecondary = Color(0xFF003824)
val AttestraSecondaryContainer = Color(0xFF005237)
val AttestraOnSecondaryContainer = Color(0xFF70FDBE)
val AttestraError = Color(0xFFFFB4AB)
val AttestraErrorContainer = Color(0xFF93000A)
val AttestraOnError = Color(0xFF690005)
val AttestraOnErrorContainer = Color(0xFFFFDAD6)
val AttestraSurfaceContainerLowest = Color(0xFF0A0D13)
val AttestraSurfaceContainerLow = Color(0xFF141923)
val AttestraSurfaceContainer = Color(0xFF181E2A)
val AttestraSurfaceContainerHigh = Color(0xFF1E2534)
val AttestraSurfaceContainerHighest = Color(0xFF252D3E)
val AttestraOutline = Color(0xFF434753)
val AttestraOutlineVariant = Color(0xFF2E333F)
val AttestraLightPaperCanvas = Color(0xFFF7F8F8)
val AttestraLightClearSurface = Color(0xFFFFFFFF)
val AttestraLightCharcoalInk = Color(0xFF20282B)
val AttestraLightSlateCopy = Color(0xFF4E5B60)
val AttestraLightMistBorder = Color(0xFFDCE2E3)
val AttestraLightHarborTeal = Color(0xFF176B64)
val AttestraLightOnHarborTeal = Color(0xFFFFFFFF)
val AttestraLightTealContainer = Color(0xFFD2E8E4)
val AttestraLightOnTealContainer = Color(0xFF00201D)
val AttestraLightErrorInk = Color(0xFF9B3435)
val AttestraLightErrorContainer = Color(0xFFFFDAD6)
val AttestraLightOnError = Color(0xFFFFFFFF)
val AttestraLightOnErrorContainer = Color(0xFF410002)
val AttestraLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val AttestraLightSurfaceContainerLow = Color(0xFFF2F4F4)
val AttestraLightSurfaceContainer = Color(0xFFECEEEE)
val AttestraLightSurfaceContainerHigh = Color(0xFFE6E8E8)
val AttestraLightSurfaceContainerHighest = Color(0xFFE0E2E2)
```

## `auth/src/main/java/com/apexfission/android/auth/ui/theme/Theme.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/theme/Theme.kt)

```kotlin
package com.apexfission.android.auth.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AttestraAuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)
```

## `auth/src/main/java/com/apexfission/android/auth/ui/theme/Type.kt`

[Source](../../auth/src/main/java/com/apexfission/android/auth/ui/theme/Type.kt)

```kotlin
package com.apexfission.android.auth.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```
