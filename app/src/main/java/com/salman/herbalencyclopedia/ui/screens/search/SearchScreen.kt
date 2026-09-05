package com.salman.herbalencyclopedia.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.search.HerbSearch
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.HerbCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    herbs: List<Herb>,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onHerbClick: (Herb) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // كان البحث هنا `contains` حرفياً فقط، فلا يطابق "الزعتر" مع "زعتر"
    // (أداة تعريف)، ولا نصاً مشكَّلاً، ولا اسماً بديلاً/مرادفاً — وهذا كان
    // سبب عجز البحث عن إيجاد أعشاب موجودة فعلاً في الموسوعة. [HerbSearch]
    // يطبّع النص (يزيل التشكيل، يوحّد صور الألف/التاء المربوطة...) ويوسّع
    // بمرادفات القاموس المحلي المرفق مع التطبيق (بلا إنترنت ولا تكلفة)، مع
    // ترتيب النتائج حسب دقة المطابقة بدل ترتيب عشوائي: عشبة طابق اسمها
    // الاستعلام تظهر دوماً قبل عشبة طابقتها فقط عبر الفوائد/الاستخدام/غيرها.
    val results = remember(query, herbs) { HerbSearch.search(query, herbs) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                title = {
                    // حقل نص شفاف بلا حدود بدل OutlinedTextField الافتراضي:
                    // قبل هذا التعديل كان الحقل يرسم صندوقاً بحدّه الخاص فوق
                    // شريط الزجاج السائل (زجاج داخل زجاج)، فيبدو غريباً وغير
                    // منسجم مع بقية التطبيق. هذا الشكل يذوب داخل الشريط
                    // العلوي كأنه جزء منه، بأيقونة بحث بادئة توضّح وظيفته
                    // فوراً.
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("ابحث عن عشبة أو فائدة...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                GlassIconButton(onClick = { query = "" }, size = 34.dp) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
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
            query.isBlank() -> EmptyView(
                message = "اكتب اسم عشبة أو فائدة للبحث",
                modifier = Modifier.padding(padding)
            )
            results.isEmpty() -> EmptyView(
                message = "لا توجد نتائج لـ \"$query\"",
                modifier = Modifier.padding(padding)
            )
            // نفس إصلاح شاشات قوائم الأعشاب الأخرى: شبكة متكيّفة بدل عمود
            // واحد ثابت يمدّد البطاقة بعرض الشاشة كاملاً على تابلت.
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.herb.id }) { result ->
                    Column(modifier = Modifier.animateItem()) {
                        HerbCard(
                            herb = result.herb,
                            isFavorite = result.herb.id in favoriteIds,
                            onClick = { onHerbClick(result.herb) },
                            onToggleFavorite = { onToggleFavorite(result.herb.id) }
                        )
                        // العشبة هنا لم يطابق اسمها الاستعلام (وإلا لَما احتجنا
                        // شرحاً) — بطاقتها وحدها تعرض "الفوائد" دوماً بصرف النظر
                        // عن سبب المطابقة الفعلي، فتظهر أحياناً بلا أي علاقة
                        // ظاهرة بما بحث عنه المستخدم. هذا السطر يوضح الحقل الذي
                        // وُجدت فيه المطابقة فعلاً (الاستخدام/التحذيرات/...) مع
                        // مقطع منه، كي لا تبدو النتيجة عشوائية.
                        if (!result.matchedByName && result.matchLabel != null && result.matchSnippet != null) {
                            Text(
                                text = "ورد في ${result.matchLabel}: ${result.matchSnippet}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
