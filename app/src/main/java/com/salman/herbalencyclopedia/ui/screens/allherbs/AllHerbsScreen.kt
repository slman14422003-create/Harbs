package com.salman.herbalencyclopedia.ui.screens.allherbs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import com.salman.herbalencyclopedia.ui.components.TopBarBrandTitle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.search.HerbSearch
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard
import com.salman.herbalencyclopedia.ui.util.rememberWindowSizeInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHerbsScreen(herbs: List<Herb>, favoriteIds: Set<String>, onHerbClick: (Herb) -> Unit, onToggleFavorite: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    // نفس إصلاح شاشة البحث المستقلة (SearchScreen): بحث مطبَّع وموسَّع
    // بمرادفات محلية بدل `contains` حرفي فقط — انظر توثيق [HerbSearch].
    val filtered = remember(query, herbs) { if (query.isBlank()) herbs else HerbSearch.search(query, herbs) }
    // كانت هذه القائمة LazyColumn بعمود واحد ثابت: على تابلت أو نافذة
    // عريضة تتمدد بطاقة العشبة بعرض الشاشة كاملاً (قد يتجاوز 800dp) بدل
    // الاستفادة من العرض. LazyVerticalGrid مع Adaptive تُبقي عموداً واحداً
    // بجوال عادي (لأن عرض البطاقة الطبيعي ~340dp أعرض من عرض الجوال) وتزيد
    // الأعمدة تلقائياً كلما اتسعت الشاشة فعلياً.
    val windowInfo = rememberWindowSizeInfo()
    // This screen is a bottom-nav root destination (see HerbalNavGraph), so it
    // intentionally has no back arrow — matches HomeScreen's top bar. Title
    // now uses the same icon-badge + subtitle style as Home/Favorites instead
    // of bare text, so the bar doesn't look empty.
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // انظر التعليق بنفس المكان في HomeScreen: هذه شاشة جذر أيضاً، وترك
        // الحافة السفلية الافتراضية هنا يُضاعف الفراغ فوق الشريط العائم.
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            GlassTopBar(
                large = true,
                title = {
                    TopBarBrandTitle(
                        icon = Icons.Filled.MenuBook,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "كل الأعشاب",
                        subtitle = "${herbs.size} عشبة في الموسوعة"
                    )
                }
            )
        }
    ) { padding ->
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { herb -> HerbCard(herb, herb.id in favoriteIds, { onHerbClick(herb) }, { onToggleFavorite(herb.id) }) }
                }
            }
        }
    }
}
