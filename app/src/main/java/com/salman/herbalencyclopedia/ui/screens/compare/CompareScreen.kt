package com.salman.herbalencyclopedia.ui.screens.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(herbs: List<Herb>, onBack: () -> Unit) {
    var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val selected = selectedIds.mapNotNull { id -> herbs.firstOrNull { it.id == id } }
    Scaffold(topBar = {
        TopAppBar(title = { Text("مقارنة الأعشاب") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
        }, actions = {
            if (selected.isNotEmpty()) TextButton(onClick = { selectedIds = emptyList() }) { Text("مسح") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("اختر عشبتين أو ثلاثاً", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(herbs) { herb ->
                    FilterChip(selected = herb.id in selectedIds, onClick = {
                        selectedIds = if (herb.id in selectedIds) selectedIds - herb.id else if (selectedIds.size < 3) selectedIds + herb.id else selectedIds
                    }, label = { Text(herb.name) })
                }
            }
            if (selected.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Text("لم تتم إضافة أي عشبة للمقارنة بعد.", Modifier.padding(20.dp))
                }
            } else {
                selected.forEach { herb ->
                    Card(shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(herb.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { selectedIds = selectedIds - herb.id }) { Icon(Icons.Filled.Close, "إزالة") }
                            }
                            CompareRow("الفوائد", herb.benefits)
                            CompareRow("التحذيرات", herb.warnings)
                            CompareRow("الأضرار", herb.harms)
                            CompareRow("الاستخدام", herb.usage)
                            CompareRow("ملاحظات", herb.notes)
                        }
                    }
                }
                OutlinedButton(onClick = { /* Native print can be wired by PrintManager in host */ }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Print, null); Spacer(Modifier.width(8.dp)); Text("طباعة المقارنة")
                }
            }
        }
    }
}

@Composable private fun CompareRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}
