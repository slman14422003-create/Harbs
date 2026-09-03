package com.salman.herbalencyclopedia.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.*
import com.salman.herbalencyclopedia.ui.theme.staggeredEntrance
import com.salman.herbalencyclopedia.ui.util.rememberWindowSizeInfo
import java.util.Calendar

/**
 * قبل هذا التعديل كان الشريط العلوي يعرض جملة تعريفية ثابتة لا تتغيّر أبداً
 * ("معرفة موثوقة • تجربة هادئة • تصميم حديث"). هذه الدالة تستبدلها بتحية
 * فعلية مبنية على وقت الجهاز، كي يشعر الشريط العلوي بأنه "حي" ومخصص لكل
 * زيارة بدل شعار تسويقي جامد.
 */
private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (hour in 5..11) "أهلاً بك، صباح الخير" else "أهلاً بك، مساء الخير"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<Category>,
    herbs: List<Herb>,
    isLoading: Boolean,
    error: String?,
    isAdmin: Boolean,
    onRetry: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onSemoClick: () -> Unit,
    onBlendsClick: () -> Unit
) {
    // كانت الشبكة GridCells.Fixed(2) ثابتة بعمودين دائماً: على تابلت أو
    // نافذة عريضة هذا يعني بطاقتين متمددتين بعرض هائل بدل الاستفادة من
    // المساحة. GridCells.Adaptive مع الحد الأدنى القادم من محرك اكتشاف
    // الشاشة يحسب عدد الأعمدة ديناميكياً من العرض الفعلي المتاح: يبقى
    // عمودين على جوال عادي، ويزيد تلقائياً إلى 3-5 أعمدة كلما اتسعت
    // الشاشة، دون أي عتبة مكتوبة يدوياً هنا.
    val windowInfo = rememberWindowSizeInfo()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // هذه الشاشة تظهر دائماً فوق OneUiFloatingNavBar (شاشة جذر ضمن
        // topRoutes)، والـBox الخارجي في HerbalNavGraph يكون قد حجز أصلاً
        // كامل ارتفاع الشريط العائم (شاملاً حاجز نظام التنقّل السفلي، لأن
        // الشريط نفسه يطبّق windowInsetsPadding(navigationBars) داخلياً).
        // لو تُرك Scaffold هنا على قيمته الافتراضية (safeDrawing) سيحجز
        // إضافياً نفس ارتفاع شريط النظام السفلي من جديد، فتظهر فجوة
        // فارغة مضاعفة (سماكة/مسافة) بين آخر عنصر بالمحتوى وبين الشريط
        // العائم نفسه. لذلك نُبقي فقط حجز الحافة العلوية (شريط الحالة)
        // اللازمة لـGlassTopBar، ونُصفّر الحافة السفلية هنا.
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            GlassTopBar(
                large = true,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // شارة دائرية بهوية التطبيق (نفس أيقونة شاشة البداية)
                        // بدل عنوان نصي مجرّد، لإحساس علامة تجارية أوضح.
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "موسوعة الأعشاب الطبية",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                greetingForNow(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        GlassIconButton(onClick = onAdminClick, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "لوحة التحكم")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Search,
                    label = "بحث",
                    onClick = onSearchClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.AutoAwesome,
                    label = "سيمو",
                    onClick = onSemoClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Blender,
                    label = "الخلطات",
                    onClick = onBlendsClick
                )
            }

            when {
                isLoading -> LoadingView(Modifier.fillMaxSize())
                error != null -> ErrorView(error, onRetry, Modifier.fillMaxSize())
                categories.isEmpty() -> EmptyView("لا توجد تصنيفات بعد", Modifier.fillMaxSize())
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = windowInfo.gridMinCellWidth),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                        CategoryCard(
                            category = category,
                            herbCount = herbs.count { it.categoryId == category.id },
                            onClick = { onCategoryClick(category) },
                            // بدل ظهور كل البطاقات دفعة واحدة، كل بطاقة تتلاشى
                            // وتنزلق للأعلى بعد اللي قبلها بفارق بسيط — مرة
                            // واحدة عند تحميل الشاشة، بلا أي تكرار لانهائي.
                            modifier = Modifier
                                .animateItem()
                                .staggeredEntrance(index)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            // كان padding أفقي 16.dp مع Spacer عرضه 8.dp يترك مساحة ضيقة
            // جداً أمام النص داخل عنصر بعرض ١/٣ الشاشة فقط (ثلاث بطاقات
            // متجاورة بوزن متساوٍ) — تكفي لكلمة قصيرة مثل "بحث" أو "سيمو"
            // لكن ليس لكلمة أطول مثل "الخلطات"، فكانت تلتف لسطر ثانٍ ثم
            // تُقصّ داخل الارتفاع الثابت 54.dp فيظهر السطر الثاني (الحرف
            // الأخير) وحده مقطوعاً. تقليل الحشو والفراغ بينهما يمنح النص
            // مساحة إضافية كافية، و maxLines/overflow يبقيان كشبكة أمان
            // نهائية تمنع تكرار المشكلة مع أي تسمية أطول مستقبلاً.
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
