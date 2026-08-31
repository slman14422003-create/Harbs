package com.salman.herbalencyclopedia.ui.screens.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassButton
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassOutlinedButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.image.ImageCompressor
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditHerbScreen(
    existingHerb: Herb?,
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (Herb, (Boolean, String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(existingHerb?.name ?: "") }
    var categoryId by remember { mutableStateOf(existingHerb?.categoryId) }
    var benefits by remember { mutableStateOf(existingHerb?.benefits ?: "") }
    var usage by remember { mutableStateOf(existingHerb?.usage ?: "") }
    var warnings by remember { mutableStateOf(existingHerb?.warnings ?: "") }
    var harms by remember { mutableStateOf(existingHerb?.harms ?: "") }
    var notes by remember { mutableStateOf(existingHerb?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(existingHerb?.imageUrl ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var isCompressingImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            errorMessage = null
            isCompressingImage = true
            // الضغط ينفَّذ الآن على خيط خلفي (راجع ImageCompressor) بدل تجميد
            // الواجهة أثناء معالجة الصور الكبيرة.
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

    val selectedCategoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "بدون تصنيف"

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text(if (existingHerb == null) "إضافة عشبة" else "تعديل عشبة") },
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
                label = { Text("اسم العشبة") }, modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("التصنيف") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categoryId = category.id
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = benefits, onValueChange = { benefits = it },
                label = { Text("الفوائد") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = usage, onValueChange = { usage = it },
                label = { Text("طريقة الاستخدام") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = warnings, onValueChange = { warnings = it },
                label = { Text("التحذيرات") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = harms, onValueChange = { harms = it },
                label = { Text("الأضرار المحتملة") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            Text("صورة العشبة", style = MaterialTheme.typography.titleMedium)
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
                    val herb = Herb(
                        id = existingHerb?.id ?: "",
                        name = name,
                        categoryId = categoryId,
                        benefits = benefits,
                        warnings = warnings,
                        harms = harms,
                        usage = usage,
                        notes = notes,
                        imageUrl = imageUrl.ifBlank { null }
                    )
                    onSave(herb) { success, message ->
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
