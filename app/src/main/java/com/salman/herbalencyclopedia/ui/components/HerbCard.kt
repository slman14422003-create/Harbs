package com.salman.herbalencyclopedia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.theme.rememberPressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerbCard(
    herb: Herb,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, pressedScale = 0.97f)
    val shape = RoundedCornerShape(20.dp)

    // بطاقة العشبة أصبحت الآن على طراز الزجاج السائل نفسه المستخدم في
    // GlassButton بدل Card عادية بخلفية Material3 مسطّحة، لتوحيد الهوية
    // البصرية مع بقية التطبيق. Surface هنا شفافة وتحمل فقط النقر/الظل،
    // بينما LiquidGlassSurface يرسم طبقات الزجاج نفسها وتحترم وضع الأداء
    // (عالي/اقتصادي) تلقائياً.
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(pressScale)
            .fillMaxWidth()
            .shadow(1.dp, shape),
        shape = shape,
        color = Color.Transparent
    ) {
        LiquidGlassSurface(
            shape = shape,
            modifier = Modifier.fillMaxWidth(),
            tint = MaterialTheme.colorScheme.surfaceContainer,
            // بطاقة صف قصيرة (~80dp)، وليست بطاقة شبكة مربّعة كبيرة — انظر
            // توثيق [compact] في LiquidGlassSurface لسبب هذا التبديل (كان
            // سبب ظهور "مستطيل أبيض" غير متسق فوق بطاقات الأعشاب).
            compact = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // صورة العشبة محتوى فوق طبقة الزجاج (content) وليست جزءاً
                // من الطبقة المموَّهة، فتبقى واضحة تماماً وغير متأثرة بالتمويه.
                HerbThumbnail(imageUrl = herb.imageUrl)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = herb.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = herb.benefits,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                GlassIconButton(onClick = onToggleFavorite, size = 38.dp) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * @param previewOnClick إذا كانت true وتوجد صورة فعلية، يفتح الضغط على
 * الصورة معاينة بحجم كامل الشاشة (انظر [ImagePreviewDialog]) بدل تجاهل
 * الضغطة أو تفعيل نقرة الصف كاملاً (مفيد تحديداً في شاشة تفاصيل العشبة،
 * حيث الصورة ليست جزءاً من صف قابل للنقر أصلاً). في بطاقة القائمة
 * (HerbCard) تبقى false افتراضياً لأن نقرة الصف بالكامل تفتح التفاصيل.
 */
@Composable
fun HerbThumbnail(
    imageUrl: String?,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    previewOnClick: Boolean = false
) {
    val shape = CircleShape
    var showPreview by remember { mutableStateOf(false) }

    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .then(
                    if (previewOnClick) Modifier.clickable { showPreview = true } else Modifier
                )
        )
    }

    if (showPreview && !imageUrl.isNullOrBlank()) {
        ImagePreviewDialog(imageUrl = imageUrl, onDismiss = { showPreview = false })
    }
}
