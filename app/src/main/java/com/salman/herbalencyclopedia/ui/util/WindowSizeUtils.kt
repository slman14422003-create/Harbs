package com.salman.herbalencyclopedia.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * محرك اكتشاف حجم الشاشة الحقيقي للتطبيق.
 *
 * قبل هذا الملف لم يكن هناك أي تمييز بين جوال وتابلت في كل الشاشات:
 * شبكة التصنيفات في [com.salman.herbalencyclopedia.ui.screens.home.HomeScreen]
 * كانت GridCells.Fixed(2) بشكل ثابت، وقوائم الأعشاب (كل الأعشاب/المفضلة/
 * التصنيف/البحث) كانت LazyColumn بعمود واحد دائماً. على تابلت أو نافذة
 * مكبّرة (شاشة مقسومة / طي) هذا يعني بطاقة واحدة تتمدد بعرض الشاشة كاملاً
 * أو شبكة بعمودين فقط تترك فراغاً هائلاً على الجانبين — نفس التخطيط
 * تماماً على جوال 360dp وتابلت 1280dp.
 *
 * الاعتماد هنا على LocalConfiguration.screenWidthDp/screenHeightDp بدل حجم
 * ثابت لمرة واحدة، لأن هذه القيم تُحدَّث تلقائياً مع كل تغيير حقيقي بالحجم
 * المتاح فعلياً للتطبيق (تدوير الجهاز، سحب حجم نافذة في وضع الشاشة
 * المقسومة، فتح/طي جهاز قابل للطي) — فيعيد remember(widthDp, heightDp)
 * حساب التصنيف فوراً بلا حاجة لإعادة تشغيل الشاشة، وبلا الاعتماد على
 * DisplayMetrics الثابتة عند الإقلاع فقط والتي لا تلتقط هذه التغييرات.
 * هذا ما يجعله "ديناميكياً ودقيقاً": يقرأ من نفس مصدر الحقيقة الذي
 * يستخدمه Compose نفسه لقياس الشاشة، لا رقماً مفترضاً مسبقاً.
 *
 * حدود التصنيف (600dp / 840dp) هي نفسها المعتمدة رسمياً من Material 3
 * لـ Window Size Classes (compact/medium/expanded)، فتُطابق سلوك تطبيقات
 * أندرويد الأخرى بدل عتبات مخترعة قد تُصنّف تابلت صغير كجوال أو العكس.
 */
enum class ScreenWidthClass { COMPACT, MEDIUM, EXPANDED }
enum class ScreenHeightClass { COMPACT, MEDIUM, EXPANDED }

data class WindowSizeInfo(
    val widthDp: Int,
    val heightDp: Int,
    val widthClass: ScreenWidthClass,
    val heightClass: ScreenHeightClass,
    /** true من عرض 600dp فأكثر: تابلت، جوال أفقي كبير، أو نافذة مقسومة عريضة. */
    val isTablet: Boolean,
    val isLandscape: Boolean,
    /** true من MEDIUM فأعلى: شاشة تتّسع لشريط تنقّل جانبي بدل شريط عائم سفلي. */
    val useNavigationRail: Boolean,
    /** الحد الأدنى المقترح لعرض خلية الشبكة، يكبر قليلاً على الشاشات الواسعة. */
    val gridMinCellWidth: Dp,
    /** أقصى عرض مقترح لعمود محتوى نصي/قوائم كي لا يتمدد بلا حدود على تابلت عريض. */
    val contentMaxWidth: Dp
)

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp) {
        val widthClass = when {
            widthDp < 600 -> ScreenWidthClass.COMPACT
            widthDp < 840 -> ScreenWidthClass.MEDIUM
            else -> ScreenWidthClass.EXPANDED
        }
        val heightClass = when {
            heightDp < 480 -> ScreenHeightClass.COMPACT
            heightDp < 900 -> ScreenHeightClass.MEDIUM
            else -> ScreenHeightClass.EXPANDED
        }
        WindowSizeInfo(
            widthDp = widthDp,
            heightDp = heightDp,
            widthClass = widthClass,
            heightClass = heightClass,
            isTablet = widthDp >= 600,
            isLandscape = widthDp > heightDp,
            useNavigationRail = widthClass != ScreenWidthClass.COMPACT,
            gridMinCellWidth = when (widthClass) {
                ScreenWidthClass.COMPACT -> 152.dp
                ScreenWidthClass.MEDIUM -> 168.dp
                ScreenWidthClass.EXPANDED -> 180.dp
            },
            contentMaxWidth = when (widthClass) {
                ScreenWidthClass.COMPACT -> Dp.Unspecified
                ScreenWidthClass.MEDIUM -> 760.dp
                ScreenWidthClass.EXPANDED -> 960.dp
            }
        )
    }
}

/**
 * يميّز نمط التنقّل الحالي بالجهاز: true لتنقّل الإيماءات (سحب من الحافة،
 * حاجز سفلي رفيع)، false لأزرار التنقل التقليدية الثلاثة (حاجز أثخن
 * بوضوح). القياس هنا حي عبر WindowInsets.navigationBars فيتحدّث تلقائياً
 * إن بدّل المستخدم نمط التنقّل من الإعدادات دون إغلاق التطبيق، بدل قراءة
 * ثابتة عند الإقلاع فقط.
 *
 * يُستخدم لضبط المسافة حول شريط التنقّل العائم [com.salman.herbalencyclopedia.ui.components.OneUiFloatingNavBar]
 * كي لا يبتعد كثيراً عن حافة الشاشة في وضع الإيماءات، أو يلتصق بأزرار
 * التنقل التقليدية في الوضع الآخر.
 */
@Composable
fun isGestureNavigation(): Boolean {
    val density = LocalDensity.current
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
    return bottomInsetDp < 32.dp
}
