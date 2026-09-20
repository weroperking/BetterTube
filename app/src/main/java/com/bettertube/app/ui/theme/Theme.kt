package com.bettertube.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.Black,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = Color.Black,
    secondary = BrandPrimaryDark,
    onSecondary = Color.Black,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BgSecondary,
    onSurfaceVariant = TextSecondary,
    outline = Divider
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.Black,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = Color.Black,
    secondary = BrandPrimaryDark,
    onSecondary = Color.Black,
    background = BgDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = Divider
)

@Composable
fun BetterTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is explicitly disabled to enforce the BetterTube palette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
