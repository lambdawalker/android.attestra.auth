package com.apexfission.android.attestra.auth

import androidx.compose.runtime.Composable
import com.apexfission.android.attestra.auth.identity.IdentityIntegrationUnavailable

internal const val HAS_IDENTITY_DEMO = false
@Composable internal fun IdentityDemo(onExit: () -> Unit) = IdentityIntegrationUnavailable(onExit)
@Composable internal fun DirectIdentityCapture(onExit: () -> Unit) = LiveCaptureFlow(onExit)
@Composable internal fun AccountIdentityFlow(passkeyAdded: Boolean, onExit: () -> Unit) = LiveCaptureFlow(onExit)
