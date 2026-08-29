package com.salman.herbalencyclopedia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.ui.theme.rememberPressScale

/**
 * قبل هذا التعديل كانت كل بطاقة تصنيف تعرض نفس الأيقونة (Eco) بنفس
 * التدرّج اللوني، فتبدو التصنيفات متطابقة بصرياً رغم اختلاف أسمائها،
 * وحقل category.icon القادم من Firestore لم يكن يُستخدم في أي مكان.
 * هذه الخريطة تربط كلمات دالة من اسم التصنيف (أو حقل icon إن توفّر)
 * بأيقونة ولون مميزين، مع لون احتياطي متنوع حسب ترتيب التصنيف حتى لا
 * تتكرر الألوان بلا داعٍ.
 */
private val categoryPalette = listOf(
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFAD1457),
    Color(0xFFEF6C00), Color(0xFF00695C), Color(0xFF6A1B9A),
    Color(0xFFC62828), Color(0xFF00838F)
)

private fun iconFor(category: Category): ImageVector {
    val key = (category.icon ?: category.name)
    val n = category.name
    return when {
        key.contains("digest", true) || n.contains("هضم") || n.contains("معدة") -> Icons.Filled.Restaurant
        key.contains("immun", true) || n.contains("مناع") -> Icons.Filled.Shield
        key.contains("skin", true) || n.contains("جلد") || n.contains("بشرة") -> Icons.Filled.Face
        key.contains("respirat", true) || n.contains("تنفس") || n.contains("صدر") -> Icons.Filled.Air
        key.contains("nerv", true) || n.contains("أعصاب") || n.contains("نوم") -> Icons.Filled.Psychology
        key.contains("heart", true) || n.contains("قلب") || n.contains("دور") -> Icons.Filled.Favorite
        key.contains("pain", true) || n.contains("ألم") || n.contains("التهاب") -> Icons.Filled.Healing
        key.contains("women", true) || n.contains("نسائية") || n.contains("حمل") -> Icons.Filled.Female
        key.contains("weight", true) || n.contains("وزن") || n.contains("تخسيس") -> Icons.Filled.MonitorWeight
        key.contains("energy", true) || n.contains("طاقة") || n.contains("نشاط") -> Icons.Filled.Bolt
        key.contains("kidney", true) || n.contains("كلى") || n.contains("بول") -> Icons.Filled.WaterDrop
        key.contains("bone", true) || n.contains("عظام") || n.contains("مفاصل") -> Icons.Filled.Accessibility
        key.contains("liver", true) || n.contains("كبد") || n.contains("سموم") -> Icons.Filled.FilterAlt
        else -> Icons.Filled.Eco
    }
}

private fun colorFor(category: Category): Color =
    categoryPalette[(category.id.hashCode().takeIf { category.id.isNotBlank() } ?: category.name.hashCode())
        .let { Math.floorMod(it, categoryPalette.size) }]

@Composable
fun CategoryCard(
    category: Category,
    herbCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    val accent = colorFor(category)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, pressedScale = 0.95f)

    // نفس أسلوب الزجاج السائل المستخدم في GlassButton وHerbCard: Surface
    // شفافة تحمل النقر/الظل، وLiquidGlassSurface يرسم طبقات الزجاج نفسها
    // (تمويه + توهّج + لمعان) وتحترم وضع الأداء تلقائياً.
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(pressScale)
            .aspectRatio(1f)
            .shadow(1.dp, shape),
        shape = shape,
        color = Color.Transparent
    ) {
        LiquidGlassSurface(
            shape = shape,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // أيقونة التصنيف محتوى فوق طبقة الزجاج (content)، فتبقى
                // واضحة وغير متأثرة بالتمويه خلفها.
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.10f))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconFor(category), contentDescription = null, tint = accent)
                }
                Column {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "$herbCount عشبة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
