package com.salman.herbalencyclopedia

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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salman.herbalencyclopedia.ui.AppViewModel
import com.salman.herbalencyclopedia.ui.AppViewModelFactory
import com.salman.herbalencyclopedia.ui.navigation.HerbalNavGraph
import com.salman.herbalencyclopedia.ui.theme.HerbalEncyclopediaTheme
import com.salman.herbalencyclopedia.ui.theme.LocalPerformanceMode
import com.salman.herbalencyclopedia.ui.theme.PerformanceMode

class MainActivity : ComponentActivity() {
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
            val useDark = darkModePref ?: isSystemInDarkTheme()

            SideEffect {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }

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
                CompositionLocalProvider(LocalPerformanceMode provides performanceMode) {
                    HerbalNavGraph(
                        appViewModel = appViewModel,
                        preferencesRepository = container.preferencesRepository
                    )
                }
            }
        }
    }
}
