package com.salman.herbalencyclopedia.data.translate

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * يترجم نصوصاً عربية (اسم/فوائد/طريقة استخدام العشبة... إلخ) إلى لغة
 * الهدف عبر [GoogleTranslateService]، مع تخزين مؤقت دائم على القرص
 * (SharedPreferences مستقلة) بالنص الأصلي كمفتاح (بعد تجزئته) — فلا
 * تُترجَم نفس الفقرة مرتين عبر الشبكة: أول عرض بالإنجليزية لكل عشبة يمرّ
 * فعلياً بترجمة جوجل، وكل عرض لاحق (حتى بعد إغلاق التطبيق) فوري من
 * الذاكرة المحلية.
 *
 * إصلاح مهم لمشكلة "تعليق" التطبيق عند أول تفعيل للإنجليزية: كانت كل
 * حقول كل الأعشاب (قد تصل لمئات الطلبات) تُرسَل دفعة واحدة بلا أي حد
 * أقصى للتزامن (AppViewModel.translateHerbs يستخدم async/awaitAll لكل
 * حقل من كل عشبة معاً) — إغراق الجهاز بمئات الاتصالات الشبكية المتزامنة
 * فعلياً هو ما كان يجعل الواجهة تبدو "هنغانة" لفترة طويلة، وليس أي عملية
 * حسابية ثقيلة على الخيط الرئيسي. [MAX_CONCURRENT_REQUESTS] يحدّ عدد
 * الطلبات الفعلية على الشبكة في نفس اللحظة، فتمر البقية بالتتابع بدل
 * التزاحم دفعة واحدة، دون التأثير على النتيجة أو على التخزين المؤقت.
 */
class TranslationRepository(context: Context) {

    private val cache = context.applicationContext
        .getSharedPreferences("translation_cache", Context.MODE_PRIVATE)

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 6
    }

    // static حتى يبقى الحد الأقصى للتزامن مشتركاً بين كل استخدامات
    // TranslationRepository في التطبيق (AppViewModel وtr() في AppStrings.kt
    // كلاهما ينشئ نسخته الخاصة)، لا نسخة منفصلة لكل استدعاء.
    private val networkSemaphore = sharedSemaphore

    suspend fun translate(text: String, targetLang: String, sourceLang: String = "ar"): String {
        if (text.isBlank() || targetLang == sourceLang) return text

        val key = cacheKey(text, sourceLang, targetLang)
        cache.getString(key, null)?.let { return it }

        val translated = networkSemaphore.withPermit {
            GoogleTranslateService.translate(text, targetLang, sourceLang)
        } ?: return text // فشل الشبكة/الواجهة: نعرض النص الأصلي بدل نص فارغ أو خطأ.

        withContext(Dispatchers.IO) {
            cache.edit().putString(key, translated).apply()
        }
        return translated
    }

    /** يترجم عدة نصوص بالتوازي (ضمن حد [MAX_CONCURRENT_REQUESTS] أعلاه) بدل التتابع الكامل. */
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

/** انظر التعليق أعلى [TranslationRepository.networkSemaphore]. */
private val sharedSemaphore = Semaphore(6)

