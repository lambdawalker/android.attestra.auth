package com.apexfission.android.attestra.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.apexfission.android.attestra.auth.email.EmailOnboardingHost
import com.apexfission.android.attestra.auth.email.VerificationLink
import com.apexfission.android.attestra.auth.email.parseVerificationLink
import com.apexfission.android.attestra.auth.ui.onboarding.OnboardingBusinessActions
import com.apexfission.android.attestra.auth.ui.onboarding.OnboardingCatalog
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

class MainActivity : ComponentActivity() {
    private var incomingLink by mutableStateOf<VerificationLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingLink = intent.takeIf { it?.action == Intent.ACTION_VIEW }
            ?.data?.let { parseVerificationLink(it, BuildConfig.AUTH_LINK_HOST) }
        enableEdgeToEdge()
        setContent {
            AttestraAuthTheme {
                if (BuildConfig.AUTH_API_BASE_URL.isBlank()) {
                    // Safe design preview until a deployment URL is supplied at build time.
                    OnboardingCatalog(actions = OnboardingBusinessActions())
                } else {
                    EmailOnboardingHost(
                        apiBaseUrl = BuildConfig.AUTH_API_BASE_URL,
                        link = incomingLink,
                        onLinkConsumed = {
                            incomingLink = null
                            intent?.data = null
                        },
                        onSignInRequired = { notice("Email sign-in will be available with the login flow.") },
                        onPasskeyRequested = { notice("Passkey creation will be available with the passkey flow.") },
                        onPasskeyDeferred = { notice("Your email is verified. You can add a passkey later.") },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLink = intent.takeIf { it.action == Intent.ACTION_VIEW }
            ?.data?.let { parseVerificationLink(it, BuildConfig.AUTH_LINK_HOST) }
    }

    private fun notice(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
