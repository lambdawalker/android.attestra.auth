package com.apexfission.android.attestra.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.attestra.auth.email.SecureAuthStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns reset across activity recreation so onboarding cannot race storage deletion. */
internal class LocalUserResetViewModel(application: Application) : AndroidViewModel(application) {
    var resetting by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun deleteLocalUser() {
        if (resetting) return
        resetting = true
        message = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    resetLocalUser(app.noBackupFilesDir) { SecureAuthStorage(app).clearLocalUser() }
                }
                message = "Local user deleted. You can start onboarding again."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Some local data could not be cleared. Please retry."
            } finally {
                resetting = false
            }
        }
    }
}
