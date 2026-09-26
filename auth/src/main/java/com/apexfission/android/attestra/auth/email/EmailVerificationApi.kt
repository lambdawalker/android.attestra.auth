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

class EmailVerificationApi(
    baseUrl: String,
    private val client: HttpClient,
    private val log: (Int, String) -> Unit = { priority, message -> Log.println(priority, "AttestraEmailApi", message) },
) : EmailVerificationGateway {
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
        val response = client.post("$root/confirm") { contentType(ContentType.Application.Json); setBody(body) }
        response.requireStatus(200, "confirm")
        return response.body()
    }

    private suspend fun HttpResponse.requireStatus(expected: Int, operation: String) {
        if (status.value == expected) {
            log(Log.DEBUG, "$operation returned HTTP ${status.value}")
            return
        }
        val serverCode = runCatching { EmailHttpClient.json.decodeFromString<ErrorReply>(bodyAsText()) }.getOrNull()
        val kind = when (serverCode?.error) {
            "invalid_request" -> EmailApiError.Kind.INVALID_REQUEST
            "incorrect_code" -> EmailApiError.Kind.INCORRECT_CODE
            "attempt_limit" -> EmailApiError.Kind.ATTEMPT_LIMIT
            "link_unusable" -> EmailApiError.Kind.LINK_UNUSABLE
            "confirmed_sign_in_required" -> EmailApiError.Kind.SIGN_IN_REQUIRED
            "confirmation_in_progress" -> EmailApiError.Kind.IN_PROGRESS
            else -> EmailApiError.Kind.UNAVAILABLE
        }
        log(Log.WARN, "$operation returned HTTP ${status.value} ($kind)")
        throw EmailApiError(kind, serverCode?.attemptsRemaining)
    }
}
