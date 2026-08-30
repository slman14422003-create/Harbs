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

/** مزج بسيط بين لون الهوية وقاعدة محايدة، يُستخدم لاشتقاق درجة سطح واحدة. */
private fun tone(hue: Color, base: Color, amount: Float) = lerp(hue, base, amount)

/**
 * سبب ظهور الألوان "باهتة" في الوضع الداكن تحديدًا: كل أدوار الأسطح كانت
 * تُشتق من [ThemePalette.light80] — وهو لون فاتح شبه-باستيل مُصمَّم أصلاً
 * ليُستخدم كنص/تمييز (primary) فوق خلفية داكنة، وليس كمصدر تلوين. مزج لون
 * باهت أصلاً بنسبة كبيرة من الأسود ينتج رماديًا يميل قليلاً للون بدل لون
 * واضح. الحل: اشتقاق الأسطح من [ThemePalette.light40] (النسخة المشبعة
 * الأصلية من اللوحة) بدل النسخة الباستيل، مع خفض نسب المزج الأساسية قليلاً
 * حتى تظهر الصبغة اللونية بوضوح على الكروت وأشرطة الزجاج السائل، بينما
 * تبقى primary/secondary/tertiary نفسها فاتحة كما هي لضمان تباين واضح
 * للنصوص فوق الخلفية الداكنة.
 */

/**
 * بدون هذا الحساب، اللوحة المختارة كانت تغيّر primary/secondary/tertiary
 * فقط، بينما باقي أدوار الألوان (primaryContainer, onPrimaryContainer...)
 * التي تستخدمها أغلب الكروت والأزرار بالتطبيق تبقى على قيم Material
 * الافتراضية الأرجوانية — فيبدو التغيير وكأنه "لا يعمل بشكل كامل".
 *
 * المشكلة نفسها كانت أعمق في أدوار الأسطح (surfaceContainer/surfaceContainerHigh
 * وغيرها) التي لم تكن تُمرَّر إطلاقاً: كل الكروت وأشرطة الزجاج السائل
 * والأزرار تعتمد عليها (انظر LiquidGlassSurface وGlassIconButton
 * وCategoryCard)، فكانت تبقى دائماً على القيم الأرجوانية الافتراضية لـ
 * Material3 — وهذا بالضبط سبب ظهور "الوضع النهاري" بلون بنفسجي باهت في كل
 * مكان مهما كانت اللوحة المختارة. هذه الدوال تشتق كل أدوار السطح من نفس
 * لون اللوحة (hue) بدرجات مختلفة من المزج مع قاعدة محايدة فاتحة/داكنة، بدل
 * تركها على قيم Material الافتراضية الثابتة.
 */
private fun lightSchemeFor(palette: ThemePalette): androidx.compose.material3.ColorScheme {
    val hue = palette.light40
    val base = SurfaceLight
    return lightColorScheme(
        primary = palette.light40, onPrimary = Color.White,
        primaryContainer = tone(hue, Color.White, 0.82f),
        onPrimaryContainer = tone(hue, Color.Black, 0.55f),
        secondary = palette.secondary40, onSecondary = Color.White,
        secondaryContainer = tone(palette.secondary40, Color.White, 0.82f),
        onSecondaryContainer = tone(palette.secondary40, Color.Black, 0.55f),
        tertiary = palette.tertiary40, onTertiary = Color.White,
        tertiaryContainer = tone(palette.tertiary40, Color.White, 0.82f),
        onTertiaryContainer = tone(palette.tertiary40, Color.Black, 0.55f),
        background = tone(hue, base, 0.92f),
        onBackground = tone(hue, Color.Black, 0.72f),
        surface = tone(hue, base, 0.92f),
        onSurface = tone(hue, Color.Black, 0.72f),
        surfaceVariant = tone(hue, Color.White, 0.78f),
        onSurfaceVariant = tone(hue, Color.Black, 0.48f),
        surfaceDim = tone(hue, base, 0.76f),
        surfaceBright = tone(hue, base, 0.96f),
        surfaceContainerLowest = tone(hue, Color.White, 0.98f),
        surfaceContainerLow = tone(hue, base, 0.87f),
        surfaceContainer = tone(hue, base, 0.82f),
        surfaceContainerHigh = tone(hue, base, 0.75f),
        surfaceContainerHighest = tone(hue, base, 0.68f),
        outline = tone(hue, Color(0xFF787E74), 0.55f),
        outlineVariant = tone(hue, Color.White, 0.65f),
        inverseSurface = tone(hue, Color.Black, 0.80f),
        inverseOnSurface = tone(hue, Color.White, 0.90f),
        inversePrimary = palette.light80,
        scrim = Color.Black
    )
}

private fun darkSchemeFor(palette: ThemePalette): androidx.compose.material3.ColorScheme {
    // مصدر تلوين الأسطح هو اللون المُشبع (light40)، وليس نسخة primary
    // الباستيل (light80) — هذا وحده هو الفارق بين مظهر "باهت رمادي" ومظهر
    // "ملوّن بوضوح" في الوضع الداكن.
    val hue = palette.light40
    val base = SurfaceDark
    return darkColorScheme(
        primary = palette.light80, onPrimary = Color.Black,
        primaryContainer = tone(hue, Color.Black, 0.45f),
        onPrimaryContainer = tone(hue, Color.White, 0.30f),
        secondary = palette.secondary80, onSecondary = Color.Black,
        secondaryContainer = tone(palette.secondary40, Color.Black, 0.45f),
        onSecondaryContainer = tone(palette.secondary80, Color.White, 0.30f),
        tertiary = palette.tertiary80, onTertiary = Color.Black,
        tertiaryContainer = tone(palette.tertiary40, Color.Black, 0.45f),
        onTertiaryContainer = tone(palette.tertiary80, Color.White, 0.30f),
        background = tone(hue, base, 0.88f),
        onBackground = tone(hue, Color.White, 0.88f),
        surface = tone(hue, base, 0.88f),
        onSurface = tone(hue, Color.White, 0.88f),
        surfaceVariant = tone(hue, Color.Black, 0.65f),
        onSurfaceVariant = tone(hue, Color.White, 0.55f),
        surfaceDim = tone(hue, base, 0.86f),
        surfaceBright = tone(hue, base, 0.45f),
        surfaceContainerLowest = tone(hue, Color.Black, 0.94f),
        surfaceContainerLow = tone(hue, base, 0.80f),
        surfaceContainer = tone(hue, base, 0.70f),
        surfaceContainerHigh = tone(hue, base, 0.58f),
        surfaceContainerHighest = tone(hue, base, 0.46f),
        outline = tone(hue, Color(0xFF9AA497), 0.35f),
        outlineVariant = tone(hue, Color.Black, 0.45f),
        inverseSurface = tone(hue, Color.White, 0.85f),
        inverseOnSurface = tone(hue, Color.Black, 0.80f),
        inversePrimary = palette.light40,
        scrim = Color.Black
    )
}

@Composable
fun HerbalEncyclopediaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
