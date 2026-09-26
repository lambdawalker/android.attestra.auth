package com.apexfission.android.attestra.auth.email

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

@Serializable private data class SignupBody(val email: String, @SerialName("code_challenge") val challenge: String, @SerialName("code_challenge_method") val method: String)
@Serializable private data class SignupReply(@SerialName("request_id") val requestId: String)
@Serializable private data class ResendBody(@SerialName("request_id") val requestId: String)
@Serializable private data class ConfirmBody(
    @SerialName("request_id") val requestId: String,
    @SerialName("token_b") val tokenB: String,
    @SerialName("token_a") val tokenA: String? = null,
    @SerialName("token_c") val tokenC: String? = null,
)
@Serializable private data class ErrorReply(val error: String, @SerialName("attempts_remaining") val attemptsRemaining: Int? = null)

@Serializable data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)

class EmailApiError(val kind: Kind, val attemptsRemaining: Int? = null) : Exception(kind.name) {
    enum class Kind { INVALID_REQUEST, INCORRECT_CODE, ATTEMPT_LIMIT, LINK_UNUSABLE, SIGN_IN_REQUIRED, IN_PROGRESS, UNAVAILABLE }
}

object EmailHttpClient {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    fun create(): HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) { json(EmailHttpClient.json) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
    }
}

interface EmailVerificationGateway {
    suspend fun signup(email: String, challenge: String): String
    suspend fun resend(requestId: String)
    suspend fun confirmLocal(requestId: String, tokenB: String, tokenA: String): AuthSession
    suspend fun confirmCode(requestId: String, tokenB: String, tokenC: String): AuthSession
}

class EmailVerificationApi(baseUrl: String, private val client: HttpClient) : EmailVerificationGateway {
    private companion object { const val TAG = "EmailDebugX" }
    private val root = baseUrl.trimEnd('/').also {
        require(it.startsWith("https://") && it.substringAfter("https://").isNotBlank() && !it.contains('?') && !it.contains('#')) {
            "Email API URL must be an HTTPS origin"
        }
    }

    override suspend fun signup(email: String, challenge: String): String {
        val response = client.post("$root/signup") { contentType(ContentType.Application.Json); setBody(SignupBody(email, challenge, "S256")) }
        response.requireStatus(202, "signup")
        return response.body<SignupReply>().requestId
    }

    override suspend fun resend(requestId: String) {
        client.post("$root/resend") { contentType(ContentType.Application.Json); setBody(ResendBody(requestId)) }.requireStatus(202, "resend")
    }

    override suspend fun confirmLocal(requestId: String, tokenB: String, tokenA: String): AuthSession =
        confirm(ConfirmBody(requestId, tokenB, tokenA = tokenA))

    override suspend fun confirmCode(requestId: String, tokenB: String, tokenC: String): AuthSession =
        confirm(ConfirmBody(requestId, tokenB, tokenC = tokenC))

    private suspend fun confirm(body: ConfirmBody): AuthSession {
        Log.d(TAG, "POST /confirm request_id=${body.requestId} proof=${if (body.tokenA != null) "A+B" else "B+C"} url=$root/confirm")
        val response = client.post("$root/confirm") { contentType(ContentType.Application.Json); setBody(body) }
        response.requireStatus(200, "confirm")
        return try {
            response.body<AuthSession>().also {
                Log.d(TAG, "POST /confirm decoded session: access=${it.accessToken.isNotEmpty()} id=${it.idToken.isNotEmpty()} refresh=${it.refreshToken.isNotEmpty()} expires=${it.expiresIn}")
            }
        } catch (error: Exception) {
            Log.e(TAG, "POST /confirm HTTP 200 but session decoding failed", error)
            throw error
        }
    }

    private suspend fun HttpResponse.requireStatus(expected: Int, operation: String) {
        val trace = headers["x-request-id"].orEmpty()
        if (status.value == expected) {
            Log.d(TAG, "$operation returned HTTP ${status.value} trace_id=$trace")
            return
        }
        val responseText = bodyAsText()
        val serverCode = runCatching { EmailHttpClient.json.decodeFromString<ErrorReply>(responseText) }.getOrNull()
        val kind = when (serverCode?.error) {
            "invalid_request" -> EmailApiError.Kind.INVALID_REQUEST
            "incorrect_code" -> EmailApiError.Kind.INCORRECT_CODE
            "attempt_limit" -> EmailApiError.Kind.ATTEMPT_LIMIT
            "link_unusable" -> EmailApiError.Kind.LINK_UNUSABLE
            "confirmed_sign_in_required" -> EmailApiError.Kind.SIGN_IN_REQUIRED
            "confirmation_in_progress" -> EmailApiError.Kind.IN_PROGRESS
            else -> EmailApiError.Kind.UNAVAILABLE
        }
        Log.e(TAG, "$operation returned HTTP ${status.value} trace_id=$trace body=$responseText mapped=$kind")
        throw EmailApiError(kind, serverCode?.attemptsRemaining)
    }
}
