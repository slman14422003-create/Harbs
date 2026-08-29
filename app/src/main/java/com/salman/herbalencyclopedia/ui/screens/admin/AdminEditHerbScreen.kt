package com.salman.herbalencyclopedia.ui.screens.admin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb

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
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val compressed = compressImageToDataUrl(context, it)
            if (compressed != null) {
                imageUrl = compressed
                errorMessage = null
            } else {
                errorMessage = "تعذّر معالجة هذه الصورة (قد تكون كبيرة جداً)، جرّب صورة أخرى"
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
                GlassButton(onClick = { imagePicker.launch("image/*") }) { Text("اختيار صورة") }
                if (imageUrl.isNotBlank()) GlassOutlinedButton(onClick = { imageUrl = "" }) { Text("مسح") }
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
                enabled = name.isNotBlank() && !isSaving,
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


private fun compressImageToDataUrl(context: android.content.Context, uri: android.net.Uri): String? {
    return runCatching {
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return null

        // Firestore يرفض أي مستند يتجاوز 1 ميجابايت إجمالاً (كل الحقول
        // مجتمعة)، وترميز base64 يكبّر حجم الصورة الأصلي بحوالي 33%.
        // لذلك نحجز هامشاً لبقية حقول العشبة ونضغط الصورة بشكل متكرر —
        // نقلّل الجودة أولاً، وإذا ما كفت نصغّر الأبعاد كمان — إلى أن
        // يصير الناتج تحت هذا السقف فعلياً بدل ما نكتشف الفشل بعد محاولة
        // الحفظ في Firestore.
        val maxDataUrlBytes = 650_000
        var maxDimension = 900
        var quality = 82
        var dataUrl: String

        while (true) {
            val scale = minOf(1f, maxDimension.toFloat() / maxOf(source.width, source.height))
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else source

            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

            if (dataUrl.length <= maxDataUrlBytes || (quality <= 35 && maxDimension <= 400)) break

            if (quality > 35) {
                quality -= 15
            } else {
                maxDimension = (maxDimension * 0.75f).toInt()
            }
        }

        // إذا ظلت الصورة أكبر من الحد المسموح حتى بعد أقصى ضغط ممكن، لا
        // نرجع نصاً سيفشل حفظه لاحقاً بصمت — نرجع null ليظهر خطأ واضح.
        if (dataUrl.length > maxDataUrlBytes) null else dataUrl
    }.getOrNull()
}
