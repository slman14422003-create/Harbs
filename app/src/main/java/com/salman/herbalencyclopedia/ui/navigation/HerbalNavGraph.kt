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
import com.salman.herbalencyclopedia.ui.screens.admin.*
import com.salman.herbalencyclopedia.ui.screens.allherbs.AllHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.auth.LoginScreen
import com.salman.herbalencyclopedia.ui.screens.category.CategoryHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.compare.CompareScreen
import com.salman.herbalencyclopedia.ui.screens.favorites.FavoritesScreen
import com.salman.herbalencyclopedia.ui.screens.help.HelpScreen
import com.salman.herbalencyclopedia.ui.screens.herbdetail.HerbDetailScreen
import com.salman.herbalencyclopedia.ui.screens.home.HomeScreen
import com.salman.herbalencyclopedia.ui.screens.search.SearchScreen
import com.salman.herbalencyclopedia.ui.screens.settings.SettingsScreen
import com.salman.herbalencyclopedia.ui.screens.tools.AdminToolsScreen
import kotlinx.coroutines.launch

@Composable
fun HerbalNavGraph(appViewModel: AppViewModel, preferencesRepository: PreferencesRepository, navController: NavHostController = rememberNavController()) {
    val uiState by appViewModel.uiState.collectAsState()
    val favoriteIds by appViewModel.favoriteIds.collectAsState()
    val darkMode by preferencesRepository.darkMode.collectAsState(initial = null)
    val dynamicColor by preferencesRepository.dynamicColor.collectAsState(initial = true)
    val fontScale by preferencesRepository.fontScale.collectAsState(initial = 0)
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val topRoutes = setOf(Screen.Home.route, Screen.AllHerbs.route, Screen.Favorites.route, Screen.Settings.route)
    val scope = rememberCoroutineScope()

    Scaffold(bottomBar = {
        if (current in topRoutes) NavigationBar {
            NavItem("الرئيسية", Icons.Filled.Home, Screen.Home.route, current, navController)
            NavItem("الأعشاب", Icons.Filled.MenuBook, Screen.AllHerbs.route, current, navController)
            NavItem("المفضلة", Icons.Filled.Favorite, Screen.Favorites.route, current, navController)
            NavItem("الإعدادات", Icons.Filled.Settings, Screen.Settings.route, current, navController)
        }
    }) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            NavHost(navController, Screen.Home.route) {
                composable(Screen.Home.route) { HomeScreen(uiState.categories, uiState.herbs, uiState.isLoading, uiState.error, appViewModel.isAdmin, appViewModel::refresh, { c -> navController.navigate(Screen.CategoryHerbs.createRoute(c.id,c.name)) }, { navController.navigate(Screen.Search.route) }, { navController.navigate(Screen.Favorites.route) }, { navController.navigate(Screen.Settings.route) }, { navController.navigate(Screen.Admin.route) }, { navController.navigate(Screen.Compare.route) }) }
                composable(Screen.AllHerbs.route) { AllHerbsScreen(uiState.herbs, favoriteIds, {}, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Favorites.route) { FavoritesScreen(uiState.herbs.filter { it.id in favoriteIds }, { }, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Settings.route) { SettingsScreen(appViewModel.isLoggedIn, appViewModel.isAdmin, darkMode, dynamicColor, fontScale, { navController.popBackStack() }, { scope.launch { preferencesRepository.setDarkMode(it) } }, { scope.launch { preferencesRepository.setDynamicColor(it) } }, { scope.launch { preferencesRepository.setFontScale(it) } }, { navController.navigate(Screen.Login.route) }, { appViewModel.logout() }, { navController.navigate(Screen.Help.route) }, { if (appViewModel.isAdmin) navController.navigate(Screen.AdminTools.route) }) }
                composable(Screen.Search.route) { SearchScreen(uiState.herbs, favoriteIds, { navController.popBackStack() }, { h -> navController.navigate(Screen.HerbDetail.createRoute(h.id)) }, appViewModel::toggleFavorite) }
                composable(Screen.Compare.route) { CompareScreen(uiState.herbs, { navController.popBackStack() }) }
                composable(Screen.Help.route) { HelpScreen { navController.popBackStack() } }
                composable(Screen.CategoryHerbs.route, arguments=listOf(navArgument("categoryId"){type=NavType.StringType},navArgument("categoryName"){type=NavType.StringType})) { e -> val id=e.arguments?.getString("categoryId") ?: ""; val name=e.arguments?.getString("categoryName") ?: ""; CategoryHerbsScreen(name, uiState.herbs.filter { it.categoryId==id }, favoriteIds, {navController.popBackStack()}, {h->navController.navigate(Screen.HerbDetail.createRoute(h.id))}, appViewModel::toggleFavorite) }
                composable(Screen.HerbDetail.route, arguments=listOf(navArgument("herbId"){type=NavType.StringType})) { e -> uiState.herbs.firstOrNull { it.id==e.arguments?.getString("herbId") }?.let { h -> HerbDetailScreen(h, h.id in favoriteIds, {navController.popBackStack()}, {appViewModel.toggleFavorite(h.id)}) } }
                composable(Screen.Login.route) { LoginScreen({navController.popBackStack()}, appViewModel::login, appViewModel::register) {navController.popBackStack()} }
                composable(Screen.Admin.route) { AdminListScreen(uiState.herbs, {navController.popBackStack()}, {navController.navigate(Screen.AdminEdit.createRoute(Screen.AdminEdit.NEW))}, {h->navController.navigate(Screen.AdminEdit.createRoute(h.id))}, {h->appViewModel.deleteHerb(h.id) { _, _ -> }}) { navController.navigate(Screen.AdminTools.route) } }
                composable(Screen.AdminTools.route) { AdminToolsScreen(uiState.categories, uiState.herbs, {navController.popBackStack()}, appViewModel::refresh, {n->appViewModel.addCategory(n)}, {id->appViewModel.deleteCategory(id)}, {appViewModel.deleteAllHerbs()}, {appViewModel.deleteAllData()}, {appViewModel.testConnection {ok,msg-> /* result shown by app state; keep UI non-blocking */ }}, {appViewModel.clearFavorites()}, {json->appViewModel.restoreBackup(json){_,_->}}) }
                composable(Screen.AdminEdit.route, arguments=listOf(navArgument("herbId"){type=NavType.StringType})) { e -> val id=e.arguments?.getString("herbId"); val existing=uiState.herbs.firstOrNull {it.id==id}; AdminEditHerbScreen(existing, uiState.categories, {navController.popBackStack()}, {h,cb->if(existing==null) appViewModel.addHerb(h,cb) else appViewModel.updateHerb(h,cb)}) }
            }
        }
    }
}

@Composable private fun RowScope.NavItem(label:String, icon:androidx.compose.ui.graphics.vector.ImageVector, route:String, current:String?, nav:NavHostController) { NavigationBarItem(selected=current==route,onClick={nav.navigate(route){popUpTo(Screen.Home.route);launchSingleTop=true}},icon={Icon(icon,null)},label={Text(label)}) }
