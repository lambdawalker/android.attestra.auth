package com.apexfission.android.attestra.auth.ui.onboarding.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.ui.theme.AttestraOnSurface
import com.apexfission.android.attestra.auth.ui.theme.AttestraOnSurfaceVariant
import com.apexfission.android.attestra.auth.ui.theme.AttestraPrimary
import com.apexfission.android.attestra.auth.ui.theme.AttestraSecondary
import com.apexfission.android.attestra.auth.ui.theme.AttestraSurface
import com.apexfission.android.attestra.auth.ui.theme.AttestraSurfaceContainerHigh
import com.apexfission.android.attestra.auth.ui.theme.AttestraSurfaceContainerLow

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
        containerColor = AttestraSurface,
        topBar = {
            TopAppBar(
                title = { Text("Attestra", fontWeight = FontWeight.Bold, color = AttestraOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AttestraOnSurface)
                    }
                },
                actions = {
                    Text(
                        step,
                        style = MaterialTheme.typography.labelMedium,
                        color = AttestraOnSurfaceVariant,
                        modifier = Modifier.padding(end = 20.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AttestraSurface),
            )
        },
        bottomBar = {
            if (primaryLabel != null || secondaryLabel != null) {
                Surface(
                    color = AttestraSurface,
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
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                            ) { Text(primaryLabel) }
                        }
                        if (secondaryLabel != null) {
                            OutlinedButton(
                                onClick = onSecondary,
                                enabled = secondaryEnabled,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) { Text(secondaryLabel, color = AttestraOnSurface) }
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
                        Modifier.size(48.dp).background(AttestraSurfaceContainerHigh, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = AttestraPrimary)
                    }
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AttestraOnSurface)
                }
                content()
            }
        }
    }
}

@Composable
fun OnboardingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AttestraSurfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

@Composable
fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AttestraOnSurfaceVariant)
}

@Composable
fun StatusRow(label: String, detail: String, complete: Boolean = true) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (complete) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                tint = if (complete) AttestraSecondary else AttestraOnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(label, fontWeight = FontWeight.SemiBold, color = AttestraOnSurface)
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = AttestraOnSurfaceVariant)
        HorizontalDivider(color = AttestraSurfaceContainerHigh)
    }
}

@Composable
fun WaitingIndicator(description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CircularProgressIndicator(color = AttestraPrimary)
        BodyText(description)
    }
}
