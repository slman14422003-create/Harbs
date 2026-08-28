package com.salman.herbalencyclopedia.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListScreen(
    herbs: List<Herb>,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (Herb) -> Unit,
    onDelete: (Herb) -> Unit,
    onTools: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<Herb?>(null) }

    Scaffold(
        topBar = {
            GlassTopBar(
                title = { Text("لوحة تحكم الأدمن") },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtendedFloatingActionButton(onClick = onTools, icon = { Icon(Icons.Filled.Settings, null) }, text = { Text("أدوات") })
                ExtendedFloatingActionButton(onClick = onAddNew, icon = {
                Icon(Icons.Filled.Add, contentDescription = null)
            }, text = { Text("إضافة عشبة") })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(herbs, key = { it.id }) { herb ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = herb.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        GlassIconButton(onClick = { onEdit(herb) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                        }
                        GlassIconButton(onClick = { pendingDelete = herb }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { herb ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف \"${herb.name}\"؟") },
            text = { Text("لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(herb)
                    pendingDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") }
            }
        )
    }
}
