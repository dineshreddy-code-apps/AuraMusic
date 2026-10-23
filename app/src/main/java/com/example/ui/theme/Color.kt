package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Signature Apple Music + Spotify Inspired Palette
val AuraCrimsonLight = Color(0xFFFA243C)
val AuraCrimsonDark = Color(0xFFFF375F)
val AuraEmerald = Color(0xFF1DB954)
val AuraAmber = Color(0xFFFF9F0A)
val AuraIndigo = Color(0xFF5E5CE6)
val AuraLightBlue = Color(0xFF64D2FF)

// Light Mode Surfaces (Minimalist Apple Music Alabaster & Frosted Pearl)
val LightBackground = Color(0xFFFAFAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F1F6)
val LightSurfaceContainer = Color(0xFFEAEAF2)
val LightOnBackground = Color(0xFF111114)
val LightOnSurface = Color(0xFF16161A)
val LightOnSurfaceVariant = Color(0xFF686873)
val LightOutline = Color(0xFFDCDCE5)

// Dark Mode Surfaces (Obsidian Studio & Elevated Charcoal)
val DarkBackground = Color(0xFF0A0A0E)
val DarkSurface = Color(0xFF15151C)
val DarkSurfaceVariant = Color(0xFF20202B)
val DarkSurfaceContainer = Color(0xFF282836)
val DarkOnBackground = Color(0xFFF5F5F7)
val DarkOnSurface = Color(0xFFF5F5F7)
val DarkOnSurfaceVariant = Color(0xFF9898A6)
val DarkOutline = Color(0xFF2F2F3E)

// Curated Album & Singer Cover Art Palette Seeds
val StudioCoverPalettes = listOf(
    0xFFFA243CL, // Crimson Pulse
    0xFF5E5CE6L, // Electric Indigo
    0xFF1DB954L, // Spotify Emerald
    0xFFFF9F0AL, // Sunset Amber
    0xFF0A84FFL, // Cobalt Vinyl
    0xFFBF5AF2L, // Velvet Violet
    0xFF00C7BEL, // Acoustic Teal
    0xFFFF6482L  // Coral Synth
)

fun parseColorOrDefault(hex: Long): Color = Color(hex)
