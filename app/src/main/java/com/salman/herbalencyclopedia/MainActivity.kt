package com.salman.herbalencyclopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val container = (application as HerbalApp).container

        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(container))
            val darkModePref by container.preferencesRepository.darkMode.collectAsState(initial = null)
            val dynamicColorPref by container.preferencesRepository.dynamicColor.collectAsState(initial = true)
            val fontScale by container.preferencesRepository.fontScale.collectAsState(initial = 0)
            val themePalette by container.preferencesRepository.themePalette.collectAsState(
                initial = com.salman.herbalencyclopedia.ui.theme.ThemePalette.LEAF
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
                HerbalNavGraph(
                    appViewModel = appViewModel,
                    preferencesRepository = container.preferencesRepository
                )
            }
        }
    }
}
