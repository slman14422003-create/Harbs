package com.salman.herbalencyclopedia.ui.screens.allherbs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHerbsScreen(herbs: List<Herb>, favoriteIds: Set<String>, onHerbClick: (Herb) -> Unit, onToggleFavorite: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = herbs.filter { query.isBlank() || it.name.contains(query, true) || it.benefits.contains(query, true) }
    // This screen is a bottom-nav root destination (see HerbalNavGraph), so it
    // intentionally has no back arrow — matches HomeScreen's top bar.
    Scaffold(topBar = { GlassTopBar(large = true, title = { Text("كل الأعشاب") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("ابحث في الموسوعة") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (filtered.isEmpty()) {
                EmptyView(message = "لا توجد نتائج لـ \"$query\"", modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { herb -> HerbCard(herb, herb.id in favoriteIds, { onHerbClick(herb) }, { onToggleFavorite(herb.id) }) }
                }
            }
        }
    }
}
