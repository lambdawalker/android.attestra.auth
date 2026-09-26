package com.apexfission.android.attestra.auth.ui.onboarding.signin

import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame

@Composable
fun ReturnSignInScreen(initialEmail: String, error: String?, onPasskey: (String) -> Unit, onEmail: (String) -> Unit, onBack: () -> Unit) {
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    val valid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    OnboardingFrame(title = "Sign in to continue", icon = Icons.Default.Lock, step = "Account", onBack = onBack,
        primaryLabel = "Sign in with passkey", primaryEnabled = valid, onPrimary = { onPasskey(email.trim()) },
        secondaryLabel = "Send an email code", secondaryEnabled = valid, onSecondary = { onEmail(email.trim()) }) {
        OnboardingCard {
            BodyText("Enter your email, then choose how to sign in. You can use an email code if a passkey is unavailable on this device.")
            OutlinedTextField(email, { email = it }, label = { Text("Email address") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            if (error != null) BodyText(error)
        }
    }
}

@Composable
fun ReturnEmailCodeScreen(email: String, error: String?, onVerify: (String) -> Unit, onBack: () -> Unit) {
    var code by rememberSaveable(email) { mutableStateOf("") }
    OnboardingFrame(title = "Sign in with email", icon = Icons.Default.Lock, step = "Account", onBack = onBack,
        primaryLabel = "Verify code", primaryEnabled = code.length == 6, onPrimary = { onVerify(code) }) {
        OnboardingCard {
            BodyText("Enter the six-digit sign-in code sent to $email. This is separate from your signup confirmation code.")
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("Six-digit code") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            if (error != null) BodyText(error)
        }
    }
}
