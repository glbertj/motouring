package com.valid.motouring.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MotouringColorScheme = darkColorScheme(
    primary = Amber500,
    secondary = Red500,
    background = Charcoal900,
    surface = Charcoal800,
    surfaceVariant = Charcoal700,
    onPrimary = Charcoal900,
    onBackground = OffWhite,
    onSurface = OffWhite,
    onSurfaceVariant = Muted,
    error = Red500,
)

@Composable
fun MotouringTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotouringColorScheme,
        typography = MotouringTypography,
        content = content,
    )
}
