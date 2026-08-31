package com.salman.herbalencyclopedia.data.ai

import com.salman.herbalencyclopedia.data.model.Herb

/**
 * إعدادات قابلة للتعديل من "أدوات المطور" داخل التطبيق (AdminToolsScreen)
 * دون الحاجة لإعادة بناء التطبيق: عتبات التشابه وكلمات الإيقاف الإضافية.
 * القيم تُحمَّل من PreferencesRepository عند بدء التطبيق وتُطبَّق هنا مباشرة
 * ([applyStoredSettings])، وأي تعديل من شاشة الأدوات يُحدّثها فوراً وحياً
 * في نفس الجلسة (بلا حاجة لإعادة تشغيل) لأن كل الشاشات تقرأ من هذا الكائن.
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

    val defaultSimilarityThreshold = 0.34
    val defaultSearchThreshold = 0.12

    fun resetToDefaults() {
        similarityThreshold = defaultSimilarityThreshold
        searchThreshold = defaultSearchThreshold
        extraStopWords = emptySet()
    }
}

/**
 * مساعد المقارنة الذكي — يعمل بالكامل داخل الجهاز، بلا اتصال إنترنت، بلا
 * مفتاح API، وبلا أي تكلفة أو إعداد. يقرأ نصوص الأعشاب الموجودة أصلاً في
 * الموسوعة (الفوائد، الاستخدام، التحذيرات، الأضرار، الملاحظات)، يحلّلها،
 * يوجد المشترك والمختلف بينها، وينسّقها في مقارنة منظّمة، كما يجيب فورياً
 * على أسئلة المستخدم الحرة ضمن شاشة المقارنة — كأنها محادثة مباشرة معه.
 *
 * "تدريبه" هنا يعني ضبط عتباته وقاموسه من بيانات الموسوعة نفسها (لا يوجد
 * نموذج شبكة عصبية يحتاج تدريباً فعلياً) — انظر [AiConfig] للقيم القابلة
 * للتعديل حياً من أدوات المطور.
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

    private fun wordsOf(text: String): Set<String> =
        normalize(text).split(Regex("\\s+"))
            .filter { it.length > 1 && it !in stopWords }
            .toSet()

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

    // ── المقارنة المنظّمة (يُستخدم في بطاقات المقارنة بالشاشة) ─────────

    /** نقطة مقارنة واحدة (فائدة، تحذير...) مع قائمة الأعشاب التي وردت فيها. */
    data class ComparisonPoint(val text: String, val herbIds: List<String>)

    /**
     * يبني مقارنة نقطة-بنقطة لحقل معيّن (مثل الفوائد) عبر الأعشاب المختارة:
     * يجمع النقاط المتشابهة معنوياً من أعشاب مختلفة في نقطة واحدة مشتركة،
     * ويترك النقاط المنفردة كما هي، بحيث تُعرض النتيجة منظّمة وواضحة.
     */
    fun compareField(herbs: List<Herb>, field: (Herb) -> String): List<ComparisonPoint> {
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
                        val sim = jaccard(pw, wordsOf(otherPoints[pj]))
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

    /** أسئلة سريعة مقترحة تُعرض كأزرار فوق مربع الدردشة. */
    fun quickSuggestions(herbs: List<Herb>): List<String> =
        if (herbs.size >= 2) listOf(
            "ما أبرز الفروقات بينها؟",
            "أيهما أكثر أماناً؟",
            "هل يمكن الجمع بينهما؟",
            "ما طريقة استخدام كل منها؟"
        ) else listOf(
            "ما فوائدها؟",
            "ما طريقة استخدامها؟",
            "هل لها تحذيرات؟"
        )

    // ── الدردشة الذكية (إجابة حرة على أسئلة المستخدم) ──────────────────

    /**
     * يجيب على سؤال حر بالاعتماد على بيانات عشبة واحدة أو أكثر. لا يوجد هنا
     * أي حجب أو تقييد صناعي على المحتوى — المساعد محلي بالكامل ويستخدم فقط
     * نصوص الموسوعة التي أدخلها المطوّر، فيجيب دوماً بأفضل ما يتوفر لديه من
     * معلومات، ويوضّح بصراحة عندما لا تتوفر بيانات كافية بدل رفض الإجابة.
     */
    fun answer(question: String, herbs: List<Herb>): String {
        if (herbs.isEmpty()) return "اختر عشبة واحدة على الأقل ليبدأ المساعد بالإجابة على أسئلتك 🌿"
        val qNorm = normalize(question)
        if (qNorm.isBlank()) return "تفضّل، اسأل عن الفوائد أو الاستخدام أو التحذيرات لـ ${herbNames(herbs)}."

        return when {
            containsAny(qNorm, listOf("مرحبا", "اهلا", "أهلا", "السلام عليكم", "hello", "hi")) ->
                "أهلاً 👋 أنا مساعد الأعشاب الذكي. اسألني عن ${herbNames(herbs)}: فوائدها، طريقة استخدامها، أو تحذيراتها، وسأجيبك فوراً من بيانات الموسوعة."

            containsAny(qNorm, listOf("شكرا", "شكراً", "تسلم", "يعطيك العافية", "مشكور")) ->
                "عفواً 🌿 تفضّل بأي سؤال آخر عن ${herbNames(herbs)}."

            herbs.size >= 2 && containsAny(qNorm, listOf("جمع", "دمج", "معا", "معاً", "سوية", "سويا", "نفس الوقت", "تفاعل", "خلط")) ->
                buildCombineAnswer(herbs)

            containsAny(qNorm, listOf("خطر", "اضرار", "أضرار", "تحذير", "حامل", "حمل", "رضاعة", "رضاعه", "طفل", "اطفال", "أطفال", "امان", "أمان", "اثار جانبية", "آثار جانبية")) ->
                buildSafetyAnswer(herbs, qNorm)

            containsAny(qNorm, listOf("استخدام", "استعمال", "طريقة", "طريقه", "كيف استخدم", "جرعة", "جرعه", "مقدار")) ->
                buildUsageAnswer(herbs)

            herbs.size >= 2 && containsAny(qNorm, listOf("فرق", "يختلف", "اختلاف", "افضل", "أفضل", "احسن", "أحسن", "ايهما", "أيهما")) ->
                buildOverview(herbs) + "\n\n" + buildSafetyGlance(herbs)

            containsAny(qNorm, listOf("فائدة", "فائده", "فوائد", "يفيد", "علاج", "يعالج", "مفيد")) ->
                buildBenefitsAnswer(herbs)

            else -> buildGeneralSearchAnswer(question, herbs)
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

    private fun buildGeneralSearchAnswer(question: String, herbs: List<Herb>): String {
        val qWords = wordsOf(question)
        if (qWords.isEmpty()) return fallbackHelp(herbs)

        data class Hit(val herb: Herb, val field: String, val text: String, val score: Double)

        val fieldsLabeled = listOf<Pair<String, (Herb) -> String>>(
            "الفوائد" to { it.benefits },
            "الاستخدام" to { it.usage },
            "التحذيرات" to { it.warnings },
            "الأضرار" to { it.harms },
            "ملاحظات" to { it.notes }
        )

        val hits = mutableListOf<Hit>()
        val threshold = AiConfig.searchThreshold
        herbs.forEach { herb ->
            fieldsLabeled.forEach { (label, getter) ->
                splitPoints(getter(herb)).forEach { point ->
                    val sim = jaccard(qWords, wordsOf(point))
                    if (sim > threshold) hits += Hit(herb, label, point, sim)
                }
            }
        }

        val top = hits.sortedByDescending { it.score }.take(4)
        if (top.isEmpty()) return fallbackHelp(herbs)

        return buildString {
            append("وجدت هذه المعلومات ذات الصلة:\n\n")
            top.groupBy { it.herb }.forEach { (herb, herbHits) ->
                append("🔸 ${herb.name}:\n")
                herbHits.forEach { append("• [${it.field}] ${it.text}\n") }
            }
        }
    }

    private fun fallbackHelp(herbs: List<Herb>): String =
        "لم أجد إجابة مباشرة لسؤالك ضمن بيانات ${herbNames(herbs)}. جرّب أن تسأل عن: الفوائد، الاستخدام، التحذيرات، أو الفرق بينها إن اخترت أكثر من عشبة."
}
