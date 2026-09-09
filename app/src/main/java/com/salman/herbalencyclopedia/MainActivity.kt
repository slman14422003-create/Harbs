package com.salman.herbalencyclopedia

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salman.herbalencyclopedia.data.repository.PreferencesRepository
import com.salman.herbalencyclopedia.ui.AppViewModel
import com.salman.herbalencyclopedia.ui.AppViewModelFactory
import com.salman.herbalencyclopedia.ui.navigation.HerbalNavGraph
import com.salman.herbalencyclopedia.ui.theme.HerbalEncyclopediaTheme
import com.salman.herbalencyclopedia.ui.theme.LocalPerformanceMode
import com.salman.herbalencyclopedia.ui.theme.LocalRefreshRateTier
import com.salman.herbalencyclopedia.ui.theme.PerformanceMode
import com.salman.herbalencyclopedia.ui.theme.applyPreferredRefreshRate
import com.salman.herbalencyclopedia.ui.theme.currentRefreshRateHz
import com.salman.herbalencyclopedia.ui.theme.nearestRefreshRateTier
import com.salman.herbalencyclopedia.ui.util.AppLanguage
import com.salman.herbalencyclopedia.ui.util.LocalAppLanguage
import com.salman.herbalencyclopedia.ui.util.LocaleManager

class MainActivity : ComponentActivity() {

    // يُستدعى قبل onCreate وقبل أي Compose/ViewModel: يغلّف الـ Context
    // بلغة المستخدم المحفوظة (عربي/إنجليزي) حتى تتبع كل موارد Android
    // (بما فيها اتجاه RTL/LTR الافتراضي وأي مورد @string لاحق) تلك اللغة
    // بدل لغة نظام الجهاز نفسه. راجع PreferencesRepository.getSavedLanguageCodeBlocking وLocaleManager.
    override fun attachBaseContext(newBase: Context) {
        val languageCode = PreferencesRepository.getSavedLanguageCodeBlocking(newBase)
        super.attachBaseContext(LocaleManager.wrapContext(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Prevent screenshots/screen-capture of the app, including the admin area.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val container = (application as HerbalApp).container

        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(container))
            val darkModePref by container.preferencesRepository.darkMode.collectAsState(initial = null)
            val dynamicColorPref by container.preferencesRepository.dynamicColor.collectAsState(initial = false)
            val fontScale by container.preferencesRepository.fontScale.collectAsState(initial = 0)
            val themePalette by container.preferencesRepository.themePalette.collectAsState(
                initial = com.salman.herbalencyclopedia.ui.theme.ThemePalette.LEAF
            )
            val performanceMode by container.preferencesRepository.performanceMode.collectAsState(
                initial = PerformanceMode.HIGH_QUALITY
            )
            val appLanguage by container.preferencesRepository.appLanguage.collectAsState(
                initial = AppLanguage.ARABIC
            )
            val useDark = darkModePref ?: isSystemInDarkTheme()

            // معدل تحديث الشاشة الفعلي الحالي (مُقرَّب لأقرب طبقة من
            // 20/40/60/90/120 — راجع RefreshRate.kt)، يُعاد حسابه في كل
            // مرة يتغيّر فيها وضع الأداء المختار (تبديله من الإعدادات
            // يُعيد تشغيل SideEffect التالي، الذي يطلب من النظام وضع عرض
            // جديد أولاً ثم يقرأ المعدل الفعلي الناتج).
            var refreshRateTier by remember { mutableIntStateOf(60) }
            SideEffect {
                applyPreferredRefreshRate(this@MainActivity, performanceMode)
                refreshRateTier = nearestRefreshRateTier(currentRefreshRateHz())
            }

            SideEffect {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }

            // اتجاه التخطيط يتبع الآن لغة التطبيق المختارة من الإعدادات
            // (عربي = RTL، إنجليزي = LTR) بدل فرض RTL دائماً كما كان
            // سابقاً حين كان التطبيق عربياً فقط بلا أي لغة أخرى. راجع
            // AppLanguage وLocalAppLanguage وSettingsScreen لآلية التبديل
            // الكاملة (بما فيها إعادة إنشاء النشاط عبر attachBaseContext
            // أعلاه لتطبيق اللغة على موارد Android أيضاً وليس فقط اتجاه Compose).
            CompositionLocalProvider(
                LocalAppLanguage provides appLanguage,
                LocalLayoutDirection provides if (appLanguage == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                HerbalEncyclopediaTheme(
                    darkTheme = useDark,
                    dynamicColor = dynamicColorPref,
                    palette = themePalette,
                    fontScale = fontScale
                ) {
                    // بدون هذا، اختيار "اقتصادي" من الإعدادات كان يُحفظ في
                    // DataStore فقط دون أي أثر فعلي: LocalPerformanceMode لم
                    // يكن يُزوَّد (provide) بالقيمة الحقيقية في أي مكان بالتطبيق،
                    // فكانت كل مكوّنات الزجاج السائل (LiquidGlassSurface وغيرها)
                    // تقرأ دائماً القيمة الافتراضية HIGH_QUALITY بغض النظر عن
                    // اختيار المستخدم — هذا هو إصلاح "الزر الاقتصادي".
                    CompositionLocalProvider(
                        LocalPerformanceMode provides performanceMode,
                        LocalRefreshRateTier provides refreshRateTier
                    ) {
                        HerbalNavGraph(
                            appViewModel = appViewModel,
                            preferencesRepository = container.preferencesRepository
                        )
                    }
                }
            }
        }
    }
}

