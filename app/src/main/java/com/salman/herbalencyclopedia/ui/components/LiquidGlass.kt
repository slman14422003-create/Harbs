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
    // بطاقات الشبكات (تصنيفات، أعشاب...) تظهر بالعشرات معاً على نفس
    // الشاشة؛ شعاع لمعان لانهائي يلمع على كل بطاقة بنفس التوقيت يبدو
    // متكرراً ومزعجاً بدل "حي". لذلك القيمة الافتراضية مفعّلة (لأزرار/
    // أشرطة علوية مفردة) لكن تُطفأ صراحة في بطاقات الشبكة.
    sheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val highQuality = LocalPerformanceMode.current.isHighQuality

    Box(modifier = modifier.clip(shape)) {
        if (highQuality && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(46f, 46f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset((-16).dp, (-18).dp)
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.65f), CircleShape)
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(18.dp, 16.dp)
                        .size(58.dp)
                        .background(glowColor.copy(alpha = 0.55f), CircleShape)
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(tint.copy(alpha = 0.92f), tint.copy(alpha = 0.74f))
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
                        listOf(Color.White.copy(alpha = borderAlpha + 0.14f), Color.White.copy(alpha = borderAlpha * 0.35f))
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
