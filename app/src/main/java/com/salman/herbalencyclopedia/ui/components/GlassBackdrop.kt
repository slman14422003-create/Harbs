package com.salman.herbalencyclopedia.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * "الزجاج الحقيقي": بدل تدرّج شفاف تقريبي (الزجاج القديم في [LiquidGlassSurface])
 * الذي لا يعكس ما تحته فعلياً، هذا الملف يسجّل الخلفية الحيّة للشاشة
 * (خلفية AmbientBackground + محتوى NavHost) في طبقة رسم واحدة عبر
 * [Modifier.glassBackdropSource]، ثم يسمح لأي عنصر عائم فوقها (الشريط
 * السفلي، الشريط العلوي) بأخذ نسخة مموَّهة فعلياً منها عبر
 * [Modifier.glassBackdropBlur] — تماماً كزجاج iOS/Android الحديث حيث
 * ترى ألوان الواجهة وهي تتحرك خلف الزجاج بدل بقعة لون ثابتة.
 *
 * يعتمد على واجهة GraphicsLayer المُقدَّمة في Compose UI 1.7 (متوفرة عبر
 * compose-bom 2024.09 فما فوق)، والتمويه الحقيقي (RenderEffect) لا يتوفر
 * إلا على أندرويد 12 فأعلى؛ لذلك كل استدعاء هنا يُستخدم دائماً خلف تحقّق
 * `Build.VERSION.SDK_INT >= S` تماماً كبقية طبقات "الزجاج السائل" الأخرى
 * بالمشروع (انظر LiquidGlass.kt وAmbientBackground.kt)، مع رجوع تلقائي
 * للزجاج التقريبي القديم على ما دون ذلك.
 */
@Stable
class GlassBackdropState {
    /** طبقة الرسم المسجَّلة لخلفية الشاشة، تُملأ من [Modifier.glassBackdropSource]. */
    var layer by mutableStateOf<androidx.compose.ui.graphics.layer.GraphicsLayer?>(null)

    /** موضع مصدر الخلفية بالنسبة لجذر الشاشة، لمحاذاة الترجمة عند القراءة. */
    var sourceOrigin by mutableStateOf(Offset.Zero)
}

/** مصدر افتراضي منفصل (بلا اتصال فعلي) يُستخدم فقط إن نُسي تزويد الحالة الحقيقية. */
val LocalGlassBackdrop = compositionLocalOf { GlassBackdropState() }

/**
 * يُوضع على الحاوية الأكبر التي تمثّل "خلفية التطبيق الحيّة" (هنا:
 * AmbientBackground الممتدة كامل الشاشة خلف NavHost) كي تُسجَّل كل إطار
 * في طبقة رسم منفصلة يعاد استخدامها لاحقاً عند رسم الزجاج، بلا أي تكلفة
 * إضافية على الرسم الطبيعي على الشاشة (التسجيل والرسم الفعلي يحدثان معاً).
 */
fun Modifier.glassBackdropSource(state: GlassBackdropState): Modifier = composed {
    val graphicsLayer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    this
        .onGloballyPositioned { coordinates ->
            state.sourceOrigin = coordinates.positionInRoot()
        }
        .drawWithContent {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            graphicsLayer.record(density = density, layoutDirection = layoutDirection, size = IntSize(w, h)) {
                this@drawWithContent.drawContent()
            }
            state.layer = graphicsLayer
            drawLayer(graphicsLayer)
        }
}

/**
 * يُوضع على عنصر عائم زجاجي (الشريط السفلي/العلوي) ليرسم خلفه نسخة
 * مموَّهة فعلياً من [GlassBackdropState.layer] بدل تدرّج لوني تقريبي.
 * يُنشئ طبقة رسم منفصلة خاصة به (لا يعدّل طبقة المصدر نفسها) كي لا يؤثّر
 * التمويه هنا على رسم الخلفية الطبيعي على الشاشة أو على أي مستهلك آخر
 * بنصف قطر تمويه مختلف.
 */
fun Modifier.glassBackdropBlur(
    state: GlassBackdropState,
    blurRadius: Dp = 26.dp,
    tint: Color = Color.White,
    tintAlpha: Float = 0.30f
): Modifier = composed {
    val consumerLayer = rememberGraphicsLayer()
    var myPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    // التمويه الحقيقي (RenderEffect) غير متاح إلا من أندرويد 12 (API 31)
    // فما فوق — قيد من نظام التشغيل نفسه، لا حل برمجي بديل رخيص له. على
    // ما قبل ذلك كان هذا المكوّن يتوقف بالكامل ويرجع للتدرّج التقريبي
    // القديم، فتظهر كبسولة مصمتة بلا أي أثر حي للمحتوى خلفها بتاتاً على
    // كل تلك الأجهزة — وهذا بالضبط ما ظهر بلقطة شاشة جهاز أقدم. الآن حتى
    // بلا RenderEffect نرسم نفس المحتوى الحي خلف الشريط فعلياً (بلا
    // تمويه، لكن شفّافاً وحقيقياً يتحرك مع التمرير) مع صبغة أعلى قليلاً
    // تعوّض غياب التمويه — "زجاج شفّاف حي" بدل "زجاج مموَّه" فقط على تلك
    // الأجهزة، بدل العودة للتقريب الثابت القديم بالكامل.
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurPx = with(density) { blurRadius.toPx() }
    val renderEffect = remember(blurPx, supportsBlur) {
        if (supportsBlur) RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP).asComposeRenderEffect() else null
    }

    this
        .onGloballyPositioned { myPositionInRoot = it.positionInRoot() }
        .drawWithContent {
            val source = state.layer
            val w = size.width.toInt()
            val h = size.height.toInt()
            if (source != null && w > 0 && h > 0) {
                val offset = state.sourceOrigin - myPositionInRoot
                consumerLayer.record(density = density, layoutDirection = layoutDirection, size = IntSize(w, h)) {
                    translate(offset.x, offset.y) {
                        drawLayer(source)
                    }
                }
                if (renderEffect != null) consumerLayer.renderEffect = renderEffect
                drawLayer(consumerLayer)
                drawRect(
                    tint.copy(
                        alpha = if (renderEffect != null) tintAlpha
                        else (tintAlpha + 0.22f).coerceAtMost(0.75f)
                    )
                )
            }
            drawContent()
        }
}
