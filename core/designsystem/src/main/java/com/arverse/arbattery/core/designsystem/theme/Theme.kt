package com.arverse.arbattery.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = TextPrimaryDark,
    background = OneUiDarkBackground,
    onBackground = TextPrimaryDark,
    surface = OneUiDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = OneUiDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = OneUiDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    onPrimary = OneUiLightSurface,
    background = OneUiLightBackground,
    onBackground = TextPrimaryLight,
    surface = OneUiLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = OneUiLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OneUiLightBorder
)

@Composable
fun ArBatteryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = OneUiShapes,
        typography = OneUiTypography,
        content = content
    )
}
