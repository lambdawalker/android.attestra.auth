package com.apexfission.android.attestra.auth.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme
import com.apexfission.android.attestra.auth.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteEmailVerificationScreen(
    email: String = "name@example.invalid",
    onBackClicked: () -> Unit = {},
    onVerifyClicked: (String) -> Unit = {},
    onResendClicked: () -> Unit = {}
) {
    var passcodeDigits by remember { mutableStateOf(List(6) { "" }) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    var isLoading by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AttestraSurface.copy(alpha = 0.9f))
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Logo / Brand
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AttestraPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Attestra Logo",
                                    tint = AttestraPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Attestra",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AttestraOnSurface
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = AttestraOnSurface
                            )
                        }
                    },
                    actions = {
                        // Step badge 2/5
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AttestraSurfaceContainerHigh,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "2/5",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AttestraOnSurfaceVariant
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                // Progress bar (40%)
                LinearProgressIndicator(
                    progress = { 0.4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AttestraPrimary,
                    trackColor = AttestraSurfaceContainerHighest.copy(alpha = 0.6f)
                )
            }
        },
        containerColor = AttestraSurface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Title Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AttestraSurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = AttestraPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Complete email verification",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AttestraOnSurface,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                // Main Card Container
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AttestraSurfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Enter the six-digit code from your email to confirm on this device.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AttestraOnSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        )

                        // Passcode digits inputs (6 inputs)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 0 until 6) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (passcodeDigits[i].isNotEmpty()) AttestraSurfaceContainerHigh
                                                else AttestraSurfaceContainer
                                            )
                                            .border(
                                                width = if (passcodeDigits[i].isNotEmpty()) 2.dp else 0.dp,
                                                color = if (passcodeDigits[i].isNotEmpty()) AttestraPrimary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = passcodeDigits[i],
                                            onValueChange = { newValue ->
                                                val filtered = newValue.filter { it.isDigit() }.take(1)
                                                if (filtered != passcodeDigits[i]) {
                                                    val updated = passcodeDigits.toMutableList()
                                                    updated[i] = filtered
                                                    passcodeDigits = updated

                                                    // Auto advance focus
                                                    if (filtered.isNotEmpty() && i < 5) {
                                                        focusRequesters[i + 1].requestFocus()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .focusRequester(focusRequesters[i]),
                                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                                color = AttestraOnSurface,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    innerTextField()
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Confirmation code",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = AttestraOnSurface
                                    )
                                )
                                Text(
                                    text = "6 digits",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AttestraOnSurfaceVariant
                                    )
                                )
                            }

                            Text(
                                text = "We need the code because this device does not have the session where you started.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AttestraOnSurfaceVariant,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Email item row
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AttestraSurfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(AttestraPrimaryContainer.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mail,
                                            contentDescription = null,
                                            tint = AttestraPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = AttestraOnSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(AttestraSecondary)
                                            )
                                            Text(
                                                text = "Verification link opened",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = AttestraSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = AttestraPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            onVerifyClicked(passcodeDigits.joinToString(""))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVerified) AttestraSecondary else Color(0xFF3B82F6),
                            contentColor = if (isVerified) AttestraOnSecondary else Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Securing session...")
                        } else if (isVerified) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Verification preview")
                        } else {
                            Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Complete email verification")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }

                    TextButton(
                        onClick = onResendClicked,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Resend email",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AttestraOnSurfaceVariant
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AttestraSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Your confirmation code is separate from the link.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AttestraOnSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CompleteEmailVerificationScreenPreview() {
    AttestraAuthTheme {
        CompleteEmailVerificationScreen()
    }
}
