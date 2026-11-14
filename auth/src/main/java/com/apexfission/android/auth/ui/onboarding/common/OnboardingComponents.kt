package com.apexfission.android.auth.ui.onboarding.common

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Component providing the standard layout frame and navigation bar for onboarding screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFrame(
    title: String,
    icon: ImageVector,
    step: String,
    onBack: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    secondaryEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Attestra", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.auth_back), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Text(
                        authText(step),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 20.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (primaryLabel != null || secondaryLabel != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (primaryLabel != null) {
                            Button(
                                onClick = onPrimary,
                                enabled = primaryEnabled,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                            ) { Text(authText(primaryLabel)) }
                        }
                        if (secondaryLabel != null) {
                            OutlinedButton(
                                onClick = onSecondary,
                                enabled = secondaryEnabled,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                            ) { Text(authText(secondaryLabel)) }
                        }
                    }
                }
            }
        }
    ) { inset ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(authText(title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                content()
            }
        }
    }
}

/**
 * Component providing a standard styled card for onboarding content.
 */
@Composable
fun OnboardingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

internal fun normalizeEmailOneTimeCode(value: String): String =
    value.filter(Char::isDigit).take(6)

/**
 * Shared six-digit email OTP input. The six slots are decoration around one
 * logical text field so keyboard input and clipboard paste behave like a normal field.
 */
@Composable
fun EmailOneTimeCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    val update: (String) -> Unit = { raw -> onValueChange(normalizeEmailOneTimeCode(raw)) }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = update,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .focusRequester(focus)
                .contentType(ContentType("emailOTPCode"))
                .semantics { contentDescription = contentDescriptionText },
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(6) { index ->
                            Surface(
                                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.extraSmall,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        value.getOrNull(index)?.toString() ?: "",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.matchParentSize().alpha(0.01f)) { innerTextField() }
                }
            },
        )
        TextButton(
            onClick = {
                clipboard.getText()?.text?.let(update)
                focus.requestFocus()
            },
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(R.string.auth_paste_code))
        }
    }
}

/**
 * Component providing a standard styled text for body paragraphs in onboarding screens.
 */
@Composable
fun BodyText(text: String) {
    Text(authText(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Component providing a row to display the status of a specific item or requirement.
 */
@Composable
fun StatusRow(label: String, detail: String, complete: Boolean = true) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (complete) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                tint = if (complete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

/**
 * Component providing a styled circular progress indicator and description text.
 */
@Composable
fun WaitingIndicator(description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        BodyText(description)
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingFramePreview() {
    AttestraAuthTheme {
        OnboardingFrame(
            title = "Test Frame",
            icon = Icons.Default.Shield,
            step = "1/1",
            onBack = {},
            primaryLabel = "Continue",
            secondaryLabel = "Cancel"
        ) {
            OnboardingCard {
                BodyText("This is body text inside an onboarding card.")
                StatusRow("Feature", "Detail text", complete = true)
                WaitingIndicator("Please wait...")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "OnboardingCard", showBackground = true)
@Composable
private fun OnboardingCardPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                OnboardingCard { BodyText("Review your account details.") }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "BodyText", showBackground = true)
@Composable
private fun BodyTextPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                BodyText("Your progress is saved on this device.")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "StatusRow", showBackground = true)
@Composable
private fun StatusRowPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                StatusRow("Email confirmed", "Continue with your account", complete = true)
                StatusRow("ID capture", "Waiting for your document", complete = false)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "WaitingIndicator", showBackground = true)
@Composable
private fun WaitingIndicatorPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                WaitingIndicator("Preparing your document…")
            }
        }
    }
}
