package com.salman.herbalencyclopedia.ui.screens.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminToolsScreen(categories: List<Category>, herbs: List<Herb>, onBack: () -> Unit, onRefresh: () -> Unit, onAddCategory: (String) -> Unit, onDeleteCategory: (String) -> Unit, onDeleteAllHerbs: () -> Unit, onDeleteAllData: () -> Unit, onTestConnection: () -> Unit, onClearFavorites: () -> Unit, onRestoreBackup: (String) -> Unit) {
    val context = LocalContext.current
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }.getOrNull()?.let(onRestoreBackup) }
    }
    var categoryName by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { GlassTopBar(title = { Text("أدوات الإدارة") }, navigationIcon = { GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("الصيانة والمزامنة", style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.Sync, "تحديث البيانات", "جلب أحدث نسخة من Firestore", onRefresh) }
            item { AdminButton(Icons.Filled.NetworkCheck, "اختبار الاتصال", "التحقق من الوصول إلى البيانات", onTestConnection) }
            item { AdminButton(Icons.Filled.Backup, "نسخة احتياطية", "مشاركة JSON تشمل الأعشاب والتصنيفات", { shareText(context, "نسخة موسوعة الأعشاب", backupJson(categories, herbs)) }) }
            item { AdminButton(Icons.Filled.Restore, "استعادة نسخة", "استيراد JSON إلى Firestore", { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }) }
            item { AdminButton(Icons.Filled.TableChart, "تصدير CSV", "تصدير جميع الأعشاب كملف نصي CSV", { shareText(context, "herbs.csv", csvText(herbs)) }) }
            item { AdminButton(Icons.Filled.Share, "مشاركة التطبيق", "فتح مشاركة النظام", { shareApp(context) }) }
            item { AdminButton(Icons.Filled.Link, "نسخ رابط التطبيق", "نسخ رابط المشروع إلى الحافظة", { context.getSystemService(Context.CLIPBOARD_SERVICE).let { (it as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("app", "https://github.com/")); } }) }
            item { Text("التصنيفات", style = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(categoryName, { categoryName = it }, label = { Text("اسم تصنيف جديد") }, singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { TextButton(enabled = categoryName.isNotBlank(), onClick = { onAddCategory(categoryName.trim()); categoryName = "" }) { Text("إضافة") } }) }
            items(categories, key = { it.id }) { c -> ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text("${herbs.count { it.categoryId == c.id }} عشبة") }, trailingContent = { GlassIconButton(onClick = { confirmAction = "category:${c.id}" }) { Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.error) } }) }
            item { Text("إجراءات خطرة", style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.DeleteSweep, "مسح جميع الأعشاب", "حذف كل الأعشاب من Firestore", { confirmAction = "herbs" }, danger = true) }
            item { AdminButton(Icons.Filled.DeleteForever, "حذف كل البيانات", "حذف الأعشاب والتصنيفات", { confirmAction = "all" }, danger = true) }
            item { AdminButton(Icons.Filled.CleaningServices, "تنظيف المفضلة", "حذف المفضلة المحلية", onClearFavorites) }
        }
    }
    confirmAction?.let { action -> AlertDialog(onDismissRequest = { confirmAction = null }, title = { Text("تأكيد العملية") }, text = { Text("هذا الإجراء لا يمكن التراجع عنه. هل تريد المتابعة؟") }, confirmButton = { TextButton(onClick = { when { action == "herbs" -> onDeleteAllHerbs(); action == "all" -> onDeleteAllData(); action.startsWith("category:") -> onDeleteCategory(action.substringAfter(':')) }; confirmAction = null }) { Text("متابعة", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("إلغاء") } }) }
}

@Composable private fun AdminButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, danger: Boolean = false) { Card(onClick = onClick, shape = MaterialTheme.shapes.extraLarge) { ListItem(leadingContent = { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }, headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }) } }
private fun backupJson(categories: List<Category>, herbs: List<Herb>): String {
    val root = org.json.JSONObject()
    root.put("categories", org.json.JSONArray().apply { categories.forEach { put(org.json.JSONObject().apply { put("id",it.id); put("name",it.name); put("icon",it.icon ?: "") }) } })
    root.put("herbs", org.json.JSONArray().apply { herbs.forEach { put(org.json.JSONObject().apply { put("id",it.id); put("name",it.name); put("categoryId",it.categoryId ?: ""); put("benefits",it.benefits); put("warnings",it.warnings); put("harms",it.harms); put("usage",it.usage); put("notes",it.notes); put("imageUrl",it.imageUrl ?: "") }) } })
    return root.toString(2)
}
private fun csvText(herbs: List<Herb>): String {
    fun e(s:String) = "\"" + s.replace("\"", "\"\"") + "\""
    return buildString { appendLine("name,category_id,benefits,warnings,harms,usage,notes,image_url"); herbs.forEach { appendLine(listOf(it.name,it.categoryId ?: "",it.benefits,it.warnings,it.harms,it.usage,it.notes,it.imageUrl ?: "").joinToString(",", transform=::e)) } }
}
private fun shareText(context: Context, title: String, text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, title)) }
private fun shareApp(context: Context) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "موسوعة الأعشاب الطبية") }, "مشاركة")) }
