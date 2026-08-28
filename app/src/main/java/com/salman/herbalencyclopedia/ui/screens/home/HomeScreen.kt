package com.salman.herbalencyclopedia.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Balance
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
    onAdminClick: () -> Unit,
    onCompareClick: () -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Column { Text("موسوعة الأعشاب الطبية", style = MaterialTheme.typography.headlineMedium); Text("اكتشف الأعشاب وفوائدها", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
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
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onSearchClick, label = { Text("بحث") }, leadingIcon = { Icon(Icons.Filled.Search, null) })
                AssistChip(onClick = onFavoritesClick, label = { Text("مفضلة") }, leadingIcon = { Icon(Icons.Filled.Favorite, null) })
                AssistChip(onClick = onCompareClick, label = { Text("مقارنة") }, leadingIcon = { Icon(Icons.Filled.Balance, null) })
            }
            when {
            isLoading -> LoadingView()
            error != null -> ErrorView(message = error, onRetry = onRetry)
            categories.isEmpty() -> EmptyView(message = "لا توجد تصنيفات بعد")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
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
}
