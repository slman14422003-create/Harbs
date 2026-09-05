package com.salman.herbalencyclopedia.ui.screens.allherbs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import com.salman.herbalencyclopedia.ui.components.TopBarBrandTitle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.search.HerbSearch
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHerbsScreen(
    herbs: List<Herb>,
    favoriteIds: Set<String>,
    onHerbClick: (Herb) -> Unit,
    onToggleFavorite: (String) -> Unit,
    // "سحب للتحديث" (نفس مبدأ فيسبوك/إنستغرام): يفرض جولة حقيقية إلى خادم
    // Firestore (تجاوزاً للكاش المحلي) لتأكيد أن ما يُعرض هو أحدث بيانات
    // فعلاً، بدل انتظار المزامنة الحيّة التلقائية بصمت. القيمتان اختياريتان
    // (بقيمة افتراضية بلا تأثير) كي تبقى أي استدعاءات سابقة للشاشة صحيحة
    // دون تعديل، لكن HerbalNavGraph يمرّرهما فعلياً من AppViewModel (انظر
    // uiState.isLoading و[AppViewModel.refresh] هناك).
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    // نفس إصلاح شاشة البحث المستقلة (SearchScreen): بحث مطبَّع وموسَّع
    // بمرادفات محلية بدل `contains` حرفي فقط — انظر توثيق [HerbSearch].
    val filtered = remember(query, herbs) { if (query.isBlank()) herbs else HerbSearch.search(query, herbs) }
    // كانت هذه القائمة شبكة (LazyVerticalGrid) متعددة الأعمدة: بطاقة العشبة
    // (HerbCard) مصمَّمة أصلاً كصفّ بعرض كامل (صورة + عنوان + وصف بسطرين +
    // زر مفضّلة) وليست بطاقة مربّعة، فضغطها إلى عمود بعرض النصف كان يقصّ
    // نصّها ويُظهر البطاقات جنباً إلى جنب بدل قائمة مقروءة. LazyColumn بعمود
    // واحد يعرض كل عشبة بعرض كامل تحت التي قبلها، بنفس طراز شاشتي المفضّلة
    // والبحث.
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
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
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { herb -> HerbCard(herb, herb.id in favoriteIds, { onHerbClick(herb) }, { onToggleFavorite(herb.id) }) }
                    }
                }
            }
        }
    }
}
