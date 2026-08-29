package com.salman.herbalencyclopedia.ui.screens.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminToolsScreen(
    categories: List<Category>,
    herbs: List<Herb>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteAllHerbs: ((Boolean, String?) -> Unit) -> Unit,
    onDeleteAllData: ((Boolean, String?) -> Unit) -> Unit,
    onTestConnection: ((Boolean, String?) -> Unit) -> Unit,
    onClearFavorites: () -> Unit,
    onRestoreBackup: (String, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(ok: Boolean, message: String?) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message ?: if (ok) "تمت العملية بنجاح" else "حدث خطأ، حاول مرة أخرى"
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val json = uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }.getOrNull() }
        if (json != null) {
            onRestoreBackup(json) { ok, msg -> notify(ok, msg ?: if (ok) "تمت استعادة النسخة الاحتياطية" else null) }
        } else {
            notify(false, "تعذّرت قراءة الملف المحدد")
        }
    }
    var categoryName by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { GlassTopBar(title = { Text("أدوات الإدارة") }, navigationIcon = { GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("الصيانة والمزامنة", style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.Sync, "تحديث البيانات", "جلب أحدث نسخة من Firestore", { onRefresh(); notify(true, "جاري تحديث البيانات") }) }
            item { AdminButton(Icons.Filled.NetworkCheck, "اختبار الاتصال", "التحقق من الوصول إلى البيانات", { onTestConnection { ok, msg -> notify(ok, msg) } }) }
            item { AdminButton(Icons.Filled.Backup, "نسخة احتياطية", "مشاركة JSON تشمل الأعشاب والتصنيفات", { shareText(context, "نسخة موسوعة الأعشاب", backupJson(categories, herbs)) }) }
            item { AdminButton(Icons.Filled.Restore, "استعادة نسخة", "استيراد JSON إلى Firestore", { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }) }
            item { AdminButton(Icons.Filled.TableChart, "تصدير CSV", "تصدير جميع الأعشاب كملف نصي CSV", { shareText(context, "herbs.csv", csvText(herbs)) }) }
            item { AdminButton(Icons.Filled.Share, "مشاركة التطبيق", "فتح مشاركة النظام", { shareApp(context) }) }
            item { AdminButton(Icons.Filled.Link, "نسخ رابط التطبيق", "نسخ رابط المشروع إلى الحافظة", { context.getSystemService(Context.CLIPBOARD_SERVICE).let { (it as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("app", "https://github.com/")); }; notify(true, "تم نسخ الرابط") }) }
            item { Text("التصنيفات", style = MaterialTheme.typography.titleLarge) }
            item {
                OutlinedTextField(
                    categoryName, { categoryName = it }, label = { Text("اسم تصنيف جديد") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(enabled = categoryName.isNotBlank(), onClick = {
                            val name = categoryName.trim()
                            onAddCategory(name) { ok, msg -> notify(ok, msg ?: if (ok) "تمت إضافة التصنيف" else null) }
                            categoryName = ""
                        }) { Text("إضافة") }
                    }
                )
            }
            items(categories, key = { it.id }) { c -> ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text("${herbs.count { it.categoryId == c.id }} عشبة") }, trailingContent = { GlassIconButton(onClick = { confirmAction = "category:${c.id}" }) { Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.error) } }) }
            item { Text("إجراءات خطرة", style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.DeleteSweep, "مسح جميع الأعشاب", "حذف كل الأعشاب من Firestore", { confirmAction = "herbs" }, danger = true) }
            item { AdminButton(Icons.Filled.DeleteForever, "حذف كل البيانات", "حذف الأعشاب والتصنيفات", { confirmAction = "all" }, danger = true) }
            item { AdminButton(Icons.Filled.CleaningServices, "تنظيف المفضلة", "حذف المفضلة المحلية", { onClearFavorites(); notify(true, "تم تنظيف المفضلة") }) }
        }
    }
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("تأكيد العملية") },
            text = { Text("هذا الإجراء لا يمكن التراجع عنه. هل تريد المتابعة؟") },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        action == "herbs" -> onDeleteAllHerbs { ok, msg -> notify(ok, msg ?: if (ok) "تم حذف جميع الأعشاب" else null) }
                        action == "all" -> onDeleteAllData { ok, msg -> notify(ok, msg ?: if (ok) "تم حذف جميع البيانات" else null) }
                        action.startsWith("category:") -> onDeleteCategory(action.substringAfter(':')) { ok, msg -> notify(ok, msg ?: if (ok) "تم حذف التصنيف" else null) }
                    }
                    confirmAction = null
                }) { Text("متابعة", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("إلغاء") } }
        )
    }
}

@Composable private fun AdminButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, danger: Boolean = false) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            leadingContent = {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
            },
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) }
        )
    }
}
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
