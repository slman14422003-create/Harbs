package com.salman.herbalencyclopedia.ui.screens.compare

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassOutlinedButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import kotlinx.coroutines.launch

private const val MAX_COMPARE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(herbs: List<Herb>, onBack: () -> Unit) {
    var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val selected = selectedIds.mapNotNull { id -> herbs.firstOrNull { it.id == id } }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassTopBar(title = { Text("مقارنة الأعشاب") }, navigationIcon = {
                GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            }, actions = {
                if (selected.isNotEmpty()) TextButton(onClick = { selectedIds = emptyList() }) { Text("مسح") }
            })
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("اختر عشبتين أو ثلاثاً", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(herbs) { herb ->
                    val isSelected = herb.id in selectedIds
                    FilterChip(selected = isSelected, onClick = {
                        when {
                            isSelected -> selectedIds = selectedIds - herb.id
                            selectedIds.size < MAX_COMPARE -> selectedIds = selectedIds + herb.id
                            else -> scope.launch {
                                snackbarHostState.showSnackbar("يمكن مقارنة $MAX_COMPARE أعشاب كحد أقصى، أزل واحدة أولاً")
                            }
                        }
                    }, label = { Text(herb.name) })
                }
            }
            if (selected.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Text("لم تتم إضافة أي عشبة للمقارنة بعد.", Modifier.padding(20.dp))
                }
            } else {
                selected.forEach { herb ->
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(herb.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                GlassIconButton(onClick = { selectedIds = selectedIds - herb.id }) { Icon(Icons.Filled.Close, "إزالة") }
                            }
                            CompareRow("الفوائد", herb.benefits)
                            CompareRow("التحذيرات", herb.warnings)
                            CompareRow("الأضرار", herb.harms)
                            CompareRow("الاستخدام", herb.usage)
                            CompareRow("ملاحظات", herb.notes)
                        }
                    }
                }
                GlassOutlinedButton(
                    onClick = {
                        val shareText = buildCompareShareText(selected)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "مقارنة الأعشاب")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة المقارنة"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.IosShare, null); Spacer(Modifier.width(8.dp)); Text("مشاركة / طباعة المقارنة")
                }
            }
        }
    }
}

private fun buildCompareShareText(herbs: List<Herb>): String = buildString {
    appendLine("مقارنة الأعشاب")
    appendLine()
    herbs.forEach { herb ->
        appendLine("• ${herb.name}")
        if (herb.benefits.isNotBlank()) appendLine("  الفوائد: ${herb.benefits}")
        if (herb.warnings.isNotBlank()) appendLine("  التحذيرات: ${herb.warnings}")
        if (herb.harms.isNotBlank()) appendLine("  الأضرار: ${herb.harms}")
        if (herb.usage.isNotBlank()) appendLine("  الاستخدام: ${herb.usage}")
        if (herb.notes.isNotBlank()) appendLine("  ملاحظات: ${herb.notes}")
        appendLine()
    }
}

@Composable private fun CompareRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}
