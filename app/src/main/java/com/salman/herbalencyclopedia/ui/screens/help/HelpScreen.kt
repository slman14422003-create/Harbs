package com.salman.herbalencyclopedia.ui.screens.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val faqs = listOf(
        "كيف أبحث عن عشبة؟" to "استخدم البحث من الصفحة الرئيسية، ويمكن البحث بالاسم أو داخل الفوائد.",
        "كيف أضيف عشبة للمفضلة؟" to "افتح تفاصيل العشبة واضغط زر القلب. المفضلة محفوظة على جهازك.",
        "هل تعمل الموسوعة بدون إنترنت؟" to "البيانات التي تم تحميلها سابقاً يمكن أن تبقى متاحة عبر كاش Firestore المحلي، بينما التحديث يحتاج اتصالاً.",
        "كيف أدخل لوحة الإدارة؟" to "من الإعدادات اختر تسجيل الدخول، وبعد نجاح Firebase Auth وامتلاك UID المسؤول ستظهر أدوات الإدارة.",
        "هل المعلومات الطبية بديل عن الطبيب؟" to "لا. الموسوعة مرجع معلوماتي وليست بديلاً عن استشارة الطبيب أو الصيدلي."
    )
    Scaffold(topBar = { GlassTopBar(title = { Text("المساعدة") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) { Column(Modifier.padding(20.dp)) { Icon(Icons.Filled.HelpOutline, null); Spacer(Modifier.height(8.dp)); Text("مرحباً بك في موسوعة الأعشاب الطبية", style = MaterialTheme.typography.headlineSmall); Text("واجهة حديثة، بحث سريع، مفضلة، مقارنة، وإدارة كاملة للمحتوى.") } } }
            item { Text("الأسئلة الشائعة", style = MaterialTheme.typography.titleLarge) }
            items(faqs) { (q, a) ->
                var open by remember { mutableStateOf(false) }
                Card(onClick = { open = !open }, shape = MaterialTheme.shapes.large) { Column(Modifier.padding(16.dp)) { Row { Icon(Icons.Filled.Search, null); Spacer(Modifier.width(8.dp)); Text(q, style = MaterialTheme.typography.titleMedium) }; if (open) { Spacer(Modifier.height(8.dp)); Text(a) } } }
            }
        }
    }
}
