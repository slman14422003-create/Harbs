package com.salman.herbalencyclopedia.data.ai

import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Herb
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * إعدادات قابلة للتعديل من "أدوات المطور" داخل التطبيق (AdminToolsScreen)
 * دون الحاجة لإعادة بناء التطبيق: عتبات التشابه وكلمات الإيقاف الإضافية.
 * القيم تُحمَّل من PreferencesRepository عند بدء التطبيق وتُطبَّق هنا مباشرة،
 * وأي تعديل من شاشة الأدوات يُحدّثها فوراً وحياً في نفس الجلسة (بلا حاجة
 * لإعادة تشغيل) لأن كل الشاشات تقرأ من هذا الكائن.
 */
object AiConfig {
    /** بُعد تجميع النقاط المتشابهة من أعشاب مختلفة في نقطة مشتركة واحدة (0..1). */
    var similarityThreshold: Double = 0.34
        set(value) { field = value.coerceIn(0.05, 0.95) }

    /** حد قبول نتيجة في البحث الحر عن سؤال المستخدم (0..1). قيمة أقل = إجابات أكثر لكن أقل دقة. */
    var searchThreshold: Double = 0.12
        set(value) { field = value.coerceIn(0.02, 0.9) }

    /** كلمات إيقاف إضافية يضيفها المطوّر (تُستبعد من التحليل، بصيغة مُطبَّعة أو خام). */
    var extraStopWords: Set<String> = emptySet()

    /**
     * مرادفات يعلّمها المطوّر لسيمو: كل مُدخل هو "كلمة جديدة → الكلمة القياسية
     * التي يجب أن يفهمها سيمو بدلاً منها" (مثال: "ينفع" → "فائدة"). تُستخدم
     * في كل عمليات تحليل النص (المقارنة والبحث الحر وأمثلة التدريب أدناه)،
     * فتوسّع فهم سيمو للعبارات المرادفة بلا حاجة لتغيير أي كود.
     */
    var synonyms: Map<String, String> = emptyMap()

    /**
     * "حالات مدرَّبة" يضيفها المطوّر يدوياً: سؤال نموذجي مع الرد المطلوب
     * بالضبط. عند سؤال المستخدم شيئاً مشابهاً بدرجة كافية لأحد هذه الأمثلة
     * (حسب [trainedMatchThreshold])، يرد سيمو بالنص المدرَّب مباشرة بدل
     * الاعتماد على المنطق العام — وهذه هي آلية "تطوير النماذج وفهم الحالات"
     * اليدوية: تعليم مباشر بلا إعادة بناء التطبيق.
     */
    var trainedExamples: List<TrainedExample> = emptyList()

    /** حد التشابه (0..1) الذي يجب أن تبلغه رسالة المستخدم مع مثال مدرَّب ليُستخدم رده مباشرة. */
    var trainedMatchThreshold: Double = 0.45
        set(value) { field = value.coerceIn(0.1, 0.95) }

    /**
     * حالات "تعلّمها سيمو بنفسه" من واقع الاستخدام: عندما يجيب سيمو بحرية
     * (بحث حر ضمن بيانات الموسوعة) ويُقيّم المستخدم الرد بـ 👍 في شاشة
     * الدردشة، تُحفَظ نقطة السؤال↔الرد هنا تلقائياً — فتصبح إجابته على أسئلة
     * مشابهة لاحقاً فورية وواثقة دون أي تدخل من المطوّر. هذا هو المعنى
     * العملي لعبارة "نموذج يعتمد على الموسوعة ويطوّر نفسه بنفسه": كل حالة
     * متعلَّمة مصدرها إجابة استُخرجت أصلاً من نصوص الموسوعة، ومصدر الثقة بها
     * هو تقييم صريح من مستخدم حقيقي، وليس تخميناً. تبقى منفصلة عن
     * [trainedExamples] (تدريب المطوّر اليدوي المحمي دوماً)، وقابلة للمراجعة
     * أو الحذف أو "الترقية" لتدريب يدوي دائم من أدوات المطور.
     */
    var autoLearnedExamples: List<TrainedExample> = emptyList()

    /** تفعيل/تعطيل التعلّم الذاتي من تقييمات المستخدمين (👍/👎) في شاشة الدردشة. */
    var autoLearnEnabled: Boolean = true

    val defaultSimilarityThreshold = 0.34
    val defaultSearchThreshold = 0.12
    val defaultTrainedThreshold = 0.45
    val defaultAutoLearnEnabled = true

    fun resetToDefaults() {
        similarityThreshold = defaultSimilarityThreshold
        searchThreshold = defaultSearchThreshold
        extraStopWords = emptySet()
        synonyms = emptyMap()
        trainedExamples = emptyList()
        trainedMatchThreshold = defaultTrainedThreshold
        autoLearnedExamples = emptyList()
        autoLearnEnabled = defaultAutoLearnEnabled
    }
}

/** حالة تدريب واحدة: سؤال نموذجي والرد المخصّص الذي يجب أن يعطيه سيمو له. */
data class TrainedExample(val pattern: String, val response: String)

/**
 * سيمو — المساعد الذكي للموسوعة. يعمل بالكامل داخل الجهاز، بلا اتصال
 * إنترنت، بلا مفتاح API، وبلا أي تكلفة أو إعداد. يقرأ نصوص الأعشاب الموجودة
 * أصلاً في الموسوعة (الفوائد، الاستخدام، التحذيرات، الأضرار، الملاحظات)،
 * يحلّلها، يجيب بحرية على أي سؤال، ولا يبني مقارنة منظّمة بين أكثر من عشبة
 * إلا عندما يُطلب منه ذلك صراحة (باختيار عشبتين أو ذكرهما بالاسم في السؤال).
 *
 * "تدريبه" يحدث على ثلاثة مستويات:
 * 1) يدوياً من أدوات المطور (مرادفات + حالات مدرَّبة، انظر [AiConfig]).
 * 2) قاموس عربي عام مرفق محلياً مع التطبيق ([DictionaryLexicon]، مبني من
 *    Rabih Dictionary وArabic WordNet) يوسّع فهم الكلمات والمرادفات العامة
 *    بلا أي تدخل يدوي وبلا إنترنت — تفصيل ذلك في توثيق [DictionaryLexicon].
 * 3) ذاتياً أثناء الاستخدام الفعلي: [CorpusIndex] يُبنى تلقائياً من نصوص
 *    الموسوعة نفسها ليكتشف أوزان الكلمات وعلاقاتها الضمنية دون أي تدخل
 *    يدوي، و[recordFeedback] يحوّل تقييمات المستخدمين (👍) على إجابات
 *    البحث الحر إلى حالات مدرَّبة تلقائياً — أي أن سيمو يعتمد بالكامل على
 *    بيانات الموسوعة، ثم يراكم فوقها خبرة من استخدامه الفعلي.
 * لا يوجد هنا نموذج شبكة عصبية يحتاج تدريباً فعلياً؛ هذا "تعلّم" رمزي بحت
 * (إحصائي + تغذية راجعة) مناسب لتشغيل محلي بالكامل دون إنترنت أو معالجة ثقيلة.
 */
object HerbAssistant {

    // ── أدوات معالجة نصوص عربية بسيطة ──────────────────────────────────

    private val baseStopWords = setOf(
        "من", "في", "على", "الى", "إلى", "عن", "مع", "هذا", "هذه", "ذلك", "تلك",
        "التي", "الذي", "و", "أو", "او", "ثم", "قد", "لا", "لم", "لن", "كان",
        "يكون", "تكون", "بعض", "كل", "أي", "اي", "ما", "هل", "أن", "ان", "كما",
        "حيث", "بين", "بعد", "قبل", "عند", "أيضا", "ايضا", "جدا", "جداً",
        "يمكن", "يجب", "يفضل", "غير", "دون", "بدون", "لها", "له", "بها", "به"
    )

    private val stopWords: Set<String>
        get() = if (AiConfig.extraStopWords.isEmpty()) baseStopWords
                else baseStopWords + AiConfig.extraStopWords.map { normalize(it) }

    private fun normalize(text: String): String {
        var t = text
        t = t.replace(Regex("[\\u064B-\\u0652]"), "") // إزالة التشكيل
        t = t.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        t = t.replace('ى', 'ي').replace('ة', 'ه')
        t = t.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        return t.trim().lowercase()
    }

    private fun wordsOf(text: String): Set<String> {
        val base = normalize(text).split(Regex("\\s+"))
            .filter { it.length > 1 && it !in stopWords }
            .toSet()
        return applySynonyms(base)
    }

    /**
     * يستبدل كل كلمة بمرادفها القياسي إن وُجد في [AiConfig.synonyms] (بعد
     * تطبيع الطرفين)، بحيث تُحسب "ينفع" و"يفيد" مثلاً ككلمة واحدة أثناء أي
     * مقارنة أو بحث — هذا هو أثر "تعليم سيمو كلمات جديدة" يدوياً على أرض الواقع.
     */
    private fun applySynonyms(words: Set<String>): Set<String> {
        if (AiConfig.synonyms.isEmpty()) return words
        val table = AiConfig.synonyms.entries.associate { (k, v) -> normalize(k) to normalize(v) }
        if (table.isEmpty()) return words
        return words.map { table[it] ?: it }.toSet()
    }

    /** يقسّم فقرة حرة إلى نقاط قصيرة قابلة للمقارنة والعرض كعناصر منفصلة. */
    private fun splitPoints(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text.split(Regex("[،,.\\n؛;]|\\s-\\s"))
            .map { it.trim().trim('-', ' ') }
            .filter { it.length > 2 }
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val inter = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 0.0 else inter / union
    }

    private fun containsAny(normalizedText: String, terms: List<String>): Boolean =
        terms.any { normalizedText.contains(normalize(it)) }

    private fun herbNames(herbs: List<Herb>): String = herbs.joinToString(" و") { it.name }

    // ── فهرس "مفهوم تلقائياً" من نصوص الموسوعة نفسها ────────────────────

    /**
     * فهرس يُبنى تلقائياً من نصوص الموسوعة نفسها (لا يحتاج أي تدخل يدوي)
     * ليمنح سيمو فهماً أعمق من مجرد تقاطع كلمات بسيط:
     *
     * 1) وزن كل كلمة (IDF تقريبي): كلمة نادرة الورود عبر كل نقاط الموسوعة
     *    (اسم عرَض أو استخدام مميّز) تعني أكثر من كلمة شائعة جداً (كـ"يساعد")،
     *    فتُعطى وزناً أعلى عند حساب التشابه بين سؤال المستخدم ونص الموسوعة.
     * 2) علاقات ضمنية بين الكلمات: كلمتان تتكرران معاً كثيراً نسبياً داخل
     *    نفس النقطة عبر أعشاب مختلفة تُعتبر "مرتبطتين" تلقائياً (نسخة مبسّطة
     *    من مقياس PMI)، فيتوسّع فهم سؤال المستخدم بها دون أي مرادف يُضاف
     *    يدوياً من المطوّر — تعلّم توزيعي بحت من بيانات الموسوعة ذاتها.
     *
     * يُعاد بناؤه فقط عند تغيّر قائمة الأعشاب أو الخلطات المرجعة (انظر
     * [corpusIndexFor]).
     *
     * يُبنى الآن من قائمة نصوص خام مسطّحة ([fieldTexts]) بدل قائمة أعشاب
     * مباشرة، بحيث يمكن تغذيته بنصوص الأعشاب *والخلطات* معاً — وهذا هو ما
     * يجعل "فهم" سيمو (أوزان الكلمات وعلاقاتها الضمنية) يشمل كل الموسوعة
     * فعلياً بدل الاقتصار على الأعشاب وحدها.
     */
    private class CorpusIndex(fieldTexts: List<String>) {
        private val idf: Map<String, Double>
        private val related: Map<String, List<String>>

        init {
            val points = mutableListOf<Set<String>>()
            fieldTexts.forEach { field ->
                splitPoints(field).forEach { p ->
                    val w = wordsOf(p)
                    if (w.isNotEmpty()) points += w
                }
            }
            val docCount = points.size.coerceAtLeast(1)
            val df = mutableMapOf<String, Int>()
            points.forEach { pts -> pts.forEach { w -> df[w] = (df[w] ?: 0) + 1 } }
            idf = df.mapValues { (_, d) -> ln(docCount.toDouble() / d.toDouble() + 1.0) }

            val coOccur = mutableMapOf<Pair<String, String>, Int>()
            points.forEach { pts ->
                val list = pts.toList()
                for (i in list.indices) for (j in i + 1 until list.size) {
                    val a = list[i]; val b = list[j]
                    val key = if (a < b) a to b else b to a
                    coOccur[key] = (coOccur[key] ?: 0) + 1
                }
            }
            val relatedMap = mutableMapOf<String, MutableList<Pair<String, Double>>>()
            coOccur.forEach { (pair, count) ->
                if (count < 2) return@forEach // تحوّطاً من تطابق عابر لمرة واحدة
                val (a, b) = pair
                val score = count.toDouble() / sqrt((df[a] ?: 1).toDouble() * (df[b] ?: 1).toDouble())
                if (score >= 0.5) {
                    relatedMap.getOrPut(a) { mutableListOf() } += b to score
                    relatedMap.getOrPut(b) { mutableListOf() } += a to score
                }
            }
            related = relatedMap.mapValues { (_, l) -> l.sortedByDescending { it.second }.take(3).map { it.first } }
        }

        fun weightOf(word: String): Double = idf[word] ?: 1.0

        /** يوسّع كلمات السؤال بالعلاقات المكتشَفة تلقائياً (إضافة فهم ضمني، لا حذف). */
        fun expand(words: Set<String>): Set<String> =
            if (words.isEmpty()) words else words + words.flatMap { related[it].orEmpty() }
    }

    // ── تدريب سيمو الافتتاحي: أكثر من 500 صياغة محادثة جاهزة ────────────

    /**
     * "تدريب سيمو الافتتاحي" — أكثر من 500 صياغة عربية/عامية مختلفة (تحية،
     * شكر، وداع، سؤال عن الحال، هوية سيمو، قدراته، اعتذار، مجاملة، ورد
     * بسيط بنعم/لا) يتعرّف عليها سيمو مباشرة فور تثبيت التطبيق، دون أي حاجة
     * لبحث حر في الموسوعة ودون أي تدخل من المطوّر بعد التثبيت — هذا هو
     * "تدريبه على أكثر من 500 سؤال" كما طُلب، منفصل تماماً عن
     * [AiConfig.trainedExamples] (تدريب المطوّر اليدوي القابل للتعديل من
     * أدوات المطور) و[AiConfig.autoLearnedExamples] (تعلّمه الذاتي من
     * تقييمات المستخدمين). الصياغات تُبنى من حاصل ضرب (كلمات أساسية ×
     * لواحق شائعة) بدل كتابة كل صياغة يدوياً، فتغطّي مئات الاحتمالات
     * الواقعية التي يكتبها المستخدم فعلاً بأقل قدر من التكرار في الكود.
     */
    private object ConversationalSeed {
        private data class Category(
            val words: List<String>,
            val suffixes: List<String>,
            val responses: List<String>,
            /** مطابقة تامة فقط (بلا startsWith) — للعبارات القصيرة جداً القابلة للالتباس مثل "لا". */
            val exactOnly: Boolean = false
        )

        private val greeting = Category(
            words = listOf(
                "مرحبا", "مرحباً", "مرحبتين", "هلا", "هلا بيك", "هلا فيك", "أهلا", "اهلا",
                "أهلا وسهلا", "اهلا وسهلا", "اهلين", "يا هلا", "صباح الخير", "صباح النور",
                "مساء الخير", "مساء النور", "السلام عليكم", "وعليكم السلام", "هاي", "هالو",
                "hello", "hi", "hey", "يا سيمو"
            ),
            suffixes = listOf("", " سيمو", " يا سيمو", " كيف الحال", "؟", " فيك خير", " يا صديقي"),
            responses = listOf(
                "أهلاً 👋 أنا سيمو، مساعدك الذكي في عالم الأعشاب. اسألني عن أي عشبة تريدها: فوائدها، طريقة استخدامها، تحذيراتها، أو اطلب مني مقارنة بين أكثر من عشبة، وسأجيبك فوراً من بيانات الموسوعة.",
                "هلا فيك 🌿 سيمو حاضر، جاهز أساعدك بأي سؤال عن الأعشاب في الموسوعة."
            )
        )

        private val thanks = Category(
            words = listOf(
                "شكرا", "شكراً", "شكرا الك", "شكرا كتير", "يعطيك العافية", "يعطيك الف عافية",
                "تسلم", "تسلم ايدك", "تسلمي", "الله يعطيك العافية", "مشكور", "مشكورة",
                "ممنون", "ممنونة", "جزاك الله خير", "تسلملي", "الله يخليك", "يسلمو"
            ),
            suffixes = listOf("", " سيمو", " كتير", " جدا", " يا سيمو", "!"),
            responses = listOf(
                "عفواً 🌿 أنا سيمو، دائماً هنا لأي سؤال آخر عن الأعشاب.",
                "العفو 🌱 سعيد إني قدرت أساعدك، تحت أمرك بأي وقت."
            )
        )

        private val farewell = Category(
            words = listOf(
                "مع السلامة", "باي", "وداعا", "الى اللقاء", "إلى اللقاء", "تصبح على خير",
                "تصبحين على خير", "نهارك سعيد", "بشوفك", "نلتقي لاحقا", "سلام", "bye"
            ),
            suffixes = listOf("", " سيمو", " يا سيمو", "!"),
            responses = listOf(
                "مع السلامة 🌿 ارجع أي وقت تحتاج فيه سيمو لسؤال عن الأعشاب.",
                "إلى اللقاء 🌱 سيمو موجود دائماً هنا لما تحتاجني."
            )
        )

        private val wellbeing = Category(
            words = listOf(
                "كيفك", "كيف حالك", "شلونك", "شو اخبارك", "اخبارك ايه", "عامل ايه",
                "كيف الصحة", "كيفك اليوم", "انت منيح", "شو مسوي", "ايش الأخبار", "شخبارك"
            ),
            suffixes = listOf("", " سيمو", " يا سيمو", "؟"),
            responses = listOf(
                "الحمد لله تمام 🌿 أنا سيمو وجاهز دائماً، كيف أقدر أساعدك اليوم بموضوع الأعشاب؟",
                "بخير وجاهز أساعدك 🌱 شو الموضوع اللي حابب تسألني عنه؟"
            )
        )

        private val identity = Category(
            words = listOf(
                "من انت", "مين انت", "شو اسمك", "ما اسمك", "انت مين", "عرفني فيك",
                "احكيلي عنك", "مين سيمو", "who are you", "what is your name"
            ),
            suffixes = listOf("", "؟", " سيمو"),
            responses = listOf(
                "أنا سيمو 🌿، المساعد الذكي لموسوعة الأعشاب. أعمل بالكامل على جهازك دون إنترنت، وأجيبك من بيانات الأعشاب الموجودة في الموسوعة عن الفوائد والاستخدام والتحذيرات."
            )
        )

        private val capability = Category(
            words = listOf(
                "شو تقدر تعمل", "ماذا تستطيع ان تفعل", "ايش بتعرف تعمل", "شو بتعرف",
                "ما هي قدراتك", "كيف تساعدني", "بماذا تساعدني", "وش تسوي",
                "ماذا تفعل", "what can you do"
            ),
            suffixes = listOf("", "؟", " سيمو"),
            responses = listOf(
                "أقدر أجاوبك عن فوائد أي عشبة في الموسوعة، طريقة استخدامها، تحذيراتها وأضرارها، وأقدر كمان أقارن بين أكثر من عشبة إذا طلبت ذلك صراحة — جرّب اسألني عن اسم عشبة مباشرة."
            )
        )

        private val apology = Category(
            words = listOf("اسف", "آسف", "اسفة", "آسفة", "معذرة", "سامحني", "عفوا", "سوري", "sorry"),
            suffixes = listOf("", " سيمو", "!"),
            responses = listOf("ولا يهمك 🌿 لا داعي للاعتذار، خبرني كيف أقدر أساعدك.")
        )

        private val compliment = Category(
            words = listOf(
                "برافو", "احسنت", "رائع", "ممتاز", "تمام", "حلو", "perfect", "nice",
                "great job", "كفو", "روعة", "جميل"
            ),
            suffixes = listOf("", " سيمو", "!"),
            responses = listOf("شكراً لكلامك الطيب 🌿 سعيد إني أفدتك، تحت أمرك بأي سؤال ثاني.")
        )

        private val smalltalk = Category(
            words = listOf("نعم", "ايوة", "أيوة", "تمام", "اوك", "ok", "لا", "لأ", "مافي شي", "خلاص"),
            suffixes = listOf(""),
            responses = listOf("تمام 🌿 خبرني إذا حابب تسأل عن عشبة معيّنة أو أي موضوع بالموسوعة."),
            exactOnly = true
        )

        private val all = listOf(
            greeting, thanks, farewell, wellbeing, identity, capability, apology, compliment, smalltalk
        )

        /** صياغة واحدة مسطّحة: النص، ردّه، وهل مطابقتها تامة فقط (بلا استثناء). */
        private data class Flat(val phrase: String, val response: String, val exactOnly: Boolean)

        /** كل الصياغات مسطّحة، تُبنى مرة واحدة فقط عند أول استخدام. */
        private val flattened: List<Flat> by lazy {
            val out = mutableListOf<Flat>()
            all.forEach { cat ->
                cat.words.forEach { w ->
                    cat.suffixes.forEach { s ->
                        val phrase = (w + s).trim()
                        if (phrase.isNotBlank()) {
                            val idx = phrase.hashCode().let { if (it < 0) -it else it } % cat.responses.size
                            out += Flat(phrase, cat.responses[idx], cat.exactOnly)
                        }
                    }
                }
            }
            out
        }

        /** عدد الصياغات المدرَّبة فعلياً (لأغراض العرض/التوثيق فقط). */
        val phrasingCount: Int get() = flattened.size

        /**
         * يطابق سؤال المستخدم (بعد التطبيع) بأطول صياغة مدرَّبة مطابقة تماماً،
         * أو تكون الصياغة بداية للسؤال مع فارق طفيف جداً بعدها (٣ أحرف كحد
         * أقصى، لالتقاط علامات ترقيم أو مسافات) — بحيث لا تُخطف أسئلة حقيقية
         * عن الأعشاب تحتوي بالصدفة على جزء من عبارة تحية قصيرة. الصياغات
         * القصيرة الملتبسة (مثل "لا") تتطلب مطابقة تامة فقط.
         */
        fun match(question: String): String? {
            val qNorm = normalize(question)
            if (qNorm.isBlank()) return null
            var best: Flat? = null
            var bestLen = -1
            for (flat in flattened) {
                val pNorm = normalize(flat.phrase)
                if (pNorm.isEmpty()) continue
                val isMatch = qNorm == pNorm ||
                    (!flat.exactOnly && qNorm.startsWith(pNorm) && qNorm.length - pNorm.length <= 3)
                if (isMatch && pNorm.length > bestLen) {
                    best = flat
                    bestLen = pNorm.length
                }
            }
            return best?.response
        }
    }

    // ── مجموعات مرادفات مواضيع صحية شائعة (مشكلة "التنحيف") ─────────────

    /**
     * مشكلة حقيقية أُبلغ عنها: سؤال عن "التنحيف" (أو اقتراح عشبة له) كان لا
     * يُطابق أي عشبة تقريباً، رغم وجود بيانات فعلية في الموسوعة تتحدث عن
     * الموضوع نفسه بكلمات أخرى (إنقاص الوزن، حرق الدهون، الرجيم، السمنة...).
     * السبب: لا [ArabicLexicon] (تجذير لغوي بحت، لا علاقة صرفية بين
     * "تنحيف" و"دهون" مثلاً) ولا [DictionaryLexicon] العام (قاموس لغوي
     * عام، لا يربط بالضرورة بين كل المرادفات العامية/الصحية الشائعة لموضوع
     * بعينه) يغطّيان هذا النوع من "الترادف الموضوعي" (كلمات مختلفة الجذر
     * تماماً لكنها تدل على نفس الموضوع الصحي عملياً).
     *
     * هذا الحل مخصَّص: مجموعات كلمات يدوية لكل موضوع، أي كلمة في المجموعة
     * تُوسَّع لتشمل *كل* كلمات مجموعتها عند التحليل — تماماً كمرادفات
     * [AiConfig.synonyms] لكن مبنية داخل الكود كتغطية أساسية جاهزة فور
     * التثبيت، بلا حاجة لأي إعداد يدوي من المطوّر. يمكن إضافة مجموعات
     * جديدة لأي موضوع آخر يتكرر فشل مطابقته لاحقاً بنفس الطريقة.
     */
    private object HealthTopicSynonyms {
        private val rawClusters: List<Set<String>> = listOf(
            // التنحيف/إنقاص الوزن — المجموعة التي عالجت المشكلة المُبلَغ عنها.
            setOf(
                "تنحيف", "تخسيس", "رجيم", "دايت", "حمية", "انقاص", "إنقاص",
                "الوزن", "وزن", "دهون", "الدهون", "سمنة", "السمنة", "نحافة",
                "حرق", "خسارة", "تخفيف", "كرش", "الكرش"
            )
        )

        private val clusters: List<Set<String>> by lazy { rawClusters.map { c -> c.map { normalize(it) }.toSet() } }

        private val lookup: Map<String, Set<String>> by lazy {
            val map = mutableMapOf<String, MutableSet<String>>()
            clusters.forEach { cluster -> cluster.forEach { word -> map.getOrPut(word) { mutableSetOf() }.addAll(cluster) } }
            map
        }

        /** كل كلمات كل المجموعات مسطّحة — تُستخدم لتفعيل نية سؤال محدَّد (مثل فروع فائدة/اقتراح). */
        val allWords: List<String> by lazy { clusters.flatten() }

        /** يوسّع كلمات السؤال بمرادفات مجموعتها الموضوعية إن وُجدت (إضافة فهم، لا حذف). */
        fun expand(words: Set<String>): Set<String> =
            if (words.isEmpty()) words else words + words.flatMap { lookup[it].orEmpty() }

        /** يعيد مجموعة الموضوع كاملة إن ذكر نص السؤال (المُطبَّع) أي كلمة منها، وإلا null. */
        fun clusterMentionedIn(qNorm: String): Set<String>? =
            clusters.firstOrNull { cluster -> cluster.any { qNorm.contains(it) } }
    }

    // ذاكرة تخزين مؤقت بسيطة: يُعاد بناء الفهرس فقط عند تغيّر مرجع قائمة
    // الأعشاب أو الخلطات (تُنشئ شاشات التطبيق قائمة جديدة عند أي تحديث فعلي
    // للبيانات).
    private var cachedIndex: CorpusIndex? = null
    private var cachedForHerbs: List<Herb>? = null
    private var cachedForBlends: List<Blend>? = null

    /**
     * [blends] اختيارية (افتراضياً فارغة) حتى تبقى بقية الاستخدامات الحالية
     * (المقارنة بين أعشاب محدَّدة، اقتراح عشبة) كما هي بلا أي تغيير — فهرس
     * مبني من الأعشاب فقط، وهو السياق الصحيح لها. يُمرَّر [blends] فقط من
     * البحث الحر العام ([buildGeneralSearchAnswer]) حيث "كل الموسوعة" تشمل
     * الخلطات أيضاً.
     */
    private fun corpusIndexFor(herbs: List<Herb>, blends: List<Blend> = emptyList()): CorpusIndex {
        val current = cachedIndex
        if (current != null && cachedForHerbs === herbs && cachedForBlends === blends) return current
        val herbTexts = herbs.flatMap { listOf(it.name, it.benefits, it.usage, it.warnings, it.harms, it.notes) }
        val blendTexts = blends.flatMap { listOf(it.name, it.benefits, it.usage, it.warnings, it.notes) }
        val built = CorpusIndex(herbTexts + blendTexts)
        cachedIndex = built
        cachedForHerbs = herbs
        cachedForBlends = blends
        return built
    }

    /** تشابه Jaccard موزون بأهمية الكلمات (IDF) بدل عدّها بالتساوي. */
    private fun weightedSimilarity(index: CorpusIndex, a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val union = a union b
        val unionWeight = union.sumOf { index.weightOf(it) }
        if (unionWeight == 0.0) return 0.0
        val interWeight = (a intersect b).sumOf { index.weightOf(it) }
        return interWeight / unionWeight
    }

    /**
     * "تغطية" غير متماثلة: أي نسبة من *وزن كلمات السؤال* (وليس الجملة كاملة)
     * وُجدت فعلياً ضمن نقطة نص معيّنة. الفارق الجوهري عن [weightedSimilarity]
     * أعلاه أن المقام هنا هو وزن كلمات *السؤال فقط*، لا وزن كل الكلمات
     * المجتمعة (سؤال + نقطة) — فنقطة نص طويلة ومفصّلة من الموسوعة (كما هو
     * شائع في حقل الفوائد) لا تُعاقَب لمجرد طولها طالما أنها تحوي فعلاً
     * كلمات سؤال المستخدم؛ هذا بالضبط ما كان يجعل أسئلة قصيرة عن مواضيع
     * مذكورة فعلاً في الموسوعة (مثل "فوائد بذور الكتان" أو "عشبة لتحسين
     * النوم") لا تظهر أي نتيجة رغم وجود البيانات فعلياً، لأن Jaccard
     * المتماثل وحده كان يُذيب المطابقة الحقيقية داخل بقية كلمات النقطة
     * الطويلة. يبقى [weightedSimilarity] كما هو ويُستخدم فقط في مقارنة
     * نص-بنص بين أعشاب مختلفة ([compareField]/[buildOverview]) حيث الطرفان
     * متكافئان بالطول عادةً، وهو السياق الصحيح لمقياس متماثل.
     */
    private fun queryCoverage(index: CorpusIndex, queryWords: Set<String>, pointWords: Set<String>): Double {
        if (queryWords.isEmpty() || pointWords.isEmpty()) return 0.0
        val queryWeight = queryWords.sumOf { index.weightOf(it) }
        if (queryWeight == 0.0) return 0.0
        val matchedWeight = (queryWords intersect pointWords).sumOf { index.weightOf(it) }
        return matchedWeight / queryWeight
    }

    /** أي نسبة من كلمات السؤال (النصية الخام، بلا وزن) موجودة حرفياً كسلسلة
     * فرعية داخل نص النقطة المُطبَّع — شبكة أمان أخيرة مستقلة تماماً عن أي
     * تقطيع كلمات أو قواميس، لضمان أن "البحث الحقيقي بكل البيانات" الذي
     * يطلبه المستخدم يلتقط حتى الحالات التي يفشل فيها تقطيع الكلمات لأي سبب. */
    private fun rawContainmentRatio(queryWords: Set<String>, pointNormalized: String): Double {
        if (queryWords.isEmpty()) return 0.0
        val matched = queryWords.count { it.length > 1 && pointNormalized.contains(it) }
        return matched.toDouble() / queryWords.size
    }

    /**
     * الدرجة النهائية المستخدمة فعلياً لمطابقة سؤال المستخدم بنقطة نص واحدة
     * في البحث الحر والاقتراح: أعلى قيمة بين المقاييس الثلاثة أعلاه، بحيث لا
     * يفوّت سيمو مطابقة حقيقية بسبب ضعف مقياس واحد بعينه في حالة معيّنة.
     */
    private fun matchScore(index: CorpusIndex, queryWords: Set<String>, point: String): Double {
        val pointWords = wordsOf(point)
        return maxOf(
            weightedSimilarity(index, queryWords, pointWords),
            queryCoverage(index, queryWords, pointWords),
            rawContainmentRatio(queryWords, normalize(point))
        )
    }

    /**
     * يقارن سؤال المستخدم بكل "الحالات المدرَّبة" — اليدوية أولاً
     * ([AiConfig.trainedExamples]، أولوية مطلقة دوماً لأنها من مراجعة
     * المطوّر مباشرة)، ثم المتعلَّمة ذاتياً ([AiConfig.autoLearnedExamples])
     * بعتبة أعلى قليلاً تحوّطاً لأنها غير مراجَعة يدوياً. يعيد أقرب رد
     * مخصّص إن تجاوز التشابه العتبة المناسبة، وإلا يعيد null ليكمل سيمو
     * بمنطقه العام.
     *
     * المطابقة تستخدم الآن [richWordsOf] (تجذير + قاموس مرادفات) بدل كلمات
     * الجملة الحرفية فقط — سابقاً كانت صياغة مرادفة تماماً لسؤال مدرَّب
     * (مثل "شو ينفع الزنجبيل" بدل "ما فوائد الزنجبيل") قد لا تُطابق الحالة
     * المدرَّبة إطلاقاً رغم تطابق المعنى، لأن [jaccard] كان يقارن الكلمات
     * الحرفية فقط دون أي فهم للمرادفات — وهذا بالضبط ما يجعل "تعلّم سيمو"
     * الذاتي من التقييمات مفيداً فعلياً لصياغات لاحقة مشابهة معنوياً لا
     * حرفياً فقط.
     */
    private fun matchTrainedExample(question: String): String? {
        val qWords = richWordsOf(question)
        if (qWords.isEmpty()) return null

        var bestManual: String? = null
        var bestManualScore = 0.0
        exampleWords(AiConfig.trainedExamples).forEach { (example, words) ->
            val score = jaccard(qWords, words)
            if (score > bestManualScore) { bestManualScore = score; bestManual = example.response }
        }
        if (bestManualScore >= AiConfig.trainedMatchThreshold) return bestManual

        if (!AiConfig.autoLearnEnabled || AiConfig.autoLearnedExamples.isEmpty()) return null
        var bestAuto: String? = null
        var bestAutoScore = 0.0
        exampleWords(AiConfig.autoLearnedExamples).forEach { (example, words) ->
            val score = jaccard(qWords, words)
            if (score > bestAutoScore) { bestAutoScore = score; bestAuto = example.response }
        }
        val autoThreshold = (AiConfig.trainedMatchThreshold + 0.15).coerceAtMost(0.95)
        return if (bestAutoScore >= autoThreshold) bestAuto else null
    }

    // ذاكرتا تخزين مؤقت لكلمات أنماط الحالات المدرَّبة (يدوياً/ذاتياً) بعد
    // توسيعها بـ[richWordsOf] — إعادة استعلام قاموس المرادفات (قاعدة
    // SQLite) لكل الأنماط في كل رسالة دردشة مكلفة بلا داعٍ ما دامت قائمة
    // الأنماط نفسها لم تتغيّر؛ بنفس أسلوب [corpusIndexFor] (تحقّق من مرجع
    // القائمة نفسها، لا محتواها، لأن كل شاشة تُنشئ قائمة جديدة عند أي تعديل
    // فعلي من أدوات المطور أو التعلّم الذاتي).
    private var cachedTrainedRef: List<TrainedExample>? = null
    private var cachedTrainedWords: List<Pair<TrainedExample, Set<String>>> = emptyList()
    private var cachedAutoRef: List<TrainedExample>? = null
    private var cachedAutoWords: List<Pair<TrainedExample, Set<String>>> = emptyList()

    private fun exampleWords(examples: List<TrainedExample>): List<Pair<TrainedExample, Set<String>>> {
        val isTrained = examples === AiConfig.trainedExamples
        if (isTrained) {
            if (cachedTrainedRef === examples) return cachedTrainedWords
            val built = examples.map { it to richWordsOf(it.pattern) }
            cachedTrainedRef = examples
            cachedTrainedWords = built
            return built
        }
        if (cachedAutoRef === examples) return cachedAutoWords
        val built = examples.map { it to richWordsOf(it.pattern) }
        cachedAutoRef = examples
        cachedAutoWords = built
        return built
    }

    /** أقصى عدد حالات يحتفظ بها التعلّم الذاتي؛ الأقدم يُستبعد أولاً عند التجاوز. */
    private const val MAX_AUTO_LEARNED_EXAMPLES = 200

    /**
     * يسجّل تقييم المستخدم (👍/👎) على إجابة "قابلة للتعلّم" (انظر
     * [AssistantReply.learnable]) في شاشة الدردشة:
     * - عند الإعجاب: تُحفظ نقطة السؤال↔الرد كحالة يتعلّمها سيمو تلقائياً،
     *   بشرط ألا تكون قريبة بما يكفي من حالة متعلَّمة سابقاً (تفادي التكرار)،
     *   مع سقف أقصى لعدد الحالات ([MAX_AUTO_LEARNED_EXAMPLES]) يُستبعد عنده
     *   الأقدم أولاً.
     * - عند عدم الإعجاب: يُزال أي مثال متعلَّم ذاتياً يطابق هذا السؤال بدرجة
     *   كافية — أي أن سيمو "يتراجع" عن خطأ تعلّمه بنفسه — دون أي تأثير على
     *   حالات تدريب المطوّر اليدوية، المحمية دوماً من هذا المسار.
     * فحص "التكرار" هنا يستخدم [richWordsOf] أيضاً (بدل الكلمات الحرفية)
     * حتى يتّسق مع [matchTrainedExample] تماماً: لا يُحفَظ سؤال كحالة جديدة
     * إن كانت هناك حالة مرادفة معنوياً محفوظة أصلاً (لا حرفياً مطابقة فقط)،
     * فلا تتكدّس نسخ شبه مكرَّرة من نفس المعنى بصياغات مختلفة.
     * يعيد القائمة المحدَّثة مباشرة ليحفظها المستدعي (PreferencesRepository).
     */
    fun recordFeedback(question: String, reply: String, helpful: Boolean): List<TrainedExample> {
        val qWords = richWordsOf(question)
        if (qWords.isEmpty()) return AiConfig.autoLearnedExamples

        if (!helpful) {
            val remaining = AiConfig.autoLearnedExamples.filterNot {
                jaccard(qWords, richWordsOf(it.pattern)) >= AiConfig.trainedMatchThreshold
            }
            AiConfig.autoLearnedExamples = remaining
            return remaining
        }

        if (!AiConfig.autoLearnEnabled) return AiConfig.autoLearnedExamples
        val alreadyKnown = AiConfig.autoLearnedExamples.any {
            jaccard(qWords, richWordsOf(it.pattern)) >= AiConfig.trainedMatchThreshold
        }
        if (alreadyKnown) return AiConfig.autoLearnedExamples

        val updated = (AiConfig.autoLearnedExamples + TrainedExample(question.trim(), reply))
            .takeLast(MAX_AUTO_LEARNED_EXAMPLES)
        AiConfig.autoLearnedExamples = updated
        return updated
    }

    // ── المقارنة المنظّمة (يُستخدم في بطاقات المقارنة بالشاشة) ─────────

    /** نقطة مقارنة واحدة (فائدة، تحذير...) مع قائمة الأعشاب التي وردت فيها. */
    data class ComparisonPoint(val text: String, val herbIds: List<String>)

    /**
     * يبني مقارنة نقطة-بنقطة لحقل معيّن (مثل الفوائد) عبر الأعشاب المختارة:
     * يجمع النقاط المتشابهة معنوياً من أعشاب مختلفة في نقطة واحدة مشتركة
     * (بتشابه موزون بأهمية الكلمات عبر [CorpusIndex] بدل عدّ بسيط)، ويترك
     * النقاط المنفردة كما هي، بحيث تُعرض النتيجة منظّمة وواضحة.
     */
    fun compareField(herbs: List<Herb>, field: (Herb) -> String): List<ComparisonPoint> {
        val index = corpusIndexFor(herbs)
        val perHerbPoints = herbs.map { it.id to splitPoints(field(it)) }
        val usedFlags = perHerbPoints.map { (_, pts) -> BooleanArray(pts.size) }
        val results = mutableListOf<ComparisonPoint>()
        val threshold = AiConfig.similarityThreshold

        for (hi in herbs.indices) {
            val (herbId, points) = perHerbPoints[hi]
            for (pi in points.indices) {
                if (usedFlags[hi][pi]) continue
                usedFlags[hi][pi] = true
                val point = points[pi]
                val matched = mutableListOf(herbId)
                val pw = wordsOf(point)
                for (hj in herbs.indices) {
                    if (hj == hi) continue
                    val (otherId, otherPoints) = perHerbPoints[hj]
                    // كانت هذه الحلقة تضمّ *كل* نقطة من عشبة hj تتجاوز العتبة،
                    // لا أفضلها فقط — فتُستهلك (تُعلَّم "مستخدمة") نقاط عدة من
                    // نفس العشبة لنفس نقطة المرساة رغم أن واحدة فقط تظهر فعلياً
                    // في matched، فتُفقد النقاط الأخرى من نتيجة المقارنة كلياً
                    // (لا تظهر لا هنا ولا كمجموعة منفصلة لاحقاً) رغم أنها قد
                    // كانت الأنسب فعلاً لمرساة أخرى ستُفحص بعدها. نختار الآن
                    // فقط أفضل نقطة غير مستخدمة (الأعلى تشابهاً) من كل عشبة
                    // أخرى، فلا تُفقد نقاط بلا داعٍ وتكون كل مجموعة مقارنة
                    // مبنية على أدقّ تطابق متاح فعلاً.
                    var bestPj = -1
                    var bestSim = 0.0
                    for (pj in otherPoints.indices) {
                        if (usedFlags[hj][pj]) continue
                        val sim = weightedSimilarity(index, pw, wordsOf(otherPoints[pj]))
                        if (sim >= threshold && sim > bestSim) {
                            bestSim = sim
                            bestPj = pj
                        }
                    }
                    if (bestPj >= 0) {
                        usedFlags[hj][bestPj] = true
                        if (otherId !in matched) matched += otherId
                    }
                }
                results += ComparisonPoint(point, matched)
            }
        }
        return results
    }

    /** ملخّص نصّي عام يوضّح أبرز المشترك والمختلف بين الأعشاب المختارة. */
    fun buildOverview(herbs: List<Herb>): String {
        if (herbs.size < 2) return ""
        val benefitPoints = compareField(herbs) { it.benefits }
        val shared = benefitPoints.filter { it.herbIds.size == herbs.size }
        return buildString {
            append("قارنتُ بين ${herbNames(herbs)} بناءً على بيانات الموسوعة. ")
            if (shared.isNotEmpty()) {
                append("تشترك جميعها في: ${shared.take(3).joinToString("، ") { it.text }}. ")
            }
            herbs.forEach { herb ->
                val unique = benefitPoints.filter { it.herbIds.size == 1 && it.herbIds.first() == herb.id }
                if (unique.isNotEmpty()) {
                    append("وتنفرد ${herb.name} بـ: ${unique.take(2).joinToString("، ") { it.text }}. ")
                }
            }
            if (shared.isEmpty() && herbs.all { herb -> benefitPoints.none { herb.id in it.herbIds } }) {
                append("لا تتوفر بيانات فوائد كافية لهذه الأعشاب بعد لعرض ملخّص تفصيلي.")
            }
        }
    }

    /**
     * يبحث عن الأعشاب المذكورة صراحةً باسمها داخل نص السؤال الحر، على
     * مستويين متتاليين (الأدق أولاً) بدل اشتراط تطابق حرفي تام لاسم العشبة
     * كاملاً كما كان سابقاً — وهو ما كان يفشل بأي فرق بسيط في الصياغة (مثل
     * "ال" التعريف، أو ترتيب كلمات مختلف، أو ذكر الاسم ضمن عبارة أطول)
     * فيُحوّل سؤالاً واضحاً عن عشبة محدَّدة إلى بحث عام غامض ضمن كل الموسوعة:
     * 1) احتواء حرفي كامل لاسم العشبة ضمن نص السؤال (الأدق، وكما كان سابقاً).
     * 2) إن لم يوجد ذلك: تطابق *كل* كلمات اسم العشبة، كلمة كلمة (باحتواء كل
     *    كلمة ضمن الأخرى، فيلتقط تلقائياً فرق "ال" التعريف أو صيغة الجمع/
     *    المفرد)، ضمن كلمات السؤال — يكفي أن تكون كل كلمات الاسم مذكورة
     *    بأي صيغة قريبة، ولا يشترط ترتيبها أو تطابقها حرفياً بالكامل.
     */
    fun relevantHerbs(question: String, allHerbs: List<Herb>): List<Herb> {
        val qNorm = normalize(question)
        if (qNorm.isBlank()) return emptyList()

        val literal = allHerbs.filter { herb -> herb.name.isNotBlank() && qNorm.contains(normalize(herb.name)) }
        if (literal.isNotEmpty()) return literal

        val qTokens = qNorm.split(Regex("\\s+")).filter { it.length > 1 }
        if (qTokens.isEmpty()) return emptyList()

        return allHerbs.filter { herb ->
            if (herb.name.isBlank()) return@filter false
            val nameTokens = normalize(herb.name).split(Regex("\\s+")).filter { it.length > 1 }
            if (nameTokens.isEmpty()) return@filter false
            nameTokens.all { nt -> qTokens.any { qt -> qt == nt || qt.contains(nt) || nt.contains(qt) } }
        }
    }

    /** أسئلة سريعة مقترحة تُعرض كأزرار فوق مربع الدردشة. */
    fun quickSuggestions(selectedHerbs: List<Herb>, allHerbs: List<Herb> = emptyList()): List<String> = when {
        selectedHerbs.size >= 2 -> listOf(
            "ما أبرز الفروقات بينها؟",
            "أيهما أكثر أماناً؟",
            "هل يمكن الجمع بينهما؟",
            "ما طريقة استخدام كل منها؟"
        )
        selectedHerbs.size == 1 -> listOf(
            "ما فوائدها؟",
            "ما طريقة استخدامها؟",
            "هل لها تحذيرات؟"
        )
        else -> buildList {
            val sample = allHerbs.filter { it.name.isNotBlank() }.take(2)
            if (sample.isNotEmpty()) add("ما فوائد ${sample[0].name}؟")
            if (sample.size >= 2) add("قارن بين ${sample[0].name} و ${sample[1].name}")
            add("ما هي الأعشاب الآمنة أثناء الحمل؟")
            add("اقترح عشبة لتحسين النوم")
        }
    }

    // ── الدردشة الذكية (إجابة حرة على أسئلة المستخدم) ──────────────────

    /**
     * رد سيمو مع بيان ما إذا كان "قابلاً للتعلّم الذاتي" منه: فقط إجابات
     * البحث الحر الفعلية (وليست الترحيب/الشكر/إجابة حالة مدرَّبة مسبقاً، إذ
     * لا معنى لإعادة تعلّم شيء متعلَّم أو ثابت أصلاً) تُعرض معها أزرار تقييم
     * في شاشة الدردشة، ليقرر المستخدم إن كانت مفيدة فتُحفظ عبر [recordFeedback].
     */
    data class AssistantReply(val text: String, val learnable: Boolean)

    /** توافقاً مع الاستدعاءات القديمة (مثل اختبار أدوات المطور) التي تحتاج النص فقط. */
    fun answer(question: String, herbs: List<Herb>, allowCompare: Boolean = true, blends: List<Blend> = emptyList()): String =
        answerDetailed(question, herbs, allowCompare, blends).text

    /**
     * يجيب على سؤال حر بالاعتماد على بيانات عشبة واحدة أو أكثر. لا يوجد هنا
     * أي حجب أو تقييد صناعي على المحتوى — المساعد محلي بالكامل ويستخدم فقط
     * نصوص الموسوعة التي أدخلها المطوّر (يدوياً أو عبر التعلّم الذاتي)، فيجيب
     * دوماً بأفضل ما يتوفر لديه من معلومات، ويوضّح بصراحة عندما لا تتوفر
     * بيانات كافية بدل رفض الإجابة.
     *
     * [allowCompare] يحدد ما إذا كان مسموحاً تفعيل منطق "المقارنة/الدمج"
     * المنظّم (يتطلب عشبتين محددتين بوضوح عبر اختيار المستخدم أو ذكرهما
     * بالاسم في السؤال). عند `false` (مثلاً حين تُمرَّر كل الموسوعة كسياق
     * افتراضي لعدم وجود أعشاب محددة) يظل سيمو يجيب بحرية، لكن دون أن "يقارن"
     * تلقائياً بين عشرات الأعشاب التي لم يطلبها أحد — تماماً كما لا يقارن
     * إلا إذا طُلب منه ذلك صراحة.
     */
    fun answerDetailed(question: String, herbs: List<Herb>, allowCompare: Boolean = true, blends: List<Blend> = emptyList()): AssistantReply {
        val qNorm = normalize(question)
        if (qNorm.isBlank()) {
            return AssistantReply("تفضّل، اسأل سيمو عن أي عشبة: فوائدها، طريقة استخدامها، أو تحذيراتها.", false)
        }

        // أولوية مطلقة للحالات المدرَّبة (يدوياً من المطوّر، أو ذاتياً من
        // تقييمات المستخدمين السابقة) — إن وُجدت مطابقة كافية، يستخدم سيمو
        // ردّها مباشرة قبل أي منطق عام آخر. هذا التحقق (وتحقق ConversationalSeed
        // بعده) يسبقان عمداً فحص "هل تتوفر بيانات أعشاب؟" أدناه: الترحيب
        // والشكر والسؤال عن الحال وهوية سيمو لا تحتاج أي بيانات أعشاب إطلاقاً،
        // فكانت تُخطَف سابقاً برسالة "لم أجد معلومات" فقط لأن قائمة الأعشاب
        // لم تكن قد حُمِّلت بعد من Firestore (أو كانت فارغة لأي سبب مؤقت) —
        // خلل معماري يجعل سؤالاً بسيطاً مثل "كيفك" يعتمد خطأً على بيانات لا
        // علاقة لها به إطلاقاً.
        matchTrainedExample(question)?.let { return AssistantReply(it, false) }

        // تدريب سيمو الافتتاحي (أكثر من 500 صياغة محادثة، انظر [ConversationalSeed])
        // يأتي بعد تدريب المطوّر مباشرة وقبل أي منطق آخر: يغطي التحية والشكر
        // والوداع وأسئلة الهوية والقدرات، فلا تحتاج هذه لأي بحث في الموسوعة.
        ConversationalSeed.match(question)?.let { return AssistantReply(it, false) }

        // من هنا فقط (بعد استبعاد كل ردود المحادثة العامة التي لا تحتاج بيانات)
        // يصبح فحص توفّر بيانات الموسوعة منطقياً: أي سؤال متبقٍ يحتاج فعلاً
        // للبحث ضمن نصوص الأعشاب، فإن لم تتوفر بعد نعتذر بوضوح بدل الانهيار.
        if (herbs.isEmpty() && blends.isEmpty()) {
            return AssistantReply("لم أجد في الموسوعة معلومات كافية للإجابة على هذا السؤال بعد 🌿", false)
        }

        // "محدَّد" = عدد قليل من الأعشاب المستهدفة فعلياً (باختيار المستخدم أو
        // ذكرها بالاسم) — عندها فقط تُبنى إجابات مفصّلة لكل عشبة على حدة.
        // إن كان السياق هو كامل الموسوعة (لم يُطلب/يُحدَّد شيء)، يُستخدم
        // البحث الحر بدل تكرار كل عشبة، تفادياً لإغراق الدردشة بإجابة ضخمة
        // لم يطلبها أحد — نفس مبدأ "لا مقارنة أو استعراض إلا عند الطلب".
        val specific = herbs.size <= 3
        return when {
            containsAny(qNorm, listOf("مرحبا", "اهلا", "أهلا", "السلام عليكم", "hello", "hi")) ->
                AssistantReply(
                    "أهلاً 👋 أنا سيمو، مساعدك الذكي في عالم الأعشاب. اسألني عن أي عشبة تريدها: فوائدها، طريقة استخدامها، تحذيراتها، أو اطلب مني مقارنة بين أكثر من عشبة، وسأجيبك فوراً من بيانات الموسوعة.",
                    false
                )

            containsAny(qNorm, listOf("شكرا", "شكراً", "تسلم", "يعطيك العافية", "مشكور")) ->
                AssistantReply("عفواً 🌿 أنا سيمو، دائماً هنا لأي سؤال آخر عن الأعشاب.", false)

            allowCompare && specific && herbs.size >= 2 && containsAny(qNorm, listOf("جمع", "دمج", "معا", "معاً", "سوية", "سويا", "نفس الوقت", "تفاعل", "خلط")) ->
                AssistantReply(buildCombineAnswer(herbs), false)

            specific && containsAny(qNorm, listOf("خطر", "اضرار", "أضرار", "تحذير", "حامل", "حمل", "رضاعة", "رضاعه", "طفل", "اطفال", "أطفال", "امان", "أمان", "اثار جانبية", "آثار جانبية")) ->
                AssistantReply(buildSafetyAnswer(herbs, qNorm), false)

            specific && containsAny(qNorm, listOf("استخدام", "استعمال", "طريقة", "طريقه", "كيف استخدم", "جرعة", "جرعه", "مقدار")) ->
                AssistantReply(buildUsageAnswer(herbs), false)

            allowCompare && specific && herbs.size >= 2 && containsAny(qNorm, listOf("فرق", "يختلف", "اختلاف", "افضل", "أفضل", "احسن", "أحسن", "ايهما", "أيهما", "قارن", "مقارنة")) ->
                AssistantReply(buildOverview(herbs) + "\n\n" + buildSafetyGlance(herbs), false)

            specific && containsAny(qNorm, listOf("فائدة", "فائده", "فوائد", "يفيد", "علاج", "يعالج", "مفيد") + HealthTopicSynonyms.allWords) ->
                AssistantReply(buildBenefitsAnswer(herbs, qNorm), false)

            // "اقترح/رشّح/انصحني بعشبة": فقط عندما لا توجد عشبة محدَّدة سلفاً
            // (لا إرفاق ولا ذكر اسم صريح) — عندها "الاقتراح" له معنى فعلياً،
            // وهو اختيار الأنسب من كامل الموسوعة بدل عشبة واحدة معروفة أصلاً.
            // إن كانت هناك عشبة محدَّدة، تُعامَل الكلمة كجزء عادي من سؤال عادي
            // (فتُغطّى أصلاً عبر فروع الفائدة/الاستخدام أعلاه) بدل خطفها هنا.
            !specific && isSuggestionIntent(qNorm) -> {
                val (text, learnable) = buildSuggestionAnswer(question, herbs)
                AssistantReply(text, learnable)
            }

            else -> {
                val (text, learnable) = buildGeneralSearchAnswer(question, herbs, blends)
                AssistantReply(text, learnable)
            }
        }
    }

    private fun buildCombineAnswer(herbs: List<Herb>): String = buildString {
        append("لا تحتوي الموسوعة على قاعدة بيانات مخصّصة لتفاعلات الأعشاب مع بعضها، لذا لا يمكنني الجزم بأمان الجمع بين ${herbNames(herbs)}.\n\n")
        append("أبرز التحذيرات المسجّلة لكل عشبة على حدة:\n")
        herbs.forEach { herb ->
            val cautions = (splitPoints(herb.warnings) + splitPoints(herb.harms)).take(2)
            append("🔸 ${herb.name}: ")
            append(if (cautions.isNotEmpty()) cautions.joinToString("، ") else "لا توجد تحذيرات مسجّلة")
            append("\n")
        }
        append("\nالأفضل استشارة طبيب أو صيدلاني قبل الجمع بينهما، خصوصاً مع وجود أدوية أو حالة صحية.")
    }

    private fun buildSafetyAnswer(herbs: List<Herb>, qNorm: String): String {
        val pregnancyAsked = containsAny(qNorm, listOf("حامل", "حمل", "رضاعة", "رضاعه", "رضع"))
        return buildString {
            herbs.forEach { herb ->
                val points = splitPoints(herb.warnings) + splitPoints(herb.harms)
                append("🔸 ${herb.name}:\n")
                if (points.isEmpty()) {
                    append("لا توجد تحذيرات أو أضرار مسجّلة في الموسوعة لهذه العشبة.\n")
                } else {
                    val relevant = if (pregnancyAsked)
                        points.filter { containsAny(normalize(it), listOf("حامل", "حمل", "رضاعة", "رضاعه", "رضع")) }
                    else points
                    val toShow = relevant.ifEmpty { points }.take(3)
                    toShow.forEach { append("• $it\n") }
                    if (pregnancyAsked && relevant.isEmpty()) {
                        append("(لم يُذكر صراحةً الحمل أو الرضاعة، يُستحسن استشارة الطبيب للتأكد)\n")
                    }
                }
            }
            append("\nهذه المعلومات للاطلاع فقط ولا تُغني عن استشارة مختص.")
        }
    }

    private fun buildUsageAnswer(herbs: List<Herb>): String = buildString {
        herbs.forEach { herb ->
            append("🔸 ${herb.name}: ")
            append(herb.usage.ifBlank { "لا توجد طريقة استخدام مسجّلة." })
            append("\n")
        }
    }

    /**
     * [qNorm] (افتراضياً فارغ للتوافق مع الاستدعاءات القديمة) يتيح تمييز
     * النقاط المرتبطة بموضوع محدَّد ذكره السؤال (مثل "تنحيف"/"وزن"، انظر
     * [HealthTopicSynonyms]) من بين كل فوائد العشبة، بدل عرض النص كاملاً
     * بلا تمييز حين يسأل المستخدم فعلياً عن غرض بعينه لا عن الفوائد عموماً
     * — نفس مبدأ فلترة سؤال الحمل/الرضاعة في [buildSafetyAnswer] تماماً.
     */
    private fun buildBenefitsAnswer(herbs: List<Herb>, qNorm: String = ""): String {
        if (herbs.size < 2) {
            val herb = herbs.first()
            val points = splitPoints(herb.benefits)
            if (points.isEmpty()) {
                return "🔸 ${herb.name}: لا توجد فوائد مسجّلة لهذه العشبة في الموسوعة بعد."
            }
            val topic = HealthTopicSynonyms.clusterMentionedIn(qNorm)
            val relevant = if (topic != null) points.filter { containsAny(normalize(it), topic.toList()) } else emptyList()
            return if (relevant.isNotEmpty()) {
                "🔸 ${herb.name}:\n" + relevant.joinToString("\n") { "• $it" }
            } else {
                "🔸 ${herb.name}: ${herb.benefits}"
            }
        }
        val points = compareField(herbs) { it.benefits }
        val shared = points.filter { it.herbIds.size == herbs.size }
        return buildString {
            if (shared.isNotEmpty()) {
                append("مشترك بين ${herbNames(herbs)}: ${shared.joinToString("، ") { it.text }}\n\n")
            }
            herbs.forEach { herb ->
                val mine = points.filter { it.herbIds.size < herbs.size && herb.id in it.herbIds }
                if (mine.isNotEmpty()) {
                    append("🔸 ${herb.name} تنفرد بـ: ${mine.joinToString("، ") { it.text }}\n")
                }
            }
            if (shared.isEmpty() && herbs.all { herb -> points.none { herb.id in it.herbIds } }) {
                append("لا توجد فوائد مسجّلة لهذه الأعشاب في الموسوعة بعد.")
            }
        }
    }

    private fun buildSafetyGlance(herbs: List<Herb>): String {
        val counts = herbs.associateWith { splitPoints(it.warnings).size + splitPoints(it.harms).size }
        val minCount = counts.values.minOrNull() ?: 0
        val safest = counts.entries.firstOrNull { it.value == minCount }?.key
        return if (safest != null && counts.values.distinct().size > 1)
            "من ناحية عدد التحذيرات المسجّلة فقط، تبدو ${safest.name} الأقل تحذيرات — لكن هذا لا يعني أنها الأنسب لحالتك؛ استشر مختصاً دوماً."
        else
            "عدد التحذيرات المسجّلة متقارب بين الأعشاب المختارة."
    }

    // ── اقتراح عشبة مناسبة لهدف/عرض معيّن ("اقترح عشبة لتحسين النوم") ───

    /**
     * كلمات/عبارات "أمر الاقتراح" نفسها — لا تحمل أي دلالة عن الهدف الفعلي
     * (النوم، الهضم...) وتُستبعد من كلمات المطابقة في [buildSuggestionAnswer]
     * حتى لا "تُغرق" حساب التشابه بكلمات لا علاقة لها بنص الموسوعة.
     */
    private val suggestionFillerWords: Set<String> by lazy {
        listOf(
            "اقترح", "اقتراح", "اقترحلي", "رشح", "رشحلي", "رشحي", "انصح", "انصحني",
            "انصحيني", "أنصحني", "توصية", "اوصي", "أوصي", "توصي", "عشبة", "عشب",
            "اعشاب", "أعشاب", "نبات", "نبتة", "نباتات"
        ).map { normalize(it) }.toSet()
    }

    /** هل يطلب المستخدم اقتراحاً/توصية بعشبة (بدل سؤال مباشر عن عشبة معروفة)؟ */
    private fun isSuggestionIntent(qNorm: String): Boolean = containsAny(
        qNorm,
        listOf(
            "اقترح", "اقتراح", "رشح", "رشحلي", "رشحي", "انصح", "انصحني", "انصحيني",
            "أنصحني", "توصية", "اوصي", "أوصي", "توصي", "افضل عشبة", "أفضل عشبة",
            "احسن عشبة", "أحسن عشبة", "عشبة تساعد", "عشبة تفيد", "عشبة ل",
            "علاج طبيعي", "حل طبيعي", "دواء طبيعي", "وش تنصح", "وش ينفع", "شو ينفع لل"
        )
    )

    /** أفضل نقطة مطابِقة لعشبة واحدة مع درجة تشابهها ومصدر الحقل. */
    private data class HerbMatch(val text: String, val field: String, val score: Double)

    /**
     * يبني اقتراحاً مرتَّباً لأفضل الأعشاب المتاحة لهدف/عرض ورد في السؤال،
     * عبر نفس خط أنابيب "تحليل → بحث → تحليل بيانات → تنظيم" المستخدم في
     * البحث الحر ([buildGeneralSearchAnswer])، لكن بتجميع على مستوى العشبة
     * (أفضل نقطة لكل عشبة) بدل عرض نقاط مبعثرة، وبترجيح حقل الفوائد أعلى من
     * غيره (لأن "الاقتراح" يعني عملياً: أي عشبة تفيد في هذا الغرض تحديداً)،
     * فتظهر أفضل ٣ أعشاب مرشَّحة مع سبب الترشيح من نص الموسوعة نفسه.
     */
    private fun buildSuggestionAnswer(question: String, herbs: List<Herb>): Pair<String, Boolean> {
        val index = corpusIndexFor(herbs)
        val qWords = analyzeQuestion(question, index) - suggestionFillerWords
        if (qWords.isEmpty()) {
            return "خبرني أكثر عن الهدف أو العرض اللي حابب عشبة تساعدك فيه (مثل: النوم، الهضم، المناعة، التوتر...) وسأبحث لك ضمن الموسوعة 🌿" to false
        }

        val fieldWeight = mapOf(
            "الفوائد" to 1.15, "الاستخدام" to 0.9, "ملاحظات" to 0.85,
            "التحذيرات" to 0.4, "الأضرار" to 0.4,
            // وزن منخفض عمداً: هدف "الاقتراح" هو الغرض/العرض (نوم، هضم...)
            // لا اسم العشبة نفسه، فيبقى حقل الاسم مساعداً فقط (مثلاً عندما
            // يذكر المستخدم اسماً علمياً ضمن سؤاله) دون أن يطغى على تطابق
            // الفوائد الفعلي.
            "الاسم" to 0.5
        )

        val bestPerHerb = mutableMapOf<Herb, HerbMatch>()
        herbs.forEach { herb ->
            searchableFields.forEach { (label, getter) ->
                val weight = fieldWeight[label] ?: 1.0
                splitPoints(getter(herb)).forEach { point ->
                    val sim = matchScore(index, qWords, point) * weight
                    val current = bestPerHerb[herb]
                    if (current == null || sim > current.score) {
                        bestPerHerb[herb] = HerbMatch(point, label, sim)
                    }
                }
            }
        }

        val ranked = bestPerHerb.entries
            .filter { it.value.score > AiConfig.searchThreshold }
            .sortedByDescending { it.value.score }
            .take(3)

        if (ranked.isEmpty()) {
            return "لم أجد في بيانات الموسوعة عشبة ترتبط مباشرة بما طلبته. جرّب صياغة الهدف بكلمة مختلفة، أو اذكر عرضاً أو فائدة أكثر تحديداً." to false
        }

        val text = buildString {
            append("بحثت وحلّلت بيانات الموسوعة، وهذه أنسب الأعشاب المتوفرة لهدفك:\n\n")
            ranked.forEachIndexed { i, entry ->
                val herb = entry.key
                val match = entry.value
                append("${i + 1}. 🔸 ${herb.name}\n")
                append("   • [${match.field}] ${match.text}\n")
            }
            append("\nهذه النتائج مبنية فقط على نصوص الموسوعة، وليست بديلاً عن استشارة طبيب أو صيدلاني، خصوصاً مع وجود حمل أو أدوية أو حالة صحية مزمنة.")
        }
        return text to true
    }

    private data class SearchHit(val herb: Herb, val field: String, val text: String, val score: Double)

    // اسم العشبة (يحوي غالباً الاسم العلمي بالإنجليزية إلى جانب الاسم الشائع
    // بالعربية معاً في نفس الحقل، كما هو مخزَّن فعلياً في الموسوعة) لم يكن
    // يدخل البحث الحر أو الاقتراح إطلاقاً — كان يُفحص فقط عبر [relevantHerbs]
    // (احتواء حرفي تام). فسؤال حرّ يذكر الاسم العلمي أو جزءاً منه فقط (مثل
    // "Cucurbita" أو "لب القرع" ضمن سؤال أطول) دون التطابق الحرفي الكامل
    // الذي يشترطه [relevantHerbs] كان لا يجد العشبة إطلاقاً رغم أن اسمها
    // يحمل الإجابة مباشرة. إضافته كحقل بحث عادي (بنفس خط أنابيب التحليل/
    // المطابقة الموزون) توسّع دقة "البحث والمطابقة" دون أي تغيير في المنطق.
    private val searchableFields = listOf<Pair<String, (Herb) -> String>>(
        "الاسم" to { it.name },
        "الفوائد" to { it.benefits },
        "الاستخدام" to { it.usage },
        "التحذيرات" to { it.warnings },
        "الأضرار" to { it.harms },
        "ملاحظات" to { it.notes }
    )

    // ── دعم الخلطات ("الخلطات" — Blend) في البحث الحر العام ─────────────
    //
    // سيمو كان يقرأ فقط مجموعة "herbs" ولا يعرف بوجود مجموعة "blends"
    // إطلاقاً، رغم أنها جزء أصيل من الموسوعة (تُعرض له شاشتها الخاصة في
    // التطبيق تماماً كالأعشاب). سؤال حر عن خلطة بالاسم أو عن أي محتوى من
    // نصوصها كان يحصل دوماً على "لم أجد" رغم توفّر الإجابة فعلاً في قاعدة
    // البيانات. هذا القسم يوسّع البحث الحر (لا المقارنة المنظّمة بين
    // عشبتين تحديداً، التي تبقى للأعشاب فقط) ليشمل الخلطات أيضاً، بنفس خط
    // أنابيب التحليل/المطابقة الموزون المستخدم للأعشاب تماماً.

    private data class BlendHit(val blend: Blend, val field: String, val text: String, val score: Double)

    private val blendSearchableFields = listOf<Pair<String, (Blend) -> String>>(
        "الاسم" to { it.name },
        "الفوائد" to { it.benefits },
        "الاستخدام" to { it.usage },
        "التحذيرات" to { it.warnings },
        "ملاحظات" to { it.notes }
    )

    private fun gatherBlendCandidates(qWords: Set<String>, blends: List<Blend>, index: CorpusIndex): List<BlendHit> {
        val threshold = AiConfig.searchThreshold
        val hits = mutableListOf<BlendHit>()
        blends.forEach { blend ->
            blendSearchableFields.forEach { (label, getter) ->
                splitPoints(getter(blend)).forEach { point ->
                    val sim = matchScore(index, qWords, point)
                    if (sim > threshold) hits += BlendHit(blend, label, point, sim)
                }
            }
        }
        return hits
    }

    private fun organizeBlendHits(hits: List<BlendHit>): Map<Blend, List<BlendHit>> =
        hits.groupBy { it.blend }
            .entries
            .sortedByDescending { (_, blendHits) -> blendHits.maxOf { it.score } }
            .take(2)
            .associate { (blend, blendHits) -> blend to blendHits.sortedByDescending { it.score }.take(3) }

    /**
     * البحث الحر الكامل في كل نصوص الموسوعة، على مراحل واضحة ومنفصلة —
     * بالضبط تسلسل "سؤال → تحليل → تفكير → تنظيم → تجميع النتيجة وإرسالها":
     * 1) [analyzeQuestion]  — يحلّل سؤال المستخدم إلى كلمات مفتاحية،
     *    ويوسّعها بعلاقات [CorpusIndex] الضمنية + جذور [ArabicLexicon]
     *    التقريبية، بحيث يفهم صيغاً لم تُذكر حرفياً في نص الموسوعة.
     * 2) [gatherCandidates] — "يفكّر" بالإجابة عبر مسح كل حقول كل الأعشاب
     *    المتاحة ومقارنة كل نقطة فيها بكلمات السؤال (تشابه موزون بالأهمية
     *    IDF بدل عدّ الكلمات بالتساوي)، ويحتفظ فقط بما يتجاوز عتبة القبول.
     * 3) [organizeHits]     — "ينسّق الأفكار": يرتّب النتائج حسب الصلة، ثم
     *    يجمّعها تحت كل عشبة معاً بدل تشتيتها.
     * 4) [composeAnswer]    — يجمع كل هذا في رد واحد مقروء ويُعيده جاهزاً
     *    للعرض في الدردشة.
     * يعيد النص + بياناً هل عُثر فعلاً على نتائج ذات صلة (`true`) أم أن الرد
     * كان رسالة تعذّر عامة (`false`) — يُستخدم هذا لتحديد أهلية الرد للتعلّم
     * الذاتي (انظر [AssistantReply.learnable]).
     */
    private fun buildGeneralSearchAnswer(question: String, herbs: List<Herb>, blends: List<Blend> = emptyList()): Pair<String, Boolean> {
        val index = corpusIndexFor(herbs, blends)
        val qWords = analyzeQuestion(question, index)
        if (qWords.isEmpty()) return fallbackHelp(herbs) to false

        val hits = gatherCandidates(qWords, herbs, index)
        val blendHits = gatherBlendCandidates(qWords, blends, index)
        if (hits.isEmpty() && blendHits.isEmpty()) return fallbackHelp(herbs) to false

        val organized = organizeHits(hits)
        val organizedBlends = organizeBlendHits(blendHits)
        return composeAnswer(organized, organizedBlends) to true
    }

    /**
     * المرحلة ١ — تحليل السؤال: كلمات مفتاحية، مُوسَّعة على ثلاث مراحل
     * متتالية (كل مرحلة تضيف احتمالات مطابقة أكثر، بلا حذف لما قبلها):
     * 1) علاقات الموسوعة الضمنية ([CorpusIndex.expand]).
     * 2) جذور اللغة التقريبية ([ArabicLexicon.expand]) — تُطبَّق *قبل*
     *    القاموس عمداً (كانت بعده سابقاً): [DictionaryLexicon.synonymsOf]
     *    يبحث عن الكلمة بصيغتها المُطبَّعة تماماً كمفتاح أساسي في قاعدة
     *    البيانات، فكلمة سؤال بصيغة مختلفة عن الصيغة المخزَّنة (جمع، لاحقة
     *    ضمير، أداة تعريف...) كانت تفوّت أي مرادف موجود فعلياً لجذرها لمجرد
     *    اختلاف الصيغة — لا لعدم وجود المرادف. تجذير الكلمة أولاً ثم البحث
     *    عن مرادفات كل من الصيغة الأصلية *و* جذرها يرفع فعلياً عدد المرادفات
     *    التي يستخدمها سيمو من نفس القاموس المرفق دون أي بيانات إضافية.
     * 3) مرادفات القاموس الخارجي المرفق محلياً ([DictionaryLexicon.expand]
     *    — Rabih Dictionary + Arabic WordNet، بلا إنترنت ولا تكلفة)، وهذا
     *    ما يمكّن سيمو من فهم اسم بديل لعشبة أو مرادف عام لكلمة في السؤال
     *    (مثل "دواء" بدل "علاج") لم تُذكر حرفياً في نص الموسوعة.
     */
    private fun analyzeQuestion(question: String, index: CorpusIndex): Set<String> {
        val base = wordsOf(question)
        if (base.isEmpty()) return base
        val expandedByCorpus = index.expand(base)
        val expandedByStems = ArabicLexicon.expand(expandedByCorpus)
        val expandedByDictionary = DictionaryLexicon.expand(expandedByStems)
        return HealthTopicSynonyms.expand(expandedByDictionary)
    }

    /**
     * توسيع "خفيف" مماثل لِـ[analyzeQuestion] (تجذير + قاموس المرادفات)
     * لكن بلا حاجة لفهرس موسوعة ([CorpusIndex]) — يُستخدم لمطابقة نصوص
     * قصيرة مستقلة عن نصوص عشبة معيّنة (حالات التدريب اليدوي/الذاتي)، حتى
     * تفهم مطابقة الحالات المدرَّبة صياغات مرادفة لا الصياغة الحرفية فقط
     * — وهذا هو أثر "تعلّم سيمو من المرادفات" فعلياً على الحالات التي
     * يحفظها من تقييمات المستخدمين.
     */
    private fun richWordsOf(text: String): Set<String> {
        val base = wordsOf(text)
        if (base.isEmpty()) return base
        return HealthTopicSynonyms.expand(DictionaryLexicon.expand(ArabicLexicon.expand(base)))
    }

    /** المرحلة ٢ — "التفكير بالإجابة": مسح كل نقاط كل حقل، وترجيح كل نقطة حسب مدى صلتها الفعلية بالسؤال. */
    private fun gatherCandidates(qWords: Set<String>, herbs: List<Herb>, index: CorpusIndex): List<SearchHit> {
        val threshold = AiConfig.searchThreshold
        val hits = mutableListOf<SearchHit>()
        herbs.forEach { herb ->
            searchableFields.forEach { (label, getter) ->
                splitPoints(getter(herb)).forEach { point ->
                    val sim = matchScore(index, qWords, point)
                    if (sim > threshold) hits += SearchHit(herb, label, point, sim)
                }
            }
        }
        return hits
    }

    /**
     * المرحلة ٣ — تنسيق الأفكار: تُجمَّع كل النقاط حسب العشبة أولاً، ثم
     * تُرتَّب *الأعشاب نفسها* تنازلياً حسب أقوى نقطة لديها (لا الاكتفاء
     * بترتيب النقاط المبعثرة عالمياً كما كان سابقاً)، فتظهر العشبة الأكثر
     * صلة بالسؤال أولاً دوماً، مع أفضل ٣ نقاط من نصوصها فقط — هذا هو
     * "التحليل" الفعلي لبيانات الموسوعة بدل عرض أول ٤ نقاط بغضّ النظر عن
     * مصدرها.
     */
    private fun organizeHits(hits: List<SearchHit>): Map<Herb, List<SearchHit>> =
        hits.groupBy { it.herb }
            .entries
            .sortedByDescending { (_, herbHits) -> herbHits.maxOf { it.score } }
            .take(3)
            .associate { (herb, herbHits) -> herb to herbHits.sortedByDescending { it.score }.take(3) }

    /**
     * المرحلة ٤ — تجميع النتيجة النهائية وإرسالها كرد واحد مقروء. تُعرض
     * نتائج الأعشاب أولاً ثم الخلطات (إن وُجدت) في قسم منفصل بعلامة مميّزة
     * (🧪) حتى يُدرك المستخدم أن الرد قد يخلط بين نوعين مختلفين من عناصر
     * الموسوعة.
     */
    private fun composeAnswer(
        organized: Map<Herb, List<SearchHit>>,
        organizedBlends: Map<Blend, List<BlendHit>> = emptyMap()
    ): String = buildString {
        append("بحثت وحلّلت بيانات الموسوعة، وهذه أقرب النتائج لسؤالك:\n\n")
        organized.forEach { (herb, herbHits) ->
            append("🔸 ${herb.name}:\n")
            herbHits.forEach { append("• [${it.field}] ${it.text}\n") }
            append("\n")
        }
        organizedBlends.forEach { (blend, blendHits) ->
            append("🧪 خلطة ${blend.name}:\n")
            blendHits.forEach { append("• [${it.field}] ${it.text}\n") }
            append("\n")
        }
    }.trimEnd()

    private fun fallbackHelp(herbs: List<Herb>): String =
        if (herbs.size <= 3)
            "لم أجد إجابة مباشرة لسؤالك ضمن بيانات ${herbNames(herbs)}. جرّب أن تسأل عن: الفوائد، الاستخدام، التحذيرات، أو الفرق بينها إن ذكرت أكثر من عشبة."
        else
            "لم أجد إجابة مباشرة لسؤالك في الموسوعة. جرّب ذكر اسم عشبة معيّنة، أو اسأل عن أعراض/فائدة محددة تبحث عن عشبة لها."
}
