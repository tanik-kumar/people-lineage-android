package com.tanik.peoplelineage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SeedGreen,
    secondary = AccentRust,
    tertiary = AccentMuted,
    background = SurfaceWarm,
    surface = CardWarm,
    surfaceVariant = SurfaceContainerWarm,
    primaryContainer = PrimaryContainerWarm,
    onPrimaryContainer = PrimaryDeep,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = TextStrong,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextStrong,
    onSurface = TextStrong,
    onSurfaceVariant = TextSoft,
    outline = BorderSoft,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    secondary = AccentRust,
    tertiary = AccentMuted,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF24312C),
    primaryContainer = Color(0xFF24473C),
    onPrimaryContainer = Color(0xFFD7ECE3),
    secondaryContainer = DarkAccentContainer,
    onSecondaryContainer = Color(0xFFF7E5DA),
    onPrimary = Color(0xFF10211A),
    onSecondary = Color.White,
    onBackground = Color(0xFFF2F5F2),
    onSurface = Color(0xFFF2F5F2),
    onSurfaceVariant = Color(0xFFB2BDB6),
    outline = Color(0xFF495750),
)

@Composable
fun PeopleLineageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
