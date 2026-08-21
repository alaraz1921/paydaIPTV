package com.payda.iptv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PayDaColorScheme = darkColorScheme(
    primary = PayDaPrimary,
    onPrimary = PayDaOnPrimary,
    secondary = PayDaTextSecondary,
    onSecondary = PayDaBackground,
    tertiary = PayDaSurfaceFocused,
    background = PayDaBackground,
    onBackground = PayDaTextPrimary,
    surface = PayDaSurface,
    onSurface = PayDaTextPrimary,
    surfaceVariant = PayDaSurfaceHigh,
    onSurfaceVariant = PayDaTextSecondary,
    outline = PayDaBorder,
    error = PayDaError,
    onError = PayDaBackground,
)

@Composable
fun PayDaIPTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PayDaColorScheme,
        typography = Typography,
        content = content
    )
}
