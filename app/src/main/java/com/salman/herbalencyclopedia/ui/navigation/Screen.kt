package com.salman.herbalencyclopedia.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object CategoryHerbs : Screen("category/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String) =
            "category/$categoryId/${android.net.Uri.encode(categoryName)}"
    }
    data object HerbDetail : Screen("herb/{herbId}") {
        fun createRoute(herbId: String) = "herb/$herbId"
    }
    data object Search : Screen("search")
    data object Favorites : Screen("favorites")
    data object Login : Screen("login")
    data object Admin : Screen("admin")
    data object AdminEdit : Screen("admin/edit/{herbId}") {
        const val NEW = "new"
        fun createRoute(herbId: String) = "admin/edit/$herbId"
    }
    data object Settings : Screen("settings")
}
