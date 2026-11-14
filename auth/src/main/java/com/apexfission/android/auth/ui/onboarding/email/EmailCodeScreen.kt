package com.apexfission.android.auth.ui.onboarding.email

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.EmailOneTimeCodeField
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that prompts the user to enter the six-digit code sent to their email.
 */
@Composable
fun EmailCodeScreen(
    email: String,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    error: String? = null,
    attemptsRemaining: Int? = null,
    canResend: Boolean = true,
) {
    var code by remember { mutableStateOf("") }
    val codeDescription = stringResource(R.string.auth_six_digit_confirmation_code)
    val autofillManager = LocalAutofillManager.current
    OnboardingFrame(
        title = stringResource(R.string.auth_complete_email_verification),
        icon = Icons.Default.Mail,
        step = "2/5",
        onBack = {
            autofillManager?.cancel()
            onBack()
        },
        primaryLabel = if (error == null) {
            stringResource(R.string.auth_verify_email)
        } else {
            stringResource(R.string.auth_try_again)
        },
        onPrimary = {
            autofillManager?.cancel()
            onVerify(code)
        },
        primaryEnabled = code.length == 6,
        secondaryLabel = stringResource(R.string.auth_resend_email),
        onSecondary = {
            autofillManager?.cancel()
            onResend()
        },
        secondaryEnabled = canResend,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_enter_the_six_digit_code_from_the_email_to_confirm_this_address))
            Text(
                stringResource(R.string.auth_confirmation_code),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            EmailOneTimeCodeField(
                value = code,
                onValueChange = { code = it },
                contentDescriptionText = codeDescription,
            )
            if (error != null) {
                Text(
                    authText(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (attemptsRemaining != null) {
                Text(
                    stringResource(R.string.auth_attempts_remaining_1_s, attemptsRemaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (email.isNotBlank()) {
                Text(
                    email,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BodyText(stringResource(R.string.auth_the_code_is_separate_from_the_link_entering_it_does_not_submit_au))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailCodeScreenPreview() {
    AttestraAuthTheme {
        EmailCodeScreen("name@example.invalid", {}, {}, {})
    }
}
