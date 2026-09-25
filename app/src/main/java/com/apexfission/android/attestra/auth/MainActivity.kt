package com.apexfission.android.attestra.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apexfission.android.attestra.auth.ui.onboarding.OnboardingBusinessActions
import com.apexfission.android.attestra.auth.ui.onboarding.OnboardingCatalog
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttestraAuthTheme {
                // UI gallery until the onboarding host routes real backend outcomes.
                OnboardingCatalog(actions = OnboardingBusinessActions())
            }
        }
    }
}
