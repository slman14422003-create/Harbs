package com.salman.herbalencyclopedia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salman.herbalencyclopedia.data.repository.PreferencesRepository
import com.salman.herbalencyclopedia.ui.AppViewModel
import com.salman.herbalencyclopedia.ui.screens.admin.AdminEditHerbScreen
import com.salman.herbalencyclopedia.ui.screens.admin.AdminListScreen
import com.salman.herbalencyclopedia.ui.screens.auth.LoginScreen
import com.salman.herbalencyclopedia.ui.screens.category.CategoryHerbsScreen
import com.salman.herbalencyclopedia.ui.screens.favorites.FavoritesScreen
import com.salman.herbalencyclopedia.ui.screens.herbdetail.HerbDetailScreen
import com.salman.herbalencyclopedia.ui.screens.home.HomeScreen
import com.salman.herbalencyclopedia.ui.screens.search.SearchScreen
import com.salman.herbalencyclopedia.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun HerbalNavGraph(
    appViewModel: AppViewModel,
    preferencesRepository: PreferencesRepository,
    navController: NavHostController = rememberNavController()
) {
    val uiState by appViewModel.uiState.collectAsState()
    val favoriteIds by appViewModel.favoriteIds.collectAsState()
    val scope = rememberCoroutineScopeCompat()
    val context = LocalContext.current

    val darkMode by preferencesRepository.darkMode.collectAsState(initial = null)
    val dynamicColor by preferencesRepository.dynamicColor.collectAsState(initial = true)

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                categories = uiState.categories,
                herbs = uiState.herbs,
                isLoading = uiState.isLoading,
                error = uiState.error,
                isAdmin = appViewModel.isAdmin,
                onRetry = appViewModel::refresh,
                onCategoryClick = { category ->
                    navController.navigate(Screen.CategoryHerbs.createRoute(category.id, category.name))
                },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onFavoritesClick = { navController.navigate(Screen.Favorites.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onAdminClick = { navController.navigate(Screen.Admin.route) }
            )
        }

        composable(
            Screen.CategoryHerbs.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val herbsInCategory = uiState.herbs.filter { it.categoryId == categoryId }
            CategoryHerbsScreen(
                categoryName = categoryName,
                herbs = herbsInCategory,
                favoriteIds = favoriteIds,
                onBack = { navController.popBackStack() },
                onHerbClick = { herb -> navController.navigate(Screen.HerbDetail.createRoute(herb.id)) },
                onToggleFavorite = appViewModel::toggleFavorite
            )
        }

        composable(
            Screen.HerbDetail.route,
            arguments = listOf(navArgument("herbId") { type = NavType.StringType })
        ) { backStackEntry ->
            val herbId = backStackEntry.arguments?.getString("herbId")
            val herb = uiState.herbs.firstOrNull { it.id == herbId }
            if (herb != null) {
                HerbDetailScreen(
                    herb = herb,
                    isFavorite = herb.id in favoriteIds,
                    onBack = { navController.popBackStack() },
                    onToggleFavorite = { appViewModel.toggleFavorite(herb.id) }
                )
            }
        }

        composable(Screen.Search.route) {
            SearchScreen(
                herbs = uiState.herbs,
                favoriteIds = favoriteIds,
                onBack = { navController.popBackStack() },
                onHerbClick = { herb -> navController.navigate(Screen.HerbDetail.createRoute(herb.id)) },
                onToggleFavorite = appViewModel::toggleFavorite
            )
        }

        composable(Screen.Favorites.route) {
            val favoriteHerbs = uiState.herbs.filter { it.id in favoriteIds }
            FavoritesScreen(
                favoriteHerbs = favoriteHerbs,
                onBack = { navController.popBackStack() },
                onHerbClick = { herb -> navController.navigate(Screen.HerbDetail.createRoute(herb.id)) },
                onToggleFavorite = appViewModel::toggleFavorite
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLogin = appViewModel::login,
                onRegister = appViewModel::register,
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.Admin.route) {
            AdminListScreen(
                herbs = uiState.herbs,
                onBack = { navController.popBackStack() },
                onAddNew = { navController.navigate(Screen.AdminEdit.createRoute(Screen.AdminEdit.NEW)) },
                onEdit = { herb -> navController.navigate(Screen.AdminEdit.createRoute(herb.id)) },
                onDelete = { herb -> appViewModel.deleteHerb(herb.id) { _, _ -> } }
            )
        }

        composable(
            Screen.AdminEdit.route,
            arguments = listOf(navArgument("herbId") { type = NavType.StringType })
        ) { backStackEntry ->
            val herbId = backStackEntry.arguments?.getString("herbId")
            val existingHerb = uiState.herbs.firstOrNull { it.id == herbId }
            AdminEditHerbScreen(
                existingHerb = existingHerb,
                categories = uiState.categories,
                onBack = { navController.popBackStack() },
                onSave = { herb, callback ->
                    if (existingHerb == null) appViewModel.addHerb(herb, callback)
                    else appViewModel.updateHerb(herb, callback)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                isLoggedIn = appViewModel.isLoggedIn,
                darkMode = darkMode,
                dynamicColor = dynamicColor,
                onBack = { navController.popBackStack() },
                onDarkModeChange = { value ->
                    scope.launch { preferencesRepository.setDarkMode(value) }
                },
                onDynamicColorChange = { value ->
                    scope.launch { preferencesRepository.setDynamicColor(value) }
                },
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onLogoutClick = { appViewModel.logout() }
            )
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
