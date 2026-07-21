package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val AmoledColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    secondary = AmoledSecondary,
    background = AmoledBackground,
    surface = AmoledSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val BlueColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    background = BlueBackground,
    surface = BlueSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = BlueOnBackground,
    onSurface = BlueOnBackground
)

private val GreenColorScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    background = GreenBackground,
    surface = GreenSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = GreenOnBackground,
    onSurface = GreenOnBackground
)

private val PurpleColorScheme = darkColorScheme(
    primary = PurpleNeonPrimary,
    secondary = PurpleNeonSecondary,
    background = PurpleNeonBackground,
    surface = PurpleNeonSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = PurpleNeonOnBackground,
    onSurface = PurpleNeonOnBackground
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
