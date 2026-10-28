package com.apexfission.android.attestra.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
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

private enum class EntryScreen { Home, Catalog, Onboarding, IdentityDemo, IdentityCapture }

class MainActivity : ComponentActivity() {
    private companion object { const val TAG = "EmailDebugX" }
    private val environment by lazy { ViewModelProvider(this)[EnvironmentViewModel::class.java] }
    private val associationCheck by lazy { ViewModelProvider(this)[AssociationCheckViewModel::class.java] }
    private val localReset by lazy { ViewModelProvider(this)[LocalUserResetViewModel::class.java] }
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
            val associationWarning = associationCheck.warning
            LaunchedEffect(associationWarning) {
                associationWarning?.let { notice(it); associationCheck.consumeWarning() }
            }
            var destination by rememberSaveable {
                mutableStateOf(if (incomingLink != null && !localReset.resetting) EntryScreen.Onboarding else EntryScreen.Home)
            }
            val resetting = localReset.resetting
            LaunchedEffect(incomingLink, resetting) {
                if (incomingLink != null && !resetting) destination = EntryScreen.Onboarding
            }
            LaunchedEffect(destination) {
                Log.d(TAG, "Showing ${destination.name} screen")
                if (destination == EntryScreen.Onboarding && environment.configuration == null) {
                    Log.w(TAG, "Waiting for environment configuration")
                }
            }
            BackHandler(enabled = destination != EntryScreen.Home) {
                destination = EntryScreen.Home
                clearIncomingLink()
            }
            BackHandler(enabled = resetting) { /* Finish the local reset before leaving. */ }
            AttestraAuthTheme {
                when (destination) {
                    EntryScreen.Home -> OnboardingEntryScreen(
                        hasBackend = environment.configuration != null,
                        resetting = resetting,
                        resetMessage = localReset.message ?: environment.message,
                        onDeleteLocalUser = {
                            if (!resetting) {
                                clearIncomingLink()
                                localReset.deleteLocalUser()
                            }
                        },
                        onStart = { destination = EntryScreen.Onboarding },
                        onCatalog = { destination = EntryScreen.Catalog },
                        onIdentityDemo = if (HAS_IDENTITY_DEMO) { { destination = EntryScreen.IdentityDemo } } else null,
                    )
                    EntryScreen.IdentityDemo -> IdentityDemo(onExit = { destination = EntryScreen.Home })
                    EntryScreen.IdentityCapture -> {
                        val config = environment.configuration
                        if (HAS_IDENTITY_DEMO) DirectIdentityCapture(onExit = { destination = EntryScreen.Catalog })
                        else if (config != null) CompositionLocalProvider(LocalBackendConfiguration provides config) {
                            DirectIdentityCapture(onExit = { destination = EntryScreen.Catalog })
                        } else EnvironmentStatusScreen(environment.loading, environment.message, environment::load) { destination = EntryScreen.Catalog }
                    }
                    EntryScreen.Catalog -> OnboardingCatalog(actions = OnboardingBusinessActions(
                        onOpenIdentityCapture = { destination = EntryScreen.IdentityCapture },
                    ))
                    EntryScreen.Onboarding -> {
                        if (environment.configuration == null) {
                            EnvironmentStatusScreen(environment.loading, environment.message, environment::load, onBack = {
                                destination = EntryScreen.Home
                                clearIncomingLink()
                            })
                        } else {
                            val config = requireNotNull(environment.configuration)
                            CompositionLocalProvider(LocalBackendConfiguration provides config) {
                            EmailOnboardingHost(
                                apiBaseUrl = config.apiUrl,
                                onboarding = config.onboardingPolicy(),
                                onRetryConfiguration = environment::refreshRuntimeConfiguration,
                                link = incomingLink,
                                onLinkConsumed = ::clearIncomingLink,
                                identityContent = { passkeyAdded, exit -> AccountIdentityFlow(passkeyAdded, exit) },
                                onPasskeyDeferred = { destination = EntryScreen.Home; clearIncomingLink() },
                            )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        environment.refreshRuntimeConfiguration()
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
