package com.salman.herbalencyclopedia.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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

/**
 * إصلاح: "الألوان الديناميكية" (حسب الخلفية) كانت تتجاهل كل ما سبق تماماً
 * — تُستدعى dynamicLightColorScheme/dynamicDarkColorScheme من أندرويد
 * مباشرة وتُستخدم أسطحها كما هي، بينما كل لوحة يدوية تمر عبر tone() أعلاه
 * لتضمن تشبّعاً وسطوعاً ثابتين ومدروسين لكل دور لوني. أسطح Material You
 * الافتراضية من أندرويد محايدة جداً عمداً (خلفية شبه بيضاء/سوداء بأثر لوني
 * خافت جداً)، فكانت النتيجة أن اللون المُستخرج من الخلفية يظهر كأنه "طبقة"
 * أو لمعة خفيفة فوق تصميم رمادي أصلاً — بينما اللوحات اليدوية تُلوّن كل
 * سطح فعلياً بنفس الصيغة. الآن lightSchemeFor/darkSchemeFor تقبلان أي ثلاثة
 * ألوان "هوية" (hue/secondary/tertiary) بدل الاقتصار على [ThemePalette]،
 * فتُستخرج ألوان primary/secondary/tertiary فقط من نظام أندرويد الديناميكي
 * (وهي فعلاً "حسب الخلفية") ثم تمرّ عبر نفس خط tone() الموحّد — فتُطبَّق
 * بنفس القوة والتناسق الكاملين كأي لوحة يدوية، بدل أن تبقى محايدة وتُطبَّق
 * كلمعة سطحية فوق تصميم غير ملوّن فعلياً.
 */
private fun lightSchemeFor(hue: Color, sec: Color, ter: Color, neutral: Boolean): androidx.compose.material3.ColorScheme {
    // خيار "بدون تلوين": تشبّع كل الأدوار اللونية يُصفَّر هنا (بغضّ النظر
    // عمّا تطلبه كل tone() أدناه)، فتنتج نفس درجات السطوع المدروسة تماماً
    // لكن رمادية محضة — الخلفية تصبح أبيض عملياً (سطوع 0.99 بلا تشبّع)
    // بدل أي ميل لوني، وهذا بالضبط ما طلبه المستخدم لهذا الخيار.
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
        background = t(hue, 0.08f, 0.97f),
        onBackground = t(hue, 0.20f, 0.16f),
        surface = t(hue, 0.08f, 0.97f),
        onSurface = t(hue, 0.20f, 0.16f),
        surfaceVariant = t(hue, 0.14f, 0.90f),
        onSurfaceVariant = t(hue, 0.20f, 0.34f),
        surfaceDim = t(hue, 0.12f, 0.84f),
        surfaceBright = t(hue, 0.08f, 0.97f),
        // كانت هذه Color.White صريح بلا أي علاقة بـ tone(): أبيض خالص بلا
        // أي أثر للون اللوحة المختارة، بينما كل بقية الأسطح تتدرّج بنفس
        // الصيغة الموحّدة t(hue, ...). النتيجة كانت أخفّ سطح بالتطبيق
        // (البطاقات الأعلى ارتفاعاً، صفحات الإدخال...) يبدو "مبتوراً" عن
        // بقية التدرّج اللوني — يقفز فجأة لأبيض محايد بدل الاستمرار بنفس
        // نفَس اللون الخفيف الذي تراه بقية الأسطح، وهو تحديداً إحساس
        // "الألوان مو ظابطة" في الوضع النهاري. نفس صيغة t() هنا بسطوع
        // أعلى قليلاً من surfaceContainerLow يُبقيه أفتح سطح فعلاً لكن
        // متّسقاً مع بقية التدرّج (وحياديّاً تماماً تلقائياً مع خيار "بدون
        // تلوين" بفضل نفس آلية t() لا حاجة لأي استثناء إضافي).
        surfaceContainerLowest = t(hue, 0.05f, 0.995f),
        surfaceContainerLow = t(hue, 0.09f, 0.95f),
        surfaceContainer = t(hue, 0.12f, 0.92f),
        // تشبّع أعلى قليلاً هنا تحديداً (0.15→0.24 / 0.18→0.28) بدل بقية
        // الأسطح: هذان أكثر سطحين "بارزين" استخداماً (بطاقات مرتفعة،
        // الشريط العلوي/السفلي الزجاجي...)، فحصّتهما من هوية اللوحة اللونية
        // كانت الأخفّ من كل الأسطح رغم كونها الأكثر ظهوراً — وهذا تحديداً
        // ما يجعل عناصر الزجاج تبدو رمادية عامة بدل متجانسة مع لون الثيم.
        // القيمة V لم تتغيّر (لا أثر على تباين النص فوقها).
        surfaceContainerHigh = t(hue, 0.24f, 0.88f),
        surfaceContainerHighest = t(hue, 0.28f, 0.84f),
        outline = t(hue, 0.12f, 0.50f),
        outlineVariant = t(hue, 0.12f, 0.78f),
        inverseSurface = t(hue, 0.15f, 0.20f),
        inverseOnSurface = t(hue, 0.06f, 0.97f),
        inversePrimary = t(hue, 0.45f, 0.80f),
        scrim = Color.Black
    )
}

private fun darkSchemeFor(hue: Color, sec: Color, ter: Color, neutral: Boolean): androidx.compose.material3.ColorScheme {
    // نفس مبدأ lightSchemeFor أعلاه: بلا أي تشبّع لوني لخيار "بدون تلوين"،
    // فتبقى فقط درجات السطوع نفسها المدروسة أصلاً لراحة العين، لكن رمادية
    // محضة — خلفية قريبة من الأسود (سطوع 0.14) بلا أي ميل لوني.
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
        // نفس ملاحظة النسخة الفاتحة أعلاه: تشبّع أعلى لهذين السطحين
        // تحديداً فقط (البطاقات المرتفعة والأشرطة الزجاجية)، بلا أي تغيير
        // على V فلا يتأثر تباين النص فوقهما.
        surfaceContainerHigh = t(hue, 0.28f, 0.25f),
        surfaceContainerHighest = t(hue, 0.30f, 0.30f),
        outline = t(hue, 0.12f, 0.60f),
        outlineVariant = t(hue, 0.16f, 0.32f),
        inverseSurface = t(hue, 0.08f, 0.94f),
        inverseOnSurface = t(hue, 0.16f, 0.18f),
        inversePrimary = t(hue, 0.60f, 0.45f),
        scrim = Color.Black
    )
}

/**
 * قدرة جديدة: تحريك التبديل بين لوحات الألوان (نهاري↔ليلي، أو تغيير الهوية
 * اللونية/تفعيل الألوان الديناميكية من الإعدادات) بدل قفزة لونية فورية على
 * كل عنصر بالتطبيق دفعة واحدة في إطار رسم واحد فقط. هذا التبديل المفاجئ
 * تحديداً هو ما يجعل التطبيق يشعر بعدم الاتساق رغم أن باقي الحركات بالتطبيق
 * ناعمة (راجع AppMotion في Animations.kt) — لحظة تبديل الوضع نفسها كانت
 * الاستثناء الوحيد الخالي من أي حركة. كل لون هنا يُتحرَّك على حدة بنفس
 * منحنى/مدة [AppMotion.smooth] المستخدمة في باقي التطبيق، فيتحوّل المظهر
 * كاملاً بانسيابية موحّدة بدل قفزة فجائية. التكلفة زهيدة جداً وتُذكر فقط
 * لحظة التبديل نفسه (وليست حركة مستمرة)، فلا داعي لتقييدها بوضع الأداء
 * كبقية حركات Animations.kt المستمرة.
 */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = AppMotion.smooth<Color>()
    return ColorScheme(
        primary = animateColorAsState(primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(inversePrimary, spec, label = "inversePrimary").value,
        secondary = animateColorAsState(secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(onSecondary, spec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(tertiary, spec, label = "tertiary").value,
        onTertiary = animateColorAsState(onTertiary, spec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(tertiaryContainer, spec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(onTertiaryContainer, spec, label = "onTertiaryContainer").value,
        background = animateColorAsState(background, spec, label = "background").value,
        onBackground = animateColorAsState(onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(surface, spec, label = "surface").value,
        onSurface = animateColorAsState(onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(surfaceTint, spec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(inverseSurface, spec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(inverseOnSurface, spec, label = "inverseOnSurface").value,
        error = animateColorAsState(error, spec, label = "error").value,
        onError = animateColorAsState(onError, spec, label = "onError").value,
        errorContainer = animateColorAsState(errorContainer, spec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(onErrorContainer, spec, label = "onErrorContainer").value,
        outline = animateColorAsState(outline, spec, label = "outline").value,
        outlineVariant = animateColorAsState(outlineVariant, spec, label = "outlineVariant").value,
        scrim = animateColorAsState(scrim, spec, label = "scrim").value,
        surfaceBright = animateColorAsState(surfaceBright, spec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(surfaceDim, spec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(surfaceContainer, spec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(surfaceContainerHigh, spec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(surfaceContainerHighest, spec, label = "surfaceContainerHighest").value,
        surfaceContainerLow = animateColorAsState(surfaceContainerLow, spec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(surfaceContainerLowest, spec, label = "surfaceContainerLowest").value
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
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // نستخرج فقط هوية الألوان (primary/secondary/tertiary) من نظام
            // أندرويد — وهذا ما يجعلها فعلاً "حسب الخلفية" — ثم نمرّرها عبر
            // نفس خط tone() المستخدم للوحات اليدوية بدل استخدام أسطح
            // Material You المحايدة كما هي (راجع التوثيق أعلى lightSchemeFor).
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme) {
                darkSchemeFor(dynamic.primary, dynamic.secondary, dynamic.tertiary, neutral = false)
            } else {
                lightSchemeFor(dynamic.primary, dynamic.secondary, dynamic.tertiary, neutral = false)
            }
        }
        darkTheme -> darkSchemeFor(palette.light40, palette.secondary40, palette.tertiary40, neutral = palette == ThemePalette.NONE)
        else -> lightSchemeFor(palette.light40, palette.secondary40, palette.tertiary40, neutral = palette == ThemePalette.NONE)
    }.animated()

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
