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
import com.salman.herbalencyclopedia.data.ai.AiConfig
import com.salman.herbalencyclopedia.data.ai.HerbAssistant
import com.salman.herbalencyclopedia.data.ai.TrainedExample
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.util.ResponsiveScreenContent
import com.salman.herbalencyclopedia.ui.util.rememberWindowSizeInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    onRestoreBackup: (String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateSettingsClick: () -> Unit,
    // ── إعدادات "سيمو المساعد" (HerbAssistant) — تُمرَّر حيّة من
    // DataStore عبر HerbalNavGraph، وتُعدَّل هنا مباشرة كـ "أدوات مطور" ──
    aiSimilarityThreshold: Float = AiConfig.defaultSimilarityThreshold.toFloat(),
    aiSearchThreshold: Float = AiConfig.defaultSearchThreshold.toFloat(),
    aiExtraStopWords: Set<String> = emptySet(),
    onSetAiSimilarityThreshold: (Float) -> Unit = {},
    onSetAiSearchThreshold: (Float) -> Unit = {},
    onSetAiExtraStopWords: (Set<String>) -> Unit = {},
    onResetAiSettings: () -> Unit = {},
    // ── تدريب سيمو المخصّص: مرادفات وحالات (سؤال ← رد) يعلّمها المطوّر ──
    aiSynonyms: Map<String, String> = emptyMap(),
    aiTrainedExamples: List<TrainedExample> = emptyList(),
    aiTrainedThreshold: Float = AiConfig.defaultTrainedThreshold.toFloat(),
    onSetAiSynonyms: (Map<String, String>) -> Unit = {},
    onSetAiTrainedExamples: (List<TrainedExample>) -> Unit = {},
    onSetAiTrainedThreshold: (Float) -> Unit = {}
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
        ResponsiveScreenContent(windowInfo = rememberWindowSizeInfo(), modifier = Modifier.padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("الصيانة والمزامنة", style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.Sync, "تحديث البيانات", "جلب أحدث نسخة من Firestore", { onRefresh(); notify(true, "جاري تحديث البيانات") }) }
            item { AdminButton(Icons.Filled.NetworkCheck, "اختبار الاتصال", "التحقق من الوصول إلى البيانات", { onTestConnection { ok, msg -> notify(ok, msg) } }) }
            item { AdminButton(Icons.Filled.Backup, "نسخة احتياطية", "مشاركة JSON تشمل الأعشاب والتصنيفات", { shareText(context, "نسخة موسوعة الأعشاب", backupJson(categories, herbs)) }) }
            item { AdminButton(Icons.Filled.Restore, "استعادة نسخة", "استيراد JSON إلى Firestore", { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }) }
            item { AdminButton(Icons.Filled.TableChart, "تصدير CSV", "تصدير جميع الأعشاب كملف نصي CSV", { shareText(context, "herbs.csv", csvText(herbs)) }) }
            item { AdminButton(Icons.Filled.Share, "مشاركة التطبيق", "فتح مشاركة النظام", { shareApp(context) }) }
            item { AdminButton(Icons.Filled.Link, "نسخ رابط التطبيق", "نسخ رابط المشروع إلى الحافظة", { context.getSystemService(Context.CLIPBOARD_SERVICE).let { (it as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("app", "https://github.com/")); }; notify(true, "تم نسخ الرابط") }) }
            item { AdminButton(Icons.Filled.SystemUpdate, "إعدادات التحديثات", "تعديل مستودع ورابط وملاحظات التحديث", onUpdateSettingsClick) }
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
            item {
                AiAssistantDevTools(
                    herbs = herbs,
                    similarityThreshold = aiSimilarityThreshold,
                    searchThreshold = aiSearchThreshold,
                    extraStopWords = aiExtraStopWords,
                    onSimilarityChange = onSetAiSimilarityThreshold,
                    onSearchThresholdChange = onSetAiSearchThreshold,
                    onExtraStopWordsChange = onSetAiExtraStopWords,
                    onReset = { onResetAiSettings(); notify(true, "تمت إعادة ضبط إعدادات المساعد الذكي") }
                )
            }
            item {
                AiTrainingDevTools(
                    synonyms = aiSynonyms,
                    trainedExamples = aiTrainedExamples,
                    trainedThreshold = aiTrainedThreshold,
                    onSynonymsChange = onSetAiSynonyms,
                    onTrainedExamplesChange = onSetAiTrainedExamples,
                    onTrainedThresholdChange = onSetAiTrainedThreshold
                )
            }
        }
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
/**
 * أدوات مطور لضبط "تدريب" سيمو المساعد (HerbAssistant): عتبتا
 * التشابه المستخدمتان في تجميع النقاط والبحث الحر، وكلمات إيقاف إضافية
 * لتحسين تحليل النصوص العربية الخاصة بالموسوعة، مع مساحة اختبار حيّة
 * تُظهر إجابة المساعد فوراً على أي سؤال باستخدام أعشاب حقيقية من القاعدة —
 * كل تغيير هنا يُحفظ ويُطبَّق مباشرة بلا أي حظر أو قيد إضافي على الإجابات.
 */
@Composable
private fun AiAssistantDevTools(
    herbs: List<Herb>,
    similarityThreshold: Float,
    searchThreshold: Float,
    extraStopWords: Set<String>,
    onSimilarityChange: (Float) -> Unit,
    onSearchThresholdChange: (Float) -> Unit,
    onExtraStopWordsChange: (Set<String>) -> Unit,
    onReset: () -> Unit
) {
    var stopWordsText by remember(extraStopWords) { mutableStateOf(extraStopWords.joinToString(", ")) }
    var testQuestion by remember { mutableStateOf("") }
    var testHerbIds by remember { mutableStateOf(setOf<String>()) }
    var testAnswer by remember { mutableStateOf<String?>(null) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("سيمو المساعد (تدريب/ضبط)", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text(
                "المساعد يعمل محلياً بالكامل من بيانات الموسوعة نفسها، بلا اتصال إنترنت وبلا أي حظر أو قيد على الإجابات. عدّل العتبات هنا لتحسين دقّة \"تدريبه\" فوراً.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("حساسية تجميع النقاط المتشابهة: ${(similarityThreshold * 100).roundToIntPct()}%", style = MaterialTheme.typography.labelLarge)
                Slider(value = similarityThreshold, onValueChange = onSimilarityChange, valueRange = 0.05f..0.95f)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("حساسية البحث الحر عن إجابة: ${(searchThreshold * 100).roundToIntPct()}%", style = MaterialTheme.typography.labelLarge)
                Slider(value = searchThreshold, onValueChange = onSearchThresholdChange, valueRange = 0.02f..0.9f)
            }

            OutlinedTextField(
                value = stopWordsText,
                onValueChange = { stopWordsText = it },
                label = { Text("كلمات إيقاف إضافية (مفصولة بفاصلة)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    onExtraStopWordsChange(stopWordsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet())
                }) { Text("حفظ الكلمات") }
                TextButton(onClick = onReset) { Text("إعادة الضبط الافتراضي") }
            }

            HorizontalDivider()

            Text("اختبار حيّ", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            LazyColumn(Modifier.heightIn(max = 130.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(herbs, key = { it.id }) { herb ->
                    FilterChip(
                        selected = herb.id in testHerbIds,
                        onClick = { testHerbIds = if (herb.id in testHerbIds) testHerbIds - herb.id else testHerbIds + herb.id },
                        label = { Text(herb.name) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            OutlinedTextField(
                value = testQuestion,
                onValueChange = { testQuestion = it },
                label = { Text("جرّب سؤالاً") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(enabled = testQuestion.isNotBlank() && testHerbIds.isNotEmpty(), onClick = {
                        val selectedHerbs = herbs.filter { it.id in testHerbIds }
                        testAnswer = HerbAssistant.answer(testQuestion, selectedHerbs)
                    }) { Text("اسأل") }
                }
            )
            if (testAnswer != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(testAnswer.orEmpty(), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun Float.roundToIntPct(): Int = this.roundToInt()

/**
 * أدوات مطور لـ"تطوير الفهم" الفعلي لسيمو بما يتجاوز عتبات المطابقة: يضيف
 * المطوّر هنا مرادفات (كلمات جديدة يفهمها سيمو كأنها كلمة أخرى معروفة له)
 * وحالات مدرَّبة كاملة (سؤال نموذجي + الرد المطلوب بالضبط)، فيتعلّم سيمو
 * التعامل مع صياغات أو حالات لم يغطها المنطق العام جيداً — كل ذلك يُحفظ
 * ويُطبَّق فوراً بلا إعادة بناء التطبيق.
 */
@Composable
private fun AiTrainingDevTools(
    synonyms: Map<String, String>,
    trainedExamples: List<TrainedExample>,
    trainedThreshold: Float,
    onSynonymsChange: (Map<String, String>) -> Unit,
    onTrainedExamplesChange: (List<TrainedExample>) -> Unit,
    onTrainedThresholdChange: (Float) -> Unit
) {
    var newSynonymWord by remember { mutableStateOf("") }
    var newSynonymMeaning by remember { mutableStateOf("") }
    var newPattern by remember { mutableStateOf("") }
    var newResponse by remember { mutableStateOf("") }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.School, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("تدريب سيمو المخصّص", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text(
                "علّم سيمو كلمات ومرادفات جديدة، أو درّبه على حالات وأسئلة بعينها بردٍ تكتبه أنت بنفسك — يُستخدم فوراً في كل محادثة قادمة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── عتبة مطابقة الحالات المدرَّبة ──
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("حساسية مطابقة الحالات المدرَّبة: ${(trainedThreshold * 100).roundToIntPct()}%", style = MaterialTheme.typography.labelLarge)
                Slider(value = trainedThreshold, onValueChange = onTrainedThresholdChange, valueRange = 0.1f..0.95f)
                Text(
                    "كلما قلّت النسبة، كفى تشابه أبسط بين سؤال المستخدم والمثال المدرَّب ليُستخدم رده مباشرة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // ── مرادفات ──
            Text("مرادفات (كلمات جديدة يفهمها سيمو)", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            if (synonyms.isEmpty()) {
                Text("لا توجد مرادفات مضافة بعد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    synonyms.forEach { (word, meaning) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$word  ⇦  $meaning", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            GlassIconButton(onClick = { onSynonymsChange(synonyms - word) }, size = 32.dp) {
                                Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newSynonymWord,
                    onValueChange = { newSynonymWord = it },
                    label = { Text("كلمة جديدة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newSynonymMeaning,
                    onValueChange = { newSynonymMeaning = it },
                    label = { Text("تُفهم كـ") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(
                enabled = newSynonymWord.isNotBlank() && newSynonymMeaning.isNotBlank(),
                onClick = {
                    onSynonymsChange(synonyms + (newSynonymWord.trim() to newSynonymMeaning.trim()))
                    newSynonymWord = ""; newSynonymMeaning = ""
                }
            ) { Text("إضافة مرادف") }

            HorizontalDivider()

            // ── حالات مدرَّبة ──
            Text("حالات مدرَّبة (سؤال ← رد مخصّص)", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            if (trainedExamples.isEmpty()) {
                Text("لا توجد حالات مدرَّبة بعد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    trainedExamples.forEach { example ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text("س: ${example.pattern}", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                    Text("ج: ${example.response}", style = MaterialTheme.typography.bodySmall)
                                }
                                GlassIconButton(onClick = { onTrainedExamplesChange(trainedExamples - example) }, size = 32.dp) {
                                    Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                label = { Text("سؤال نموذجي") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newResponse,
                onValueChange = { newResponse = it },
                label = { Text("الرد المطلوب") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            TextButton(
                enabled = newPattern.isNotBlank() && newResponse.isNotBlank(),
                onClick = {
                    onTrainedExamplesChange(trainedExamples + TrainedExample(newPattern.trim(), newResponse.trim()))
                    newPattern = ""; newResponse = ""
                }
            ) { Text("إضافة حالة") }
        }
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
