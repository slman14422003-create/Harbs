package com.salman.herbalencyclopedia.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/**
 * قاموس مرادفات عربي عام، مبني مسبقاً من مصدرين حرّين ومفتوحين ومرفقين
 * محلياً بالكامل مع التطبيق — بنفس فلسفة [ArabicLexicon] بالضبط: بلا
 * إنترنت، بلا مفتاح API، وبلا أي تكلفة أو اشتراك أو حد استخدام (وهذا هو
 * المقصود بـ"محرك بحث مضمون ومجاني": مضمون لأنه لا يعتمد على خادم خارجي قد
 * يتعطل أو يُبطئ الاتصال، ومجاني لأنه بلا أي رسوم أو مفتاح API):
 *
 * 1) **Rabih Dictionary** — قاموس مرادفات عربي عام (~52 ألف مدخل، كل كلمة
 *    مع مجموعة مرادفاتها وجذرها).
 * 2) **Arabic WordNet v4** — الترجمة العربية لـ Open English WordNet؛
 *    تُستخرج منه فقط مجموعات الكلمات التي تقع ضمن نفس "synset" (أي تُعتبر
 *    مترادفة معنوياً)، بما فيها بعض المرادفات الإنجليزية الشائعة (مثل
 *    "thyme" لكلمة "زعتر") مما يساعد سيمو والبحث على فهم الاسم الأجنبي أو
 *    العلمي أيضاً.
 *
 * الملف المرفق (`assets/lexicon/ar_synonyms.json.gz`) نتيجة سكربت تحضير
 * بيانات (خارج التطبيق، غير مضمّن في الـ APK) يدمج المصدرين، يستبعد كلمات
 * الإيقاف والكلمات القصيرة جداً، ويحدّ كل كلمة بحد أقصى من المرادفات لإبقاء
 * حجم الملف صغيراً (~4 م.ب مضغوطاً فقط رغم دمج مصدرين). المفاتيح داخله
 * مُطبَّعة مسبقاً بنفس قواعد `normalize()` المستخدمة في هذا التطبيق (إزالة
 * التشكيل، توحيد الألف والتاء المربوطة...) كي تُطابق مباشرة مخرجات
 * `wordsOf()`/`normalize()` دون أي معالجة إضافية وقت التشغيل.
 *
 * يُحمَّل مرة واحدة فقط عند إقلاع التطبيق ([preload]، يُستدعى من
 * `HerbalApp.onCreate`) على خيط IO في الخلفية دون أي تأخير محسوس على بدء
 * التطبيق. وبما أن "التطبيق يجب ألا يعتمد على نجاح هذا التحميل"، أي فشل
 * (ملف تالف نادراً، مساحة تخزين مؤقت ممتلئة...) يُعامَل بهدوء تام: يستمر كل
 * من [HerbAssistant] وشاشات البحث بالعمل بالضبط كما كانا قبل إضافة هذا
 * القاموس، فقط بلا توسيع مرادفات إضافي، دون أي استثناء يصل للواجهة أو
 * تعطيل لأي ميزة أخرى.
 */
object DictionaryLexicon {

    @Volatile
    private var table: Map<String, List<String>> = emptyMap()

    @Volatile
    var isReady: Boolean = false
        private set

    /** عدد الكلمات المحمَّلة فعلياً (مفيد لعرضه في "أدوات المطور" كتأكيد أن القاموس حُمِّل). */
    val loadedWordCount: Int
        get() = table.size

    /**
     * يُحمَّل القاموس من ملف الأصول المضغوط. آمن الاستدعاء أكثر من مرة
     * (يتجاهل الاستدعاءات اللاحقة إن كان محمَّلاً أصلاً بنجاح). لا يرمي أي
     * استثناء للخارج مهما حدث.
     */
    suspend fun preload(context: Context) {
        if (isReady) return
        withContext(Dispatchers.IO) {
            try {
                val bytes = context.assets.open("lexicon/ar_synonyms.json.gz").use { input ->
                    GZIPInputStream(input).readBytes()
                }
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                val map = HashMap<String, List<String>>(json.length())
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = json.optJSONArray(key) ?: continue
                    val list = ArrayList<String>(arr.length())
                    for (i in 0 until arr.length()) list += arr.optString(i)
                    if (list.isNotEmpty()) map[key] = list
                }
                table = map
                isReady = true
            } catch (_: Exception) {
                // فشل هادئ ومقصود: البحث/سيمو يستمران بلا توسيع قاموس بدل تعطّل التطبيق.
                table = emptyMap()
                isReady = false
            }
        }
    }

    /** المرادفات المباشرة لكلمة واحدة مُطبَّعة مسبقاً (فارغة إن لم تُحمَّل بعد أو لم توجد). */
    fun synonymsOf(normalizedWord: String): List<String> = table[normalizedWord].orEmpty()

    /**
     * يوسّع مجموعة كلمات مُطبَّعة بمرادفاتها من القاموس (بنفس نمط
     * [ArabicLexicon.expand]: إضافة فقط، لا حذف للكلمات الأصلية). يُستخدم في
     * كل من البحث الحر لسيمو ([HerbAssistant]) وشاشات البحث المباشر عن
     * الأعشاب.
     */
    fun expand(words: Set<String>): Set<String> {
        if (words.isEmpty() || table.isEmpty()) return words
        return words + words.flatMap { table[it].orEmpty() }
    }
}
