package com.apexfission.android.attestra.auth.ui.onboarding.email

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

class EmailCodeScreenshotTest {

    @PreviewTest
    @Preview(name = "Empty", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF0F131B)
    @Composable
    fun emptyState() {
        AttestraAuthTheme {
            EmailCodeScreen(
                email = "user@example.com",
                onBack = {},
                onVerify = {},
                onResend = {},
                error = null,
                attemptsRemaining = null,
                canResend = true
            )
        }
    }

    @PreviewTest
    @Preview(name = "With Error", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF0F131B)
    @Composable
    fun withError() {
        AttestraAuthTheme {
            EmailCodeScreen(
                email = "user@example.com",
                onBack = {},
                onVerify = {},
                onResend = {},
                error = "Incorrect code. Please try again.",
                attemptsRemaining = 2,
                canResend = true
            )
        }
    }

    @PreviewTest
    @Preview(name = "Cannot Resend", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF0F131B)
    @Composable
    fun cannotResend() {
        AttestraAuthTheme {
            EmailCodeScreen(
                email = "user@example.com",
                onBack = {},
                onVerify = {},
                onResend = {},
                error = null,
                attemptsRemaining = null,
                canResend = false
            )
        }
    }
}
