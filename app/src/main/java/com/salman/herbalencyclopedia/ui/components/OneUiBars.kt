package com.salman.herbalencyclopedia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val border = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        surface.copy(alpha = 0.98f),
                        surface.copy(alpha = 0.90f)
                    )
                )
            )
            .drawBehind {
                drawLine(
                    color = border,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
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
                    scrolledContainerColor = Color.Transparent
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
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        }
    }
}

data class OneUiNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun OneUiFloatingNavBar(
    items: List<OneUiNavItem>,
    currentRoute: String?,
    onItemClick: (OneUiNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(16.dp, shape, clip = false)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(container.copy(alpha = 0.98f), container.copy(alpha = 0.92f))
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), shape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
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
private fun OneUiFloatingNavItem(
    item: OneUiNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(180),
        label = "navBackground"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "navContent"
    )
    // نبضة خفيفة على الأيقونة عند الاختيار بدل التبديل المفاجئ.
    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = com.salman.herbalencyclopedia.ui.theme.AppMotion.bouncy(),
        label = "navIconScale"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(background, CircleShape)
            // العرض يتوسّع/يتقلّص بسلاسة عند ظهور/اختفاء التسمية بدل القفز
            // المباشر بين حالتي "أيقونة فقط" و"أيقونة + نص".
            .animateContentSize(animationSpec = com.salman.herbalencyclopedia.ui.theme.AppMotion.bouncy())
            .padding(horizontal = if (selected) 16.dp else 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = content,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale)
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
            enter = androidx.compose.animation.fadeIn(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth()) +
                androidx.compose.animation.expandHorizontally(com.salman.herbalencyclopedia.ui.theme.AppMotion.bouncy()),
            exit = androidx.compose.animation.fadeOut(com.salman.herbalencyclopedia.ui.theme.AppMotion.smooth()) +
                androidx.compose.animation.shrinkHorizontally(com.salman.herbalencyclopedia.ui.theme.AppMotion.bouncy())
        ) {
            Text(item.label, color = content, fontWeight = FontWeight.SemiBold)
        }
    }
}
