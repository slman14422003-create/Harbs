package com.salman.herbalencyclopedia.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.*

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassTopBar(
                large = true,
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("موسوعة الأعشاب الطبية", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "معرفة موثوقة • تجربة هادئة • تصميم حديث",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (isAdmin) {
                        GlassIconButton(onClick = onAdminClick, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "لوحة التحكم")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Search,
                    label = "بحث",
                    onClick = onSearchClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Balance,
                    label = "مقارنة",
                    onClick = onCompareClick
                )
            }

            when {
                isLoading -> LoadingView(Modifier.fillMaxSize())
                error != null -> ErrorView(error, onRetry, Modifier.fillMaxSize())
                categories.isEmpty() -> EmptyView("لا توجد تصنيفات بعد", Modifier.fillMaxSize())
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            herbCount = herbs.count { it.categoryId == category.id },
                            onClick = { onCategoryClick(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
