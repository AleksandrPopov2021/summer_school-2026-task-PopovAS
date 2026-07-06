package ru.vertical.climbing.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color_White,
    secondary = BrandOrange,
    onSecondary = Color_White,
    background = BrandSand,
    onBackground = BrandCharcoal,
    surface = NeutralSurface,
    onSurface = BrandCharcoal,
    error = BrandError,
    onError = Color_White,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color_White,
    secondary = BrandOrange,
    onSecondary = Color_White,
    background = NeutralSurfaceDark,
    onBackground = BrandSand,
    surface = BrandGreenDark,
    onSurface = BrandSand,
    error = BrandError,
    onError = Color_White,
)

/** Тема приложения «Вертикаль» (Material 3). */
@Composable
fun VerticalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VerticalTypography,
        content = content,
    )
}
