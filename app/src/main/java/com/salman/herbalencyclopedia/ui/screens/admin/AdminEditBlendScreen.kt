package com.salman.herbalencyclopedia.ui.screens.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.salman.herbalencyclopedia.data.image.ImageCompressor
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.GlassButton
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassOutlinedButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import kotlinx.coroutines.launch

/**
 * نموذج إضافة/تعديل خلطة — للأدمن فقط. يشبه [AdminEditHerbScreen] تماماً
 * لكن بدل تصنيف واحد، يختار الأدمن مجموعة أعشاب موجودة (من نفس مجموعة
 * "herbs" في Firestore) لتكوّن مكوّنات الخلطة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditBlendScreen(
    existingBlend: Blend?,
    herbs: List<Herb>,
    onBack: () -> Unit,
    onSave: (Blend, (Boolean, String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(existingBlend?.name ?: "") }
    var selectedHerbIds by remember { mutableStateOf(existingBlend?.herbIds?.toSet() ?: emptySet()) }
    var benefits by remember { mutableStateOf(existingBlend?.benefits ?: "") }
    var usage by remember { mutableStateOf(existingBlend?.usage ?: "") }
    var warnings by remember { mutableStateOf(existingBlend?.warnings ?: "") }
    var notes by remember { mutableStateOf(existingBlend?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(existingBlend?.imageUrl ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCompressingImage by remember { mutableStateOf(false) }
    var herbPickerExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            errorMessage = null
            isCompressingImage = true
            coroutineScope.launch {
                val compressed = ImageCompressor.compressToDataUrl(context, it)
                isCompressingImage = false
                if (compressed != null) {
                    imageUrl = compressed
                } else {
                    errorMessage = "تعذّر معالجة هذه الصورة (قد تكون كبيرة جداً)، جرّب صورة أخرى"
                }
            }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text(if (existingBlend == null) "إضافة خلطة" else "تعديل خلطة") },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("اسم الخلطة") }, modifier = Modifier.fillMaxWidth()
            )

            Text("مكوّنات الخلطة", style = MaterialTheme.typography.titleMedium)
            Surface(
                onClick = { herbPickerExpanded = !herbPickerExpanded },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedHerbIds.isEmpty()) "اختر الأعشاب (${herbs.size} متاحة)"
                        else herbs.filter { it.id in selectedHerbIds }.joinToString(", ") { it.name },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (herbPickerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (herbPickerExpanded) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        if (herbs.isEmpty()) {
                            Text(
                                "لا توجد أعشاب في الموسوعة بعد",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        herbs.forEach { herb ->
                            val checked = herb.id in selectedHerbIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedHerbIds = if (checked) selectedHerbIds - herb.id else selectedHerbIds + herb.id
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedHerbIds = if (isChecked) selectedHerbIds + herb.id else selectedHerbIds - herb.id
                                    }
                                )
                                Text(herb.name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = benefits, onValueChange = { benefits = it },
                label = { Text("الفوائد") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = usage, onValueChange = { usage = it },
                label = { Text("طريقة التحضير والاستخدام") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = warnings, onValueChange = { warnings = it },
                label = { Text("التحذيرات") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )

            Text("صورة الخلطة", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(onClick = { imagePicker.launch("image/*") }, enabled = !isCompressingImage) { Text("اختيار صورة") }
                if (imageUrl.isNotBlank() && !isCompressingImage) GlassOutlinedButton(onClick = { imageUrl = "" }) { Text("مسح") }
            }
            if (isCompressingImage) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("جاري ضغط الصورة...", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (imageUrl.isNotBlank()) AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp))

            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            GlassButton(
                onClick = {
                    isSaving = true
                    errorMessage = null
                    val blend = Blend(
                        id = existingBlend?.id ?: "",
                        name = name,
                        herbIds = selectedHerbIds.toList(),
                        benefits = benefits,
                        usage = usage,
                        warnings = warnings,
                        notes = notes,
                        imageUrl = imageUrl.ifBlank { null }
                    )
                    onSave(blend) { success, message ->
                        isSaving = false
                        if (success) onBack() else errorMessage = message
                    }
                },
                enabled = name.isNotBlank() && !isSaving && !isCompressingImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("حفظ")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
