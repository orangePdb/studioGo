package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalEditorSyntaxColors = staticCompositionLocalOf { DarkSyntaxColors }

private val StudioDarkColorScheme = darkColorScheme(
    primary = AndroidEmerald,
    onPrimary = Color(0xFF072112),
    primaryContainer = Color(0xFF143524),
    onPrimaryContainer = Color(0xFF8EF4B8),
    secondary = StudioBlue,
    onSecondary = Color(0xFF081A3A),
    secondaryContainer = Color(0xFF1C2D4D),
    onSecondaryContainer = Color(0xFFBFD7FF),
    tertiary = KotlinViolet,
    onTertiary = Color(0xFF201042),
    tertiaryContainer = Color(0xFF2E224C),
    onTertiaryContainer = Color(0xFFE5DAFF),
    background = StudioBgDark,
    onBackground = StudioTextPrimaryDark,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimaryDark,
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = StudioTextSecondaryDark,
    surfaceContainer = StudioSurfaceDark,
    surfaceContainerHigh = StudioSurfaceVariantDark,
    surfaceContainerHighest = StudioSurfaceElevatedDark,
    outline = StudioBorderDark,
    outlineVariant = Color(0xFF262B38),
    error = ErrorCrimson,
    onError = Color(0xFF3F0D0D),
    errorContainer = Color(0xFF4B191C),
    onErrorContainer = Color(0xFFFECACA),
)

private val StudioLightColorScheme = lightColorScheme(
    primary = AndroidEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = StudioBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = StudioBgLight,
    onBackground = StudioTextPrimaryLight,
    surface = StudioSurfaceLight,
    onSurface = StudioTextPrimaryLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = StudioTextSecondaryLight,
    surfaceContainer = StudioSurfaceLight,
    surfaceContainerHigh = StudioSurfaceVariantLight,
    surfaceContainerHighest = StudioSurfaceElevatedLight,
    outline = StudioBorderLight,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

@Composable
fun StudioDroidTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    val syntaxColors = if (darkTheme) DarkSyntaxColors else LightSyntaxColors

    CompositionLocalProvider(LocalEditorSyntaxColors provides syntaxColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StudioTypography,
            content = content,
        )
    }
}
