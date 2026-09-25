package com.apexfission.android.attestra.auth.ui.onboarding

import android.util.Patterns
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.ui.theme.AttestraError
import com.apexfission.android.attestra.auth.ui.theme.AttestraOnSurface
import com.apexfission.android.attestra.auth.ui.theme.AttestraOnSurfaceVariant
import com.apexfission.android.attestra.auth.ui.theme.AttestraSurfaceContainerHigh

@Composable
fun EmailStartScreen(onBack: () -> Unit, onContinue: (String) -> Unit) {
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
        }
    }
}

@Composable
fun EmailWaitScreen(
    email: String, onBack: () -> Unit, onResend: () -> Unit, onChangeEmail: () -> Unit,
    canResend: Boolean = true, resendHelp: String? = null,
) {
    OnboardingFrame(
        title = "Check your email", icon = Icons.Default.Visibility, step = "2/5", onBack = onBack,
        primaryLabel = "Resend email", onPrimary = onResend, primaryEnabled = canResend,
        secondaryLabel = "Change email address", onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            BodyText("If a message arrives, open its confirmation link for:")
            Text(email, fontWeight = FontWeight.SemiBold)
            BodyText("Keep the six-digit code in that email available in case it is requested.")
            if (resendHelp != null) BodyText(resendHelp)
        }
    }
}

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
    val focus = androidx.compose.runtime.remember { FocusRequester() }
    OnboardingFrame(
        title = "Complete email verification", icon = Icons.Default.Mail, step = "2/5",
        onBack = onBack, primaryLabel = if (error == null) "Verify email" else "Try again",
        onPrimary = { onVerify(code) }, primaryEnabled = code.length == 6,
        secondaryLabel = "Resend email", onSecondary = onResend, secondaryEnabled = canResend,
    ) {
        OnboardingCard {
            BodyText("Enter the six-digit code from the email you just opened to confirm on this device.")
            Text("Confirmation code", fontWeight = FontWeight.SemiBold)
            // One actual input supports paste and autofill; the boxes are presentation only.
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
                                    color = AttestraSurfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            code.getOrNull(index)?.toString() ?: "",
                                            color = AttestraOnSurface,
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
            if (error != null) Text(error, color = AttestraError, style = MaterialTheme.typography.bodyMedium)
            if (attemptsRemaining != null) {
                Text("Attempts remaining: $attemptsRemaining", style = MaterialTheme.typography.bodySmall)
            }
            Text(email, style = MaterialTheme.typography.labelMedium, color = AttestraOnSurfaceVariant)
            BodyText("The code is separate from the link. Entering it does not submit automatically.")
        }
    }
}

@Composable
fun EmailAttemptLimitScreen(
    email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit,
    canRequestEmail: Boolean, retryAfter: String? = null,
) {
    OnboardingFrame(
        title = "Too many code attempts", icon = Icons.Default.LockClock, step = "2/5", onBack = onBack,
        primaryLabel = "Request a new email", primaryEnabled = canRequestEmail, onPrimary = onRequestEmail,
        secondaryLabel = "Change email address", onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            Text("This code can no longer be used", fontWeight = FontWeight.SemiBold)
            BodyText("Request another email when available. Open its latest link and use its new code together.")
            Text(email, color = AttestraOnSurfaceVariant)
            if (!canRequestEmail) BodyText(retryAfter ?: "Please try again later.")
        }
    }
}

@Composable
fun EmailLinkUnusableScreen(email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit) {
    OnboardingFrame(
        title = "This confirmation link can’t be used", icon = Icons.Default.LinkOff, step = "2/5", onBack = onBack,
        primaryLabel = "Request a new email", onPrimary = onRequestEmail,
        secondaryLabel = "Change email address", onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            BodyText("This link may have expired or been replaced. Request another email and open its latest link.")
            Text(email, color = AttestraOnSurfaceVariant)
            BodyText("The old link and code cannot be reused.")
        }
    }
}

@Composable
fun EmailSessionRecoveryScreen(onBack: () -> Unit, onSignIn: () -> Unit) {
    OnboardingFrame(
        title = "Email verified", icon = Icons.Default.MarkEmailRead, step = "2/5", onBack = onBack,
        primaryLabel = "Sign in to continue", onPrimary = onSignIn,
    ) {
        OnboardingCard {
            BodyText("Your email was confirmed, but this device could not finish signing in.")
            BodyText("Sign in with an email code to continue to passkey setup. You do not need to confirm your email again.")
        }
    }
}

enum class LoadingTask(val title: String, val description: String, val step: String) {
    CONFIRM_EMAIL("Verifying your email", "We are confirming your email and preparing your session.", "2/5"),
    RESEND_EMAIL("Requesting another email", "We are processing your request.", "2/5"),
    OPEN_PASSKEY_MANAGER("Opening your passkey manager", "Follow your device or password manager to create a passkey.", "3/5"),
    SAVE_PASSKEY("Saving your passkey", "We are adding the passkey to your account.", "3/5"),
    READ_DOCUMENT("Reading your document", "We are extracting details for you to review.", "ID check"),
    SUBMIT_ID("Submitting your ID details", "We are sending your reviewed details for an identity check.", "ID check"),
    CHECK_IDENTITY("Checking your identity", "Your details have been submitted. We will show the result when available.", "ID check"),
}

@Composable
fun OnboardingLoadingScreen(task: LoadingTask, onBack: () -> Unit, onLeave: (() -> Unit)? = null) {
    OnboardingFrame(
        title = task.title, icon = Icons.Default.Autorenew, step = task.step, onBack = onBack,
        secondaryLabel = if (task == LoadingTask.CHECK_IDENTITY) "Go to dashboard" else null,
        onSecondary = { onLeave?.invoke() },
    ) {
        OnboardingCard {
            WaitingIndicator(task.description)
            BodyText("This may take a moment. We will update the screen when it finishes.")
        }
    }
}
