package com.salman.herbalencyclopedia.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Feedback
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import com.salman.herbalencyclopedia.ui.components.LoadingView
import com.salman.herbalencyclopedia.ui.components.TopBarBrandTitle
import com.salman.herbalencyclopedia.ui.util.ResponsiveScreenContent
import com.salman.herbalencyclopedia.ui.util.rememberWindowSizeInfo
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * صندوق ملاحظات المستخدمين — يظهر فقط للأدمن (مربوط في الإعدادات، ولا
 * يُقرأ من Firestore لغير حساب الأدمن أصلاً عبر firestore.rules). يعرض
 * كل ملاحظة بشكل منسّق: بخصوص أي عشبة/خلطة، من أرسلها (أو "مجهول")،
 * ومتى، مع إمكانية الحذف بعد معالجتها.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbackScreen(
    feedback: List<Feedback>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onDelete: (Feedback) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<Feedback?>(null) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy، HH:mm", Locale("ar")) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                large = true,
                title = {
                    TopBarBrandTitle(
                        icon = Icons.Filled.Inbox,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "ملاحظات المستخدمين",
                        subtitle = "${feedback.size} ملاحظة"
                    )
                },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading && feedback.isEmpty() -> LoadingView(Modifier.padding(padding).fillMaxSize())
            error != null && feedback.isEmpty() -> EmptyView(error, Modifier.padding(padding).fillMaxSize())
            feedback.isEmpty() -> EmptyView("لا توجد ملاحظات حالياً", Modifier.padding(padding).fillMaxSize())
            else -> ResponsiveScreenContent(windowInfo = rememberWindowSizeInfo(), modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(feedback, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "بخصوص: ${item.targetName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val typeLabel = if (item.targetType == "blend") "خلطة" else "عشبة"
                                    Text(
                                        typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                GlassIconButton(onClick = { pendingDelete = item }, size = 36.dp) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(item.message, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (item.senderName.isNullOrBlank()) Icons.Filled.PersonOff else Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    item.senderName?.ifBlank { null } ?: "مرسل مجهول",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                item.createdAt?.toDate()?.let { date ->
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        dateFormat.format(date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف هذه الملاحظة؟") },
            text = { Text("لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    pendingDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") }
            }
        )
    }
}
