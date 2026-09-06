package com.iykyk.facestory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoAccent,
    secondary = IndigoLight,
    onSecondary = Color.White,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = IndigoAccent,
    tertiary = IndigoAccent,
    onTertiary = BackgroundDeep,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun FaceStoryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}