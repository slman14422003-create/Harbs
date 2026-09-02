package com.salman.herbalencyclopedia.data.ai

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * قاموس مرادفات عربي عام، مبني مسبقاً من مصدرين حرّين ومفتوحين ومُدمَجين
 * بالكامل مع التطبيق — بنفس فلسفة [ArabicLexicon] بالضبط: بلا إنترنت، بلا
 * مفتاح API، وبلا أي تكلفة أو اشتراك أو حد استخدام:
 *
 * 1) **Rabih Dictionary** — قاموس مرادفات عربي عام.
 * 2) **Arabic WordNet v4** — الترجمة العربية لـ Open English WordNet؛
 *    تُستخرج منه فقط مجموعات الكلمات التي تقع ضمن نفس "synset" (أي تُعتبر
 *    مترادفة معنوياً)، بما فيها بعض المرادفات الإنجليزية الشائعة (مثل
 *    "thyme" لكلمة "زعتر") مما يساعد سيمو والبحث على فهم الاسم الأجنبي أو
 *    العلمي أيضاً.
 *
 * ## لماذا قاعدة بيانات SQLite بدل ملف JSON ضخم يُحمَّل بالكامل في الذاكرة؟
 * كانت النسخة السابقة تقرأ ملف JSON مضغوط (~330 ألف مدخل)، تُفكّه بالكامل
 * كسلسلة نصية ~20 م.ب في الذاكرة، ثم تبنيه كـ JSONObject كامل، ثم تحوّله
 * لخريطة HashMap ضخمة — أربع نسخ متزامنة من نفس البيانات في الذاكرة في وقت
 * واحد (ذاكرة مؤقتة قد تتجاوز 150-200 م.ب). هذا يتجاوز حدّ الـ heap
 * الافتراضي لتطبيقات أندرويد على أغلب الأجهزة متوسطة/منخفضة الإمكانيات
 * فيرمي `OutOfMemoryError` — وهذا الخطأ تحديداً *لا يرثه* `Exception` في
 * كوتلن (فئة `Error` منفصلة)، فكان يفلت من `catch (_: Exception)` القديم
 * ويُسقِط الكوروتين بصمت: يبقى [isReady] عالقاً على `false` للأبد، ومؤشر
 * "جارٍ تحميل قاموس المرادفات المحلي…" في أدوات المطور لا ينتهي أبداً — هذا
 * بالضبط العطل المُبلَّغ عنه.
 *
 * الحل: القاموس الآن **قاعدة بيانات SQLite مُدمَجة فعلياً كجزء أساسي من
 * التطبيق** بدل بنية تُحمَّل بالكامل في الذاكرة. الملف المرفق
 * (`assets/lexicon/ar_synonyms.db.gz`) يُنسخ مرة واحدة فقط (نسخ بايتات
 * متدفّق بذاكرة تخزين مؤقت صغير 64ك.ب فقط — لا يحمّل أي محتوى كامل في
 * الذاكرة أبداً) إلى تخزين التطبيق الداخلي، ثم يُفتَح كقاعدة بيانات حقيقية.
 * كل استعلام بعدها (`SELECT ... WHERE word = ?`) يُنفَّذ مباشرة عبر فهرس
 * المفتاح الأساسي (أقل من مللي ثانية)، بلا أي بنية بيانات ضخمة مقيمة في
 * الذاكرة طوال عمر التطبيق. النتيجة: استحالة عملية لتكرار عطل الذاكرة نفسه،
 * وسرعة أعلى، واستهلاك ذاكرة أقل بشكل جذري.
 *
 * يُحمَّل مرة واحدة عند إقلاع التطبيق ([preload]، يُستدعى من
 * `HerbalApp.onCreate`) على خيط IO في الخلفية دون أي تأخير محسوس على بدء
 * التطبيق. وبما أن "التطبيق يجب ألا يعتمد على نجاح هذا التحميل"، أي فشل
 * (ملف تالف نادراً، مساحة تخزين ممتلئة...) يُعامَل بهدوء تام (بما فيها أي
 * `Throwable` وليس فقط `Exception`، احتياطاً): يستمر كل من [HerbAssistant]
 * وشاشات البحث بالعمل بالضبط كما كانا قبل إضافة هذا القاموس، فقط بلا توسيع
 * مرادفات إضافي، دون أي استثناء يصل للواجهة أو تعطيل لأي ميزة أخرى.
 */
object DictionaryLexicon {

    private const val ASSET_PATH = "lexicon/ar_synonyms.db.gz"

    /**
     * اسم ملف قاعدة البيانات المنسوخة داخلياً. يتضمّن رقم إصدار عمداً: إذا
     * تغيّر ملف القاموس المرفق مستقبلاً (تحديث للتطبيق)، يكفي رفع هذا الرقم
     * كي تُنسخ النسخة الجديدة تلقائياً على كل الأجهزة بدل الاستمرار باستخدام
     * نسخة قديمة متروكة من إصدار سابق (يُنسخ الملف فقط إن لم يكن موجوداً
     * أصلاً بهذا الاسم بالذات).
     */
    private const val DB_FILE_NAME = "ar_synonyms_v1.db"

    @Volatile
    private var db: SQLiteDatabase? = null

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    private var wordCount: Int = 0

    /** عدد الكلمات المحمَّلة فعلياً (مفيد لعرضه في "أدوات المطور" كتأكيد أن القاموس جاهز). */
    val loadedWordCount: Int
        get() = wordCount

    /**
     * ينسخ قاعدة البيانات من الأصول (إن لم تكن منسوخة أصلاً) ثم يفتحها.
     * آمن الاستدعاء أكثر من مرة (يتجاهل الاستدعاءات اللاحقة إن كانت جاهزة
     * أصلاً بنجاح). لا يرمي أي استثناء أو خطأ للخارج مهما حدث.
     */
    suspend fun preload(context: Context) {
        if (isReady) return
        withContext(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath(DB_FILE_NAME)
                if (!dbFile.exists() || dbFile.length() == 0L) {
                    dbFile.parentFile?.mkdirs()
                    val tmpFile = File(dbFile.parentFile, "$DB_FILE_NAME.tmp")
                    context.assets.open(ASSET_PATH).use { assetStream ->
                        GZIPInputStream(assetStream).use { gzipStream ->
                            tmpFile.outputStream().use { out ->
                                gzipStream.copyTo(out, bufferSize = 64 * 1024)
                            }
                        }
                    }
                    if (!tmpFile.renameTo(dbFile)) {
                        tmpFile.copyTo(dbFile, overwrite = true)
                        tmpFile.delete()
                    }
                }

                val opened = SQLiteDatabase.openDatabase(
                    dbFile.path, null, SQLiteDatabase.OPEN_READONLY
                )
                opened.rawQuery("SELECT COUNT(*) FROM synonyms", null).use { cursor ->
                    if (cursor.moveToFirst()) wordCount = cursor.getInt(0)
                }
                db = opened
                isReady = true
            } catch (_: Throwable) {
                // فشل هادئ ومقصود: البحث/سيمو يستمران بلا توسيع قاموس بدل تعطّل التطبيق.
                // Throwable (وليس Exception فقط) عمداً: يشمل هذا أي OutOfMemoryError
                // أو خطأ نظام آخر لا يرثه Exception في كوتلن، وهو أساس العطل السابق.
                try { db?.close() } catch (_: Throwable) { /* تجاهل */ }
                db = null
                wordCount = 0
                isReady = false
            }
        }
    }

    /** المرادفات المباشرة لكلمة واحدة مُطبَّعة مسبقاً (فارغة إن لم تُحمَّل بعد أو لم توجد). */
    fun synonymsOf(normalizedWord: String): List<String> {
        val database = db ?: return emptyList()
        if (normalizedWord.isBlank()) return emptyList()
        return try {
            database.rawQuery(
                "SELECT syns FROM synonyms WHERE word = ? LIMIT 1",
                arrayOf(normalizedWord)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0).split('|').filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * يوسّع مجموعة كلمات مُطبَّعة بمرادفاتها من القاموس (بنفس نمط
     * [ArabicLexicon.expand]: إضافة فقط، لا حذف للكلمات الأصلية). يُستخدم في
     * كل من البحث الحر لسيمو ([HerbAssistant]) وشاشات البحث المباشر عن
     * الأعشاب.
     */
    fun expand(words: Set<String>): Set<String> {
        if (words.isEmpty() || db == null) return words
        val extra = words.flatMap { synonymsOf(it) }
        return if (extra.isEmpty()) words else words + extra
    }
}
