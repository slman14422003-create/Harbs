package com.salman.herbalencyclopedia.data.ai

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
     * يُعاد بناؤه فقط عند تغيّر قائمة الأعشاب المرجعة (انظر [corpusIndexFor]).
     */
    private class CorpusIndex(herbs: List<Herb>) {
        private val idf: Map<String, Double>
        private val related: Map<String, List<String>>

        init {
            val points = mutableListOf<Set<String>>()
            herbs.forEach { herb ->
                listOf(herb.benefits, herb.usage, herb.warnings, herb.harms, herb.notes).forEach { field ->
                    splitPoints(field).forEach { p ->
                        val w = wordsOf(p)
                        if (w.isNotEmpty()) points += w
                    }
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

    // ذاكرة تخزين مؤقت بسيطة: يُعاد بناء الفهرس فقط عند تغيّر مرجع قائمة
    // الأعشاب (تُنشئ شاشات التطبيق قائمة جديدة عند أي تحديث فعلي للبيانات).
    private var cachedIndex: CorpusIndex? = null
    private var cachedForHerbs: List<Herb>? = null

    private fun corpusIndexFor(herbs: List<Herb>): CorpusIndex {
        val current = cachedIndex
        if (current != null && cachedForHerbs === herbs) return current
        val built = CorpusIndex(herbs)
        cachedIndex = built
        cachedForHerbs = herbs
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
     * يقارن سؤال المستخدم بكل "الحالات المدرَّبة" — اليدوية أولاً
     * ([AiConfig.trainedExamples]، أولوية مطلقة دوماً لأنها من مراجعة
     * المطوّر مباشرة)، ثم المتعلَّمة ذاتياً ([AiConfig.autoLearnedExamples])
     * بعتبة أعلى قليلاً تحوّطاً لأنها غير مراجَعة يدوياً. يعيد أقرب رد
     * مخصّص إن تجاوز التشابه العتبة المناسبة، وإلا يعيد null ليكمل سيمو
     * بمنطقه العام.
     */
    private fun matchTrainedExample(question: String): String? {
        val qWords = wordsOf(question)
        if (qWords.isEmpty()) return null

        var bestManual: String? = null
        var bestManualScore = 0.0
        AiConfig.trainedExamples.forEach { example ->
            val score = jaccard(qWords, wordsOf(example.pattern))
            if (score > bestManualScore) { bestManualScore = score; bestManual = example.response }
        }
        if (bestManualScore >= AiConfig.trainedMatchThreshold) return bestManual

        if (!AiConfig.autoLearnEnabled || AiConfig.autoLearnedExamples.isEmpty()) return null
        var bestAuto: String? = null
        var bestAutoScore = 0.0
        AiConfig.autoLearnedExamples.forEach { example ->
            val score = jaccard(qWords, wordsOf(example.pattern))
            if (score > bestAutoScore) { bestAutoScore = score; bestAuto = example.response }
        }
        val autoThreshold = (AiConfig.trainedMatchThreshold + 0.15).coerceAtMost(0.95)
        return if (bestAutoScore >= autoThreshold) bestAuto else null
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
     * يعيد القائمة المحدَّثة مباشرة ليحفظها المستدعي (PreferencesRepository).
     */
    fun recordFeedback(question: String, reply: String, helpful: Boolean): List<TrainedExample> {
        val qWords = wordsOf(question)
        if (qWords.isEmpty()) return AiConfig.autoLearnedExamples

        if (!helpful) {
            val remaining = AiConfig.autoLearnedExamples.filterNot {
                jaccard(qWords, wordsOf(it.pattern)) >= AiConfig.trainedMatchThreshold
            }
            AiConfig.autoLearnedExamples = remaining
            return remaining
        }

        if (!AiConfig.autoLearnEnabled) return AiConfig.autoLearnedExamples
        val alreadyKnown = AiConfig.autoLearnedExamples.any {
            jaccard(qWords, wordsOf(it.pattern)) >= AiConfig.trainedMatchThreshold
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
                    for (pj in otherPoints.indices) {
                        if (usedFlags[hj][pj]) continue
                        val sim = weightedSimilarity(index, pw, wordsOf(otherPoints[pj]))
                        if (sim >= threshold) {
                            usedFlags[hj][pj] = true
                            if (otherId !in matched) matched += otherId
                        }
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
     * يبحث عن الأعشاب المذكورة صراحةً باسمها داخل نص السؤال الحر (مطابقة
     * جزئية بعد التطبيع)، ليتمكن "سيمو" من فهم أسئلة مثل "ما فوائد الزنجبيل؟"
     * أو "قارن بين البابونج والنعناع" دون أن يضطر المستخدم لاختيار أي عشبة
     * مسبقاً من قائمة — التحديد يتم من نص المحادثة نفسه.
     */
    fun relevantHerbs(question: String, allHerbs: List<Herb>): List<Herb> {
        val qNorm = normalize(question)
        if (qNorm.isBlank()) return emptyList()
        return allHerbs.filter { herb -> herb.name.isNotBlank() && qNorm.contains(normalize(herb.name)) }
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
    fun answer(question: String, herbs: List<Herb>, allowCompare: Boolean = true): String =
        answerDetailed(question, herbs, allowCompare).text

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
    fun answerDetailed(question: String, herbs: List<Herb>, allowCompare: Boolean = true): AssistantReply {
        if (herbs.isEmpty()) {
            return AssistantReply("لم أجد في الموسوعة معلومات كافية للإجابة على هذا السؤال بعد 🌿", false)
        }
        val qNorm = normalize(question)
        if (qNorm.isBlank()) {
            return AssistantReply("تفضّل، اسأل سيمو عن أي عشبة: فوائدها، طريقة استخدامها، أو تحذيراتها.", false)
        }

        // أولوية مطلقة للحالات المدرَّبة (يدوياً من المطوّر، أو ذاتياً من
        // تقييمات المستخدمين السابقة) — إن وُجدت مطابقة كافية، يستخدم سيمو
        // ردّها مباشرة قبل أي منطق عام آخر.
        matchTrainedExample(question)?.let { return AssistantReply(it, false) }

        // تدريب سيمو الافتتاحي (أكثر من 500 صياغة محادثة، انظر [ConversationalSeed])
        // يأتي بعد تدريب المطوّر مباشرة وقبل أي منطق آخر: يغطي التحية والشكر
        // والوداع وأسئلة الهوية والقدرات، فلا تحتاج هذه لأي بحث في الموسوعة.
        ConversationalSeed.match(question)?.let { return AssistantReply(it, false) }

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

            specific && containsAny(qNorm, listOf("فائدة", "فائده", "فوائد", "يفيد", "علاج", "يعالج", "مفيد")) ->
                AssistantReply(buildBenefitsAnswer(herbs), false)

            else -> {
                val (text, learnable) = buildGeneralSearchAnswer(question, herbs)
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

    private fun buildBenefitsAnswer(herbs: List<Herb>): String {
        if (herbs.size < 2) {
            val herb = herbs.first()
            return "🔸 ${herb.name}: ${herb.benefits.ifBlank { "لا توجد فوائد مسجّلة لهذه العشبة في الموسوعة بعد." }}"
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

    private data class SearchHit(val herb: Herb, val field: String, val text: String, val score: Double)

    private val searchableFields = listOf<Pair<String, (Herb) -> String>>(
        "الفوائد" to { it.benefits },
        "الاستخدام" to { it.usage },
        "التحذيرات" to { it.warnings },
        "الأضرار" to { it.harms },
        "ملاحظات" to { it.notes }
    )

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
    private fun buildGeneralSearchAnswer(question: String, herbs: List<Herb>): Pair<String, Boolean> {
        val index = corpusIndexFor(herbs)
        val qWords = analyzeQuestion(question, index)
        if (qWords.isEmpty()) return fallbackHelp(herbs) to false

        val hits = gatherCandidates(qWords, herbs, index)
        if (hits.isEmpty()) return fallbackHelp(herbs) to false

        val organized = organizeHits(hits)
        return composeAnswer(organized) to true
    }

    /**
     * المرحلة ١ — تحليل السؤال: كلمات مفتاحية، مُوسَّعة على ثلاث مراحل
     * متتالية (كل مرحلة تضيف احتمالات مطابقة أكثر، بلا حذف لما قبلها):
     * 1) علاقات الموسوعة الضمنية ([CorpusIndex.expand]).
     * 2) مرادفات القاموس الخارجي المرفق محلياً ([DictionaryLexicon.expand]
     *    — Rabih Dictionary + Arabic WordNet، بلا إنترنت ولا تكلفة)، وهذا
     *    ما يمكّن سيمو من فهم اسم بديل لعشبة أو مرادف عام لكلمة في السؤال
     *    (مثل "دواء" بدل "علاج") لم تُذكر حرفياً في نص الموسوعة.
     * 3) جذور اللغة التقريبية ([ArabicLexicon.expand]).
     */
    private fun analyzeQuestion(question: String, index: CorpusIndex): Set<String> {
        val base = wordsOf(question)
        if (base.isEmpty()) return base
        val expandedByCorpus = index.expand(base)
        val expandedByDictionary = DictionaryLexicon.expand(expandedByCorpus)
        return ArabicLexicon.expand(expandedByDictionary)
    }

    /** المرحلة ٢ — "التفكير بالإجابة": مسح كل نقاط كل حقل، وترجيح كل نقطة حسب مدى صلتها الفعلية بالسؤال. */
    private fun gatherCandidates(qWords: Set<String>, herbs: List<Herb>, index: CorpusIndex): List<SearchHit> {
        val threshold = AiConfig.searchThreshold
        val hits = mutableListOf<SearchHit>()
        herbs.forEach { herb ->
            searchableFields.forEach { (label, getter) ->
                splitPoints(getter(herb)).forEach { point ->
                    val sim = weightedSimilarity(index, qWords, wordsOf(point))
                    if (sim > threshold) hits += SearchHit(herb, label, point, sim)
                }
            }
        }
        return hits
    }

    /** المرحلة ٣ — تنسيق الأفكار: أفضل النتائج فقط، مجمّعة حسب العشبة بدل عرضها مبعثرة. */
    private fun organizeHits(hits: List<SearchHit>): Map<Herb, List<SearchHit>> =
        hits.sortedByDescending { it.score }
            .take(4)
            .groupBy { it.herb }

    /** المرحلة ٤ — تجميع النتيجة النهائية وإرسالها كرد واحد مقروء. */
    private fun composeAnswer(organized: Map<Herb, List<SearchHit>>): String = buildString {
        append("وجدت هذه المعلومات ذات الصلة:\n\n")
        organized.forEach { (herb, herbHits) ->
            append("🔸 ${herb.name}:\n")
            herbHits.forEach { append("• [${it.field}] ${it.text}\n") }
        }
    }

    private fun fallbackHelp(herbs: List<Herb>): String =
        if (herbs.size <= 3)
            "لم أجد إجابة مباشرة لسؤالك ضمن بيانات ${herbNames(herbs)}. جرّب أن تسأل عن: الفوائد، الاستخدام، التحذيرات، أو الفرق بينها إن ذكرت أكثر من عشبة."
        else
            "لم أجد إجابة مباشرة لسؤالك في الموسوعة. جرّب ذكر اسم عشبة معيّنة، أو اسأل عن أعراض/فائدة محددة تبحث عن عشبة لها."
}
