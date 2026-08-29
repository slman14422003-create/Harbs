package com.salman.herbalencyclopedia.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    herbs: List<Herb>,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onHerbClick: (Herb) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val results = remember(query, herbs) {
        if (query.isBlank()) emptyList()
        else herbs.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.benefits.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                title = {
                    // حقل نص شفاف بلا حدود بدل OutlinedTextField الافتراضي:
                    // قبل هذا التعديل كان الحقل يرسم صندوقاً بحدّه الخاص فوق
                    // شريط الزجاج السائل (زجاج داخل زجاج)، فيبدو غريباً وغير
                    // منسجم مع بقية التطبيق. هذا الشكل يذوب داخل الشريط
                    // العلوي كأنه جزء منه، بأيقونة بحث بادئة توضّح وظيفته
                    // فوراً.
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("ابحث عن عشبة أو فائدة...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                GlassIconButton(onClick = { query = "" }, size = 34.dp) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        when {
            query.isBlank() -> EmptyView(
                message = "اكتب اسم عشبة أو فائدة للبحث",
                modifier = Modifier.padding(padding)
            )
            results.isEmpty() -> EmptyView(
                message = "لا توجد نتائج لـ \"$query\"",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.id }) { herb ->
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
