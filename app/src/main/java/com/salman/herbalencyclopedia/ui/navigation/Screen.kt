package com.salman.herbalencyclopedia.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object AllHerbs : Screen("all_herbs")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
    data object Compare : Screen("compare")
    data object Help : Screen("help")
    data object CategoryHerbs : Screen("category/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String) = "category/$categoryId/${android.net.Uri.encode(categoryName)}"
    }
    data object HerbDetail : Screen("herb/{herbId}") { fun createRoute(herbId: String) = "herb/$herbId" }
    data object Login : Screen("login")
    data object Admin : Screen("admin")
    data object AdminEdit : Screen("admin/edit/{herbId}") { const val NEW = "new"; fun createRoute(herbId: String) = "admin/edit/$herbId" }
    data object AdminTools : Screen("admin/tools")
}
