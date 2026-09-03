package com.salman.herbalencyclopedia.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.ui.theme.LocalPerformanceMode

/**
 * "الزجاج السائل" (Liquid Glass): سطح زجاجي بطبقات حقيقية بدل تدرّج شفاف
 * بسيط —
 *  1) فقاعتا ضوء ملوّنتان تُموَّهان بتمويه حقيقي (RenderEffect.createBlurEffect)
 *     فتعطيان إحساس عمق ينبعث من خلف الزجاج،
 *  2) تدرّج زجاجي أساسي فوقهما،
 *  3) شعاع لمعان (sheen) يتحرك بهدوء عبر السطح كأن الضوء ينزلق على الزجاج،
 *  4) حدّ علوي لامع وحدّ سفلي خافت لإحساس الحافة الزجاجية.
 *
 * كل هذه الطبقات "الثقيلة" (التمويه والحركة اللانهائية) تُقرأ حالتها من
 * [LocalPerformanceMode]: في وضع "اقتصادي" تُستبعد بالكامل ويبقى فقط
 * التدرّج والحدّ الأساسيان — بلا أي تكلفة رسم إضافية — لضمان سلاسة كاملة
 * على الأجهزة الضعيفة. في وضع "أداء عالٍ" تُفعَّل كاملة على أندرويد 12+
 * (RenderEffect متاح من API 31)، وتتراجع تلقائياً لنفس شكل الوضع الاقتصادي
 * على الإصدارات الأقدم.
 */
@Composable
fun LiquidGlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    glowColor: Color = tint,
    borderAlpha: Float = 0.22f,
    // بطاقات الشبكات (تصنيفات، أعشاب...) وأزرار الأيقونات المتكرّرة (زر
    // القلب داخل كل بطاقة، أزرار التعديل/الحذف بكل صف بلوحة الإدارة...)
    // تظهر بالعشرات معاً على نفس الشاشة؛ شعاع لمعان لانهائي منفصل يعمل
    // باستمرار على كل نسخة منها معاً كان يشغّل عشرات الرسوم المتحركة
    // اللانهائية في آن واحد — استهلاك بطارية ومعالج بلا داعٍ وتقطيع أثناء
    // التمرير، خصوصاً على الأجهزة الضعيفة. لذلك القيمة الافتراضية أصبحت
    // متوقفة، وتُفعَّل صراحة فقط في العناصر الفريدة/البارزة التي تظهر
    // كنسخة واحدة على الشاشة (كالزر الأساسي GlassButton أو شعار شاشة البداية).
    sheen: Boolean = false,
    // بطاقات الصفوف الصغيرة (HerbCard، BlendCard...) بارتفاع أقل بكثير من
    // بطاقات الشبكة المربّعة (CategoryCard). فقاعتا التمويه بحجمهما الثابت
    // السابق (64dp/58dp مع تمويه 46px) كانتا مصمَّمتين لبطاقة مربّعة كبيرة؛
    // على صف قصير كانت الفقاعتان (المتموضعتان أعلى-يسار وأسفل-يمين) تتداخلان
    // وتغطيان الصف بالكامل تقريباً، فيظهر مستطيل شبه مصمت (أبيض غالباً بسبب
    // لون الفقاعة الأولى) بدل الزجاج الشفّاف المقصود — هذا بالضبط "المستطيل
    // الأبيض" الذي يظهر بشكل غير متسق بين البطاقات (يعتمد على طول نصها
    // وبالتالي ارتفاعها الفعلي). compact=true يصغّر الفقاعتين ونصف قطر
    // تمويههما بما يناسب صفاً قصيراً فلا تطغيان على العنصر كاملاً.
    compact: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val highQuality = LocalPerformanceMode.current.isHighQuality
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    // حافة الزجاج تعتمد على الوضع: في الوضع الداكن، حدّ أبيض خافت يعطي
    // إحساس "توهّج" واضحاً على خلفية داكنة. نفس الحدّ الأبيض على خلفية
    // فاتحة يكاد يكون غير مرئي تماماً (أبيض على أبيض تقريباً) — وهذا بالضبط
    // سبب شعور "شيء ناقص" في الوضع النهاري: كانت حواف كل بطاقة وزر زجاجي
    // غير مرئية. في الوضع الفاتح نستخدم حدّاً داكناً خافتاً جداً بدلاً من
    // ذلك، فيعطي نفس إحساس "حافة الزجاج" لكن بتباين يناسب خلفية فاتحة.
    val edgeColor = if (darkTheme) Color.White else Color.Black
    // 0.5 سابقاً كانت تجعل حافة البطاقات/الأشرطة الزجاجية شبه مختفية على
    // خلفية فاتحة (خصوصاً أعلى الحافة حيث القيمة الأصلية أصلاً منخفضة)،
    // فتبدو العناصر الزجاجية بلا حدود واضحة مقارنة بوضوحها في الوضع
    // الداكن — فرق تباين بين الوضعين لم يكن مقصوداً. 0.68 يعيد قدراً كافياً
    // من الوضوح دون أن يقترب من ثقل حدّ الوضع الداكن.
    val edgeAlphaScale = if (darkTheme) 1f else 0.68f
    // فقاعة الضوء الأولى كانت أبيض صريح (Color.White) بغضّ النظر عن الوضع.
    // في الوضع الداكن هذا يعطي "توهّجاً" مقصوداً وواضحاً فوق خلفية داكنة،
    // لكن في الوضع الفاتح خلفية البطاقة نفسها فاتحة أصلاً (قريبة من الأبيض)،
    // فتراكم فقاعة بيضاء إضافية فوقها لا يبدو توهّجاً بل يُبيّض المنطقة كاملة
    // ويُذيب حدود البطاقة — وهذا جزء من سبب "أخطاء التصميم" الظاهرة بالوضع
    // النهاري. نُخفّف شفافيتها بوضوح في الوضع الفاتح فقط بدل حذفها كلياً
    // (لا تزال تعطي إحساساً خفيفاً بالعمق دون أن تطغى).
    val primaryBubbleAlpha = if (darkTheme) 0.65f else 0.14f
    val secondaryBubbleAlpha = if (darkTheme) 0.55f else 0.20f
    val bubbleBlurRadius = if (compact) 22f else 46f
    val primaryBubbleSize = if (compact) 30.dp else 64.dp
    val secondaryBubbleSize = if (compact) 26.dp else 58.dp
    val primaryBubbleOffset = if (compact) (-7).dp to (-8).dp else (-16).dp to (-18).dp
    val secondaryBubbleOffset = if (compact) 8.dp to 7.dp else 18.dp to 16.dp

    Box(modifier = modifier.clip(shape)) {
        if (highQuality && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(bubbleBlurRadius, bubbleBlurRadius, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(primaryBubbleOffset.first, primaryBubbleOffset.second)
                        .size(primaryBubbleSize)
                        .background(Color.White.copy(alpha = primaryBubbleAlpha), CircleShape)
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(secondaryBubbleOffset.first, secondaryBubbleOffset.second)
                        .size(secondaryBubbleSize)
                        .background(glowColor.copy(alpha = secondaryBubbleAlpha), CircleShape)
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        // نفس ملاحظة الحدّ أعلاه: تدرّج الخلفية الأساسي بقيم
                        // الشفافية القديمة (0.92/0.74) كان قريباً جداً من لون
                        // خلفية الصفحة في الوضع الفاتح (كلاهما فاتح جداً بلا
                        // تشبّع)، فتبدو البطاقة بلا امتلاء واضح مقارنةً بوضوحها
                        // في الوضع الداكن. رفع الشفافية قليلاً في الوضع الفاتح
                        // فقط يعطي امتلاءً كافياً يميّز البطاقة عن الخلفية.
                        if (darkTheme) listOf(tint.copy(alpha = 0.92f), tint.copy(alpha = 0.74f))
                        else listOf(tint.copy(alpha = 0.97f), tint.copy(alpha = 0.88f))
                    )
                )
        )

        if (highQuality && sheen) {
            GlassSheen(modifier = Modifier.matchParentSize())
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            edgeColor.copy(alpha = (borderAlpha + 0.14f) * edgeAlphaScale),
                            edgeColor.copy(alpha = (borderAlpha * 0.35f) * edgeAlphaScale)
                        )
                    ),
                    shape = shape
                )
        )

        content()
    }
}

/** شعاع لمعان ناعم ينزلق قطرياً عبر السطح — يُعطي الزجاج إحساساً "حيّاً". */
@Composable
private fun GlassSheen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glassSheen")
    val progress by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassSheenProgress"
    )
    Canvas(modifier = modifier) {
        val bandWidth = size.width * 0.30f
        val x = progress * size.width
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                start = Offset(x - bandWidth, 0f),
                end = Offset(x + bandWidth, size.height)
            )
        )
    }
}
