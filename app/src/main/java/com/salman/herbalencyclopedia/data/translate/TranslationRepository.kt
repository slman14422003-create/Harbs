package com.salman.herbalencyclopedia.data.translate

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * يترجم نصوصاً عربية (اسم/فوائد/طريقة استخدام العشبة... إلخ) إلى لغة
 * الهدف عبر [GoogleTranslateService]، مع تخزين مؤقت دائم على القرص
 * (SharedPreferences مستقلة) بالنص الأصلي كمفتاح (بعد تجزئته) — فلا
 * تُترجَم نفس الفقرة مرتين عبر الشبكة: أول عرض بالإنجليزية لكل عشبة يمرّ
 * فعلياً بترجمة جوجل، وكل عرض لاحق (حتى بعد إغلاق التطبيق) فوري من
 * الذاكرة المحلية.
 */
class TranslationRepository(context: Context) {

    private val cache = context.applicationContext
        .getSharedPreferences("translation_cache", Context.MODE_PRIVATE)

    suspend fun translate(text: String, targetLang: String, sourceLang: String = "ar"): String {
        if (text.isBlank() || targetLang == sourceLang) return text

        val key = cacheKey(text, sourceLang, targetLang)
        cache.getString(key, null)?.let { return it }

        val translated = GoogleTranslateService.translate(text, targetLang, sourceLang)
            ?: return text // فشل الشبكة/الواجهة: نعرض النص الأصلي بدل نص فارغ أو خطأ.

        withContext(Dispatchers.IO) {
            cache.edit().putString(key, translated).apply()
        }
        return translated
    }

    /** يترجم عدة نصوص بالتوازي (بدل التتابع) لتسريع أول تحميل لقائمة أعشاب كاملة. */
    suspend fun translateAll(texts: List<String>, targetLang: String, sourceLang: String = "ar"): List<String> =
        coroutineScope {
            texts.map { async { translate(it, targetLang, sourceLang) } }.awaitAll()
        }

    private fun cacheKey(text: String, sourceLang: String, targetLang: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }
        return "$sourceLang-$targetLang:$hash"
    }
}
