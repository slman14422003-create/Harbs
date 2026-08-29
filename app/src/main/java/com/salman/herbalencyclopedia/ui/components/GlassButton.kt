package com.salman.herbalencyclopedia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * مجموعة أزرار موحّدة على طراز "الكبسولة الزجاجية" (Glass Capsule) المستخدم
 * في One UI 8.5: خلفية متدرّجة شبه شفافة، حواف مستديرة بالكامل (كبسولة)،
 * وحدّ رفيع يعطي إحساس الزجاج. هذه المكوّنات بديل مباشر لأزرار Material3
 * القياسية (Button / OutlinedButton / IconButton) لتوحيد شكل الأزرار في
 * التطبيق بالكامل.
 */

/** زر أساسي مملوء بكبسولة زجاجية بلون الهوية (primary). */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 26.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val disabledContainer = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val glassBrush = if (enabled) {
        Brush.verticalGradient(listOf(containerColor.copy(alpha = 0.96f), containerColor.copy(alpha = 0.84f)))
    } else {
        Brush.verticalGradient(listOf(disabledContainer, disabledContainer))
    }
    val borderColor = Color.White.copy(alpha = if (enabled) 0.18f else 0.06f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp).shadow(if (enabled) 3.dp else 0.dp, CircleShape),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = if (enabled) contentColor else disabledContent
    ) {
        Row(
            modifier = Modifier
                .background(glassBrush)
                .border(BorderStroke(1.dp, borderColor), CircleShape)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

/** زر ثانوي بحدّ زجاجي شفاف الخلفية، مناسب للإجراءات الأقل أهمية. */
@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 13.dp),
    content: @Composable RowScope.() -> Unit
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val glassBrush = Brush.verticalGradient(listOf(surface.copy(alpha = 0.55f), surface.copy(alpha = 0.30f)))
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.35f else 0.12f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp).shadow(if (enabled) 3.dp else 0.dp, CircleShape),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
    ) {
        Row(
            modifier = Modifier
                .background(glassBrush)
                .border(BorderStroke(1.dp, borderColor), CircleShape)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

/**
 * زر أيقونة دائري بمظهر كبسولة زجاجية — بديل مباشر لـ IconButton القياسي،
 * يُستخدم في الأشرطة العلوية وبطاقات الأعشاب وكل مكان يحتوي على أيقونة قابلة للنقر.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    val glassBrush = Brush.verticalGradient(
        listOf(containerColor.copy(alpha = 0.85f), containerColor.copy(alpha = 0.60f))
    )
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glassBrush)
                .border(BorderStroke(1.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides (if (enabled) contentColor else contentColor.copy(alpha = 0.38f))) {
                content()
            }
        }
    }
}
