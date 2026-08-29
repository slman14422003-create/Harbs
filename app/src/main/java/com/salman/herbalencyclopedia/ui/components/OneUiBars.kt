package com.salman.herbalencyclopedia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * شريط علوي بمظهر "زجاجي" (Glass) مستوحى من تصميم One UI 8.5:
 * خلفية شبه شفافة متدرّجة بدل اللون الصلب الافتراضي، مع خط فاصل رفيع جداً
 * أسفل الشريط لإعطاء إحساس العمق بدون كسر توافق الأجهزة القديمة.
 *
 * بديل مباشر لِـ TopAppBar / LargeTopAppBar القياسي في Material3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    large: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    // بطاقة زجاجية بزوايا سفلية مستديرة، بلا أي ظل أو حد علوي حتى تندمج
    // خلفيتها مباشرة مع شريط الحالة دون أي خط فاصل ظاهر بينهما.
    val container = MaterialTheme.colorScheme.surfaceContainer
    val glassBrush = Brush.verticalGradient(
        colors = listOf(container.copy(alpha = 0.98f), container.copy(alpha = 0.88f))
    )
    val hairline = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    val barShape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)

    Box(
        modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(glassBrush)
            .drawBehind {
                drawLine(
                    color = hairline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        if (large) {
            LargeTopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        } else {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        }
    }
}

/** عنصر واحد في الشريط السفلي العائم. */
data class OneUiNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * شريط تنقّل سفلي "عائم" فوق الشاشة (Floating pill) على طراز One UI 8.5:
 * كبسولة زجاجية بزوايا كاملة الاستدارة، هامش من الحواف والأسفل بدل الالتصاق
 * بحافة الشاشة، مع تحوّل العنصر المحدد إلى كبسولة داخلية تعرض التسمية.
 */
@Composable
fun OneUiFloatingNavBar(
    items: List<OneUiNavItem>,
    currentRoute: String?,
    onItemClick: (OneUiNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val glassBrush = Brush.verticalGradient(
        colors = listOf(container.copy(alpha = 0.97f), container.copy(alpha = 0.90f))
    )
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)

    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(32.dp), clip = false)
                .clip(RoundedCornerShape(32.dp))
                .background(glassBrush)
                .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                OneUiFloatingNavItem(
                    item = item,
                    selected = item.route == currentRoute,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun OneUiFloatingNavItem(item: OneUiNavItem, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(background, CircleShape)
            .animateContentSize(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
            .padding(horizontal = if (selected) 18.dp else 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = if (!selected) item.label else null, tint = content, modifier = Modifier.size(23.dp))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(120)) + expandHorizontally(tween(160, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(90)) + shrinkHorizontally(tween(120, easing = FastOutSlowInEasing))
        ) {
            Text(item.label, color = content, fontWeight = FontWeight.SemiBold)
        }
    }
}
