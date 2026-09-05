package com.salman.herbalencyclopedia.ui.screens.semo

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.window.Dialog
import com.salman.herbalencyclopedia.data.ai.HerbAssistant
import com.salman.herbalencyclopedia.data.ai.TrainedExample
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_ATTACHED = 3

/**
 * [learnable] = هل هذا الرد ناتج عن بحث حر فعلي (وليس ترحيباً/شكراً/حالة
 * مدرَّبة مسبقاً)؟ في هذه الحالة فقط تُعرض أزرار تقييم 👍/👎 تحت الفقاعة،
 * وتقييم المستخدم هو ما يغذّي "تعلّم سيمو الذاتي" (انظر [HerbAssistant.recordFeedback]).
 * [feedback] = null قبل أي تقييم، true بعد 👍، false بعد 👎 (لإخفاء الأزرار
 * واستبدالها برسالة تأكيد قصيرة بعد أول تقييم).
 */
private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val sourceQuestion: String = "",
    val learnable: Boolean = false,
    val feedback: Boolean? = null,
    val id: Long = System.nanoTime()
)

/**
 * سيمو — واجهة دردشة ذكاء اصطناعي كاملة الشاشة، وليست "شاشة مقارنة".
 * الدردشة هي الواجهة الأساسية دائماً: يمكن سؤال سيمو عن أي عشبة موجودة في
 * الموسوعة مباشرة بذكر اسمها في السؤال، دون أي حاجة لاختيار شيء مسبقاً.
 * إرفاق عشبة أو أكثر (اختياري تماماً، عبر زر "إرفاق" في الأعلى) يفيد فقط
 * في تضييق نطاق الإجابة على أعشاب بعينها؛ أما "المقارنة" المنظّمة بين أكثر
 * من عشبة فلا تحدث إطلاقاً إلا إذا طلبها المستخدم صراحة — إما بإرفاق
 * عشبتين أو أكثر، أو بذكر اسمَي عشبتين معاً في نص السؤال نفسه (مثل: "قارن
 * بين الزنجبيل والقرفة"). لا يوجد أي شرط لإضافة عشبتين لتفعيل الدردشة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemoAssistantScreen(
    herbs: List<Herb>,
    onBack: () -> Unit,
    // ── توسيع "صلاحيات" سيمو ليشمل كل الموسوعة: يمرَّر هنا حيّاً من
    // UiState.blends (نفس مصدر شاشة الخلطات)، فيدخل ضمن نطاق البحث الحر
    // العام تلقائياً — دون أي تأثير على منطق مقارنة الأعشاب المحدَّدة، الذي
    // يبقى كما هو تماماً. ──
    blends: List<Blend> = emptyList(),
    // ── تعلّم سيمو الذاتي: قائمة الحالات المتعلَّمة تُمرَّر حيّة من
    // DataStore عبر HerbalNavGraph، وأي تقييم 👍/👎 هنا يُحدّثها فوراً عبر
    // onAutoLearnedExamplesChange لتُحفظ وتُطبَّق في كل محادثة قادمة. ──
    onAutoLearnedExamplesChange: (List<TrainedExample>) -> Unit = {}
) {
    var attachedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val attached = attachedIds.mapNotNull { id -> herbs.firstOrNull { it.id == id } }
    var showAttachPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isThinking by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val chatListState = androidx.compose.foundation.lazy.rememberLazyListState()

    fun sendMessage(raw: String) {
        val question = raw.trim()
        if (question.isBlank() || isThinking) return
        messages = messages + ChatMessage(question, isUser = true)
        inputText = ""
        isThinking = true
        scope.launch {
            delay((400L..750L).random())

            // لا مقارنة أو استعراض تفصيلي إلا بطلب واضح: إما بإرفاق عشبة أو
            // أكثر يدوياً، أو بذكر اسمها صراحة داخل نص السؤال. غير ذلك يبقى
            // سيمو يبحث بحرية في كامل الموسوعة (وضع محادثة عامة).
            //
            // كان استدعاء [HerbAssistant] بأكمله (تحليل نص + مسح كل حقول كل
            // الأعشاب والخلطات + حساب تشابه موزون لكل نقطة) يعمل مباشرة على
            // نطاق rememberCoroutineScope الافتراضي، وهو مرتبط بخيط الواجهة
            // الرئيسي (Dispatchers.Main.immediate) — أي أن كل هذا الحساب
            // الثقيل كان يُنفَّذ فعلياً على خيط الواجهة نفسه، فيُجمِّد الرسوم
            // المتحركة (نبض "سيمو يكتب…"، تمرير المحادثة) لحظياً مع كل رسالة
            // على موسوعة كبيرة. withContext(Dispatchers.Default) ينقل الحساب
            // فقط إلى خيط خلفية مخصَّص للمعالجة الثقيلة، ويعود تلقائياً لخيط
            // الواجهة بعد انتهائه لتحديث الحالة (messages) بأمان.
            val mentioned = if (attached.isEmpty()) {
                withContext(Dispatchers.Default) { HerbAssistant.relevantHerbs(question, herbs) }
            } else emptyList()
            val contextHerbs: List<Herb>
            val contextBlends: List<Blend>
            val allowCompare: Boolean
            when {
                attached.isNotEmpty() -> { contextHerbs = attached; contextBlends = emptyList(); allowCompare = true }
                mentioned.isNotEmpty() -> { contextHerbs = mentioned; contextBlends = emptyList(); allowCompare = true }
                else -> { contextHerbs = herbs; contextBlends = blends; allowCompare = false }
            }

            val reply = withContext(Dispatchers.Default) {
                HerbAssistant.answerDetailed(question, contextHerbs, allowCompare, contextBlends)
            }
            messages = messages + ChatMessage(
                text = reply.text,
                isUser = false,
                sourceQuestion = question,
                learnable = reply.learnable
            )
            isThinking = false
        }
    }

    fun rateMessage(messageId: Long, helpful: Boolean) {
        val target = messages.firstOrNull { it.id == messageId } ?: return
        scope.launch {
            // نفس السبب أعلاه: [HerbAssistant.recordFeedback] يعيد تحليل نص
            // السؤال والرد الحاليين لمطابقتهما مع الحالات المتعلَّمة سابقاً —
            // حساب أخف من [answerDetailed] لكنه يبقى تحليل نص، فيُفضَّل عدم
            // تنفيذه على خيط الواجهة كذلك.
            val updated = withContext(Dispatchers.Default) {
                HerbAssistant.recordFeedback(target.sourceQuestion, target.text, helpful)
            }
            onAutoLearnedExamplesChange(updated)
            messages = messages.map { if (it.id == messageId) it.copy(feedback = helpful) else it }
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
            GlassTopBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistantAvatar(size = 26.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("سيمو المساعد")
                }
            }, navigationIcon = {
                GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            }, actions = {
                if (messages.isNotEmpty()) {
                    GlassIconButton(onClick = {
                        val shareText = buildChatShareText(messages)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "محادثة مع سيمو المساعد")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة المحادثة"))
                    }) { Icon(Icons.Filled.IosShare, "مشاركة المحادثة") }
                }
                TextButton(onClick = { showAttachPicker = true }) {
                    Icon(Icons.Filled.Eco, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (attached.isEmpty()) "إرفاق" else "${attached.size}")
                }
            })
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            // شريط الأعشاب المرفقة: اختياري بالكامل، يظهر فقط عند وجود إرفاق
            AnimatedVisibility(visible = attached.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attached, key = { it.id }) { herb ->
                        InputChip(
                            selected = true,
                            onClick = { attachedIds = attachedIds - herb.id },
                            label = { Text(herb.name) },
                            trailingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && !isThinking) {
                    WelcomeState(
                        attached = attached,
                        allHerbs = herbs,
                        onSuggestionClick = { sendMessage(it) }
                    )
                } else {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(message, onRate = { helpful -> rateMessage(message.id, helpful) })
                        }
                        if (isThinking) item(key = "typing") { TypingBubble() }
                    }
                }
            }

            HorizontalDivider()

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (attached.isEmpty()) "اسأل سيمو عن أي عشبة..." else "اسأل عن ${attached.joinToString(" أو ") { it.name }}...") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
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

    if (showAttachPicker) {
        AttachHerbsDialog(
            herbs = herbs,
            initiallySelected = attachedIds.toSet(),
            onDismiss = { showAttachPicker = false },
            onConfirm = { ids ->
                attachedIds = ids.toList()
                showAttachPicker = false
            }
        )
    }
}

/**
 * حالة الترحيب الأولى قبل أي رسالة: تشرح لسيمو نفسه بإيجاز وتعرض اقتراحات
 * سريعة جاهزة للنقر، مبنية على ما هو مرفق فعلياً (أو عيّنة من الموسوعة إن
 * لم يُرفق شيء) دون إجبار المستخدم على أي اختيار مسبق.
 */
@Composable
private fun WelcomeState(attached: List<Herb>, allHerbs: List<Herb>, onSuggestionClick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssistantAvatar(size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text("أهلاً، أنا سيمو 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "اسألني عن أي عشبة في الموسوعة، أو اطلب مقارنة بين أكثر من عشبة مباشرة داخل سؤالك — بلا اتصال إنترنت وبلا أي حظر على الإجابة.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        val suggestions = HerbAssistant.quickSuggestions(attached, allHerbs)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                SuggestionCard(suggestion) { onSuggestionClick(suggestion) }
            }
        }
    }
}

@Composable
private fun SuggestionCard(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AttachHerbsDialog(
    herbs: List<Herb>,
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp)) {
                Text("إرفاق أعشاب (اختياري)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "يفيد إرفاق عشبة أو أكثر (حتى $MAX_ATTACHED) في تضييق إجابات سيمو، ويمكّنك من طلب مقارنة صريحة بينها. ليس شرطاً لبدء الدردشة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(herbs, key = { it.id }) { herb ->
                        val isSelected = herb.id in selected
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selected = when {
                                    isSelected -> selected - herb.id
                                    selected.size < MAX_ATTACHED -> selected + herb.id
                                    else -> selected
                                }
                            },
                            label = { Text(herb.name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { selected = emptySet() }) { Text("مسح الكل") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { onConfirm(selected) }) { Text("تم") }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onRate: (Boolean) -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isUser) {
                AssistantAvatar(size = 28.dp)
                Spacer(Modifier.width(6.dp))
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        // تقييم سريع لإجابات "البحث الحر" القابلة للتعلّم الذاتي فقط: 👍
        // يحوّل هذه الإجابة إلى حالة يتعلّمها سيمو تلقائياً لأسئلة مشابهة
        // لاحقاً، و👎 يتراجع عن أي شيء تعلّمه سابقاً بنفسه لسؤال مشابه.
        if (!message.isUser && message.learnable) {
            Row(
                Modifier.padding(start = 34.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (message.feedback) {
                    null -> {
                        Text(
                            "هل كانت هذه الإجابة مفيدة؟",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        GlassIconButton(onClick = { onRate(true) }, size = 26.dp) {
                            Icon(Icons.Filled.ThumbUp, "مفيدة", modifier = Modifier.size(14.dp))
                        }
                        GlassIconButton(onClick = { onRate(false) }, size = 26.dp) {
                            Icon(Icons.Filled.ThumbDown, "غير مفيدة", modifier = Modifier.size(14.dp))
                        }
                    }
                    true -> Text(
                        "🌱 شكراً، سيتذكّر سيمو هذه الإجابة لسؤال مشابه لاحقاً.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    false -> Text(
                        "تم، لن يعتمد سيمو على هذه الإجابة تحديداً مرة أخرى.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        AssistantAvatar(size = 28.dp)
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                "سيمو يكتب…",
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .alpha(alphaValue),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssistantAvatar(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

private fun buildChatShareText(messages: List<ChatMessage>): String = buildString {
    appendLine("محادثة مع سيمو المساعد")
    appendLine()
    messages.forEach { m ->
        appendLine(if (m.isUser) "أنت: ${m.text}" else "سيمو: ${m.text}")
        appendLine()
    }
}
