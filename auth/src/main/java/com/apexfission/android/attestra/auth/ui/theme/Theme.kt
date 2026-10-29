package com.apexfission.android.attestra.auth.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AttestraPrimary,
    onPrimary = AttestraOnPrimary,
    primaryContainer = AttestraPrimaryContainer,
    onPrimaryContainer = AttestraOnPrimaryContainer,
    secondary = AttestraSecondary,
    onSecondary = AttestraOnSecondary,
    secondaryContainer = AttestraSecondaryContainer,
    onSecondaryContainer = AttestraOnSecondaryContainer,
    background = AttestraBackground,
    surface = AttestraSurface,
    onSurface = AttestraOnSurface,
    onSurfaceVariant = AttestraOnSurfaceVariant,
    error = AttestraError,
    errorContainer = AttestraErrorContainer,
    onError = AttestraOnError,
    onErrorContainer = AttestraOnErrorContainer,
    surfaceContainerLowest = AttestraSurfaceContainerLowest,
    surfaceContainerLow = AttestraSurfaceContainerLow,
    surfaceContainer = AttestraSurfaceContainer,
    surfaceContainerHigh = AttestraSurfaceContainerHigh,
    surfaceContainerHighest = AttestraSurfaceContainerHighest,
    outline = AttestraOutline,
    outlineVariant = AttestraOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = AttestraLightHarborTeal,
    onPrimary = AttestraLightOnHarborTeal,
    primaryContainer = AttestraLightTealContainer,
    onPrimaryContainer = AttestraLightOnTealContainer,
    secondary = AttestraLightHarborTeal,
    onSecondary = AttestraLightOnHarborTeal,
    secondaryContainer = AttestraLightTealContainer,
    onSecondaryContainer = AttestraLightOnTealContainer,
    background = AttestraLightPaperCanvas,
    surface = AttestraLightClearSurface,
    onSurface = AttestraLightCharcoalInk,
    onSurfaceVariant = AttestraLightSlateCopy,
    error = AttestraLightErrorInk,
    errorContainer = AttestraLightErrorContainer,
    onError = AttestraLightOnError,
    onErrorContainer = AttestraLightOnErrorContainer,
    surfaceContainerLowest = AttestraLightSurfaceContainerLowest,
    surfaceContainerLow = AttestraLightSurfaceContainerLow,
    surfaceContainer = AttestraLightSurfaceContainer,
    surfaceContainerHigh = AttestraLightSurfaceContainerHigh,
    surfaceContainerHighest = AttestraLightSurfaceContainerHighest,
    outline = AttestraLightMistBorder,
    outlineVariant = AttestraLightMistBorder,
)

@Composable
fun AttestraAuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
