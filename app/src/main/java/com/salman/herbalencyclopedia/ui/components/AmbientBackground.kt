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
import androidx.compose.runtime.remember
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
            initialValue = 0.20f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Reverse),
            label = "ambientGlowAlpha"
        )
        animated
    } else 0.16f

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
        // الإزاحة y الموجبة سابقاً (+120dp) كانت تدفع الفقاعة أسفل حافة
        // الشاشة الفعلية أكثر فأكثر رغم أنها أصلاً محاذاة لأسفل الشاشة
        // (BottomStart) — فتظهر غالباً مقصوصة تقريباً بالكامل، تاركة
        // المنطقة السفلية (حيث يطفو الشريط العائم الآن فوقها مباشرة) شبه
        // خالية من أي لون حي يستحق تمويهه خلف الزجاج. إزاحة سالبة ترفعها
        // لتظهر فعلياً في النصف السفلي المرئي من الشاشة.
        BlurredBlob(
            color = tertiary,
            alpha = glowAlpha * 0.85f,
            size = 300.dp,
            highQuality = highQuality,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-80).dp, y = (-30).dp)
        )
        // فقاعة ثالثة صغيرة بلون Primary أسفل-يمين، تحديداً في المنطقة
        // التي يطفو فوقها الشريط العائم السفلي بمعظم الشاشات (منتصف/يمين
        // أسفل الشاشة) — لضمان وجود لون حي واضح خلف الشريط دائماً بدل
        // الاعتماد فقط على امتداد الفقاعتين الكبيرتين اللتين قد لا
        // تصلانه على كل أحجام الشاشات.
        BlurredBlob(
            color = primary,
            alpha = glowAlpha * 0.9f,
            size = 220.dp,
            highQuality = highQuality,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = (-10).dp)
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
        // كانت RenderEffect.createBlurEffect تُستدعى داخل graphicsLayer{} هنا،
        // وهذا المكوّن يُعاد رسمه باستمرار (لا مرة واحدة فقط) بسبب حركة
        // glowAlpha اللانهائية التي تُغيّر شفافية اللون كل إطار طوال عمر
        // التطبيق بالكامل — أي تخصيص RenderEffect جديد نحو 60 مرة/ثانية لكل
        // من الفقاعتين معاً، باستمرار وبلا داعٍ (نصف قطر التمويه 90f ثابت
        // دوماً). الآن يُنشأ مرة واحدة فقط عبر remember.
        val blurEffect = remember {
            RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP).asComposeRenderEffect()
        }
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer { renderEffect = blurEffect }
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
