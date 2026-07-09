package com.valid.motouring.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MotouringColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Charcoal950,
    secondary = AccentPrimary,
    onSecondary = Charcoal950,
    background = Charcoal900,
    onBackground = OffWhite,
    surface = Charcoal800,
    onSurface = OffWhite,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = Muted,
    outline = Charcoal600,
    error = AccentPrimary,
    onError = Charcoal950,
)

private val MotouringShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun MotouringTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotouringColorScheme,
        typography = MotouringTypography,
        shapes = MotouringShapes,
        content = content,
    )
}
