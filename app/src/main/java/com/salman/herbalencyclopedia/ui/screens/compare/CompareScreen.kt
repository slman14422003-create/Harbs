package com.salman.herbalencyclopedia.ui.screens.compare

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.data.ai.HerbAssistant
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassOutlinedButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_COMPARE = 3

private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: Long = System.nanoTime()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(herbs: List<Herb>, onBack: () -> Unit) {
    var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val selected = selectedIds.mapNotNull { id -> herbs.firstOrNull { it.id == id } }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // مفتاح المحادثة: عند تغيير الأعشاب المختارة تبدأ دردشة جديدة تلقائياً
    val chatKey = selectedIds.sorted().joinToString(",")
    var messages by remember(chatKey) { mutableStateOf(listOf<ChatMessage>()) }
    var isThinking by remember(chatKey) { mutableStateOf(false) }
    var inputText by remember(chatKey) { mutableStateOf("") }
    val chatListState = androidx.compose.foundation.lazy.rememberLazyListState()

    fun sendMessage(raw: String) {
        val question = raw.trim()
        // لا يوجد أي حد أدنى صناعي لعدد الأعشاب لتفعيل الدردشة — عشبة واحدة
        // تكفي ليجيب المساعد من بيانات الموسوعة الخاصة بها مباشرة.
        if (question.isBlank() || selected.isEmpty() || isThinking) return
        messages = messages + ChatMessage(question, isUser = true)
        inputText = ""
        isThinking = true
        scope.launch {
            delay((450L..850L).random())
            val reply = HerbAssistant.answer(question, selected)
            messages = messages + ChatMessage(reply, isUser = false)
            isThinking = false
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        val lastIndex = messages.size - 1 + if (isThinking) 1 else 0
        if (lastIndex >= 0) chatListState.animateScrollToItem(lastIndex)
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassTopBar(title = { Text("المقارنة الذكية") }, navigationIcon = {
                GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            }, actions = {
                if (selected.isNotEmpty()) TextButton(onClick = { selectedIds = emptyList() }) { Text("مسح") }
            })
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("اختر عشبة واحدة أو أكثر (حتى $MAX_COMPARE)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "يحلّل المساعد الذكي بياناتها فوراً، ويمكنك سؤاله أي شيء ضمن هذه الشاشة — يعمل بالكامل على جهازك، بلا اتصال إنترنت، وبلا أي حظر على الإجابة طالما توفرت المعلومة في الموسوعة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(herbs) { herb ->
                    val isSelected = herb.id in selectedIds
                    FilterChip(selected = isSelected, onClick = {
                        when {
                            isSelected -> selectedIds = selectedIds - herb.id
                            selectedIds.size < MAX_COMPARE -> selectedIds = selectedIds + herb.id
                            else -> scope.launch {
                                snackbarHostState.showSnackbar("يمكن مقارنة $MAX_COMPARE أعشاب كحد أقصى، أزل واحدة أولاً")
                            }
                        }
                    }, label = { Text(herb.name) })
                }
            }

            if (selected.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("لم تتم إضافة أي عشبة بعد.", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "اختر عشبة واحدة على الأقل من الأعلى ليبدأ المساعد الذكي بعرض معلوماتها وتنظيمها لك تلقائياً، ثم اسأله أي سؤال يخطر ببالك. اختر عشبتين أو أكثر لمقارنة مباشرة بينها.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                AiOverviewCard(HerbAssistant.buildOverview(selected))

                ComparisonFieldCard("الفوائد", selected) { it.benefits }
                ComparisonFieldCard("الاستخدام", selected) { it.usage }
                ComparisonFieldCard("التحذيرات", selected) { it.warnings }
                ComparisonFieldCard("الأضرار", selected) { it.harms }
                ComparisonFieldCard("ملاحظات", selected) { it.notes }

                GlassOutlinedButton(
                    onClick = {
                        val shareText = buildCompareShareText(selected)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "مقارنة الأعشاب")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة المقارنة"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.IosShare, null); Spacer(Modifier.width(8.dp)); Text("مشاركة / طباعة")
                }

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("اسأل المساعد الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (messages.isEmpty() && !isThinking) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(HerbAssistant.quickSuggestions(selected)) { suggestion ->
                            AssistChip(onClick = { sendMessage(suggestion) }, label = { Text(suggestion) })
                        }
                    }
                }

                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(messages, key = { it.id }) { message -> ChatBubble(message) }
                    if (isThinking) item(key = "typing") { TypingBubble() }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اسأل عن ${selected.joinToString(" أو ") { it.name }}...") },
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                    )
                    GlassIconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isThinking
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "إرسال")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiOverviewCard(overview: String) {
    if (overview.isBlank()) return
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("ملخص المقارنة الذكي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(overview, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ComparisonFieldCard(title: String, herbs: List<Herb>, field: (Herb) -> String) {
    val points = remember(herbs, title) { HerbAssistant.compareField(herbs, field) }
    if (points.isEmpty()) return

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            val shared = points.filter { it.herbIds.size == herbs.size && herbs.size > 1 }
            val rest = points.filter { it.herbIds.size != herbs.size }

            if (shared.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "🤝 مشترك بين الجميع",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        shared.forEach { Text("• ${it.text}", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }

            herbs.forEach { herb ->
                val mine = rest.filter { herb.id in it.herbIds }
                if (mine.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (herbs.size > 1) {
                            Text(herb.name, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                        mine.forEach { Text("• ${it.text}", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            AssistantAvatar()
            Spacer(Modifier.width(6.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alphaValue by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "typingAlpha"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        AssistantAvatar()
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                "يكتب…",
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .alpha(alphaValue),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssistantAvatar() {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(15.dp)
        )
    }
}

private fun buildCompareShareText(herbs: List<Herb>): String = buildString {
    appendLine("مقارنة الأعشاب")
    appendLine()
    val overview = HerbAssistant.buildOverview(herbs)
    if (overview.isNotBlank()) {
        appendLine(overview)
        appendLine()
    }
    herbs.forEach { herb ->
        appendLine("• ${herb.name}")
        if (herb.benefits.isNotBlank()) appendLine("  الفوائد: ${herb.benefits}")
        if (herb.warnings.isNotBlank()) appendLine("  التحذيرات: ${herb.warnings}")
        if (herb.harms.isNotBlank()) appendLine("  الأضرار: ${herb.harms}")
        if (herb.usage.isNotBlank()) appendLine("  الاستخدام: ${herb.usage}")
        if (herb.notes.isNotBlank()) appendLine("  ملاحظات: ${herb.notes}")
        appendLine()
    }
}
