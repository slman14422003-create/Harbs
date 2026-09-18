package com.salman.herbalencyclopedia.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * شاشة التحميل المشتركة (أول تحميل حقيقي بلا أي بيانات معروضة بعد - راجع
 * كل نقطة استخدام لها: HomeScreen وAllHerbsScreen). كانت دائرة دوران مجرّدة
 * بلا أي نص، فلا تُفرَّق بصرياً عن شاشة عالقة أو معطوبة عند أول تشغيل
 * للتطبيق (خصوصاً إن استغرق أول اتصال بـFirestore بضع ثوانٍ على إنترنت
 * بطيء) - وهذا بالضبط ما أدّى إلى الالتباس المُبلَّغ عنه. الآن تُضاف رسالة
 * نصية اختيارية توضّح أن هناك عملية تحميل فعلية تجري (لا عطلاً)، بجانب خط
 * تقدّم أفقي (LinearProgressIndicator) أوضح كمؤشر "جلب بيانات" من الدائرة
 * وحدها، مع إبقاء الدائرة أيضاً لأنها الشكل الألِف لأي مؤشر تحميل عام.
 */
@Composable
fun LoadingView(modifier: Modifier = Modifier, message: String? = null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.width(160.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
            GlassButton(onClick = onRetry) { Text("إعادة المحاولة") }
        }
    }
}
