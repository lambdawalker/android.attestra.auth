package com.apexfission.android.attestra.auth.ui.onboarding.email

import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

@Composable
fun EmailStartScreen(onBack: () -> Unit, onContinue: (String) -> Unit, error: String? = null) {
    var email by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val valid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    OnboardingFrame(
        title = "Start with your email", icon = Icons.Default.MarkEmailRead, step = "1/5",
        onBack = onBack, primaryLabel = "Continue",
        onPrimary = { attempted = true; if (valid) onContinue(email.trim()) },
    ) {
        OnboardingCard {
            BodyText("Enter your email to get a confirmation link and a six-digit code.")
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, isError = attempted && !valid,
                supportingText = {
                    if (attempted && !valid) Text("Enter a valid email address.")
                    else Text("We show the same next step for every email address.")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailStartScreenPreview() {
    AttestraAuthTheme {
        EmailStartScreen({}, {})
    }
}
