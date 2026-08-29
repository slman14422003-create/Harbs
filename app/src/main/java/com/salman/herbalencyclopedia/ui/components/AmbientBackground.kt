package com.salman.herbalencyclopedia.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.ui.theme.LocalPerformanceMode

/**
 * خلفية زخرفية هادئة تُوضع مرة واحدة خلف NavHost بالكامل (بدل خلفية مسطّحة
 * فارغة في كل شاشة على حدة): تدرّج أساسي بلوني الهوية الحاليين
 * (primary/tertiary من ColorScheme، فتتبدّل تلقائياً مع أي لوحة ألوان أو
 * الوضع الداكن)، فقاعتا ضوء كبيرتان مموَّهتان في الزوايا لإحساس عمق هادئ،
 * وورقتا زخرفة خافتتان جداً (نفس أيقونة Spa المستخدمة في شاشة البداية)
 * لهوية نباتية متسقة عبر التطبيق كله.
 *
 * كل الطبقات "الثقيلة" (تمويه RenderEffect + نبض التوهّج اللانهائي) تُقرأ
 * من [LocalPerformanceMode] وتُستبعد بالكامل في الوضع الاقتصادي — بلا أي
 * تكلفة رسم إضافية، تماماً بنفس مبدأ LiquidGlassSurface.
 *
 * كل شاشة يجب أن تجعل containerColor الخاص بـ Scaffold شفافاً
 * (Color.Transparent) كي تظهر هذه الخلفية من خلفها بدل حجبها.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val highQuality = LocalPerformanceMode.current.isHighQuality
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary

    val glowAlpha = if (highQuality) {
        val transition = rememberInfiniteTransition(label = "ambientGlow")
        val animated by transition.animateFloat(
            initialValue = 0.16f,
            targetValue = 0.26f,
            animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Reverse),
            label = "ambientGlowAlpha"
        )
        animated
    } else 0.12f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // فقاعة ضوء علوية-يمينية بلون Primary.
        BlurredBlob(
            color = primary,
            alpha = glowAlpha,
            size = 260.dp,
            highQuality = highQuality,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = (-90).dp)
        )
        // فقاعة ضوء سفلية-يسارية بلون Tertiary لتوازن بصري بلونين متكاملين.
        BlurredBlob(
            color = tertiary,
            alpha = glowAlpha * 0.85f,
            size = 300.dp,
            highQuality = highQuality,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-100).dp, y = 120.dp)
        )

        // تدرّج خافت جداً فوق الفقاعات ليوحّد السطح ولا يترك حواف واضحة.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, scheme.background.copy(alpha = 0.55f))
                    )
                )
        )

        // وريقتان زخرفيتان خافتتان جداً — نفس هوية شاشة البداية، بحضور
        // بصري شبه معدوم كي لا تُلهي عن المحتوى فوقها.
        Icon(
            imageVector = Icons.Filled.Spa,
            contentDescription = null,
            tint = scheme.onBackground.copy(alpha = 0.035f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = 8.dp)
                .size(150.dp)
                .rotate(-18f)
        )
        Icon(
            imageVector = Icons.Filled.Spa,
            contentDescription = null,
            tint = scheme.onBackground.copy(alpha = 0.03f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 34.dp, y = 40.dp)
                .size(190.dp)
                .rotate(154f)
        )
    }
}

@Composable
private fun BlurredBlob(
    color: Color,
    alpha: Float,
    size: Dp,
    highQuality: Boolean,
    modifier: Modifier = Modifier
) {
    if (highQuality && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    renderEffect = RenderEffect
                        .createBlurEffect(90f, 90f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .background(color.copy(alpha = alpha), CircleShape)
        )
    } else {
        // بلا تمويه حقيقي في الوضع الاقتصادي أو ما قبل أندرويد 12: دائرة
        // بشفافية أقل فقط، بلا تكلفة RenderEffect.
        Box(
            modifier = modifier
                .size(size)
                .background(color.copy(alpha = alpha * 0.5f), CircleShape)
        )
    }
}
