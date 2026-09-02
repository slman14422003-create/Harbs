package com.salman.herbalencyclopedia.data.search

import com.salman.herbalencyclopedia.data.ai.ArabicLexicon
import com.salman.herbalencyclopedia.data.ai.DictionaryLexicon
import com.salman.herbalencyclopedia.data.model.Herb

/**
 * محرك البحث المباشر عن الأعشاب (شاشة "كل الأعشاب" وشاشة "بحث" — مختلف عن
 * البحث الحر لسيمو في [com.salman.herbalencyclopedia.data.ai.HerbAssistant]
 * الذي يجيب بجمل كاملة). كان البحث سابقاً `it.name.contains(query)` حرفياً:
 * لا يطابق "الزعتر" مع "زعتر" (أداة التعريف)، ولا "زنجبيل" مع "الزّنجبيل"
 * (تشكيل)، ولا يفهم اسماً بديلاً أو خطأ إملائياً بسيطاً — وهذا بالضبط سبب
 * عجز البحث عن إيجاد أعشاب موجودة فعلاً في الموسوعة.
 *
 * هذا المحرك محلي بالكامل (بلا إنترنت، بلا مفتاح API، بلا تكلفة، يعمل دوماً
 * حتى بلا اتصال — "مضمون ومجاني" تماماً كباقي التطبيق) ويحسّن المطابقة عبر
 * مرحلتين إضافيتين فوق المطابقة النصية المباشرة:
 * 1) **تطبيع عربي** (نفس قواعد `normalize()` في [HerbAssistant]): إزالة
 *    التشكيل، توحيد صور الألف/الياء/التاء المربوطة، فتُطابق "الزعتر" و
 *    "زعتر" و"الزّعتر" ككلمة واحدة.
 * 2) **توسيع بمرادفات القاموس** ([DictionaryLexicon]، من Rabih Dictionary
 *    وArabic WordNet): تبحث عن اسم بديل أو مرادف شائع لما كتبه المستخدم
 *    ضمن اسم العشبة أو نصوصها، لا حرفياً فقط.
 *
 * الترتيب: مطابقة الاسم أولاً (الأهم للمستخدم)، ثم مطابقة داخل نصوص العشبة
 * (الفوائد/الاستخدام/الملاحظات)، بحيث تظهر أدق النتائج أولاً بدل ترتيب عشوائي.
 */
object HerbSearch {

    private val HARAKAT = Regex("[\u064B-\u0652]")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}\\s]")
    private val SPACES = Regex("\\s+")

    /** نفس منطق التطبيع المستخدم في [HerbAssistant] (مكرَّر عمداً هنا كي يبقى
     * هذا الملف مستقلاً وقابلاً لإعادة الاستخدام من أي شاشة بلا اعتمادية
     * إضافية على تفاصيل سيمو الداخلية). */
    fun normalize(text: String): String {
        var t = text
        t = t.replace(HARAKAT, "")
        t = t.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        t = t.replace('ى', 'ي').replace('ة', 'ه')
        t = t.replace(NON_WORD, " ")
        return t.trim().lowercase()
    }

    private fun tokens(normalizedText: String): List<String> =
        normalizedText.split(SPACES).filter { it.length > 1 }

    private data class Scored(val herb: Herb, val score: Int)

    /**
     * مسافة تحرير (Levenshtein) محدودة الحجم — تُستخدم فقط لالتقاط خطأ
     * إملائي بسيط في اسم عشبة (حرف زائد/ناقص/مبدَّل) عندما تفشل كل مطابقة
     * نصية أو بمرادف أخرى. الكلمات هنا قصيرة دوماً (أسماء أعشاب مفردة) لذا
     * التكلفة مهملة حتى بخوارزمية DP الكاملة البسيطة.
     */
    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }

    /**
     * يبحث عن كل الأعشاب المطابقة للاستعلام، مرتّبة تنازلياً حسب دقة
     * المطابقة. يعيد قائمة فارغة إن كان الاستعلام فارغاً (بدل كل الأعشاب)،
     * كي تستمر شاشات البحث بعرض "اكتب للبحث" كما كانت.
     *
     * محسَّن على ثلاث جبهات فوق النسخة السابقة (كانت تتطلب مطابقة الجملة
     * كاملة حرفياً ضمن نصوص العشبة، فتفشل مع أي سؤال طبيعي متعدد الكلمات):
     * 1) **توسيع الكلمات بجذورها التقريبية** ([ArabicLexicon.lightStem])
     *    بالإضافة لمرادفات القاموس الخارجي، فتُطابق "بينفع" مع "نافع" مثلاً.
     * 2) **مطابقة على مستوى الكلمات لا الجملة كاملة**: يُحسب عدد كلمات
     *    الاستعلام (بعد التوسيع) الموجودة فعلياً في اسم العشبة أو نصوصها،
     *    وتُرجَّح النتيجة حسب *نسبة* الكلمات المطابقة، بدل اشتراط وجود
     *    الجملة بالضبط.
     * 3) **تسامح مع خطأ إملائي بسيط** في اسم العشبة (مسافة تحرير ≤ 1) عند
     *    عدم وجود أي تطابق نصي أو بمرادف بعد كل المحاولات السابقة.
     */
    fun search(query: String, herbs: List<Herb>): List<Herb> {
        val qNorm = normalize(query)
        if (qNorm.isBlank()) return emptyList()

        val qTokens = tokens(qNorm)
        if (qTokens.isEmpty()) return emptyList()

        val stemmedTokens = qTokens.map { ArabicLexicon.lightStem(it) }.filter { it.length > 1 }
        val dictSynonyms = (qTokens + stemmedTokens).flatMap { DictionaryLexicon.synonymsOf(it) }.toSet()
        val expandedTokens = (qTokens + stemmedTokens).toSet() + dictSynonyms

        val scored = herbs.mapNotNull { herb ->
            val nameNorm = normalize(herb.name)
            val nameTokens = tokens(nameNorm)
            var score = 0

            when {
                nameNorm.isBlank() -> {}
                nameNorm == qNorm -> score = 100
                nameNorm.contains(qNorm) || qNorm.contains(nameNorm) -> score = 70
                expandedTokens.any { it.length > 1 && nameNorm.contains(it) } -> score = 45
                else -> {
                    // مطابقة جزئية على مستوى الكلمات بين اسم العشبة والاستعلام
                    // الموسّع: كل كلمة مشتركة (أو تحتوي إحداهما الأخرى، لالتقاط
                    // جمع/تصغير بسيط) ترفع الدرجة تدريجياً بدل رفض النتيجة كلياً.
                    val overlap = nameTokens.count { nt ->
                        expandedTokens.any { et -> et == nt || nt.contains(et) || et.contains(nt) }
                    }
                    if (overlap > 0) score = 30 + (overlap * 6).coerceAtMost(20)
                }
            }

            // تسامح مع خطأ إملائي بسيط في الاسم (حرف مبدَّل/زائد/ناقص) فقط إن
            // فشلت كل المطابقات النصية والمرادفات أعلاه.
            if (score == 0 && qTokens.size == 1 && qNorm.length >= 3) {
                val hasCloseTypo = nameTokens.any { nt ->
                    nt.length >= 3 && kotlin.math.abs(nt.length - qNorm.length) <= 1 &&
                        editDistance(nt, qNorm) <= 1
                }
                if (hasCloseTypo) score = 55
            }

            if (score == 0) {
                val fieldsNorm = normalize(
                    herb.benefits + " " + herb.usage + " " + herb.warnings + " " +
                        herb.harms + " " + herb.notes
                )
                if (fieldsNorm.contains(qNorm)) {
                    score = 20
                } else {
                    val fieldTokens = tokens(fieldsNorm).toSet()
                    val matchedCount = expandedTokens.count { et ->
                        et.length > 1 && fieldTokens.any { it == et || it.contains(et) || et.contains(it) }
                    }
                    if (matchedCount > 0) {
                        // كلما زادت نسبة كلمات الاستعلام المطابَقة فعلياً داخل
                        // نصوص العشبة، ارتفع ترتيبها — عدد الكلمات المطابقة
                        // وحده لا يكفي مؤشراً بدون أخذ طول السؤال بالحسبان.
                        val ratio = matchedCount.toDouble() / qTokens.size.coerceAtLeast(1)
                        score = (6 + ratio * 14).toInt().coerceAtLeast(6)
                    }
                }
            }

            if (score > 0) Scored(herb, score) else null
        }

        return scored.sortedByDescending { it.score }.map { it.herb }
    }
}
