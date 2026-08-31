package com.salman.herbalencyclopedia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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

/**
 * عنوان شريط علوي "غني": شارة دائرية ملوّنة بأيقونة + عنوان + عنوان فرعي
 * (سطر سياق حي مثل عدد العناصر)، بدل نص مجرّد بلا وزن بصري. نفس نمط
 * شارة الهوية في [HomeScreen]، معاد استخدامه هنا لبقية الشاشات الجذرية
 * (المفضلة، كل الأعشاب...) كي تبدو منسجمة معه بدل شريط فارغ يبدو ناقصاً.
 */
@Composable
fun TopBarBrandTitle(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

    LiquidGlassSurface(
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false),
        tint = surface,
        borderAlpha = 0.16f,
        // الشريط العلوي ثابت وظاهر طول الوقت بكل الشاشات؛ لمعان لانهائي
        // يلمع ويعيد نفسه من الصفر باستمرار عليه يبدو مزعجاً بدل "حي" —
        // بعكس زر عائم صغير يظهر لثوانٍ. لذلك يُطفأ هنا.
        sheen = false
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
    // على تنقّل الإيماءات الحاجز السفلي (navigationBars) رفيع جداً (عادة
    // أقل من 32dp)، بينما على أزرار التنقل التقليدية الثلاثة يكون أثخن
    // بوضوح. windowInsetsPadding(navigationBars) وحده يتكفّل بعدم تداخل
    // الشريط مع نظام التنقل في الحالتين، لكن بلا أي هامش إضافي كان الشريط
    // العائم يلتصق بحافة منطقة الإيماءات مباشرة على أجهزة الإيماءات —
    // فرق بسيط لكنه يجعل الشريط "معلّقاً" بدل مستقر. هامش سفلي إضافي صغير
    // فقط في وضع الإيماءات يعيد له نفس الإحساس بالاستقرار الذي يملكه
    // تلقائياً فوق أزرار التنقل الأثخن.
    val extraBottomPadding = if (com.salman.herbalencyclopedia.ui.util.isGestureNavigation()) 8.dp else 0.dp

    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .padding(bottom = extraBottomPadding),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            shape = shape,
            modifier = Modifier.shadow(16.dp, shape, clip = false),
            tint = container,
            borderAlpha = 0.16f
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
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
}

@Composable
private fun OneUiFloatingNavItem(
    item: OneUiNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // كل خصائص التبديل هنا (اللون، حجم الأيقونة، اتساع الكبسولة، ظهور
    // النص) كانت تُقسَّم سابقاً بين نوعين مختلفين من الحركة: الألوان
    // بتوين ثابت المدة 140ms، بينما الحجم/الاتساع بنابض snappy() الذي لا
    // يملك "مدة" ثابتة أصلاً بل يستقر تقريبياً حول 150-200ms. الفارق
    // البسيط في طريقة الاستقرار بين النوعين كان يخلق إحساساً بعدم
    // التزامن (اللون يصل لوضعه النهائي بتوقيت مختلف قليلاً عن الحجم/
    // النص) — وهذا تحديداً ما يجعل الحركة "مو ظابطة". الحل: توحيد كل
    // الخصائص هنا على نفس نوع النابض وثوابته تماماً، فتصل كلها لوضعها
    // النهائي معاً كحركة واحدة متماسكة بدل أربع حركات منفصلة قليلاً.
    // (كل استدعاء snappy() أدناه منفصل عمداً — كل واحد يُستدل نوعه من
    // موضع استخدامه: Color، IntSize، Float... لكنها كلها تحمل نفس ثابتي
    // النابض [dampingRatio/stiffness] فتستقر معاً بنفس الإيقاع تماماً.)
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy(),
        label = "navBackground"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy(),
        label = "navContent"
    )
    // نبضة خفيفة على الأيقونة عند الاختيار بدل التبديل المفاجئ.
    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy(),
        label = "navIconScale"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(background, CircleShape)
            // العرض يتوسّع/يتقلّص بسلاسة عند ظهور/اختفاء التسمية بدل القفز
            // المباشر بين حالتي "أيقونة فقط" و"أيقونة + نص"، بنفس النابض
            // الموحّد أعلاه كي يتزامن مع تغيّر اللون والحجم تماماً.
            .animateContentSize(animationSpec = com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy())
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
            enter = androidx.compose.animation.fadeIn(com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy()) +
                androidx.compose.animation.expandHorizontally(com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy()),
            exit = androidx.compose.animation.fadeOut(com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy()) +
                androidx.compose.animation.shrinkHorizontally(com.salman.herbalencyclopedia.ui.theme.AppMotion.snappy())
        ) {
            Text(item.label, color = content, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * شريط تنقّل جانبي لتابلت/نافذة عريضة (Medium/Expanded)، بديل عن
 * [OneUiFloatingNavBar] الذي صُمّم لعرض جوال ضيق. قبل هذا المكوّن كان
 * الشريط العائم السفلي نفسه يُستخدم على كل الأحجام بلا استثناء: على
 * تابلت بوضع أفقي هذا يعني شريطاً عريضاً يمتد كامل عرض الشاشة أسفلها
 * بعيداً عن الإبهام في تلك الحالة، ويسرق ارتفاعاً كان يمكن استغلاله
 * لعرض محتوى أكثر. شريط جانبي ثابت العرض على الحافة (يمين الشاشة بلغة
 * RTL) هو النمط المعتاد لتطبيقات أندرويد على الشاشات الواسعة.
 */
@Composable
fun OneUiNavigationRail(
    items: List<OneUiNavItem>,
    currentRoute: String?,
    onItemClick: (OneUiNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationRailItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
