package com.salman.herbalencyclopedia.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * فحص اتصال إنترنت (لا مجرد "متصل بشبكة" — قد تكون شبكة واي فاي محلية بلا
 * إنترنت فعلي). يُستخدم في SemoAssistantScreen *قبل* أي محاولة اتصال
 * بـ[OnlineAssistant.answer]، حتى لا تُهدر أي محاولة شبكة (ومهلتها) في حالة
 * انعدام الإنترنت أصلاً — فيسقط السؤال فوراً للبحث المحلي المعتاد بلا أي
 * تأخير محسوس، تماماً كما لو أن الوضع الذكي عبر الإنترنت غير موجود إطلاقاً.
 *
 * ═══ إصلاح خلل حقيقي أُبلغ عنه: جيمناي "يتوقف" فجأة (يرجع محلياً) بلا أي
 * سبب ظاهري إلا بتشغيل VPN، رغم أن الإنترنت (وحتى رابط بروكسي Gemini
 * نفسه) يعمل فعلياً بدونه ═══
 * السبب: كان الفحص يشترط أيضاً NET_CAPABILITY_VALIDATED — وهذه العلَم لا
 * يعني "يوجد إنترنت فعلي" بشكل عام، بل تحديداً "نجح أندرويد بالوصول لخادم
 * تحقق داخلي مخصَّص من Google لهذا الغرض حصراً". في بيئة شبكة يُحجَب فيها
 * هذا الخادم تحديداً (بينما باقي الإنترنت، بما فيه بروكسي Gemini، يعمل
 * فعلياً بلا مشكلة)، يُصنِّف أندرويد الشبكة خطأً كـ"غير مُتحقَّق منها"، فيوقف
 * هذا الفحص أي محاولة اتصال بجيمناي فوراً ويسقط للمحلي بصمت — رغم أن
 * الاتصال الفعلي كان سينجح تماماً لو حاولنا. تشغيل VPN ينجح بهذا التحقق
 * الداخلي (لأنه يغيّر مسار كل شيء بما فيها خادم التحقق نفسه)، فيُفتح الطريق
 * فجأة، مما يوحي خطأً أن VPN هو "الحل"، بينما هو فقط يتجاوز فحصاً داخلياً
 * صارماً أكثر من اللازم لا علاقة له بجيمناي أو بالبروكسي نفسه.
 * الإصلاح: الاكتفاء بـNET_CAPABILITY_INTERNET (الشبكة تُقر بوجود مسار
 * للإنترنت أصلاً) دون اشتراط نجاح ذلك التحقق تحديداً. لا خسارة حقيقية في
 * الحالة المعاكسة (لا إنترنت فعلاً): استدعاء [OnlineAssistant.answer] نفسه
 * له مهلة اتصال/قراءة محدودة (راجع CONNECT_TIMEOUT_MS/READ_TIMEOUT_MS
 * أدناه) ويلتقط أي استثناء ويعيد `null` بأمان، فيسقط السؤال للمحلي تلقائياً
 * بعد تلك المهلة القصيرة بدل السقوط الفوري — فرق غير محسوس عملياً، مقابل
 * إصلاح خلل حقيقي يمنع استخدام جيمناي كلياً حين يتوفر فعلاً.
 */
fun hasActiveInternetConnection(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
        false
    }
}

/**
 * الوضع الذكي عبر الإنترنت لسيمو: يستخدم نموذج Gemini المجاني من Google
 * (راجع توثيق [AiConfig.onlineEnabled] لسبب اختياره تحديداً — متاح فعلياً
 * من سوريا دون VPN منذ رفع الحظر عنه في سبتمبر 2026). طبقة إضافية فوق
 * [HerbAssistant] المحلي تماماً، لا بديلة عنه: أي فشل هنا (لا إنترنت فعلي،
 * انتهاء مهلة، مفتاح غير صالح، خطأ خادم) يُترجَم دوماً لـ`null` بدل رمي
 * استثناء أو عرض رسالة خطأ، فيعود المتصل (SemoAssistantScreen) تلقائياً
 * لبحث [HerbAssistant] المحلي المعتاد — بلا أي فرق يلاحظه المستخدم عن
 * السلوك بلا هذا الوضع إطلاقاً، كما طُلب تحديداً.
 */
object OnlineAssistant {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 20_000

    // حد أعلى لعدد الأعشاب المُرسَلة كسياق ضمن الطلب الواحد: نافذة سياق
    // Gemini المجانية تتسع لملايين الكلمات فعلياً، لكن تحديد سقف معقول هنا
    // يحمي من طلب ضخم غير ضروري (تكلفة بيانات على المستخدم في سوريا تحديداً
    // حيث الإنترنت غالباً محدود/بطيء) في حال كانت الموسوعة كبيرة جداً.
    private const val MAX_CONTEXT_HERBS = 150
    private const val MAX_CONTEXT_BLENDS = 60
    private const val MAX_FIELD_CHARS = 600

    suspend fun answer(
        question: String,
        herbs: List<Herb>,
        blends: List<Blend>,
        allowCompare: Boolean
    ): HerbAssistant.AssistantReply? = withContext(Dispatchers.IO) {
        val apiKey = AiConfig.onlineApiKey.trim()
        val model = AiConfig.onlineModel.trim().ifBlank { AiConfig.defaultOnlineModel }
        if (apiKey.isBlank() || question.isBlank()) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            // إن ترك المطوّر [AiConfig.onlineBaseUrl] فارغاً: اتصال مباشر بخوادم
            // Google كما كان دوماً. إن وضع عنوان بروكسي خاص به (مثال: خادم
            // Cloudflare Worker ينفّذ إعادة توجيه شفافة لنفس المسار)، يُستبدَل
            // به فقط الجزء الأساسي من الرابط بينما يبقى المسار والباراميترات
            // (`/v1beta/models/...?key=...`) كما هي تماماً — فيكفي أن يكون
            // البروكسي "مرآة" بسيطة تُعيد توجيه أي طلب يصلها بنفس المسار إلى
            // generativelanguage.googleapis.com الحقيقي وتُعيد الرد كما هو.
            val baseUrl = AiConfig.onlineBaseUrl.trim().trimEnd('/').ifBlank {
                "https://generativelanguage.googleapis.com"
            }
            val endpoint = URL(
                "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"
            )
            val body = buildRequestBody(question, herbs, blends, allowCompare)
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { out ->
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val rawResponse = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                ?: return@withContext null
            if (status !in 200..299) return@withContext null
            val text = extractReplyText(rawResponse)?.trim()
            if (text.isNullOrBlank()) null else HerbAssistant.AssistantReply(text, false)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * اختبار اتصال بسيط (سؤال قصير ثابت) تستخدمه أدوات المطور للتأكد من
     * صلاحية المفتاح والنموذج قبل الاعتماد عليهما فعلياً في المحادثة —
     * يعيد نص الرد نفسه عند النجاح، أو `null` عند أي فشل (نفس منطق [answer]).
     */
    suspend fun testConnection(): String? = withContext(Dispatchers.IO) {
        answer("مرحباً", emptyList(), emptyList(), false)?.text
    }

    private fun buildRequestBody(
        question: String,
        herbs: List<Herb>,
        blends: List<Blend>,
        allowCompare: Boolean
    ): JSONObject {
        val systemPrompt = buildString {
            append(
                "أنت سيمو، مساعد ذكي متخصص حصراً بموسوعة الأعشاب الطبية هذه. " +
                    "أجب بالعربية دوماً (استخدم نفس لهجة/فصحى السؤال قدر الإمكان)، بإيجاز ووضوح، " +
                    "واعتمد فقط على بيانات الأعشاب/الخلطات المرفقة أدناه من الموسوعة. " +
                    "إن لم تتوفر معلومة كافية ضمن هذه البيانات، وضّح ذلك صراحة بدل اختلاق إجابة. "
            )
            if (allowCompare) {
                append("إن طُلب منك مقارنة بين عنصرين أو أكثر، قارن بوضوح نقطة بنقطة. ")
            } else {
                append("لا تُنشئ مقارنة منظّمة بين عدة عناصر إلا إذا طلب المستخدم ذلك صراحة في سؤاله. ")
            }
            append("لا تذكر أنك نموذج Gemini أو أي تفاصيل تقنية عن كيفية عملك؛ أنت ببساطة \"سيمو\".\n\n")
            append("بيانات الموسوعة المتاحة لك:\n\n")
            herbs.take(MAX_CONTEXT_HERBS).forEach { h -> append(herbContextBlock(h)) }
            blends.take(MAX_CONTEXT_BLENDS).forEach { b -> append(blendContextBlock(b)) }
        }
        return JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", question)))
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 800)
                }
            )
        }
    }

    private fun trimField(text: String): String =
        if (text.length > MAX_FIELD_CHARS) text.take(MAX_FIELD_CHARS) + "…" else text

    private fun herbContextBlock(h: Herb): String = buildString {
        append("### عشبة: ${h.name}\n")
        if (h.benefits.isNotBlank()) append("الفوائد: ${trimField(h.benefits)}\n")
        if (h.usage.isNotBlank()) append("الاستخدام: ${trimField(h.usage)}\n")
        if (h.warnings.isNotBlank()) append("التحذيرات: ${trimField(h.warnings)}\n")
        if (h.harms.isNotBlank()) append("الأضرار: ${trimField(h.harms)}\n")
        if (h.notes.isNotBlank()) append("ملاحظات: ${trimField(h.notes)}\n")
        append("\n")
    }

    private fun blendContextBlock(b: Blend): String = buildString {
        append("### خلطة: ${b.name}\n")
        if (b.benefits.isNotBlank()) append("الفوائد: ${trimField(b.benefits)}\n")
        if (b.usage.isNotBlank()) append("الاستخدام: ${trimField(b.usage)}\n")
        if (b.warnings.isNotBlank()) append("التحذيرات: ${trimField(b.warnings)}\n")
        if (b.notes.isNotBlank()) append("ملاحظات: ${trimField(b.notes)}\n")
        append("\n")
    }

    private fun extractReplyText(rawJson: String): String? = try {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) null
        else {
            val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
            if (parts == null) null
            else {
                val sb = StringBuilder()
                for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text", ""))
                sb.toString().ifBlank { null }
            }
        }
    } catch (e: Exception) {
        null
    }
}
