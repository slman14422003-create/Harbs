package com.salman.herbalencyclopedia.ui.screens.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryHerbsScreen(
    categoryName: String,
    herbs: List<Herb>,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onHerbClick: (Herb) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Scaffold(
        topBar = {
            GlassTopBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        if (herbs.isEmpty()) {
            EmptyView(message = "لا توجد أعشاب في هذا التصنيف بعد", modifier = Modifier.padding(padding))
        } else {
            // كان عموداً واحداً ثابتاً؛ Adaptive يوسّع تلقائياً على تابلت
            // بدل بطاقة ممدودة بعرض الشاشة كاملاً (نفس إصلاح باقي شاشات
            // قوائم الأعشاب).
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(herbs, key = { it.id }) { herb ->
                    HerbCard(
                        herb = herb,
                        isFavorite = herb.id in favoriteIds,
                        onClick = { onHerbClick(herb) },
                        onToggleFavorite = { onToggleFavorite(herb.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
