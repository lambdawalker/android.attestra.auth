package com.apexfission.android.auth.ui.onboarding.signin

import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.EmailOneTimeCodeField
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/** Screen used to prompt for an email address to resume an existing session or sign in. */
@Composable
fun ReturnSignInScreen(
    initialEmail: String,
    error: String?,
    onPasskey: (String) -> Unit,
    onEmail: (String) -> Unit,
    onBack: () -> Unit,
    onPassword: ((String) -> Unit)? = null,
) {
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    val valid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    OnboardingFrame(
        title = stringResource(R.string.auth_sign_in_to_continue_285),
        icon = Icons.Default.Lock,
        step = stringResource(R.string.auth_account),
        onBack = onBack,
        primaryLabel = stringResource(R.string.auth_sign_in_with_passkey),
        primaryEnabled = valid,
        onPrimary = { onPasskey(email.trim()) },
        secondaryLabel = stringResource(R.string.auth_send_an_email_code),
        secondaryEnabled = valid,
        onSecondary = { onEmail(email.trim()) },
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_enter_your_email_then_choose_how_to_sign_in_you_can_use_an_email))
            OutlinedTextField(
                email,
                { email = it },
                label = { Text(stringResource(R.string.auth_email_address)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (onPassword != null) {
                androidx.compose.material3.TextButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { onPassword(email.trim()) },
                    enabled = valid,
                ) {
                    Text(stringResource(R.string.auth_sign_in_with_password))
                }
            }
            if (error != null) {
                Text(
                    authText(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Screen used to input the six-digit sign-in code sent to the user's email. */
@Composable
fun ReturnEmailCodeScreen(
    email: String,
    error: String?,
    onVerify: (String) -> Unit,
    onBack: () -> Unit,
) {
    var code by remember(email) { mutableStateOf("") }
    val autofillManager = LocalAutofillManager.current
    OnboardingFrame(
        title = stringResource(R.string.auth_sign_in_with_email),
        icon = Icons.Default.Lock,
        step = stringResource(R.string.auth_account),
        onBack = {
            autofillManager?.cancel()
            onBack()
        },
        primaryLabel = stringResource(R.string.auth_verify_code),
        primaryEnabled = code.length == 6,
        onPrimary = {
            autofillManager?.cancel()
            onVerify(code)
        },
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_enter_the_six_digit_sign_in_code_sent_to_1_s_this_is_separate_fro, email))
            EmailOneTimeCodeField(
                value = code,
                onValueChange = { code = it },
                contentDescriptionText = stringResource(R.string.auth_six_digit_code),
            )
            if (error != null) {
                Text(
                    authText(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReturnSignInScreenPreview() {
    AttestraAuthTheme {
        ReturnSignInScreen(
            initialEmail = "",
            error = null,
            onPasskey = {},
            onEmail = {},
            onBack = {},
            onPassword = {},
        )
    }
}

@Preview
@Composable
private fun ReturnEmailCodeScreenPreview() {
    AttestraAuthTheme {
        ReturnEmailCodeScreen(
            email = "user@example.com",
            error = null,
            onVerify = {},
            onBack = {},
        )
    }
}
