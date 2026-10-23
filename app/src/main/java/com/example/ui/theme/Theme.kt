package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { false }

enum class AppThemeMode(val label: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark")
}

private val DarkColorScheme = darkColorScheme(
    primary = AuraCrimsonDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF420D1A),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = AuraEmerald,
    onSecondary = Color(0xFF00210B),
    secondaryContainer = Color(0xFF0E391F),
    onSecondaryContainer = Color(0xFFA2F5BC),
    tertiary = AuraIndigo,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = AuraCrimsonLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5E8),
    onPrimaryContainer = Color(0xFF3E000A),
    secondary = AuraEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDFBE6),
    onSecondaryContainer = Color(0xFF053818),
    tertiary = AuraIndigo,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline
)

@Composable
fun AuraMusicTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
