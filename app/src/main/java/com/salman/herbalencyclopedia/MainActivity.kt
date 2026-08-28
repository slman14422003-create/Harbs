package com.salman.herbalencyclopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

        val container = (application as HerbalApp).container

        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(container))
            val darkModePref by container.preferencesRepository.darkMode.collectAsState(initial = null)
            val dynamicColorPref by container.preferencesRepository.dynamicColor.collectAsState(initial = true)
            val fontScale by container.preferencesRepository.fontScale.collectAsState(initial = 0)
            val useDark = darkModePref ?: isSystemInDarkTheme()

            HerbalEncyclopediaTheme(darkTheme = useDark, dynamicColor = dynamicColorPref, fontScale = fontScale) {
                HerbalNavGraph(
                    appViewModel = appViewModel,
                    preferencesRepository = container.preferencesRepository
                )
            }
        }
    }
}
