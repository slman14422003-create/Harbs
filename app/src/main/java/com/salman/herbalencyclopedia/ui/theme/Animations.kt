package com.salman.herbalencyclopedia.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

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
}
