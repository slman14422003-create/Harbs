package com.salman.herbalencyclopedia.ui.screens.herbdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.HerbThumbnail

private data class InfoSection(
    val title: String,
    val content: String,
    val icon: ImageVector,
    val tint: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerbDetailScreen(
    herb: Herb,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sections = listOf(
        InfoSection("الفوائد", herb.benefits, Icons.Filled.Favorite, Color(0xFF2E7D32)),
        InfoSection("طريقة الاستخدام", herb.usage, Icons.Filled.LocalPharmacy, Color(0xFF1565C0)),
        InfoSection("التحذيرات", herb.warnings, Icons.Filled.WarningAmber, Color(0xFFEF6C00)),
        InfoSection("الأضرار المحتملة", herb.harms, Icons.Filled.ReportProblem, Color(0xFFC62828)),
        InfoSection("ملاحظات إضافية", herb.notes, Icons.Filled.StickyNote2, Color(0xFF6A1B9A))
    ).filter { it.content.isNotBlank() && it.content != "—" }

    Scaffold(
        topBar = {
            GlassTopBar(
                title = { Text(herb.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HerbThumbnail(imageUrl = herb.imageUrl, size = 72.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = herb.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(sections) { section ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(section.icon, contentDescription = null, tint = section.tint)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = section.content, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
