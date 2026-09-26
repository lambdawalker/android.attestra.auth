package com.apexfission.android.attestra.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.apexfission.android.attestra.auth.email.EmailOnboardingHost
import com.apexfission.android.attestra.auth.email.VerificationLink
import com.apexfission.android.attestra.auth.email.parseVerificationLink
import com.apexfission.android.attestra.auth.ui.onboarding.OnboardingBusinessActions
import com.apexfission.android.attestra.auth.ui.onboarding.OnboardingCatalog
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

private enum class EntryScreen { Home, Catalog, Onboarding }

class MainActivity : ComponentActivity() {
    private companion object { const val TAG = "EmailDebugX" }
    private var incomingLink by mutableStateOf<VerificationLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingLink = intent.takeIf { it?.action == Intent.ACTION_VIEW }
            ?.data?.let { parseVerificationLink(it, BuildConfig.AUTH_LINK_HOST) }
        Log.d("EmailDebugX", "onCreate action=${intent?.action} incoming_verification_link=${incomingLink != null} configured_api=${BuildConfig.AUTH_API_BASE_URL}")
        if (intent?.action == Intent.ACTION_VIEW) {
            Log.d(TAG, if (incomingLink != null) "Verification link received at launch" else "Unrecognized verification link at launch")
        }
        enableEdgeToEdge()
        setContent {
            var destination by rememberSaveable {
                mutableStateOf(if (incomingLink != null) EntryScreen.Onboarding else EntryScreen.Home)
            }
            LaunchedEffect(incomingLink) {
                if (incomingLink != null) destination = EntryScreen.Onboarding
            }
            LaunchedEffect(destination) {
                Log.d(TAG, "Showing ${destination.name} screen")
                if (destination == EntryScreen.Onboarding && BuildConfig.AUTH_API_BASE_URL.isBlank()) {
                    Log.w(TAG, "Live onboarding unavailable: attestraApiBaseUrl is missing")
                }
            }
            BackHandler(enabled = destination != EntryScreen.Home) {
                destination = EntryScreen.Home
                clearIncomingLink()
            }
            AttestraAuthTheme {
                when (destination) {
                    EntryScreen.Home -> OnboardingEntryScreen(
                        hasBackend = BuildConfig.AUTH_API_BASE_URL.isNotBlank(),
                        onStart = { destination = EntryScreen.Onboarding },
                        onCatalog = { destination = EntryScreen.Catalog },
                    )
                    EntryScreen.Catalog -> OnboardingCatalog(actions = OnboardingBusinessActions())
                    EntryScreen.Onboarding -> {
                        if (BuildConfig.AUTH_API_BASE_URL.isBlank()) {
                            BackendNotConfiguredScreen(onBack = {
                                destination = EntryScreen.Home
                                clearIncomingLink()
                            })
                        } else {
                            EmailOnboardingHost(
                                apiBaseUrl = BuildConfig.AUTH_API_BASE_URL,
                                link = incomingLink,
                                onLinkConsumed = ::clearIncomingLink,
                                onSignInRequired = { notice("Email sign-in will be available with the login flow.") },
                                onPasskeyDeferred = { notice("Your email is verified. You can add a passkey later.") },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLink = intent.takeIf { it.action == Intent.ACTION_VIEW }
            ?.data?.let { parseVerificationLink(it, BuildConfig.AUTH_LINK_HOST) }
        Log.d("EmailDebugX", "onNewIntent action=${intent.action} incoming_verification_link=${incomingLink != null}")
        if (intent.action == Intent.ACTION_VIEW) {
            Log.d(TAG, if (incomingLink != null) "Verification link received while app is open" else "Unrecognized verification link while app is open")
        }
    }

    private fun clearIncomingLink() {
        incomingLink = null
        intent?.data = null
    }

    private fun notice(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
