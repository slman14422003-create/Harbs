package com.salman.herbalencyclopedia.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * مصدر واحد لكل مدد وأنماط الحركة (easing) المستخدمة بالتطبيق.
 * بما أن التطبيق مبني بالكامل على Jetpack Compose، فملفات res/anim
 * (XML) الخاصة بنظام Views القديم لا تُستخدم فعلياً هنا — Compose
 * يطبّق حركاته عبر AnimationSpec بالكود مباشرة. لذلك هذا الملف هو
 * البديل العملي: كل الشاشات تسحب مدد/منحنيات الحركة من هنا بدل ما
 * كل شاشة تخترع أرقامها الخاصة، فتصير الحركة متناسقة وسلسة بكل مكان.
 */
object AppMotion {
    /** منحنى سلس ومريح للعين، يُستخدم افتراضياً لمعظم الحركات. */
    val Smooth: Easing = FastOutSlowInEasing

    /** منحنى أكثر نعومة بالبداية والنهاية، مناسب لانتقالات الشاشات الكبيرة. */
    val Silky: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    const val Quick = 180
    const val Standard = 320
    const val Slow = 480

    fun <T> smooth(durationMillis: Int = Standard) =
        tween<T>(durationMillis = durationMillis, easing = Smooth)

    fun <T> silky(durationMillis: Int = Slow) =
        tween<T>(durationMillis = durationMillis, easing = Silky)

    /** نابض موحّد لكل الحركات "الحيّة" (ضغط زر، تبديل شريط سفلي...) بدل tween الثابت. */
    fun <T> bouncy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
}

/**
 * حركة "ضغطة زجاجية" موحّدة لكل عناصر التطبيق القابلة للنقر (أزرار،
 * بطاقات الأعشاب/التصنيفات...): تصغير خفيف فوري عند الضغط ثم عودة
 * نابضة عند تركه، بدل الاعتماد على الـ ripple فقط. يُستخدم عبر تمرير
 * نفس [interactionSource] المُمرَّر لـ Card/Surface/Button حتى تُطابق
 * حالة الضغط الفعلية للعنصر.
 */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = AppMotion.bouncy(),
        label = "pressScale"
    )
}
