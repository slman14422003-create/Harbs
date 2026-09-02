package com.salman.herbalencyclopedia.data.search

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
     * يبحث عن كل الأعشاب المطابقة للاستعلام، مرتّبة تنازلياً حسب دقة
     * المطابقة. يعيد قائمة فارغة إن كان الاستعلام فارغاً (بدل كل الأعشاب)،
     * كي تستمر شاشات البحث بعرض "اكتب للبحث" كما كانت.
     */
    fun search(query: String, herbs: List<Herb>): List<Herb> {
        val qNorm = normalize(query)
        if (qNorm.isBlank()) return emptyList()

        val qTokens = tokens(qNorm)
        val synonymTokens = qTokens.flatMap { DictionaryLexicon.synonymsOf(it) }.toSet()
        val expandedTokens = qTokens.toSet() + synonymTokens

        val scored = herbs.mapNotNull { herb ->
            val nameNorm = normalize(herb.name)
            var score = 0

            when {
                nameNorm.isBlank() -> {}
                nameNorm == qNorm -> score = 100
                nameNorm.contains(qNorm) || qNorm.contains(nameNorm) -> score = 70
                expandedTokens.any { nameNorm.contains(it) } -> score = 45
            }

            if (score == 0) {
                val fieldsNorm = normalize(
                    herb.benefits + " " + herb.usage + " " + herb.warnings + " " +
                        herb.harms + " " + herb.notes
                )
                if (fieldsNorm.contains(qNorm)) score = 20
                else if (expandedTokens.any { it.length > 1 && fieldsNorm.contains(it) }) score = 10
            }

            if (score > 0) Scored(herb, score) else null
        }

        return scored.sortedByDescending { it.score }.map { it.herb }
    }
}
