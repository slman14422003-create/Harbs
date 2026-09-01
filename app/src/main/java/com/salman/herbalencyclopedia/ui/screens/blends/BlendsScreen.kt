package com.salman.herbalencyclopedia.ui.screens.blends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.ui.components.BlendCard
import com.salman.herbalencyclopedia.ui.components.EmptyView
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import com.salman.herbalencyclopedia.ui.components.TopBarBrandTitle

/**
 * شاشة "الخلطات": تعرض كل خلطات الأعشاب لكل المستخدمين (قراءة فقط)، وتضيف
 * زر إنشاء خلطة جديدة للأدمن فقط — نفس نموذج صلاحيات الأعشاب تماماً
 * (قراءة عامة، كتابة بحساب الأدمن الوحيد فقط، عبر Firestore).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendsScreen(
    blends: List<Blend>,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onBlendClick: (Blend) -> Unit,
    onAddNew: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                large = true,
                title = {
                    TopBarBrandTitle(
                        icon = Icons.Filled.Blender,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "الخلطات",
                        subtitle = "${blends.size} خلطة من الأعشاب"
                    )
                },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onAddNew,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("إضافة خلطة") }
                )
            }
        }
    ) { padding ->
        if (blends.isEmpty()) {
            EmptyView(
                message = if (isAdmin) "لا توجد خلطات بعد — أضف أول خلطة" else "لا توجد خلطات بعد",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(blends, key = { it.id }) { blend ->
                    BlendCard(
                        blend = blend,
                        ingredientCount = blend.herbIds.size,
                        onClick = { onBlendClick(blend) }
                    )
                }
            }
        }
    }
}
