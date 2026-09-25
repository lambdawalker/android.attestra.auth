package com.apexfission.android.attestra.auth.ui.onboarding.email

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

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
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    OnboardingFrame(
        title = "Complete email verification", icon = Icons.Default.Mail, step = "2/5",
        onBack = onBack, primaryLabel = if (error == null) "Verify email" else "Try again",
        onPrimary = { onVerify(code) }, primaryEnabled = code.length == 6,
        secondaryLabel = "Resend email", onSecondary = onResend, secondaryEnabled = canResend,
    ) {
        OnboardingCard {
            BodyText("Enter the six-digit code from the email you just opened to confirm on this device.")
            Text("Confirmation code", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    .semantics { contentDescription = "Six-digit confirmation code" },
                decorationBox = { innerTextField ->
                    Box {
                        Row(
                            Modifier.fillMaxWidth().clickable { focus.requestFocus() },
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            repeat(6) { index ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            code.getOrNull(index)?.toString() ?: "",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.size(1.dp).alpha(0.01f)) { innerTextField() }
                    }
                },
            )
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (attemptsRemaining != null) {
                Text("Attempts remaining: $attemptsRemaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(email, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BodyText("The code is separate from the link. Entering it does not submit automatically.")
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
