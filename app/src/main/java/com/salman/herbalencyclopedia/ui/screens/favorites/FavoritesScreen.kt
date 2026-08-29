package com.salman.herbalencyclopedia.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteHerbs: List<Herb>,
    onHerbClick: (Herb) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    // Bottom-nav root destination — no back arrow, same as HomeScreen.
    Scaffold(
        topBar = { GlassTopBar(large = true, title = { Text("المفضلة") }) }
    ) { padding ->
        if (favoriteHerbs.isEmpty()) {
            EmptyView(message = "لم تُضِف أي عشبة إلى المفضلة بعد", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favoriteHerbs, key = { it.id }) { herb ->
                    HerbCard(
                        herb = herb,
                        isFavorite = true,
                        onClick = { onHerbClick(herb) },
                        onToggleFavorite = { onToggleFavorite(herb.id) }
                    )
                }
            }
        }
    }
}
