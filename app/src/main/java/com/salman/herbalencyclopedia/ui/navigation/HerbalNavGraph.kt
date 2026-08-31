package com.salman.herbalencyclopedia.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.salman.herbalencyclopedia.data.repository.PreferencesRepository
import com.salman.herbalencyclopedia.ui.AppViewModel
import com.salman.herbalencyclopedia.ui.components.OneUiFloatingNavBar
import com.salman.herbalencyclopedia.ui.components.OneUiNavItem
import com.salman.herbalencyclopedia.ui.screens.admin.*
import com.salman.herbalencyclopedia.ui.screens.allherbs.AllHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.auth.LoginScreen
import com.salman.herbalencyclopedia.ui.screens.category.CategoryHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.compare.CompareScreen
import com.salman.herbalencyclopedia.ui.screens.favorites.FavoritesScreen
import com.salman.herbalencyclopedia.ui.screens.help.HelpScreen
import com.salman.herbalencyclopedia.ui.screens.herbdetail.HerbDetailScreen
import com.salman.herbalencyclopedia.ui.screens.home.HomeScreen
import com.salman.herbalencyclopedia.ui.screens.onboarding.WelcomeScreen
import com.salman.herbalencyclopedia.ui.screens.search.SearchScreen
import com.salman.herbalencyclopedia.ui.screens.settings.SettingsScreen
import com.salman.herbalencyclopedia.ui.screens.splash.SplashScreen
import com.salman.herbalencyclopedia.ui.screens.tools.AdminToolsScreen
import kotlinx.coroutines.launch

@Composable
fun HerbalNavGraph(appViewModel: AppViewModel, preferencesRepository: PreferencesRepository, navController: NavHostController = rememberNavController()) {
    val uiState by appViewModel.uiState.collectAsState()
    val favoriteIds by appViewModel.favoriteIds.collectAsState()
    val darkMode by preferencesRepository.darkMode.collectAsState(initial = null)
    // null = لم تُقرأ بعد من DataStore. نتعامل معها كـ "لم يوافق بعد" (نفس
    // معاملة false) لتفادي أي وميض للشاشة الرئيسية قبل عرض شاشة الترحيب.
    val termsAccepted by preferencesRepository.termsAccepted.collectAsState(initial = null)
    val dynamicColor by preferencesRepository.dynamicColor.collectAsState(initial = true)
    val fontScale by preferencesRepository.fontScale.collectAsState(initial = 0)
    val themePalette by preferencesRepository.themePalette.collectAsState(
        initial = com.salman.herbalencyclopedia.ui.theme.ThemePalette.LEAF
    )
    val performanceMode by preferencesRepository.performanceMode.collectAsState(
        initial = com.salman.herbalencyclopedia.ui.theme.PerformanceMode.HIGH_QUALITY
    )
    val updateState by appViewModel.updateState.collectAsState()
    val downloadState by appViewModel.downloadState.collectAsState()
    val updateConfig by appViewModel.updateConfigState.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val topRoutes = setOf(Screen.Home.route, Screen.AllHerbs.route, Screen.Favorites.route, Screen.Settings.route)
    val scope = rememberCoroutineScope()
    val bottomNavItems = remember {
        listOf(
            OneUiNavItem("الرئيسية", Icons.Filled.Home, Screen.Home.route),
            OneUiNavItem("الأعشاب", Icons.Filled.MenuBook, Screen.AllHerbs.route),
            OneUiNavItem("المفضلة", Icons.Filled.Favorite, Screen.Favorites.route),
            OneUiNavItem("الإعدادات", Icons.Filled.Settings, Screen.Settings.route)
        )
    }

    Scaffold(
        // كل شاشة تدير حواف نظامها بنفسها: GlassTopBar/TopAppBar يتكفّل بشريط
        // الحالة العلوي، وOneUiFloatingNavBar يتكفّل بشريط التنقل السفلي عبر
        // windowInsetsPadding الخاص به. لو ترك Scaffold الخارجي هنا القيمة
        // الافتراضية (safeDrawing) لحجز مساحة إضافية لنفس الحواف، تظهر فجوة
        // مزدوجة أعلى/أسفل كل شاشة — وعلى شاشة البداية تحديداً كانت تقطع
        // التدرّج اللوني قبل أن يصل لحواف الشاشة فعلياً.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
        if (current in topRoutes) {
            OneUiFloatingNavBar(
                items = bottomNavItems,
                currentRoute = current,
                onItemClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                }
            )
        }
    }) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            com.salman.herbalencyclopedia.ui.components.AmbientBackground()
            NavHost(
                navController,
                Screen.Splash.route,
                enterTransition = {
                    androidx.compose.animation.fadeIn(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth()) +
                        androidx.compose.animation.slideInHorizontally(
                            com.salman.herbalencyclopedia.ui.theme.AppMotion.silky()
                        ) { it / 6 }
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth(
                            com.salman.herbalencyclopedia.ui.theme.AppMotion.Quick
                        )
                    )
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth())
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth()) +
                        androidx.compose.animation.slideOutHorizontally(
                            com.salman.herbalencyclopedia.ui.theme.AppMotion.silky()
                        ) { it / 6 }
                }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(onFinished = {
                        val destination = if (termsAccepted == true) Screen.Home.route else Screen.Welcome.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onViewFullPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                        onAgree = {
                            scope.launch {
                                preferencesRepository.setTermsAccepted(true)
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(Screen.Home.route) { HomeScreen(uiState.categories, uiState.herbs, uiState.isLoading, uiState.error, appViewModel.isAdmin, appViewModel::refresh, { c -> navController.navigate(Screen.CategoryHerbs.createRoute(c.id,c.name)) }, { navController.navigate(Screen.Search.route) }, { navController.navigate(Screen.Favorites.route) }, { navController.navigate(Screen.Settings.route) }, { navController.navigate(Screen.Admin.route) }, { navController.navigate(Screen.Compare.route) }) }
                composable(Screen.AllHerbs.route) { AllHerbsScreen(uiState.herbs, favoriteIds, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Favorites.route) { FavoritesScreen(uiState.herbs.filter { it.id in favoriteIds }, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Settings.route) { SettingsScreen(appViewModel.isLoggedIn, appViewModel.isAdmin, darkMode, dynamicColor, fontScale, themePalette, performanceMode, updateState, downloadState, { navController.popBackStack() }, { scope.launch { preferencesRepository.setDarkMode(it) } }, { scope.launch { preferencesRepository.setDynamicColor(it) } }, { scope.launch { preferencesRepository.setFontScale(it) } }, { scope.launch { preferencesRepository.setThemePalette(it) } }, { scope.launch { preferencesRepository.setPerformanceMode(it) } }, { navController.navigate(Screen.Login.route) }, { appViewModel.logout() }, { navController.navigate(Screen.Help.route) }, { navController.navigate(Screen.PrivacyPolicy.route) }, { if (appViewModel.isAdmin) navController.navigate(Screen.AdminTools.route) }, { ctx -> appViewModel.checkForUpdate(ctx) }, { ctx, info -> appViewModel.downloadUpdate(ctx, info) }, { ctx -> appViewModel.installUpdate(ctx) }) }
                composable(Screen.Search.route) { SearchScreen(uiState.herbs, favoriteIds, { navController.popBackStack() }, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Compare.route) { CompareScreen(uiState.herbs, { navController.popBackStack() }) }
                composable(Screen.Help.route) { HelpScreen { navController.popBackStack() } }
                composable(Screen.PrivacyPolicy.route) {
                    com.salman.herbalencyclopedia.ui.screens.privacy.PrivacyPolicyScreen { navController.popBackStack() }
                }
                composable(Screen.CategoryHerbs.route, arguments=listOf(navArgument("categoryId"){type=NavType.StringType},navArgument("categoryName"){type=NavType.StringType})) { e -> val id=e.arguments?.getString("categoryId") ?: ""; val name=e.arguments?.getString("categoryName") ?: ""; CategoryHerbsScreen(name, uiState.herbs.filter { it.categoryId==id }, favoriteIds, {navController.popBackStack()}, {h->navController.navigate(Screen.HerbDetail.createRoute(h.id))}, appViewModel::toggleFavorite) }
                composable(
                    Screen.HerbDetail.route,
                    arguments = listOf(navArgument("herbId") { type = NavType.StringType }),
                    // دخول/خروج مميّز لتفاصيل العشبة: تكبير من المنتصف + تلاشي
                    // بدل الانزلاق الأفقي العام، لإحساس "فتح البطاقة" بدل تنقّل عادي.
                    enterTransition = {
                        androidx.compose.animation.fadeIn(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth()) +
                            androidx.compose.animation.scaleIn(
                                com.salman.herbalencyclopedia.ui.theme.AppMotion.silky(),
                                initialScale = 0.92f
                            )
                    },
                    exitTransition = {
                        androidx.compose.animation.fadeOut(
                            com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth(
                                com.salman.herbalencyclopedia.ui.theme.AppMotion.Quick
                            )
                        )
                    },
                    popEnterTransition = {
                        androidx.compose.animation.fadeIn(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth())
                    },
                    popExitTransition = {
                        androidx.compose.animation.fadeOut(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth()) +
                            androidx.compose.animation.scaleOut(
                                com.salman.herbalencyclopedia.ui.theme.AppMotion.silky(),
                                targetScale = 0.92f
                            )
                    }
                ) { e ->
                    uiState.herbs.firstOrNull { it.id == e.arguments?.getString("herbId") }?.let { h ->
                        HerbDetailScreen(h, h.id in favoriteIds, { navController.popBackStack() }, { appViewModel.toggleFavorite(h.id) })
                    }
                }
                composable(Screen.Login.route) { LoginScreen({ navController.popBackStack() }, appViewModel::login) { navController.popBackStack() } }
                composable(Screen.Admin.route) { if (appViewModel.isAdmin) AdminListScreen(uiState.herbs, {navController.popBackStack()}, {navController.navigate(Screen.AdminEdit.createRoute(Screen.AdminEdit.NEW))}, {h->navController.navigate(Screen.AdminEdit.createRoute(h.id))}, {h->appViewModel.deleteHerb(h.id) { _, _ -> }}) { navController.navigate(Screen.AdminTools.route) } }
                composable(Screen.AdminTools.route) { if (appViewModel.isAdmin) AdminToolsScreen(uiState.categories, uiState.herbs, {navController.popBackStack()}, appViewModel::refresh, {n,cb->appViewModel.addCategory(n,cb)}, {id,cb->appViewModel.deleteCategory(id,cb)}, {cb->appViewModel.deleteAllHerbs(cb)}, {cb->appViewModel.deleteAllData(cb)}, {cb->appViewModel.testConnection(cb)}, {appViewModel.clearFavorites()}, {json,cb->appViewModel.restoreBackup(json,cb)}, {navController.navigate(Screen.AdminUpdate.route)}) }
                composable(Screen.AdminUpdate.route) {
                    if (appViewModel.isAdmin) {
                        LaunchedEffect(Unit) { appViewModel.loadUpdateConfig() }
                        com.salman.herbalencyclopedia.ui.screens.admin.AdminUpdateScreen(
                            config = updateConfig,
                            onBack = { navController.popBackStack() },
                            onSave = { config, cb -> appViewModel.saveUpdateConfig(config, cb) }
                        )
                    }
                }
                composable(Screen.AdminEdit.route, arguments=listOf(navArgument("herbId"){type=NavType.StringType})) { e -> if (appViewModel.isAdmin) { val id=e.arguments?.getString("herbId"); val existing=uiState.herbs.firstOrNull {it.id==id}; AdminEditHerbScreen(existing, uiState.categories, {navController.popBackStack()}, {h,cb->if(existing==null) appViewModel.addHerb(h,cb) else appViewModel.updateHerb(h,cb)}) } }
            }
        }
    }
}
