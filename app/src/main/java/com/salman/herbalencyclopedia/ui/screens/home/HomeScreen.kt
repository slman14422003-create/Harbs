package com.salman.herbalencyclopedia.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.CategoryCard
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.ErrorView
import com.salman.herbalencyclopedia.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<Category>,
    herbs: List<Herb>,
    isLoading: Boolean,
    error: String?,
    isAdmin: Boolean,
    onRetry: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("موسوعة الأعشاب الطبية") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "بحث")
                    }
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Filled.Favorite, contentDescription = "المفضلة")
                    }
                    if (isAdmin) {
                        IconButton(onClick = onAdminClick) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "لوحة التحكم")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingView(modifier = Modifier.padding(padding))
            error != null -> ErrorView(message = error, onRetry = onRetry, modifier = Modifier.padding(padding))
            categories.isEmpty() -> EmptyView(message = "لا توجد تصنيفات بعد", modifier = Modifier.padding(padding))
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    val count = herbs.count { it.categoryId == category.id }
                    CategoryCard(
                        category = category,
                        herbCount = count,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}
