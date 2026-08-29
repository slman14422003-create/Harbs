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
import androidx.compose.ui.graphics.lerp

/**
 * بدون هذا الحساب، اللوحة المختارة كانت تغيّر primary/secondary/tertiary
 * فقط، بينما باقي أدوار الألوان (primaryContainer, onPrimaryContainer...)
 * التي تستخدمها أغلب الكروت والأزرار بالتطبيق تبقى على قيم Material
 * الافتراضية الأرجوانية — فيبدو التغيير وكأنه "لا يعمل بشكل كامل".
 * هذه دوال بسيطة تشتق ألوان الـ container من نفس لون اللوحة بدل تركها ثابتة.
 */
private fun lightSchemeFor(palette: ThemePalette) = lightColorScheme(
    primary = palette.light40, onPrimary = Color.White,
    primaryContainer = lerp(palette.light40, Color.White, 0.82f),
    onPrimaryContainer = lerp(palette.light40, Color.Black, 0.55f),
    secondary = palette.secondary40, onSecondary = Color.White,
    secondaryContainer = lerp(palette.secondary40, Color.White, 0.82f),
    onSecondaryContainer = lerp(palette.secondary40, Color.Black, 0.55f),
    tertiary = palette.tertiary40, onTertiary = Color.White,
    tertiaryContainer = lerp(palette.tertiary40, Color.White, 0.82f),
    onTertiaryContainer = lerp(palette.tertiary40, Color.Black, 0.55f),
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE9EFE6),
    outline = Color(0xFF7A8676)
)

private fun darkSchemeFor(palette: ThemePalette) = darkColorScheme(
    primary = palette.light80, onPrimary = Color.Black,
    primaryContainer = lerp(palette.light80, Color.Black, 0.55f),
    onPrimaryContainer = lerp(palette.light80, Color.White, 0.35f),
    secondary = palette.secondary80, onSecondary = Color.Black,
    secondaryContainer = lerp(palette.secondary80, Color.Black, 0.55f),
    onSecondaryContainer = lerp(palette.secondary80, Color.White, 0.35f),
    tertiary = palette.tertiary80, onTertiary = Color.Black,
    tertiaryContainer = lerp(palette.tertiary80, Color.Black, 0.55f),
    onTertiaryContainer = lerp(palette.tertiary80, Color.White, 0.35f),
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
