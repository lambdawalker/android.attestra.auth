package com.apexfission.android.attestra.auth

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.attestra.auth.discovery.*
import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

internal data class BackendConfiguration(val apiUrl: String, val captureEnabled: Boolean)
internal val LocalBackendConfiguration = staticCompositionLocalOf<BackendConfiguration> {
    error("Backend configuration has not been resolved")
}

/** Activity recreation retains the resolved endpoint. Refresh only before starting auth. */
internal class EnvironmentViewModel(application: Application) : AndroidViewModel(application) {
    var configuration by mutableStateOf<BackendConfiguration?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    init { load() }

    fun load() {
        if (loading || configuration != null) return
        loading = true
        message = null
        viewModelScope.launch {
            try {
                val (config, cached) = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val prefs = app.getSharedPreferences("attestra_environment", Context.MODE_PRIVATE)
                    var cached = false
                    val config: BackendConfiguration
                    val identity: String
                    if (BuildConfig.AUTH_API_BASE_URL.isNotBlank()) {
                        config = BackendConfiguration(BuildConfig.AUTH_API_BASE_URL, BuildConfig.CAPTURE_ENABLED)
                        identity = listOf("static", BuildConfig.ATTESTRA_ENVIRONMENT, config.apiUrl,
                            BuildConfig.COGNITO_USER_POOL_ID, BuildConfig.COGNITO_CLIENT_ID).joinToString("|")
                    } else {
                        val client = environmentIndexClient()
                        val resolved = try {
                            EnvironmentIndex(client, object : EnvironmentCache {
                                override fun read(key: String) = prefs.getString(key, null)
                                override fun write(key: String, value: String) { check(prefs.edit().putString(key, value).commit()) }
                                override fun remove(key: String) { prefs.edit().remove(key).commit() }
                            }).resolve(BuildConfig.ATTESTRA_INDEX_URL, BuildConfig.ATTESTRA_ENVIRONMENT)
                        } finally { client.close() }
                        val c = resolved.entry.configuration
                        config = BackendConfiguration(c.apiUrl, c.features.idCapture)
                        cached = resolved.fromCache
                        identity = listOf(BuildConfig.ATTESTRA_INDEX_URL, resolved.entry.id, c.apiUrl,
                            c.awsRegion, c.cognitoUserPoolId, c.cognitoClientId).joinToString("|")
                    }
                    // Never send an old environment's proofs or tokens to a new backend.
                    val binding = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    if (prefs.getString("authBinding", null) != binding) {
                        SecureAuthStorage(app).clearLocalUser()
                        check(prefs.edit().putString("authBinding", binding).commit())
                    }
                    config to cached
                }
                configuration = config
                if (cached) message = "Using saved environment settings while the index is unavailable."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not load ${BuildConfig.ATTESTRA_ENVIRONMENT}. Check your connection and the environment index, then retry." }
            finally { loading = false }
        }
    }
}
