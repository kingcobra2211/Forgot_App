package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF25447C),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    background = Color(0xFF131316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2B2D35),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF00163E),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F5F8),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE2E2EC),
    onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF757780)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFDDDDDD),
    outline = Color(0xFF888888)
)

private val BlueColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E57),
    onPrimaryContainer = Color(0xFFB2F5FF),
    secondary = Color(0xFF80D8FF),
    onSecondary = Color(0xFF00354B),
    background = Color(0xFF0A192F),
    onBackground = Color(0xFFCCD6F6),
    surface = Color(0xFF172A45),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF203756),
    onSurfaceVariant = Color(0xFF8892B0),
    outline = Color(0xFF4C5D75)
)

private val GreenColorScheme = darkColorScheme(
    primary = Color(0xFF2ECC71),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF005327),
    onPrimaryContainer = Color(0xFF8DFFB2),
    secondary = Color(0xFF82E0AA),
    onSecondary = Color(0xFF00381B),
    background = Color(0xFF0F1E15),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF192C20),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF223E2D),
    onSurfaceVariant = Color(0xFFA5D6A7),
    outline = Color(0xFF4C6E56)
)

private val PurpleColorScheme = darkColorScheme(
    primary = Color(0xFFE040FB),
    onPrimary = Color(0xFF510061),
    primaryContainer = Color(0xFF730089),
    onPrimaryContainer = Color(0xFFFFD6FE),
    secondary = Color(0xFFF48FB1),
    onSecondary = Color(0xFF4C0027),
    background = Color(0xFF12001F),
    onBackground = Color(0xFFF3E5F5),
    surface = Color(0xFF24003B),
    onSurface = Color(0xFFF3E5F5),
    surfaceVariant = Color(0xFF380058),
    onSurfaceVariant = Color(0xFFE1BEE7),
    outline = Color(0xFF6A1B9A)
)

@Composable
fun ForgotTheme(
    themeKey: String = "dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeKey.lowercase()) {
        "light" -> DefaultLightColorScheme
        "dark" -> DefaultDarkColorScheme
        "amoled" -> AmoledColorScheme
        "blue" -> BlueColorScheme
        "green" -> GreenColorScheme
        "purple" -> PurpleColorScheme
        else -> DefaultDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
