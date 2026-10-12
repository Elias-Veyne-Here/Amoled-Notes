package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// AMOLED True Black Color Scheme
val AmoledColorScheme = darkColorScheme(
    primary = AmoledWhite,
    onPrimary = AmoledBlack,
    primaryContainer = SurfaceLevel2,
    onPrimaryContainer = AmoledWhite,
    secondary = TextSecondary,
    onSecondary = AmoledBlack,
    secondaryContainer = SurfaceLevel1,
    onSecondaryContainer = AmoledWhite,
    tertiary = AmoledWhite,
    onTertiary = AmoledBlack,
    background = AmoledBlack,
    onBackground = AmoledWhite,
    surface = AmoledBlack,
    onSurface = AmoledWhite,
    surfaceVariant = SurfaceLevel1,
    onSurfaceVariant = TextSecondary,
    surfaceTint = AmoledWhite,
    outline = HairlineBorder,
    outlineVariant = SurfaceLevel1,
    error = Color(0xFFFF5252),
    onError = AmoledBlack
)

// Minimalist High-Contrast Light Color Scheme
val PureLightColorScheme = lightColorScheme(
    primary = AmoledBlack,
    onPrimary = AmoledWhite,
    primaryContainer = LightSurfaceLevel2,
    onPrimaryContainer = AmoledBlack,
    secondary = LightTextSecondary,
    onSecondary = AmoledWhite,
    secondaryContainer = LightSurfaceLevel1,
    onSecondaryContainer = AmoledBlack,
    tertiary = AmoledBlack,
    onTertiary = AmoledWhite,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightCanvas,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceLevel1,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = AmoledBlack,
    outline = LightHairlineBorder,
    outlineVariant = LightSurfaceLevel1,
    error = Color(0xFFD32F2F),
    onError = AmoledWhite
)

val AmoledShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    AMOLED
}

@Composable
fun AmoledNotesTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.AMOLED -> true
    }

    val colorScheme = if (isDark) AmoledColorScheme else PureLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AmoledShapes,
        content = content
    )
}
