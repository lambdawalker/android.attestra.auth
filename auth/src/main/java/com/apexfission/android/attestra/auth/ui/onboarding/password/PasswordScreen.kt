package com.apexfission.android.attestra.auth.ui.onboarding.password

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.apexfission.android.attestra.auth.discovery.PasswordPolicy
import com.apexfission.android.attestra.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Component providing an outlined text field configured for password input with policy validation feedback.
 */
@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, policy: PasswordPolicy? = null, optional: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onChange,
        label = { Text(if (optional) "Password (optional)" else "Password") },
        singleLine = true, visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
    if (policy != null) {
        policy.requirements().forEach { BodyText(it) }
        if (value.isNotEmpty()) {
            BodyText("Strength estimate: ${policy.strength(value)}")
            BodyText(if (policy.problems(value).isEmpty()) "Meets the configured requirements" else "Does not yet meet the configured requirements")
            policy.problems(value).forEach { BodyText(it) }
        }
    }
}

/**
 * Screen used for both entering an existing password or setting a new one during onboarding.
 */
@Composable
fun PasswordScreen(value: String, onChange: (String) -> Unit, onSubmit: () -> Unit,
    onBack: () -> Unit, error: String?, policy: PasswordPolicy? = null,
    optional: Boolean = false, onSkip: () -> Unit = {}, signIn: Boolean = false) {
    OnboardingFrame(title = if (signIn) "Sign in with password" else "Set your password", icon = Icons.Default.Lock,
        step = "Account", onBack = onBack,
        primaryLabel = if (signIn) "Sign in" else "Save password",
        primaryEnabled = value.isNotEmpty() && (policy == null || policy.problems(value).isEmpty()), onPrimary = onSubmit,
        secondaryLabel = if (optional) "Skip for now" else null, onSecondary = onSkip) {
        OnboardingCard {
            PasswordField(value, onChange, policy)
            if (error != null) BodyText(error)
        }
    }
}

@Preview
@Composable
private fun PasswordScreenPreview() {
    AttestraAuthTheme {
        PasswordScreen(
            value = "",
            onChange = {},
            onSubmit = {},
            onBack = {},
            error = null
        )
    }
}
