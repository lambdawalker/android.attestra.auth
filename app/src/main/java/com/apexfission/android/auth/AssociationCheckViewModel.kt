package com.apexfission.android.auth

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface AppLinkDiagnosticState {
    data object Checking : AppLinkDiagnosticState
    data class Ready(val comparison: AssociationComparison) : AppLinkDiagnosticState
    data class Failed(
        val installedFingerprints: List<String>,
        val message: String,
    ) : AppLinkDiagnosticState
}

/** One advisory check per activity ViewModel lifetime, retained through configuration changes. */
internal class AssociationCheckViewModel(application: Application) : AndroidViewModel(application) {
    var warning by mutableStateOf<String?>(null)
        private set
    var diagnosticState by mutableStateOf<AppLinkDiagnosticState>(AppLinkDiagnosticState.Checking)
        private set
    private var checking = false

    val packageName: String get() = getApplication<Application>().packageName
    val assetLinksUrl: String
        get() = "https://${BuildConfig.AUTH_LINK_HOST}/.well-known/assetlinks.json"

    init {
        refresh()
    }

    fun refresh() {
        if (checking) return
        checking = true
        diagnosticState = AppLinkDiagnosticState.Checking
        warning = null
        viewModelScope.launch {
            try {
            val context = getApplication<Application>()
            val fingerprints = try {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val info = context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES,
                    )
                    info.signingInfo?.apkContentsSigners.orEmpty()
                        .map { signature -> signingFingerprint(signature.toByteArray()) }
                        .distinct()
                        .sorted()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }

            if (fingerprints.isEmpty()) {
                val message = "Could not read the installed APK signing certificate."
                diagnosticState = AppLinkDiagnosticState.Failed(emptyList(), message)
                warning = message
                return@launch
            }

            try {
                val comparison = withContext(Dispatchers.IO) {
                    val url = URI(
                        "https",
                        BuildConfig.AUTH_LINK_HOST,
                        "/.well-known/assetlinks.json",
                        null,
                    ).toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    try {
                        connection.instanceFollowRedirects = false
                        connection.connectTimeout = 10_000
                        connection.readTimeout = 10_000
                        connection.useCaches = false
                        check(connection.responseCode == 200) {
                            "assetlinks.json returned HTTP ${connection.responseCode}"
                        }
                        check(
                            connection.contentType
                                ?.substringBefore(';')
                                ?.trim()
                                ?.equals("application/json", ignoreCase = true) == true
                        ) {
                            "assetlinks.json did not return application/json"
                        }
                        val bytes = connection.inputStream.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (output.size() <= 1_048_576) {
                                val count = input.read(
                                    buffer,
                                    0,
                                    minOf(buffer.size, 1_048_577 - output.size()),
                                )
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        check(bytes.size <= 1_048_576) {
                            "assetlinks.json response is larger than 1 MiB"
                        }
                        associationComparison(
                            document = bytes.toString(Charsets.UTF_8),
                            packageName = context.packageName,
                            fingerprints = fingerprints.toSet(),
                        )
                    } finally {
                        connection.disconnect()
                    }
                }
                diagnosticState = AppLinkDiagnosticState.Ready(comparison)
                warning = AssociationApproval(
                    appLinks = comparison.appLinks,
                    passkeys = comparison.passkeys,
                ).warning()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val message = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "Could not load or parse assetlinks.json"
                diagnosticState = AppLinkDiagnosticState.Failed(
                    installedFingerprints = fingerprints,
                    message = message,
                )
                warning = "Couldn't check this build's domain association. $message"
            }
            } finally {
                checking = false
            }
        }
    }

    fun consumeWarning() {
        warning = null
    }
}

internal fun signingFingerprint(certificate: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(certificate)
        .joinToString(":") { byte -> "%02X".format(byte.toInt() and 0xff) }
