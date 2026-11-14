package com.apexfission.android.auth.ui.onboarding.email

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that instructs the user to check their email for a verification link or code.
 */
@Composable
fun EmailWaitScreen(
    email: String,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onEnterCode: () -> Unit,
    canResend: Boolean = true,
    resendHelp: String? = null,
    onSignIn: (() -> Unit)? = null,
) {
    OnboardingFrame(
        title = stringResource(R.string.auth_check_your_email),
        icon = Icons.Default.Visibility,
        step = "2/5",
        onBack = onBack,
        primaryLabel = stringResource(R.string.auth_resend_email),
        onPrimary = onResend,
        primaryEnabled = canResend,
        secondaryLabel = if (onSignIn != null) {
            stringResource(R.string.auth_already_verified_sign_in)
        } else {
            stringResource(R.string.auth_change_email_address)
        },
        onSecondary = {
            if (onSignIn != null) onSignIn() else onChangeEmail()
        },
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_open_the_link_or_enter_the_code_from_the_latest_email_for))
            if (email.isNotBlank()) {
                Text(email, style = MaterialTheme.typography.titleSmall)
            }
            OutlinedButton(
                onClick = onEnterCode,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(stringResource(R.string.auth_enter_verification_code))
            }
            BodyText(stringResource(R.string.auth_keep_the_six_digit_code_in_that_email_available_in_case_it_is_req))
            if (resendHelp != null) BodyText(resendHelp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailWaitScreenPreview() {
    AttestraAuthTheme {
        EmailWaitScreen("name@example.invalid", {}, {}, {}, {})
    }
}
