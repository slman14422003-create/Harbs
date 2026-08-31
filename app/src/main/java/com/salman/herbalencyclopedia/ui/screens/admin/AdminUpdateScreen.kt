package com.salman.herbalencyclopedia.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.AppUpdateConfig
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import kotlinx.coroutines.launch

/**
 * Lets the admin control the in-app update feature without shipping a new
 * build: which GitHub repo to watch for Releases, an optional forced
 * download link / version / release notes, and an optional "mandatory
 * update below this versionCode" threshold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUpdateScreen(
    config: AppUpdateConfig,
    onBack: () -> Unit,
    onSave: (AppUpdateConfig, (Boolean, String?) -> Unit) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var enabled by remember(config) { mutableStateOf(config.enabled) }
    var repo by remember(config) { mutableStateOf(config.githubRepo) }
    var versionName by remember(config) { mutableStateOf(config.overrideVersionName ?: "") }
    var notes by remember(config) { mutableStateOf(config.releaseNotesOverride ?: "") }
    var minVersionCode by remember(config) {
        mutableStateOf(if (config.minVersionCode > 0) config.minVersionCode.toString() else "")
    }
    var useProxyFallback by remember(config) { mutableStateOf(config.useProxyFallback) }
    var customProxyBaseUrl by remember(config) { mutableStateOf(config.customProxyBaseUrl ?: "") }
    var saving by remember { mutableStateOf(false) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text("إعدادات التحديثات") },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "يتحقق التطبيق تلقائياً من آخر إصدار (Release) على GitHub، ثم يفتح رابط تحميل ملف APK مباشرة عند توفر تحديث ليقوم المستخدم بتنزيله وتثبيته يدوياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("تفعيل التحقق من التحديثات", fontWeight = FontWeight.SemiBold)
                            Text(
                                "عند الإيقاف لن يظهر أي زر تحديث للمستخدمين",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("مستودع GitHub") },
                    placeholder = { Text("owner/repo") },
                    supportingText = { Text("يُقرأ منه أحدث Release تلقائياً، مثال: slman14422003-create/Harbs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("رقم إصدار مخصّص (اختياري)") },
                    supportingText = { Text("إن تُرك فارغاً يُؤخذ رقم الإصدار من tag الإصدار على GitHub") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التحديث (اختياري)") },
                    supportingText = { Text("إن تُركت فارغة يظهر للمستخدم نص ثابت: \"تم تحديث الأخطاء وإدخال تحسينات جديدة.\"") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("استخدام بروكسي عند حجب GitHub", fontWeight = FontWeight.SemiBold)
                            Text(
                                "إذا فشل الوصول المباشر لـ GitHub (كما في بعض الدول)، يعيد التطبيق المحاولة عبر مرآة بروكسي تلقائياً، دون الحاجة لـ VPN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = useProxyFallback, onCheckedChange = { useProxyFallback = it })
                    }
                }
            }
            if (useProxyFallback) {
                item {
                    OutlinedTextField(
                        value = customProxyBaseUrl,
                        onValueChange = { customProxyBaseUrl = it },
                        label = { Text("رابط بروكسي مخصّص (اختياري)") },
                        placeholder = { Text("https://my-proxy.example.com/") },
                        supportingText = { Text("إن تُرك فارغاً تُستخدم مرايا عامة معروفة تلقائياً كخطة بديلة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = minVersionCode,
                    onValueChange = { v -> minVersionCode = v.filter { it.isDigit() } },
                    label = { Text("حد التحديث الإجباري (اختياري)") },
                    supportingText = { Text("أي نسخة مثبّتة أقدم من رقم الإصدار الداخلي هذا يُعتبر تحديثها إجبارياً") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    enabled = !saving && repo.isNotBlank(),
                    onClick = {
                        saving = true
                        val newConfig = AppUpdateConfig(
                            enabled = enabled,
                            githubRepo = repo.trim(),
                            overrideVersionName = versionName.trim().ifBlank { null },
                            releaseNotesOverride = notes.trim().ifBlank { null },
                            minVersionCode = minVersionCode.toIntOrNull() ?: 0,
                            useProxyFallback = useProxyFallback,
                            customProxyBaseUrl = customProxyBaseUrl.trim().ifBlank { null }
                        )
                        onSave(newConfig) { ok, msg ->
                            saving = false
                            notify(msg ?: if (ok) "تم الحفظ" else "حدث خطأ أثناء الحفظ")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (saving) "جارٍ الحفظ..." else "حفظ الإعدادات")
                }
            }
        }
    }
}
