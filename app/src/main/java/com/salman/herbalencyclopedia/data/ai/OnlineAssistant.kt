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
 * (Google AI Studio — بلا بطاقة ائتمان، راجع توثيق [AiConfig.onlineEnabled]
 * لتفاصيل رفع الحظر عنه في سوريا في سبتمبر 2026). يُستخدَم هنا معيار
 * Gemini REST الأصلي (`/v1beta/models/{model}:generateContent`)، والمفتاح
 * يُرسَل كترويسة `x-goog-api-key` (راجع توثيق [sendRequest] أدناه لسبب
 * التحوّل عن باراميتر `?key=` القديم)، وهذا ما يبنيه
 * [sendRequest]/[buildRequestBody] أدناه.
 * طبقة إضافية فوق [HerbAssistant] المحلي تماماً، لا بديلة عنه: أي فشل هنا
 * (لا إنترنت فعلي، انتهاء مهلة، مفتاح غير صالح، خطأ خادم) يُترجَم دوماً
 * لـ`null` بدل رمي استثناء أو عرض رسالة خطأ، فيعود المتصل
 * (SemoAssistantScreen) تلقائياً لبحث [HerbAssistant] المحلي المعتاد —
 * بلا أي فرق يلاحظه المستخدم عن السلوك بلا هذا الوضع إطلاقاً، كما طُلب
 * تحديداً.
 */
object OnlineAssistant {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 20_000

    // حد أعلى لعدد الأعشاب المُرسَلة كسياق ضمن الطلب الواحد: نافذة سياق
    // "gemini-flash-latest" تتسع لحوالي مليون توكن (أوسع بكثير من الحاجة
    // الفعلية)، لكن تحديد سقف معقول هنا يحمي من طلب ضخم غير ضروري (تكلفة
    // بيانات على المستخدم، خصوصاً في مناطق إنترنت محدود/بطيء) في حال كانت
    // الموسوعة كبيرة جداً.
    private const val MAX_CONTEXT_HERBS = 150
    private const val MAX_CONTEXT_BLENDS = 60
    private const val MAX_FIELD_CHARS = 600

    /**
     * ═══ خبرة سابقة محفوظة: ردود سيمو كانت تُقصّ بمنتصف الجملة عند إجابات
     * طويلة (خصوصاً مقارنة عدة أعشاب — فوائد + تحذيرات + أضرار + طريقة
     * استخدام لكل عنصر، بالعربية التي تستهلك توكنات أكثر من الإنجليزية
     * للمعنى نفسه) كلما كان سقف طول الرد (منفصل كلياً عن سعة نافذة السياق،
     * راجع [MAX_CONTEXT_HERBS] أعلاه) ضيقاً جداً ═══
     * لتفادي تكرار نفس الخلل: [MAX_OUTPUT_TOKENS] مضبوط على 8192 (يُرسَل
     * كـ"maxOutputTokens" ضمن "generationConfig" في [buildRequestBody] —
     * حقل Gemini القياسي لطول الرد نفسه)، هامش واسع يكفي لمقارنة مفصَّلة بين
     * عدة عناصر معاً بالعربية بارتياح. وكخط دفاع أخير إن استُهلك حتى هذا
     * السقف بإجابة ضخمة جداً، [extractReplyText] أدناه يتحقق من finishReason
     * ويُلحق ملاحظة صريحة بنهاية النص المقتطَع بدل عرضه كأنه رد كامل عادي
     * بلا أي إشارة — فرق بين "إجابة تبدو غريبة الانتهاء بلا تفسير" و"إجابة
     * تشرح صراحة أنها اختُصرت". يُطبَّق هذا على مسار الدردشة الفعلي [answer]
     * وعلى [testConnectionVerbose] معاً.
     */
    private const val MAX_OUTPUT_TOKENS = 8192

    suspend fun answer(
        question: String,
        herbs: List<Herb>,
        blends: List<Blend>,
        allowCompare: Boolean
    ): HerbAssistant.AssistantReply? = withContext(Dispatchers.IO) {
        if (question.isBlank()) return@withContext null
        val rawResponse = try {
            sendRequest(question, herbs, blends, allowCompare)
        } catch (e: Exception) {
            return@withContext null
        }
        val text = extractReplyText(rawResponse)?.trim()
        if (text.isNullOrBlank()) null else HerbAssistant.AssistantReply(text, false)
    }

    /**
     * ═══ إصلاح خلل حقيقي أُبلغ عنه: "اختبار الاتصال" في أدوات المطور كان
     * يعرض دوماً نفس الرسالة العامة "تعذّر الاتصال" مهما كان سبب الفشل
     * الحقيقي — رابط بروكسي مكتوب غلط (مثال واقعي: لوحة مفاتيح أندرويد
     * صحّحت/بدّلت حرفاً تلقائياً بحقل الرابط بلا أن ينتبه المطوّر)، مفتاح
     * غير صالح، خطأ من الخادم نفسه (أو من بروكسي وسيط نفدت حصته المجانية)،
     * أو الطلب لم يصل أصلاً لأي خادم. لا فرق بينها من واجهة "تعذّر الاتصال"
     * وحدها، فيصعب معرفة أين المشكلة فعلياً (كما حصل: سجلات Cloudflare
     * Worker لم تُظهر وصول أي طلب إطلاقاً — أي أن الفشل حصل محلياً على الجهاز
     * قبل أي محاولة شبكة حقيقية، غالباً بسبب رابط بروكسي غير صالح بنيوياً
     * بعد تصحيح تلقائي من لوحة المفاتيح). الآن يُعاد سبب الفشل الفعلي (نوع
     * الاستثناء ورسالته، أو رمز حالة رد الخادم) ليظهر مباشرة بواجهة الاختبار، دون أي تغيير على السلوك الصامت
     * لـ[answer] المستخدَم فعلياً في الدردشة العادية (لا يزال دوماً يُرجع
     * `null` بصمت عند أي فشل هناك).
     */
    suspend fun testConnectionVerbose(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val raw = sendRequest("مرحباً", emptyList(), emptyList(), false)
            val text = extractReplyText(raw)?.trim()
            if (text.isNullOrBlank()) false to "وصل رد من الخادم لكن بلا نص إجابة واضح — الرد الخام: ${raw.take(300)}"
            else true to text
        } catch (e: Exception) {
            false to "${e.javaClass.simpleName}: ${e.message ?: "بلا تفاصيل إضافية"}"
        }
    }

    /** توافقاً مع الاستدعاءات القديمة (تُبقي نفس السلوك الصامت لـ[answer]). */
    suspend fun testConnection(): String? = withContext(Dispatchers.IO) {
        answer("مرحباً", emptyList(), emptyList(), false)?.text
    }

    /**
     * تُطبِّع رابط البروكسي/المرآة الذي يدخله المطوّر قبل استخدامه: تحذف أي
     * مسافات أو أسطر جديدة قد تُقحمها لوحة مفاتيح أندرويد تلقائياً (تصحيح
     * تلقائي/اقتراح كلمة) دون أن يلاحظ المطوّر، وتضيف "https://" تلقائياً
     * إن كتب المطوّر اسم النطاق فقط بلا بروتوكول (مثال: "xxx.workers.dev").
     */
    private fun normalizedBaseUrl(): String {
        val cleaned = AiConfig.onlineBaseUrl.replace(Regex("\\s+"), "").trimEnd('/')
        if (cleaned.isBlank()) return "https://generativelanguage.googleapis.com"
        return if (cleaned.startsWith("http://", ignoreCase = true) ||
            cleaned.startsWith("https://", ignoreCase = true)
        ) cleaned else "https://$cleaned"
    }

    /**
     * ينفّذ طلب الاتصال الفعلي بـGemini (مباشرة أو عبر بروكسي المطوّر) ويعيد
     * نص الرد الخام، أو يرمي استثناءً واضحاً عند أي فشل (رابط غير صالح، رد
     * غير ناجح من الخادم، انقطاع شبكة...). لا يُستخدم مباشرة من واجهة
     * الدردشة — [answer] يغلّفه بصمت، و[testConnectionVerbose] يعرض تفاصيل
     * فشله كما هي لأدوات المطور.
     */
    private fun sendRequest(
        question: String,
        herbs: List<Herb>,
        blends: List<Blend>,
        allowCompare: Boolean
    ): String {
        val apiKey = AiConfig.onlineApiKey.trim()
        val model = AiConfig.onlineModel.trim().ifBlank { AiConfig.defaultOnlineModel }
        require(apiKey.isNotBlank()) { "مفتاح Gemini API فارغ" }
        var connection: HttpURLConnection? = null
        try {
            // إن ترك المطوّر [AiConfig.onlineBaseUrl] فارغاً: اتصال مباشر بخوادم
            // Gemini (generativelanguage.googleapis.com) كما هو مضبوط افتراضياً.
            // إن وضع عنوان بروكسي خاص به (مثال: خادم Cloudflare Worker ينفّذ
            // إعادة توجيه شفافة لنفس المسار)، يُستبدَل به فقط الجزء الأساسي من
            // الرابط بينما يبقى المسار (`/v1beta/models/{model}:generateContent`)
            // كما هو تماماً — فيكفي أن يكون البروكسي "مرآة" بسيطة تُعيد توجيه
            // أي طلب يصلها بنفس المسار والباراميترات إلى Google الحقيقي وتُعيد
            // الرد كما هو.
            val endpoint = URL("${normalizedBaseUrl()}/v1beta/models/$model:generateContent")
            val body = buildRequestBody(question, herbs, blends, allowCompare, model)
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                // ═══ إصلاح خلل حقيقي أُبلغ عنه (سبتمبر 2026): طلبات Gemini كانت
                // تفشل دوماً بخطأ 401 "Request had invalid authentication
                // credentials. Expected OAuth 2 access token..." رغم أن المفتاح
                // صحيح فعلياً ═══
                // السبب: مفاتيح Gemini API الجديدة الصادرة من AI Studio تُصاغ
                // الآن بصيغة "AQ." (Auth key) بدل الصيغة القديمة "AIzaSy..."
                // (Standard key) — وغوغل أوقفت قبول مفاتيح Standard القديمة كلياً
                // بحلول سبتمبر 2026 (فتصدر AI Studio مفاتيح AQ. حصراً لأي مستخدم
                // جديد أو مفتاح جديد من الآن فصاعداً). مفاتيح AQ. هذه تُرفَض
                // تحديداً حين تُرسَل كباراميتر ?key= بالرابط (وهذا ما كان يفعله
                // هذا الملف سابقاً)، بينما توثيق Google الرسمي الحالي لمفاتيح
                // Gemini API يبني كل أمثلته الآن على إرسال المفتاح كترويسة HTTP
                // باسم "x-goog-api-key" بدلاً من ذلك — وهذه الطريقة تعمل مع
                // الصيغتين معاً (القديمة والجديدة على السواء)، فلا حاجة لتمييز
                // نوع المفتاح هنا إطلاقاً.
                // الإصلاح: التحوّل الكامل لترويسة x-goog-api-key، وحذف ?key= من
                // الرابط نهائياً (لم يعد endpoint أعلاه يتضمنه، ولا حاجة بالتالي
                // لترميز URL-encode للمفتاح الذي كان يخدم فقط ذلك الباراميتر).
                setRequestProperty("x-goog-api-key", apiKey)
            }
            connection.outputStream.use { out ->
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val rawResponse = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                ?: throw java.io.IOException("لا يوجد أي رد من الخادم (حالة $status)")
            if (status !in 200..299) {
                throw java.io.IOException("رد الخادم بحالة فشل $status: ${rawResponse.take(300)}")
            }
            return rawResponse
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildRequestBody(
        question: String,
        herbs: List<Herb>,
        blends: List<Blend>,
        allowCompare: Boolean,
        model: String
    ): JSONObject {
        val systemPrompt = buildString {
            append(
                "أنت سيمو، خبير متخصص بعلم العقاقير والنباتات الطبية (Role Prompting)، " +
                    "حصراً ضمن حدود موسوعة الأعشاب الطبية هذه. " +
                    "أجب بالعربية دوماً (استخدم نفس لهجة/فصحى السؤال قدر الإمكان)، بإيجاز ووضوح، " +
                    "واعتمد فقط على بيانات الأعشاب/الخلطات المرفقة أدناه من الموسوعة. " +
                    "إن لم تتوفر معلومة كافية ضمن هذه البيانات، وضّح ذلك صراحة بدل اختلاق إجابة. "
            )
            if (allowCompare) {
                append("إن طُلب منك مقارنة بين عنصرين أو أكثر، قارن بوضوح نقطة بنقطة. ")
            } else {
                append("لا تُنشئ مقارنة منظّمة بين عدة عناصر إلا إذا طلب المستخدم ذلك صراحة في سؤاله. ")
            }
            // ═══ ميزة أُبلغ عن طلبها: تحسين إضافي لتوجيه سيمو، يضيف صرامة
            // منهجية (فحص دقة صريح، تصحيح الفرضيات الخاطئة، نسق مخرجات
            // ثابت، منع الحشو العاطفي) فوق مبادئ الصياغة السابقة — بدون
            // تكرارها، بل دمجها بقائمة واحدة متماسكة ═══
            append(
                "اتّبع هذه المبادئ دوماً عند صياغة أي إجابة:\n" +
                    "1) التفكير المنطقي المتسلسل (Chain of Thought) وعدم الاختصار: " +
                    "حلّل السؤال داخلياً خطوة بخطوة قبل الرد (بلا كتابة هذا التحليل بالرد " +
                    "نفسه)، ولا تختصر أو تُسقط أي جزء منطقي مهم (كالتحذيرات أو طريقة " +
                    "الاستخدام) إلا إذا طلب المستخدم الاختصار صراحة.\n" +
                    "2) التحقق من صحة الفرضية (Premise Verification): إن تضمّن سؤال " +
                    "المستخدم افتراضاً تُخالفه بيانات الموسوعة المرفقة (مثال: افترض أمان " +
                    "استخدام عشبة أو مزجها بأخرى وبيانات الموسوعة تنص على تحذير من ذلك)، " +
                    "صحّح هذا الافتراض بوضوح ومباشرة في بداية ردك بدل تجاهله أو الموافقة عليه ضمناً.\n" +
                    "3) فحص الدقة والإفصاح عن الشك (Fact Checking): اعتمد فقط على بيانات " +
                    "الموسوعة المرفقة أدناه؛ إن كانت المعلومة غير موجودة ضمنها أو غير كافية، " +
                    "صرّح بذلك صراحة بدل التخمين أو اختلاق معلومة أو مصدر غير موثّق.\n" +
                    "4) نسق المخرجات: عند الأسئلة التي تحتاج شرحاً (لا الترحيب أو الأسئلة " +
                    "البسيطة جداً)، رتّب ردّك بهذا التسلسل: الإجابة المباشرة أولاً، ثم الشرح " +
                    "الداعم من بيانات الموسوعة، ثم التحذيرات/الحالات الخاصة (Edge Cases) إن " +
                    "وُجدت — بنقاط مختصرة أو عناوين فرعية، وجدول مقارنة فقط إن طُلبت مقارنة صراحة.\n" +
                    "5) قيود صارمة: بلا مقدمات أو جمل عاطفية تمهيدية أو مجاملات؛ إن لم يحدد " +
                    "السؤال مستوى الجمهور، افترض قارئاً غير متخصص وابسّط أي مصطلح طبي معقد.\n" +
                    "6) التعمّق عند الطلب: إن طلب المستخدم توسيع نقطة معينة (مثال: \"وسّع " +
                    "النقطة رقم 2\")، ركّز ردّك على تلك النقطة تحديداً بتفصيل وأمثلة عملية من " +
                    "بيانات الموسوعة، دون إعادة سرد بقية النقاط.\n"
            )
            append("لا تذكر اسم النموذج التقني الذي تعمل عليه ولا أي تفاصيل تقنية عن كيفية عملك؛ أنت ببساطة \"سيمو\".\n\n")
            append("بيانات الموسوعة المتاحة لك:\n\n")
            herbs.take(MAX_CONTEXT_HERBS).forEach { h -> append(herbContextBlock(h)) }
            blends.take(MAX_CONTEXT_BLENDS).forEach { b -> append(blendContextBlock(b)) }
        }
        // معيار Gemini REST الأصلي يفصل توجيه النظام ("system_instruction") عن
        // محتوى المحادثة ("contents" — قائمة أدوار "user"/"model"، كل عنصر
        // بصيغة "parts": [{"text": ...}])، لا قائمة "messages" الموحّدة
        // كمعيار OpenAI. "maxOutputTokens" و"temperature" يقعان ضمن كائن
        // فرعي "generationConfig" (راجع توثيق [MAX_OUTPUT_TOKENS] أعلاه) بدل
        // حقول علوية مباشرة. النموذج الافتراضي ("gemini-flash-latest") لا
        // "تفكير" داخلي خفي يلتهم من نفس حصة الرد هنا، فلا حاجة لأي إعداد
        // "thinkingConfig" إضافي. لم تُضَف "safetySettings" هنا عمداً (تُبقي
        // سلوك مرشِّحات Google الافتراضية)؛ إن قطع Gemini أي رد بسبب ذلك،
        // يظهر عبر finishReason="SAFETY" الذي يلتقطه [extractReplyText]
        // أدناه بنفس آلية التقاط finishReason="MAX_TOKENS" (اقتطاع بسبب طول
        // الرد).
        return JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemPrompt) }) })
                }
            )
            put(
                "contents",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply { put(JSONObject().apply { put("text", question) }) })
                        }
                    )
                }
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", MAX_OUTPUT_TOKENS)
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

    /**
     * راجع توثيق [MAX_OUTPUT_TOKENS] في [buildRequestBody] أعلاه: يتحقق هنا
     * من finishReason (حقل Gemini القياسي) لأول عنصر (candidate) — أي قيمة
     * غير "STOP" الطبيعية (أبرزها "MAX_TOKENS" و"SAFETY") تعني أن الرد
     * اقتُطع فعلياً قبل اكتماله، فتُلحَق ملاحظة صريحة ومختلفة لكل سبب بدل
     * عرض النص الجزئي وكأنه اكتمل بشكل طبيعي بلا أي تفسير.
     */
    private fun extractReplyText(rawJson: String): String? = try {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) null
        else {
            val candidate = candidates.getJSONObject(0)
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            val text = if (parts != null && parts.length() > 0) {
                buildString { for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text")) }
            } else null
            if (text.isNullOrBlank()) null
            else {
                val sb = StringBuilder(text)
                when (candidate.optString("finishReason")) {
                    "MAX_TOKENS" -> sb.append("\n\n[الرد طويل جداً واقتُطع هنا — جرّب صياغة أضيق للسؤال]")
                    "SAFETY" -> sb.append("\n\n[توقّف الرد هنا بسبب مرشّحات الخادم — جرّب إعادة صياغة السؤال]")
                }
                sb.toString().ifBlank { null }
            }
        }
    } catch (e: Exception) {
        null
    }
}
