package com.salman.herbalencyclopedia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

private fun lightSchemeFor(palette: ThemePalette) = lightColorScheme(
    primary = palette.light40,
    secondary = palette.secondary40,
    tertiary = palette.tertiary40,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE9EFE6),
    outline = Color(0xFF7A8676)
)

private fun darkSchemeFor(palette: ThemePalette) = darkColorScheme(
    primary = palette.light80,
    secondary = palette.secondary80,
    tertiary = palette.tertiary80,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF3B403A),
    outline = Color(0xFF9AA497)
)

@Composable
fun HerbalEncyclopediaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: ThemePalette = ThemePalette.LEAF,
    fontScale: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkSchemeFor(palette)
        else -> lightSchemeFor(palette)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HerbalTypography.scaled(fontScale),
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
