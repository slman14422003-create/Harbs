package com.salman.herbalencyclopedia.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import com.salman.herbalencyclopedia.ui.theme.ThemePalette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val fontScaleLabels = listOf("عادي", "كبير", "أكبر")
private val fontScaleSizes = listOf(15.sp, 18.sp, 21.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isLoggedIn: Boolean,
    isAdmin: Boolean,
    darkMode: Boolean?,
    dynamicColor: Boolean,
    fontScale: Int,
    themePalette: com.salman.herbalencyclopedia.ui.theme.ThemePalette,
    onBack: () -> Unit,
    onDarkModeChange: (Boolean?) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onFontScaleChange: (Int) -> Unit,
    onThemePaletteChange: (com.salman.herbalencyclopedia.ui.theme.ThemePalette) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAdminToolsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            GlassTopBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SettingsSection(title = "المظهر") {
                    ThemeModeSelector(darkMode = darkMode, onDarkModeChange = onDarkModeChange)
                    SettingsDivider()
                    SwitchRow(
                        icon = Icons.Filled.Palette,
                        iconTint = Color(0xFF7C4DFF),
                        title = "ألوان ديناميكية",
                        subtitle = "استخدام ألوان الخلفية (Material You)",
                        checked = dynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                    SettingsDivider()
                    PaletteRow(
                        enabled = !dynamicColor,
                        selected = themePalette,
                        onSelect = onThemePaletteChange
                    )
                    SettingsDivider()
                    FontScaleRow(fontScale = fontScale, onFontScaleChange = onFontScaleChange)
                }
            }

            item {
                SettingsSection(title = "الحساب") {
                    ActionRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = Color(0xFF2E7D32),
                        title = "المساعدة",
                        subtitle = "الأسئلة الشائعة وطريقة الاستخدام",
                        onClick = onHelpClick
                    )
                    if (isAdmin) {
                        SettingsDivider()
                        ActionRow(
                            icon = Icons.Filled.AdminPanelSettings,
                            iconTint = Color(0xFF1565C0),
                            title = "أدوات الإدارة",
                            subtitle = "إدارة الأعشاب والتصنيفات والبيانات",
                            onClick = onAdminToolsClick
                        )
                    }
                    SettingsDivider()
                    if (isLoggedIn) {
                        ActionRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            iconTint = Color(0xFFC62828),
                            title = "تسجيل الخروج",
                            subtitle = "إنهاء الجلسة الحالية",
                            onClick = onLogoutClick
                        )
                    } else {
                        ActionRow(
                            icon = Icons.Filled.Login,
                            iconTint = Color(0xFF00695C),
                            title = "تسجيل الدخول",
                            subtitle = "لإدارة المحتوى وحفظ التفضيلات",
                            onClick = onLoginClick
                        )
                    }
                }
            }

            item {
                Text(
                    "موسوعة الأعشاب الطبية",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(vertical = 6.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ThemeModeSelector(darkMode: Boolean?, onDarkModeChange: (Boolean?) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Filled.Brightness6, tint = Color(0xFFFF8F00))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("وضع العرض", fontWeight = FontWeight.SemiBold)
                Text(
                    when (darkMode) { null -> "يتبع النظام"; true -> "داكن"; false -> "فاتح" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOptionChip(
                selected = darkMode == null,
                icon = Icons.Filled.Contrast,
                label = "النظام",
                modifier = Modifier.weight(1f)
            ) { onDarkModeChange(null) }
            ThemeOptionChip(
                selected = darkMode == false,
                icon = Icons.Filled.LightMode,
                label = "فاتح",
                modifier = Modifier.weight(1f)
            ) { onDarkModeChange(false) }
            ThemeOptionChip(
                selected = darkMode == true,
                icon = Icons.Filled.DarkMode,
                label = "داكن",
                modifier = Modifier.weight(1f)
            ) { onDarkModeChange(true) }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier
    )
}

@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FontScaleRow(fontScale: Int, onFontScaleChange: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Filled.FormatSize, tint = Color(0xFF00838F))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("حجم النص", fontWeight = FontWeight.SemiBold)
                Text(fontScaleLabels[fontScale], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "أبج",
                fontSize = fontScaleSizes[fontScale],
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(10.dp))
        Slider(
            value = fontScale.toFloat(),
            onValueChange = { onFontScaleChange(it.roundToInt()) },
            valueRange = 0f..2f,
            steps = 1
        )
    }
}

@Composable
private fun PaletteRow(
    enabled: Boolean,
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit
) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Filled.Palette, tint = Color(0xFFAD1457))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("لوحة الألوان", fontWeight = FontWeight.SemiBold)
                Text(
                    if (enabled) "اختر لون الهوية اليدوي" else "متاحة عند إيقاف الألوان الديناميكية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemePalette.entries.forEach { palette ->
                PaletteSwatch(
                    palette = palette,
                    selected = enabled && palette == selected,
                    enabled = enabled,
                    onClick = { onSelect(palette) }
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: ThemePalette,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(palette.swatch)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = palette.label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
