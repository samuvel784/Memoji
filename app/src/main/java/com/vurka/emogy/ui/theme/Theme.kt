package com.vurka.emogy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Default: Red + Blue
private val DefaultColorScheme = darkColorScheme(
    primary = VividRed,
    secondary = RoyalBlue,
    tertiary = BrightCyan,
    background = DefaultBg,
    surface = DefaultSurface,
    surfaceVariant = DefaultSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextWhite
)

// Light: White-based
private val LightColorScheme = lightColorScheme(
    primary = VividRed,
    secondary = RoyalBlue,
    tertiary = BrightCyan,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextDark
)

// Dark: Dark-based
private val DarkColorScheme = darkColorScheme(
    primary = VividRed,
    secondary = RoyalBlue,
    tertiary = BrightCyan,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextWhite
)

@Composable
fun MemoryMatchTheme(
    themeMode: String = "Default",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "Light" -> LightColorScheme
        "Dark" -> DarkColorScheme
        else -> DefaultColorScheme
    }

    val reduceMotion = rememberReduceMotion()

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
