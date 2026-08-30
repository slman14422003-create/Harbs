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
import androidx.compose.ui.graphics.toArgb

/**
 * التلوين السابق كان يمزج (lerp) لون اللوحة مباشرة مع الأسود/الأبيض بنسب
 * مختلفة. المشكلة أن نتيجة المزج في RGB لا تضمن أي سطوع نهائي محدد: لونان
 * بنفس نسبة المزج قد ينتج عنهما سطوعان مختلفان تمامًا حسب سطوع اللون
 * الأصلي نفسه — فبعض التركيبات (خصوصًا في الوضع الداكن) كانت تُنتج نصًا
 * وخلفية متقاربين في السطوع، وهذا تحديدًا ما يجعل القراءة متعبة للعين رغم
 * أن الألوان "تبدو" غنية.
 *
 * البديل هنا: نحوّل لون اللوحة إلى HSV ونُثبّت قيمتي التشبع (S) والسطوع (V)
 * صراحة لكل دور لوني، بدل تركهما نتيجة عرضية للمزج. هذا يضمن فرق سطوع
 * كبيرًا وثابتًا بين كل سطح والنص الذي يعلوه (مثلاً سطوع 0.14 للخلفية مقابل
 * 0.94 لنصّها في الوضع الداكن) بغضّ النظر عن لوحة الألوان المختارة، مع
 * الإبقاء على درجة تشبّع خفيفة على الأسطح نفسها حتى لا تبدو رمادية.
 */
private fun tone(hue: Color, saturation: Float, value: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(hue.toArgb(), hsv)
    hsv[1] = saturation.coerceIn(0f, 1f)
    hsv[2] = value.coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun lightSchemeFor(palette: ThemePalette): androidx.compose.material3.ColorScheme {
    val hue = palette.light40
    val sec = palette.secondary40
    val ter = palette.tertiary40
    // خيار "بدون تلوين": تشبّع كل الأدوار اللونية يُصفَّر هنا (بغضّ النظر
    // عمّا تطلبه كل tone() أدناه)، فتنتج نفس درجات السطوع المدروسة تماماً
    // لكن رمادية محضة — الخلفية تصبح أبيض عملياً (سطوع 0.99 بلا تشبّع)
    // بدل أي ميل لوني، وهذا بالضبط ما طلبه المستخدم لهذا الخيار.
    val neutral = palette == ThemePalette.NONE
    fun t(hueColor: Color, saturation: Float, value: Float) =
        tone(hueColor, if (neutral) 0f else saturation, value)
    return lightColorScheme(
        primary = t(hue, 0.62f, 0.55f), onPrimary = Color.White,
        primaryContainer = t(hue, 0.30f, 0.94f),
        onPrimaryContainer = t(hue, 0.55f, 0.30f),
        secondary = t(sec, 0.35f, 0.50f), onSecondary = Color.White,
        secondaryContainer = t(sec, 0.20f, 0.94f),
        onSecondaryContainer = t(sec, 0.35f, 0.32f),
        tertiary = t(ter, 0.45f, 0.50f), onTertiary = Color.White,
        tertiaryContainer = t(ter, 0.25f, 0.94f),
        onTertiaryContainer = t(ter, 0.40f, 0.32f),
        background = t(hue, 0.06f, 0.99f),
        onBackground = t(hue, 0.20f, 0.16f),
        surface = t(hue, 0.06f, 0.99f),
        onSurface = t(hue, 0.20f, 0.16f),
        surfaceVariant = t(hue, 0.12f, 0.93f),
        onSurfaceVariant = t(hue, 0.20f, 0.34f),
        surfaceDim = t(hue, 0.10f, 0.88f),
        surfaceBright = t(hue, 0.06f, 0.99f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = t(hue, 0.07f, 0.97f),
        surfaceContainer = t(hue, 0.09f, 0.95f),
        surfaceContainerHigh = t(hue, 0.11f, 0.92f),
        surfaceContainerHighest = t(hue, 0.13f, 0.89f),
        outline = t(hue, 0.10f, 0.47f),
        outlineVariant = t(hue, 0.10f, 0.82f),
        inverseSurface = t(hue, 0.15f, 0.20f),
        inverseOnSurface = t(hue, 0.06f, 0.97f),
        inversePrimary = t(hue, 0.45f, 0.80f),
        scrim = Color.Black
    )
}

private fun darkSchemeFor(palette: ThemePalette): androidx.compose.material3.ColorScheme {
    val hue = palette.light40
    val sec = palette.secondary40
    val ter = palette.tertiary40
    // نفس مبدأ lightSchemeFor أعلاه: بلا أي تشبّع لوني لخيار "بدون تلوين"،
    // فتبقى فقط درجات السطوع نفسها المدروسة أصلاً لراحة العين، لكن رمادية
    // محضة — خلفية قريبة من الأسود (سطوع 0.14) بلا أي ميل لوني.
    val neutral = palette == ThemePalette.NONE
    fun t(hueColor: Color, saturation: Float, value: Float) =
        tone(hueColor, if (neutral) 0f else saturation, value)
    return darkColorScheme(
        primary = t(hue, 0.45f, 0.82f), onPrimary = t(hue, 0.55f, 0.16f),
        primaryContainer = t(hue, 0.45f, 0.32f),
        onPrimaryContainer = t(hue, 0.30f, 0.92f),
        secondary = t(sec, 0.28f, 0.78f), onSecondary = t(sec, 0.35f, 0.16f),
        secondaryContainer = t(sec, 0.28f, 0.30f),
        onSecondaryContainer = t(sec, 0.20f, 0.90f),
        tertiary = t(ter, 0.35f, 0.78f), onTertiary = t(ter, 0.40f, 0.16f),
        tertiaryContainer = t(ter, 0.32f, 0.30f),
        onTertiaryContainer = t(ter, 0.25f, 0.90f),
        // خلفية داكنة بدرجة تشبّع منخفضة كي تُريح العين، مع فارق سطوع كبير
        // (0.14 مقابل 0.94) يضمن وضوح النص فوقها بلا إجهاد.
        background = t(hue, 0.16f, 0.14f),
        onBackground = t(hue, 0.08f, 0.94f),
        surface = t(hue, 0.16f, 0.14f),
        onSurface = t(hue, 0.08f, 0.94f),
        surfaceVariant = t(hue, 0.20f, 0.26f),
        onSurfaceVariant = t(hue, 0.10f, 0.80f),
        surfaceDim = t(hue, 0.16f, 0.14f),
        surfaceBright = t(hue, 0.14f, 0.36f),
        surfaceContainerLowest = t(hue, 0.18f, 0.10f),
        surfaceContainerLow = t(hue, 0.17f, 0.18f),
        surfaceContainer = t(hue, 0.18f, 0.21f),
        surfaceContainerHigh = t(hue, 0.19f, 0.25f),
        surfaceContainerHighest = t(hue, 0.20f, 0.30f),
        outline = t(hue, 0.12f, 0.60f),
        outlineVariant = t(hue, 0.16f, 0.32f),
        inverseSurface = t(hue, 0.08f, 0.94f),
        inverseOnSurface = t(hue, 0.16f, 0.18f),
        inversePrimary = t(hue, 0.60f, 0.45f),
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
