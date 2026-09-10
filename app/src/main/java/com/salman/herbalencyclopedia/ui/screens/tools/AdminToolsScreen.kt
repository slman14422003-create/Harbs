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
import com.salman.herbalencyclopedia.data.ai.DictionaryLexicon
import com.salman.herbalencyclopedia.data.ai.HerbAssistant
import com.salman.herbalencyclopedia.data.ai.TrainedExample
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.salman.herbalencyclopedia.ui.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminToolsScreen(
    categories: List<Category>,
    herbs: List<Herb>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateCategory: (String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
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
    onSetAiTrainedThreshold: (Float) -> Unit = {},
    // ── تعلّم سيمو الذاتي: حالات جمعها التطبيق تلقائياً من تقييمات
    // المستخدمين (👍/👎) في شاشة الدردشة — قابلة للمراجعة والحذف أو
    // "الترقية" لتدريب يدوي دائم من هنا مباشرة. ──
    aiAutoLearnedExamples: List<TrainedExample> = emptyList(),
    aiAutoLearnEnabled: Boolean = AiConfig.defaultAutoLearnEnabled,
    onSetAiAutoLearnedExamples: (List<TrainedExample>) -> Unit = {},
    onSetAiAutoLearnEnabled: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── نصوص مُترجَمة مُحسَّبة سلفاً داخل نطاق composable: تُستخدم لاحقاً من
    // دوال/lambdas عادية غير composable (notify، restoreLauncher، أزرار
    // المشاركة) حيث لا يمكن استدعاء tr() مباشرة لأنه composable. هذا هو
    // السبب الجذري لبقاء "أدوات الإدارة" بالعربية دوماً بغض النظر عن لغة
    // التطبيق المختارة — كانت كل هذه النصوص تُمرَّر حرفياً بلا تمريرها على tr(). ──
    val msgOpSuccess = tr("تمت العملية بنجاح")
    val msgOpError = tr("حدث خطأ، حاول مرة أخرى")
    val msgBackupRestored = tr("تمت استعادة النسخة الاحتياطية")
    val msgFileReadFailed = tr("تعذّرت قراءة الملف المحدد")
    val msgCategoryAdded = tr("تمت إضافة التصنيف")
    val msgLinkCopied = tr("تم نسخ الرابط")
    val msgFavoritesCleared = tr("تم تنظيف المفضلة")
    val msgAiReset = tr("تمت إعادة ضبط إعدادات المساعد الذكي")
    val msgAllHerbsDeleted = tr("تم حذف جميع الأعشاب")
    val msgAllDataDeleted = tr("تم حذف جميع البيانات")
    val msgCategoryDeleted = tr("تم حذف التصنيف")
    val msgCategoryUpdated = tr("تم تعديل التصنيف")
    val msgDataRefreshing = tr("جاري تحديث البيانات")
    val strBackupTitle = tr("نسخة موسوعة الأعشاب")
    val strAppShareText = tr("موسوعة الأعشاب الطبية")
    val strShareChooserTitle = tr("مشاركة")

    fun notify(ok: Boolean, message: String?) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message ?: if (ok) msgOpSuccess else msgOpError
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val json = uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }.getOrNull() }
        if (json != null) {
            onRestoreBackup(json) { ok, msg -> notify(ok, msg ?: if (ok) msgBackupRestored else null) }
        } else {
            notify(false, msgFileReadFailed)
        }
    }
    var categoryName by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    // ── تعديل (إعادة تسمية) تصنيف: لم تكن هذه الميزة موجودة إطلاقاً في
    // الواجهة رغم توفّرها في طبقة البيانات — كان المسؤول قادراً فقط على
    // إضافة تصنيف جديد أو حذف تصنيف قائم، لا تصحيح اسم موجود. ──
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingCategoryName by remember { mutableStateOf("") }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { GlassTopBar(title = { Text(tr("أدوات الإدارة")) }, navigationIcon = { GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع")) } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(tr("الصيانة والمزامنة"), style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.Sync, "تحديث البيانات", "جلب أحدث نسخة من Firestore", { onRefresh(); notify(true, msgDataRefreshing) }) }
            item { AdminButton(Icons.Filled.NetworkCheck, "اختبار الاتصال", "التحقق من الوصول إلى البيانات", { onTestConnection { ok, msg -> notify(ok, msg) } }) }
            item { AdminButton(Icons.Filled.Backup, "نسخة احتياطية", "مشاركة JSON تشمل الأعشاب والتصنيفات", { shareFile(context, "harbs-backup.json", "application/json", backupJson(categories, herbs), strBackupTitle) }) }
            item { AdminButton(Icons.Filled.Restore, "استعادة نسخة", "استيراد JSON إلى Firestore", { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }) }
            item { AdminButton(Icons.Filled.TableChart, "تصدير CSV", "تصدير جميع الأعشاب كملف نصي CSV", { shareFile(context, "herbs.csv", "text/csv", csvText(herbs), "herbs.csv") }) }
            item { AdminButton(Icons.Filled.Share, "مشاركة التطبيق", "فتح مشاركة النظام", { shareApp(context, strAppShareText, strShareChooserTitle) }) }
            item { AdminButton(Icons.Filled.Link, "نسخ رابط التطبيق", "نسخ رابط المشروع إلى الحافظة", { context.getSystemService(Context.CLIPBOARD_SERVICE).let { (it as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("app", "https://github.com/")); }; notify(true, msgLinkCopied) }) }
            item { AdminButton(Icons.Filled.SystemUpdate, "إعدادات التحديثات", "تعديل مستودع ورابط وملاحظات التحديث", onUpdateSettingsClick) }
            item { Text(tr("التصنيفات"), style = MaterialTheme.typography.titleLarge) }
            item {
                OutlinedTextField(
                    categoryName, { categoryName = it }, label = { Text(tr("اسم تصنيف جديد")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(enabled = categoryName.isNotBlank(), onClick = {
                            val name = categoryName.trim()
                            onAddCategory(name) { ok, msg -> notify(ok, msg ?: if (ok) msgCategoryAdded else null) }
                            categoryName = ""
                        }) { Text(tr("إضافة")) }
                    }
                )
            }
            items(categories, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = { Text(tr("${herbs.count { it.categoryId == c.id }} عشبة")) },
                    trailingContent = {
                        Row {
                            GlassIconButton(onClick = { editingCategory = c; editingCategoryName = c.name }) {
                                Icon(Icons.Filled.Edit, tr("تعديل"))
                            }
                            GlassIconButton(onClick = { confirmAction = "category:${c.id}" }) {
                                Icon(Icons.Filled.Delete, tr("حذف"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
            item { Text(tr("إجراءات خطرة"), style = MaterialTheme.typography.titleLarge) }
            item { AdminButton(Icons.Filled.DeleteSweep, "مسح جميع الأعشاب", "حذف كل الأعشاب من Firestore", { confirmAction = "herbs" }, danger = true) }
            item { AdminButton(Icons.Filled.DeleteForever, "حذف كل البيانات", "حذف الأعشاب والتصنيفات", { confirmAction = "all" }, danger = true) }
            item { AdminButton(Icons.Filled.CleaningServices, "تنظيف المفضلة", "حذف المفضلة المحلية", { onClearFavorites(); notify(true, msgFavoritesCleared) }) }
            item {
                AiAssistantDevTools(
                    herbs = herbs,
                    similarityThreshold = aiSimilarityThreshold,
                    searchThreshold = aiSearchThreshold,
                    extraStopWords = aiExtraStopWords,
                    onSimilarityChange = onSetAiSimilarityThreshold,
                    onSearchThresholdChange = onSetAiSearchThreshold,
                    onExtraStopWordsChange = onSetAiExtraStopWords,
                    onReset = { onResetAiSettings(); notify(true, msgAiReset) }
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
            item {
                AiSelfLearningDevTools(
                    autoLearnedExamples = aiAutoLearnedExamples,
                    autoLearnEnabled = aiAutoLearnEnabled,
                    trainedExamples = aiTrainedExamples,
                    onAutoLearnedExamplesChange = onSetAiAutoLearnedExamples,
                    onAutoLearnEnabledChange = onSetAiAutoLearnEnabled,
                    onPromoteToTrained = { example ->
                        onSetAiTrainedExamples(aiTrainedExamples + example)
                        onSetAiAutoLearnedExamples(aiAutoLearnedExamples - example)
                    }
                )
            }
        }
    }
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(tr("تأكيد العملية")) },
            text = { Text(tr("هذا الإجراء لا يمكن التراجع عنه. هل تريد المتابعة؟")) },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        action == "herbs" -> onDeleteAllHerbs { ok, msg -> notify(ok, msg ?: if (ok) msgAllHerbsDeleted else null) }
                        action == "all" -> onDeleteAllData { ok, msg -> notify(ok, msg ?: if (ok) msgAllDataDeleted else null) }
                        action.startsWith("category:") -> onDeleteCategory(action.substringAfter(':')) { ok, msg -> notify(ok, msg ?: if (ok) msgCategoryDeleted else null) }
                    }
                    confirmAction = null
                }) { Text(tr("متابعة"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text(tr("إلغاء")) } }
        )
    }
    editingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text(tr("تعديل التصنيف")) },
            text = {
                OutlinedTextField(
                    value = editingCategoryName,
                    onValueChange = { editingCategoryName = it },
                    label = { Text(tr("اسم التصنيف")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editingCategoryName.isNotBlank(),
                    onClick = {
                        val newName = editingCategoryName.trim()
                        onUpdateCategory(category.id, newName) { ok, msg -> notify(ok, msg ?: if (ok) msgCategoryUpdated else null) }
                        editingCategory = null
                    }
                ) { Text(tr("حفظ")) }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text(tr("إلغاء")) } }
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
            headlineContent = { Text(tr(title)) },
            supportingContent = { Text(tr(subtitle)) }
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

    // حالة قاموس المرادفات المحلي (Rabih Dictionary + Arabic WordNet، انظر
    // DictionaryLexicon) — يُحمَّل مرة واحدة في الخلفية عند إقلاع التطبيق،
    // فيُحتمل جداً أنه جاهز بالفعل عند فتح هذه الشاشة. هذا الاستقصاء
    // (كل 300ms حتى انتهاء المحاولة) مجرّد مؤشر حيّ للمطوّر أثناء الاختبار.
    // يتوقف فور *انتهاء* المحاولة (loadAttempted) سواء نجحت أم فشلت — قبلاً
    // كان يتوقف فقط عند النجاح، فكان الفشل يبدو مطابقاً بصرياً لـ"لا يزال
    // يُحمَّل" بلا أي طريقة للتمييز بينهما أو معرفة سبب الفشل الفعلي.
    var lexiconReady by remember { mutableStateOf(DictionaryLexicon.isReady) }
    var lexiconAttempted by remember { mutableStateOf(DictionaryLexicon.loadAttempted) }
    LaunchedEffect(Unit) {
        while (!lexiconAttempted) {
            kotlinx.coroutines.delay(300)
            lexiconReady = DictionaryLexicon.isReady
            lexiconAttempted = DictionaryLexicon.loadAttempted
        }
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(tr("سيمو المساعد (تدريب/ضبط)"), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text(
                tr("المساعد يعمل محلياً بالكامل من بيانات الموسوعة نفسها، بلا اتصال إنترنت وبلا أي حظر أو قيد على الإجابات. عدّل العتبات هنا لتحسين دقّة \"تدريبه\" فوراً."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // مؤشر يؤكّد أن قاموس المرادفات المحلي (الذي يوسّع فهم البحث الحر
            // وشاشتَي البحث المباشر بمرادفات عامة) حُمِّل فعلاً — بثلاث حالات
            // مميَّزة بصرياً الآن بدل حالتين: "لا يزال يعمل" تختلف عن "انتهى
            // بفشل" (وتعرض سبب الفشل الفعلي من Logcat/lastError)، فلا يبقى
            // فشل حقيقي متنكّراً في هيئة تحميل عالق للأبد.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when {
                        lexiconReady -> Icons.Filled.CheckCircle
                        lexiconAttempted -> Icons.Filled.ErrorOutline
                        else -> Icons.Filled.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = when {
                        lexiconReady -> MaterialTheme.colorScheme.primary
                        lexiconAttempted -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        lexiconReady ->
                            tr("قاموس المرادفات المحلي جاهز (${DictionaryLexicon.loadedWordCount} كلمة، Rabih Dictionary + Arabic WordNet)")
                        lexiconAttempted ->
                            tr("فشل تحميل قاموس المرادفات المحلي: ") + (DictionaryLexicon.lastError?.let { tr(it) } ?: tr("خطأ غير معروف"))
                        else ->
                            tr("جارٍ تحميل قاموس المرادفات المحلي…")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lexiconAttempted && !lexiconReady) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("حساسية تجميع النقاط المتشابهة: ${(similarityThreshold * 100).roundToIntPct()}%"), style = MaterialTheme.typography.labelLarge)
                Slider(value = similarityThreshold, onValueChange = onSimilarityChange, valueRange = 0.05f..0.95f)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("حساسية البحث الحر عن إجابة: ${(searchThreshold * 100).roundToIntPct()}%"), style = MaterialTheme.typography.labelLarge)
                Slider(value = searchThreshold, onValueChange = onSearchThresholdChange, valueRange = 0.02f..0.9f)
            }

            OutlinedTextField(
                value = stopWordsText,
                onValueChange = { stopWordsText = it },
                label = { Text(tr("كلمات إيقاف إضافية (مفصولة بفاصلة)")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    onExtraStopWordsChange(stopWordsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet())
                }) { Text(tr("حفظ الكلمات")) }
                TextButton(onClick = onReset) { Text(tr("إعادة الضبط الافتراضي")) }
            }

            HorizontalDivider()

            Text(tr("اختبار حيّ"), style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
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
                label = { Text(tr("جرّب سؤالاً")) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(enabled = testQuestion.isNotBlank() && testHerbIds.isNotEmpty(), onClick = {
                        val selectedHerbs = herbs.filter { it.id in testHerbIds }
                        testAnswer = HerbAssistant.answer(testQuestion, selectedHerbs)
                    }) { Text(tr("اسأل")) }
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
                Text(tr("تدريب سيمو المخصّص"), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text(
                tr("علّم سيمو كلمات ومرادفات جديدة، أو درّبه على حالات وأسئلة بعينها بردٍ تكتبه أنت بنفسك — يُستخدم فوراً في كل محادثة قادمة."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── عتبة مطابقة الحالات المدرَّبة ──
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("حساسية مطابقة الحالات المدرَّبة: ${(trainedThreshold * 100).roundToIntPct()}%"), style = MaterialTheme.typography.labelLarge)
                Slider(value = trainedThreshold, onValueChange = onTrainedThresholdChange, valueRange = 0.1f..0.95f)
                Text(
                    tr("كلما قلّت النسبة، كفى تشابه أبسط بين سؤال المستخدم والمثال المدرَّب ليُستخدم رده مباشرة."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // ── مرادفات ──
            Text(tr("مرادفات (كلمات جديدة يفهمها سيمو)"), style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            if (synonyms.isEmpty()) {
                Text(tr("لا توجد مرادفات مضافة بعد."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    synonyms.forEach { (word, meaning) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$word  ⇦  $meaning", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            GlassIconButton(onClick = { onSynonymsChange(synonyms - word) }, size = 32.dp) {
                                Icon(Icons.Filled.Delete, tr("حذف"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newSynonymWord,
                    onValueChange = { newSynonymWord = it },
                    label = { Text(tr("كلمة جديدة")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newSynonymMeaning,
                    onValueChange = { newSynonymMeaning = it },
                    label = { Text(tr("تُفهم كـ")) },
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
            ) { Text(tr("إضافة مرادف")) }

            HorizontalDivider()

            // ── حالات مدرَّبة ──
            Text(tr("حالات مدرَّبة (سؤال ← رد مخصّص)"), style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            if (trainedExamples.isEmpty()) {
                Text(tr("لا توجد حالات مدرَّبة بعد."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    trainedExamples.forEach { example ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(tr("س: ${example.pattern}"), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                    Text(tr("ج: ${example.response}"), style = MaterialTheme.typography.bodySmall)
                                }
                                GlassIconButton(onClick = { onTrainedExamplesChange(trainedExamples - example) }, size = 32.dp) {
                                    Icon(Icons.Filled.Delete, tr("حذف"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                label = { Text(tr("سؤال نموذجي")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newResponse,
                onValueChange = { newResponse = it },
                label = { Text(tr("الرد المطلوب")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            TextButton(
                enabled = newPattern.isNotBlank() && newResponse.isNotBlank(),
                onClick = {
                    onTrainedExamplesChange(trainedExamples + TrainedExample(newPattern.trim(), newResponse.trim()))
                    newPattern = ""; newResponse = ""
                }
            ) { Text(tr("إضافة حالة")) }
        }
    }
}

/**
 * أدوات مطور لمراجعة "تعلّم سيمو الذاتي": كل حالة هنا وُلدت تلقائياً من
 * إجابة بحث حر أعطاها سيمو فعلاً من بيانات الموسوعة، وقيّمها مستخدم حقيقي
 * بـ 👍 في شاشة الدردشة — أي أن المصدر بالكامل هو الموسوعة + استخدام
 * فعلي، وليس تخميناً. يمكن للمطوّر من هنا: تعطيل التعلّم الذاتي كلياً،
 * حذف حالة بعينها (لو كانت غير دقيقة)، "ترقيتها" لتصبح حالة تدريب يدوية
 * دائمة (تنتقل للقائمة المحمية في [AiTrainingDevTools])، أو مسح كل ما
 * تعلّمه سيمو والبدء من جديد.
 */
@Composable
private fun AiSelfLearningDevTools(
    autoLearnedExamples: List<TrainedExample>,
    autoLearnEnabled: Boolean,
    trainedExamples: List<TrainedExample>,
    onAutoLearnedExamplesChange: (List<TrainedExample>) -> Unit,
    onAutoLearnEnabledChange: (Boolean) -> Unit,
    onPromoteToTrained: (TrainedExample) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(tr("تعلّم سيمو الذاتي"), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text(
                tr("كل حالة هنا وُلدت تلقائياً من إجابة سيمو الفعلية على بيانات الموسوعة بعد أن قيّمها مستخدم بـ 👍 في الدردشة — يعتمد سيمو على الموسوعة أولاً، ثم يراكم فوقها خبرة حقيقية من استخدامه."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr("تفعيل التعلّم الذاتي"), style = MaterialTheme.typography.labelLarge)
                    Text(
                        tr("عند التعطيل، يتوقف سيمو عن حفظ أي حالات جديدة ولا يستخدم القديمة منها في الردود."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoLearnEnabled, onCheckedChange = onAutoLearnEnabledChange)
            }

            HorizontalDivider()

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr("الحالات المتعلَّمة (${autoLearnedExamples.size})"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                if (autoLearnedExamples.isNotEmpty()) {
                    TextButton(onClick = { onAutoLearnedExamplesChange(emptyList()) }) { Text(tr("مسح الكل")) }
                }
            }

            if (autoLearnedExamples.isEmpty()) {
                Text(
                    tr("لم يتعلّم سيمو أي حالة بعد. ستظهر هنا تلقائياً أول مرة يُقيّم فيها مستخدم إجابة بحث حر بـ 👍."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    autoLearnedExamples.forEach { example ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(tr("س: ${example.pattern}"), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                Text(tr("ج: ${example.response}"), style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                                    TextButton(
                                        enabled = example !in trainedExamples,
                                        onClick = { onPromoteToTrained(example) }
                                    ) { Text(tr("ترقية لتدريب دائم")) }
                                    TextButton(onClick = { onAutoLearnedExamplesChange(autoLearnedExamples - example) }) {
                                        Text(tr("حذف"), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
    // BOM (U+FEFF) في البداية ضروري لفتح Excel/Sheets الملف بترميز UTF-8
    // الصحيح تلقائياً؛ بدونه يعرض Excel كل النصوص العربية كرموز مشوَّهة
    // (؟؟؟ أو حروف عشوائية) رغم أن الملف نفسه سليم تماماً بترميز UTF-8 —
    // هذا سبب شكوى "تصدير CSV فيه مشكلة" الأكثر شيوعاً مع بيانات عربية.
    return "\uFEFF" + buildString { appendLine("name,category_id,benefits,warnings,harms,usage,notes,image_url"); herbs.forEach { appendLine(listOf(it.name,it.categoryId ?: "",it.benefits,it.warnings,it.harms,it.usage,it.notes,it.imageUrl ?: "").joinToString(",", transform=::e)) } }
}
private fun shareApp(context: Context, shareText: String, chooserTitle: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }, chooserTitle)) }

/**
 * يكتب [text] إلى ملف حقيقي في cacheDir/exports (انظر res/xml/file_paths.xml)
 * ثم يشاركه عبر FileProvider كـ content:// Uri بدل مشاركته كنص خام مباشر.
 *
 * كانت النسخة الاحتياطية وCSV تُشارَكان سابقاً عبر Intent.EXTRA_TEXT (نص
 * خام) بدل ملف فعلي، وهذا يسبب مشكلتين حقيقيتين:
 * 1) قيد حجم صارم على بيانات الـIntent بين التطبيقات (Binder transaction،
 *    ~1 ميجابايت لكل التطبيق)؛ مع نمو عدد الأعشاب يفشل التصدير بصمت أو
 *    يتحطم التطبيق (TransactionTooLargeException) دون أي ملف يصل فعلياً.
 * 2) التطبيقات المستقبِلة (واتساب، الرسائل، إلخ) تتعامل مع النص كرسالة لا
 *    كملف .json/.csv، فيضيع الامتداد والترميز الصحيحين، ولا يمكن اختيار
 *    "استعادة نسخة" لاحقاً على نفس المحتوى مباشرة لأنه لم يعد ملفاً أصلاً.
 * الكتابة كملف حقيقي ومشاركته بامتداده ونوعه (MIME) الصحيحين تحل المشكلتين
 * معاً، وتسمح أيضاً بحفظه مباشرة عبر "حفظ في الجهاز" من قائمة المشاركة.
 */
private fun shareFile(context: Context, fileName: String, mimeType: String, text: String, chooserTitle: String) {
    val exportsDir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    val file = java.io.File(exportsDir, fileName)
    file.writeText(text, Charsets.UTF_8)
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
