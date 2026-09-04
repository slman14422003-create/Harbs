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
import com.salman.herbalencyclopedia.data.ai.AiConfig
import com.salman.herbalencyclopedia.data.ai.TrainedExample
import com.salman.herbalencyclopedia.data.repository.PreferencesRepository
import com.salman.herbalencyclopedia.ui.AppViewModel
import com.salman.herbalencyclopedia.ui.components.OneUiFloatingNavBar
import com.salman.herbalencyclopedia.ui.components.OneUiNavItem
import com.salman.herbalencyclopedia.ui.components.OneUiNavigationRail
import com.salman.herbalencyclopedia.ui.util.rememberWindowSizeInfo
import com.salman.herbalencyclopedia.ui.screens.admin.*
import com.salman.herbalencyclopedia.ui.screens.allherbs.AllHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.auth.LoginScreen
import com.salman.herbalencyclopedia.ui.screens.blends.BlendDetailScreen
import com.salman.herbalencyclopedia.ui.screens.blends.BlendsScreen
import com.salman.herbalencyclopedia.ui.screens.category.CategoryHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.semo.SemoAssistantScreen
import com.salman.herbalencyclopedia.ui.screens.favorites.FavoritesScreen
import com.salman.herbalencyclopedia.ui.screens.feedback.SendFeedbackScreen
import com.salman.herbalencyclopedia.ui.screens.help.HelpScreen
import com.salman.herbalencyclopedia.ui.screens.herbdetail.HerbDetailScreen
import com.salman.herbalencyclopedia.ui.screens.home.HomeScreen
import com.salman.herbalencyclopedia.ui.screens.onboarding.WelcomeScreen
import com.salman.herbalencyclopedia.ui.screens.search.SearchScreen
import com.salman.herbalencyclopedia.ui.screens.settings.SettingsScreen
import com.salman.herbalencyclopedia.ui.screens.splash.SplashScreen
import com.salman.herbalencyclopedia.ui.screens.terms.TermsScreen
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
    // إعدادات "سيمو المساعد" — تُحمَّل من DataStore وتُطبَّق حياً على
    // AiConfig (الكائن الذي يقرأ منه HerbAssistant مباشرة)، بحيث أي تعديل
    // من أدوات المطور ينعكس فوراً على شاشة سيمو دون إعادة تشغيل التطبيق.
    val aiSimilarityThreshold by preferencesRepository.aiSimilarityThreshold.collectAsState(
        initial = AiConfig.defaultSimilarityThreshold.toFloat()
    )
    val aiSearchThreshold by preferencesRepository.aiSearchThreshold.collectAsState(
        initial = AiConfig.defaultSearchThreshold.toFloat()
    )
    val aiExtraStopWords by preferencesRepository.aiExtraStopWords.collectAsState(initial = emptySet())
    val aiSynonyms by preferencesRepository.aiSynonyms.collectAsState(initial = emptyMap())
    val aiTrainedExamples by preferencesRepository.aiTrainedExamples.collectAsState(initial = emptyList())
    val aiTrainedThreshold by preferencesRepository.aiTrainedThreshold.collectAsState(
        initial = AiConfig.defaultTrainedThreshold.toFloat()
    )
    // "تعلّم سيمو الذاتي" — حالات يولّدها التطبيق تلقائياً من تقييمات
    // المستخدمين (👍/👎) على إجابات البحث الحر في شاشة الدردشة، منفصلة عن
    // تدريب المطوّر اليدوي أعلاه، ومحفوظة/مُطبَّقة حياً بنفس الآلية.
    val aiAutoLearnedExamples by preferencesRepository.aiAutoLearnedExamples.collectAsState(initial = emptyList())
    val aiAutoLearnEnabled by preferencesRepository.aiAutoLearnEnabled.collectAsState(
        initial = AiConfig.defaultAutoLearnEnabled
    )
    LaunchedEffect(
        aiSimilarityThreshold, aiSearchThreshold, aiExtraStopWords, aiSynonyms,
        aiTrainedExamples, aiTrainedThreshold, aiAutoLearnedExamples, aiAutoLearnEnabled
    ) {
        AiConfig.similarityThreshold = aiSimilarityThreshold.toDouble()
        AiConfig.searchThreshold = aiSearchThreshold.toDouble()
        AiConfig.extraStopWords = aiExtraStopWords
        AiConfig.synonyms = aiSynonyms
        AiConfig.trainedExamples = aiTrainedExamples
        AiConfig.trainedMatchThreshold = aiTrainedThreshold.toDouble()
        AiConfig.autoLearnedExamples = aiAutoLearnedExamples
        AiConfig.autoLearnEnabled = aiAutoLearnEnabled
    }
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
    // محرك اكتشاف الشاشة يقرر نمط التنقّل: شريط عائم سفلي بالإبهام على
    // جوال (COMPACT)، وشريط جانبي ثابت على تابلت/نافذة عريضة
    // (MEDIUM/EXPANDED) بدل تمديد نفس الشريط السفلي بعرض الشاشة كاملاً —
    // وهو ما كان يحدث سابقاً بلا أي تمييز بين الحجمين.
    val windowInfo = rememberWindowSizeInfo()
    val onNavItemClick: (OneUiNavItem) -> Unit = { item ->
        navController.navigate(item.route) {
            popUpTo(Screen.Home.route)
            launchSingleTop = true
        }
    }
    val showNav = current in topRoutes

    Scaffold(
        // كل شاشة تدير حواف نظامها بنفسها: GlassTopBar/TopAppBar يتكفّل بشريط
        // الحالة العلوي، وOneUiFloatingNavBar يتكفّل بشريط التنقل السفلي عبر
        // windowInsetsPadding الخاص به. لو ترك Scaffold الخارجي هنا القيمة
        // الافتراضية (safeDrawing) لحجز مساحة إضافية لنفس الحواف، تظهر فجوة
        // مزدوجة أعلى/أسفل كل شاشة — وعلى شاشة البداية تحديداً كانت تقطع
        // التدرّج اللوني قبل أن يصل لحواف الشاشة فعلياً.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // الشريط العائم السفلي فقط بوضع الجوال الضيق؛ على تابلت/نافذة
            // عريضة ينتقل التنقل لشريط جانبي (أدناه) فلا حاجة لحجز سفلي هنا.
            if (showNav && !windowInfo.useNavigationRail) {
                OneUiFloatingNavBar(
                    items = bottomNavItems,
                    currentRoute = current,
                    onItemClick = onNavItemClick
                )
            }
        }
    ) { inner ->
        Row(Modifier.padding(inner).fillMaxSize()) {
            // على تابلت/نافذة عريضة: شريط جانبي ثابت بجوار المحتوى بدل
            // شريط سفلي عائم يمتد بعرض الشاشة كاملاً بعيداً عن متناول
            // الإبهام في تلك الحالة.
            if (showNav && windowInfo.useNavigationRail) {
                OneUiNavigationRail(
                    items = bottomNavItems,
                    currentRoute = current,
                    onItemClick = onNavItemClick
                )
            }
        Box(Modifier.weight(1f).fillMaxSize()) {
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
                    SplashScreen(
                        isDataReady = !uiState.isLoading,
                        onFinished = {
                            val destination = if (termsAccepted == true) Screen.Home.route else Screen.Welcome.route
                            navController.navigate(destination) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
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
                composable(Screen.Home.route) { HomeScreen(uiState.categories, uiState.herbs, uiState.isLoading, uiState.error, appViewModel.isAdmin, appViewModel::refresh, { c -> navController.navigate(Screen.CategoryHerbs.createRoute(c.id,c.name)) }, { navController.navigate(Screen.Search.route) }, { navController.navigate(Screen.Favorites.route) }, { navController.navigate(Screen.Settings.route) }, { navController.navigate(Screen.Admin.route) }, { navController.navigate(Screen.SemoAssistant.route) }, { navController.navigate(Screen.Blends.route) }) }
                composable(Screen.AllHerbs.route) { AllHerbsScreen(uiState.herbs, favoriteIds, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Favorites.route) { FavoritesScreen(uiState.herbs.filter { it.id in favoriteIds }, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Settings.route) { SettingsScreen(appViewModel.isLoggedIn, appViewModel.isAdmin, darkMode, dynamicColor, fontScale, themePalette, performanceMode, updateState, downloadState, { navController.popBackStack() }, { scope.launch { preferencesRepository.setDarkMode(it) } }, { scope.launch { preferencesRepository.setDynamicColor(it) } }, { scope.launch { preferencesRepository.setFontScale(it) } }, { scope.launch { preferencesRepository.setThemePalette(it) } }, { scope.launch { preferencesRepository.setPerformanceMode(it) } }, { navController.navigate(Screen.Login.route) }, { appViewModel.logout() }, { navController.navigate(Screen.Help.route) }, { navController.navigate(Screen.PrivacyPolicy.route) }, { navController.navigate(Screen.Terms.route) }, { if (appViewModel.isAdmin) navController.navigate(Screen.AdminTools.route) }, { if (appViewModel.isAdmin) navController.navigate(Screen.AdminFeedback.route) }, { ctx -> appViewModel.checkForUpdate(ctx) }, { ctx, info -> appViewModel.downloadUpdate(ctx, info) }, { ctx -> appViewModel.installUpdate(ctx) }, { appViewModel.cancelDownload() }) }
                composable(Screen.Search.route) { SearchScreen(uiState.herbs, favoriteIds, { navController.popBackStack() }, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.SemoAssistant.route) {
                    SemoAssistantScreen(
                        herbs = uiState.herbs,
                        blends = uiState.blends,
                        onBack = { navController.popBackStack() },
                        onAutoLearnedExamplesChange = { list -> scope.launch { preferencesRepository.setAiAutoLearnedExamples(list) } }
                    )
                }
                composable(Screen.Help.route) { HelpScreen { navController.popBackStack() } }
                composable(Screen.PrivacyPolicy.route) {
                    com.salman.herbalencyclopedia.ui.screens.privacy.PrivacyPolicyScreen { navController.popBackStack() }
                }
                composable(Screen.Terms.route) {
                    TermsScreen { navController.popBackStack() }
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
                        HerbDetailScreen(h, h.id in favoriteIds, { navController.popBackStack() }, { appViewModel.toggleFavorite(h.id) }, { navController.navigate(Screen.SendFeedback.createRoute("herb", h.id, h.name)) })
                    }
                }
                composable(Screen.Login.route) { LoginScreen({ navController.popBackStack() }, appViewModel::login) { navController.popBackStack() } }
                composable(Screen.Admin.route) { if (appViewModel.isAdmin) AdminListScreen(uiState.herbs, {navController.popBackStack()}, {navController.navigate(Screen.AdminEdit.createRoute(Screen.AdminEdit.NEW))}, {h->navController.navigate(Screen.AdminEdit.createRoute(h.id))}, {h->appViewModel.deleteHerb(h.id) { _, _ -> }}) { navController.navigate(Screen.AdminTools.route) } }
                composable(Screen.AdminTools.route) {
                    if (appViewModel.isAdmin) AdminToolsScreen(
                        categories = uiState.categories,
                        herbs = uiState.herbs,
                        onBack = { navController.popBackStack() },
                        onRefresh = appViewModel::refresh,
                        onAddCategory = { n, cb -> appViewModel.addCategory(n, cb) },
                        onDeleteCategory = { id, cb -> appViewModel.deleteCategory(id, cb) },
                        onDeleteAllHerbs = { cb -> appViewModel.deleteAllHerbs(cb) },
                        onDeleteAllData = { cb -> appViewModel.deleteAllData(cb) },
                        onTestConnection = { cb -> appViewModel.testConnection(cb) },
                        onClearFavorites = { appViewModel.clearFavorites() },
                        onRestoreBackup = { json, cb -> appViewModel.restoreBackup(json, cb) },
                        onUpdateSettingsClick = { navController.navigate(Screen.AdminUpdate.route) },
                        aiSimilarityThreshold = aiSimilarityThreshold,
                        aiSearchThreshold = aiSearchThreshold,
                        aiExtraStopWords = aiExtraStopWords,
                        onSetAiSimilarityThreshold = { v -> scope.launch { preferencesRepository.setAiSimilarityThreshold(v) } },
                        onSetAiSearchThreshold = { v -> scope.launch { preferencesRepository.setAiSearchThreshold(v) } },
                        onSetAiExtraStopWords = { w -> scope.launch { preferencesRepository.setAiExtraStopWords(w) } },
                        onResetAiSettings = { scope.launch { preferencesRepository.resetAiSettings() } },
                        aiSynonyms = aiSynonyms,
                        aiTrainedExamples = aiTrainedExamples,
                        aiTrainedThreshold = aiTrainedThreshold,
                        onSetAiSynonyms = { m -> scope.launch { preferencesRepository.setAiSynonyms(m) } },
                        onSetAiTrainedExamples = { list -> scope.launch { preferencesRepository.setAiTrainedExamples(list) } },
                        aiAutoLearnedExamples = aiAutoLearnedExamples,
                        aiAutoLearnEnabled = aiAutoLearnEnabled,
                        onSetAiAutoLearnedExamples = { list -> scope.launch { preferencesRepository.setAiAutoLearnedExamples(list) } },
                        onSetAiAutoLearnEnabled = { v -> scope.launch { preferencesRepository.setAiAutoLearnEnabled(v) } },
                        onSetAiTrainedThreshold = { v -> scope.launch { preferencesRepository.setAiTrainedThreshold(v) } }
                    )
                }
                composable(Screen.AdminUpdate.route) {
                    if (appViewModel.isAdmin) {
                        LaunchedEffect(Unit) { appViewModel.loadUpdateConfig() }
                        val adminTestState by appViewModel.adminUpdateTestState.collectAsState()
                        com.salman.herbalencyclopedia.ui.screens.admin.AdminUpdateScreen(
                            config = updateConfig,
                            testState = adminTestState,
                            onBack = { navController.popBackStack() },
                            onSave = { config, cb -> appViewModel.saveUpdateConfig(config, cb) },
                            onTestNow = { ctx, config -> appViewModel.testUpdateConfig(ctx, config) }
                        )
                    }
                }
                composable(Screen.AdminEdit.route, arguments=listOf(navArgument("herbId"){type=NavType.StringType})) { e -> if (appViewModel.isAdmin) { val id=e.arguments?.getString("herbId"); val existing=uiState.herbs.firstOrNull {it.id==id}; AdminEditHerbScreen(existing, uiState.categories, {navController.popBackStack()}, {h,cb->if(existing==null) appViewModel.addHerb(h,cb) else appViewModel.updateHerb(h,cb)}) } }

                // ── الخلطات (Blends) ──────────────────────────────────
                composable(Screen.Blends.route) {
                    BlendsScreen(
                        blends = uiState.blends,
                        isAdmin = appViewModel.isAdmin,
                        onBack = { navController.popBackStack() },
                        onBlendClick = { b -> navController.navigate(Screen.BlendDetail.createRoute(b.id)) },
                        onAddNew = { navController.navigate(Screen.AdminEditBlend.createRoute(Screen.AdminEditBlend.NEW)) }
                    )
                }
                composable(Screen.BlendDetail.route, arguments = listOf(navArgument("blendId") { type = NavType.StringType })) { e ->
                    val blend = uiState.blends.firstOrNull { it.id == e.arguments?.getString("blendId") }
                    blend?.let { b ->
                        BlendDetailScreen(
                            blend = b,
                            ingredientHerbs = uiState.herbs.filter { it.id in b.herbIds },
                            isAdmin = appViewModel.isAdmin,
                            onBack = { navController.popBackStack() },
                            onIngredientClick = { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) },
                            onEdit = { navController.navigate(Screen.AdminEditBlend.createRoute(b.id)) },
                            onDelete = { appViewModel.deleteBlend(b.id) { success, _ -> if (success) navController.popBackStack() } },
                            onReportIssue = { navController.navigate(Screen.SendFeedback.createRoute("blend", b.id, b.name)) }
                        )
                    }
                }
                composable(Screen.AdminEditBlend.route, arguments = listOf(navArgument("blendId") { type = NavType.StringType })) { e ->
                    if (appViewModel.isAdmin) {
                        val id = e.arguments?.getString("blendId")
                        val existing = uiState.blends.firstOrNull { it.id == id }
                        AdminEditBlendScreen(existing, uiState.herbs, { navController.popBackStack() }, { b, cb -> if (existing == null) appViewModel.addBlend(b, cb) else appViewModel.updateBlend(b, cb) })
                    }
                }

                // ── ملاحظات المستخدمين (Feedback) ─────────────────────
                composable(
                    Screen.SendFeedback.route,
                    arguments = listOf(
                        navArgument("targetType") { type = NavType.StringType },
                        navArgument("targetId") { type = NavType.StringType },
                        navArgument("targetName") { type = NavType.StringType }
                    )
                ) { e ->
                    val targetType = e.arguments?.getString("targetType") ?: "herb"
                    val targetId = e.arguments?.getString("targetId") ?: ""
                    val targetName = e.arguments?.getString("targetName") ?: ""
                    SendFeedbackScreen(
                        targetName = targetName,
                        onBack = { navController.popBackStack() },
                        onSubmit = { senderName, message, cb ->
                            appViewModel.submitFeedback(targetType, targetId, targetName, message, senderName, cb)
                        }
                    )
                }
                composable(Screen.AdminFeedback.route) {
                    if (appViewModel.isAdmin) {
                        LaunchedEffect(Unit) { appViewModel.loadFeedback() }
                        val feedbackList by appViewModel.feedbackList.collectAsState()
                        val feedbackLoading by appViewModel.feedbackLoading.collectAsState()
                        val feedbackError by appViewModel.feedbackError.collectAsState()
                        AdminFeedbackScreen(
                            feedback = feedbackList,
                            isLoading = feedbackLoading,
                            error = feedbackError,
                            onBack = { navController.popBackStack() },
                            onDelete = { f -> appViewModel.deleteFeedback(f.id) }
                        )
                    }
                }
            }
        }
        }
    }
}
