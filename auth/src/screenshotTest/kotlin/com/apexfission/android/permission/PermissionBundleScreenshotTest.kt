package com.apexfission.android.permission

import android.Manifest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.permission.recipe.PermissionStatus
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionBundleScreen
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverview

class PermissionBundleScreenshotTest {
    private val defaultOverview = PermissionOverview {
        DefaultPermissionPage(
            "Permissions", Icons.Default.Lock,
            title = "Enable this feature",
            body = "Review the access this feature needs. You can choose which access to grant in Android’s prompts.",
        )
    }

    private val permissions = listOf(
        PermissionDescription(
            permission = Manifest.permission.CAMERA,
            label = "Camera",
            icon = Icons.Default.PhotoCamera,
            page = {
                DefaultPermissionPage(
                    "Camera", Icons.Default.PhotoCamera,
                    title = "Scan a document",
                    body = "We use the camera when you choose to scan a document."
                )
            },
        ),
        PermissionDescription(
            permission = Manifest.permission.RECORD_AUDIO,
            label = "Microphone",
            icon = Icons.Default.Mic,
        ) { DefaultPermissionPage("Microphone", Icons.Default.Mic) },
    )

    @PreviewTest
    @Preview(name = "Camera page", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun autoplayReadingProgress() {
        MaterialTheme {
            PermissionBundleScreen(
                permissions = permissions,
                statuses = listOf(PermissionStatus.NotRequested, PermissionStatus.NotRequested),
                onBack = {}, onNotNow = {}, onRequest = {}, onOpenSettings = {},
                autoAdvance = false,
                overview = defaultOverview,
                initialPage = 1, // overview segment complete, camera segment in progress
            )
        }
    }

    @PreviewTest
    @Preview(name = "Overview State", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun overviewState() {
        MaterialTheme {
            PermissionBundleScreen(
                permissions = permissions,
                statuses = listOf(PermissionStatus.NotRequested, PermissionStatus.NotRequested),
                onBack = {}, onNotNow = {}, onRequest = {}, onOpenSettings = {},
                autoAdvance = false,
                overview = defaultOverview,
                initialPage = 0,
            )
        }
    }

    @PreviewTest
    @Preview(name = "Microphone State", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun microphoneState() {
        MaterialTheme {
            PermissionBundleScreen(
                permissions = permissions,
                statuses = listOf(PermissionStatus.Granted, PermissionStatus.NotRequested),
                onBack = {}, onNotNow = {}, onRequest = {}, onOpenSettings = {},
                autoAdvance = false,
                overview = defaultOverview,
                initialPage = 2,
            )
        }
    }

    @PreviewTest
    @Preview(name = "Denied State", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun deniedState() {
        MaterialTheme {
            PermissionBundleScreen(
                permissions = permissions,
                statuses = listOf(PermissionStatus.PermanentlyDenied, PermissionStatus.NotRequested),
                onBack = {}, onNotNow = {}, onRequest = {}, onOpenSettings = {},
                autoAdvance = false,
                overview = defaultOverview,
                initialPage = 1,
            )
        }
    }
}
