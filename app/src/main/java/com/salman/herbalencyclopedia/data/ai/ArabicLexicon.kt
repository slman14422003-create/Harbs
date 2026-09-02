package com.salman.herbalencyclopedia.data.ai

/**
 * "قاموس" سيمو للغة العربية — بديل محلي وخفيف عن ملف قاموس ضخم يحوي كل
 * الكلمات: بدل حفظ مئات آلاف الكلمات (وهو ما يحتاج تحميل ملف كبير من
 * الإنترنت لا يمكن تضمينه هنا فعلياً)، هذا الصف يطبّق "تجذيراً خفيفاً"
 * (light stemming) يزيل أشهر السوابق واللواحق العربية عن أي كلمة عربية —
 * فيتعرّف عملياً على *كل* الكلمات العربية بصيغها المختلفة (جمع، تأنيث،
 * أدوات تعريف، ضمائر متصلة...) دون الحاجة لسردها واحدة واحدة، وهذا مجاني
 * بالكامل ويعمل بلا إنترنت.
 *
 * يُستخدم هذا فقط لتوسيع كلمات سؤال المستخدم في البحث الحر (إضافة احتمالات
 * أكثر للمطابقة)، ولا يستبدل [normalize]/wordsOf الأساسية في باقي التطبيق
 * (المقارنة المنظمة مثلاً) تفادياً لأي أثر جانبي غير مقصود على ميزات تعمل
 * أصلاً بشكل جيد.
 */
internal object ArabicLexicon {

    private val prefixes = listOf(
        "بال", "كال", "فال", "وال", "لل", "ال", "بـ", "كـ", "فـ", "لـ", "وـ", "و", "ف", "ب", "ك", "ل"
    )

    private val suffixes = listOf(
        "اتها", "اتهم", "اتهن", "ياتي", "كما", "هما", "تين", "ات", "ون", "ين",
        "ان", "ية", "تي", "كم", "كن", "هم", "هن", "ها", "نا", "ني", "ه", "ي", "ة"
    )

    /** أقل طول للكلمة بعد التجذير كي لا تختفي كلمات قصيرة أصلاً (تفادي الإفراط في القص). */
    private const val MIN_STEM_LENGTH = 2

    /**
     * يعيد جذراً تقريبياً واحداً للكلمة (بعد إزالة أشهر سابقة ولاحقة واحدة
     * فقط في كل مرة، بلا تكرار عميق يشوّه الكلمة). يعيد الكلمة كما هي إذا
     * كانت قصيرة أصلاً أو لم يوجد ما يُزال.
     */
    fun lightStem(word: String): String {
        if (word.length <= MIN_STEM_LENGTH + 1) return word
        var w = word

        val prefix = prefixes.firstOrNull { w.startsWith(it) && w.length - it.length >= MIN_STEM_LENGTH }
        if (prefix != null) w = w.removePrefix(prefix)

        val suffix = suffixes.firstOrNull { w.endsWith(it) && w.length - it.length >= MIN_STEM_LENGTH }
        if (suffix != null) w = w.removeSuffix(suffix)

        return w.ifBlank { word }
    }

    /** يوسّع مجموعة كلمات (إضافة الجذور التقريبية لها) دون حذف الكلمات الأصلية. */
    fun expand(words: Set<String>): Set<String> {
        if (words.isEmpty()) return words
        val stems = words.map { lightStem(it) }.filter { it.length >= MIN_STEM_LENGTH }
        return words + stems
    }
}
