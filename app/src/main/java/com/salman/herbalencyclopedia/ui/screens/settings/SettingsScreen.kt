package com.salman.herbalencyclopedia.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isLoggedIn: Boolean,
    isAdmin: Boolean,
    darkMode: Boolean?,
    dynamicColor: Boolean,
    fontScale: Int,
    onBack: () -> Unit,
    onDarkModeChange: (Boolean?) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onFontScaleChange: (Int) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAdminToolsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("المظهر", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("اتّباع نظام الجهاز")
                Switch(checked = darkMode == null, onCheckedChange = { onDarkModeChange(if (it) null else false) })
            }
            if (darkMode != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الوضع الداكن")
                    Switch(checked = darkMode, onCheckedChange = { onDarkModeChange(it) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ألوان ديناميكية (Material You)")
                Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("حجم النص", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("عادي", "كبير", "أكبر").forEachIndexed { index, label ->
                    FilterChip(selected = fontScale == index, onClick = { onFontScaleChange(index) }, label = { Text(label) })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("الحساب", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onHelpClick, modifier = Modifier.fillMaxWidth()) { Text("المساعدة") }
            if (isAdmin) OutlinedButton(onClick = onAdminToolsClick, modifier = Modifier.fillMaxWidth()) { Text("أدوات الإدارة") }
            if (isLoggedIn) {
                OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                    Text("تسجيل الخروج")
                }
            } else {
                Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                    Text("تسجيل الدخول")
                }
            }
        }
    }
}
