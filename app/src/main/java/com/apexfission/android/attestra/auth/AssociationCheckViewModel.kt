package com.apexfission.android.attestra.auth

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

/** One advisory check per activity ViewModel lifetime, retained through configuration changes. */
class AssociationCheckViewModel(application: Application) : AndroidViewModel(application) {
    var warning by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            warning = try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    @Suppress("DEPRECATION")
                    val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    val fingerprints = info.signingInfo?.apkContentsSigners.orEmpty().map { signature ->
                        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                            .joinToString(":") { byte -> "%02X".format(byte.toInt() and 0xff) }
                    }.toSet()
                    val url = URI("https", BuildConfig.AUTH_LINK_HOST, "/.well-known/assetlinks.json", null).toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    try {
                        connection.instanceFollowRedirects = false
                        connection.connectTimeout = 10_000
                        connection.readTimeout = 10_000
                        connection.useCaches = false
                        check(connection.responseCode == 200) { "Association endpoint unavailable" }
                        check(connection.contentType?.substringBefore(';')?.trim()?.equals("application/json", ignoreCase = true) == true) { "Expected JSON response" }
                        val bytes = connection.inputStream.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (output.size() <= 1_048_576) {
                                val count = input.read(buffer, 0, minOf(buffer.size, 1_048_577 - output.size()))
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        check(bytes.size <= 1_048_576) { "Association response too large" }
                        associationApproval(bytes.toString(Charsets.UTF_8), context.packageName, fingerprints).warning()
                    } finally {
                        connection.disconnect()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                "Couldn't check this build's domain association. Check connectivity or hosting; App Links and passkey approval is unknown."
            }
        }
    }

    fun consumeWarning() { warning = null }
}
