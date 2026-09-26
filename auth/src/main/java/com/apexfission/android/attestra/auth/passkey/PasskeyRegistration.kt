package com.apexfission.android.attestra.auth.passkey

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

@Serializable private data class OptionsReply(@SerialName("creation_options") val options: JsonElement)
@Serializable private data class CompleteBody(val credential: JsonElement)
@Serializable private data class CompleteReply(val registered: Boolean)
@Serializable private data class ErrorReply(val error: String)

class PasskeyApiException(val signInRequired: Boolean) : Exception(if (signInRequired) "sign_in_required" else "passkey_request_failed")
class PasskeyCancelled : Exception()
class PasskeyUnsupported : Exception()

class PasskeyRegistrationApi(baseUrl: String, private val client: HttpClient) {
    private val root = baseUrl.trimEnd('/').also { require(it.startsWith("https://")) }

    suspend fun options(accessToken: String): String {
        Log.d("EmailDebugX", "POST /passkeys/options")
        val response = client.post("$root/passkeys/options") { header("Authorization", "Bearer $accessToken"); contentType(ContentType.Application.Json); setBody("{}") }
        val body = response.bodyAsText()
        checkResponse(response.status.value, body, "options")
        return EmailHttpClient.json.decodeFromString<OptionsReply>(body).options.toString()
    }

    suspend fun complete(accessToken: String, registrationJson: String) {
        Log.d("EmailDebugX", "POST /passkeys/complete")
        val credential = EmailHttpClient.json.parseToJsonElement(registrationJson)
        val response = client.post("$root/passkeys/complete") { header("Authorization", "Bearer $accessToken"); contentType(ContentType.Application.Json); setBody(CompleteBody(credential)) }
        val body = response.bodyAsText()
        checkResponse(response.status.value, body, "complete")
        if (!EmailHttpClient.json.decodeFromString<CompleteReply>(body).registered) throw PasskeyApiException(false)
        Log.d("EmailDebugX", "Passkey registration confirmed by backend")
    }

    private fun checkResponse(status: Int, body: String, stage: String) {
        if (status == 200) { Log.d("EmailDebugX", "passkey $stage returned HTTP 200"); return }
        val code = runCatching { EmailHttpClient.json.decodeFromString<ErrorReply>(body).error }.getOrNull()
        Log.e("EmailDebugX", "passkey $stage HTTP $status error=$code")
        throw PasskeyApiException(status == 401 || code == "sign_in_required")
    }
}

class AndroidPasskeyManager(private val context: Context) {
    suspend fun signIn(options: String): String {
        if (Build.VERSION.SDK_INT < 28) throw PasskeyUnsupported()
        try {
            Log.d("EmailDebugX", "Opening Credential Manager passkey sign-in sheet")
            val response = CredentialManager.create(context).getCredential(context, GetCredentialRequest(listOf(GetPublicKeyCredentialOption(options))))
            return (response.credential as? PublicKeyCredential)?.authenticationResponseJson ?: throw PasskeyUnsupported()
        } catch (error: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d("EmailDebugX", "Passkey sign-in cancelled")
            throw PasskeyCancelled()
        } catch (error: androidx.credentials.exceptions.GetCredentialException) {
            Log.e("EmailDebugX", "Passkey sign-in provider failed", error)
            throw error
        }
    }

    suspend fun create(options: String): String {
        if (Build.VERSION.SDK_INT < 28) throw PasskeyUnsupported()
        return try {
            Log.d("EmailDebugX", "Opening Credential Manager passkey sheet")
            val response = CredentialManager.create(context).createCredential(context, CreatePublicKeyCredentialRequest(options))
            (response as? CreatePublicKeyCredentialResponse)?.registrationResponseJson ?: throw PasskeyUnsupported()
        } catch (error: CreateCredentialCancellationException) {
            Log.d("EmailDebugX", "Passkey sheet cancelled")
            throw PasskeyCancelled()
        } catch (error: CreateCredentialException) {
            Log.e("EmailDebugX", "Passkey provider failed type=${error.type}", error)
            if (error.javaClass.simpleName.contains("Unsupported") || error.javaClass.simpleName.contains("ProviderConfiguration") || error.javaClass.simpleName.contains("NoCreateOption")) throw PasskeyUnsupported()
            throw error
        }
    }
}
