package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val JaniHolographicColorScheme = darkColorScheme(
    primary = HoloCyan,
    onPrimary = HoloVoid,
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = HoloCyan,
    secondary = HoloViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1E59),
    onSecondaryContainer = Color(0xFFE0D4FF),
    tertiary = HoloMint,
    onTertiary = HoloVoid,
    tertiaryContainer = Color(0xFF073B2E),
    onTertiaryContainer = HoloMint,
    background = HoloVoid,
    onBackground = HoloTextPrimary,
    surface = HoloObsidian,
    onSurface = HoloTextPrimary,
    surfaceVariant = HoloGlass,
    onSurfaceVariant = HoloTextSecondary,
    surfaceContainerLow = HoloObsidian,
    surfaceContainer = HoloGlass,
    surfaceContainerHigh = HoloGlassElevated,
    outline = HoloBorder,
    outlineVariant = HoloBorderGlow,
    error = HoloRose,
    onError = Color.White,
    errorContainer = Color(0xFF4A1021),
    onErrorContainer = Color(0xFFFFD9E0)
)

val JaniShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JaniAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JaniHolographicColorScheme,
        typography = JaniTypography,
        shapes = JaniShapes,
        content = content
    )
}
