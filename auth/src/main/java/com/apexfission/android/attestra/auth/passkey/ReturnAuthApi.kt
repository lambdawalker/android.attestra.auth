package com.apexfission.android.attestra.auth.passkey

import android.util.Log
import com.apexfission.android.attestra.auth.email.AuthSession
import com.apexfission.android.attestra.auth.email.EmailHttpClient
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
@Serializable private data class EmailRequest(val email: String)
@Serializable private data class EmailAnswer(val email: String, val session: String, val code: String)
@Serializable private data class PasskeyAnswer(val email: String, val session: String, val credential: JsonElement)
@Serializable private data class RefreshRequest(@SerialName("refresh_token") val token: String)
@Serializable data class AccountStatus(@SerialName("passkey_registered") val passkeyRegistered: Boolean)
class SignInError(val expired: Boolean = false) : Exception(if (expired) "sign_in_required" else "authentication_failed")

class ReturnAuthApi(baseUrl: String, private val client: HttpClient) {
    private val root = baseUrl.trimEnd('/').also { require(it.startsWith("https://")) }
    private suspend inline fun <reified T> checked(path: String, block: io.ktor.client.request.HttpRequestBuilder.() -> Unit): T {
        val response = client.post("$root/auth/$path", block)
        val text = response.bodyAsText()
        Log.d("EmailDebugX", "auth/$path HTTP ${response.status.value} trace_id=${response.headers["x-request-id"]}")
        if (response.status.value != 200) throw SignInError(response.status.value == 401)
        return EmailHttpClient.json.decodeFromString(text)
    }
    suspend fun start(email: String, passkey: Boolean): SignInChallenge = checked(if (passkey) "passkey/start" else "email/start") {
        contentType(ContentType.Application.Json); setBody(EmailRequest(email))
    }
    suspend fun finishEmail(email: String, session: String, code: String): AuthSession = checked("email/complete") {
        contentType(ContentType.Application.Json); setBody(EmailAnswer(email, session, code))
    }
    suspend fun finishPasskey(email: String, session: String, credential: String): AuthSession = checked("passkey/complete") {
        contentType(ContentType.Application.Json); setBody(PasskeyAnswer(email, session, EmailHttpClient.json.parseToJsonElement(credential)))
    }
    suspend fun refresh(refresh: String): AuthSession = checked("refresh") {
        contentType(ContentType.Application.Json); setBody(RefreshRequest(refresh))
    }
    suspend fun status(access: String): AccountStatus = checked("status") {
        header("Authorization", "Bearer $access"); contentType(ContentType.Application.Json); setBody("{}")
    }
}
