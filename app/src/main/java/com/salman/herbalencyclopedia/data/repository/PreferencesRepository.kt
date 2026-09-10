package com.salman.herbalencyclopedia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salman.herbalencyclopedia.data.ai.AiConfig
import com.salman.herbalencyclopedia.data.ai.TrainedExample
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import org.json.JSONArray
import org.json.JSONObject
import com.salman.herbalencyclopedia.ui.theme.PerformanceMode
import com.salman.herbalencyclopedia.ui.theme.ThemePalette
import com.salman.herbalencyclopedia.ui.theme.recommendedPerformanceMode
import com.salman.herbalencyclopedia.ui.util.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "herbal_prefs")

/** فاصل داخلي لتشفير أزواج (مرادف/حالة مدرَّبة) داخل قيمة نصية واحدة، لأن
 *  DataStore يخزّن Set<String> فقط دون بنية key-value متداخلة. حرف تحكم
 *  غير مرئي وشبه مستحيل ورودُه ضمن نص عربي طبيعي يكتبه المطوّر. */
private const val AI_ENTRY_SEP = "\u241F"

/** لقطة كاملة من كاش الموسوعة المحلي (راجع [PreferencesRepository.loadCachedCatalog]). */
data class CatalogSnapshot(
    val herbs: List<Herb>,
    val categories: List<Category>,
    val blends: List<Blend>,
    /** وقت آخر مزامنة ناجحة مع Firestore بالمللي ثانية (epoch). */
    val lastSyncAt: Long
)

/**
 * Stores favorite herb IDs and the dark-mode preference locally on-device
 * (kept out of Firestore since these are per-device, not account, settings).
 */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorite_herb_ids")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SCALE = intPreferencesKey("font_scale")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        // لغة واجهة التطبيق (ar/en) — راجع AppLanguage وSettingsScreen وميزة
        // الترجمة التلقائية في AppViewModel.translateHerbs/translateCategories/translateBlends.
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        // هل شاهد المستخدم شاشة ترحيب/شروط استخدام سيمو تحديداً (منفصلة عن
        // شاشة ترحيب التطبيق العامة أعلاه)؟ تُعرض مرة واحدة فقط عند أول
        // فتح لسيمو بعد التثبيت — انظر SemoIntroScreen وHerbalNavGraph.
        val SEMO_INTRO_SEEN = booleanPreferencesKey("semo_intro_seen")
        // إعدادات "مساعد المقارنة الذكي" (HerbAssistant) — قابلة للتعديل من
        // أدوات المطور (AdminToolsScreen) لضبط/"تدريب" سلوك المطابقة النصية
        // المحلية دون الحاجة لإعادة بناء التطبيق.
        val AI_SIMILARITY_THRESHOLD = floatPreferencesKey("ai_similarity_threshold")
        val AI_SEARCH_THRESHOLD = floatPreferencesKey("ai_search_threshold")
        val AI_EXTRA_STOPWORDS = stringSetPreferencesKey("ai_extra_stopwords")
        // "تدريب سيمو المخصّص": مرادفات يفهمها + حالات (سؤال↔رد) يعلّمها
        // المطوّر يدوياً من أدوات المطور، تُطبَّق حياً بلا إعادة بناء التطبيق.
        val AI_SYNONYMS = stringSetPreferencesKey("ai_synonyms")
        val AI_TRAINED_EXAMPLES = stringSetPreferencesKey("ai_trained_examples")
        val AI_TRAINED_THRESHOLD = floatPreferencesKey("ai_trained_threshold")
        // "التعلّم الذاتي": حالات يتعلّمها سيمو تلقائياً من تقييمات المستخدمين
        // (👍) على إجابات البحث الحر — منفصلة عن حالات المطوّر اليدوية أعلاه.
        val AI_AUTO_LEARNED_EXAMPLES = stringSetPreferencesKey("ai_auto_learned_examples")
        val AI_AUTO_LEARN_ENABLED = booleanPreferencesKey("ai_auto_learn_enabled")
        // كاش كامل لمحتوى الموسوعة (راجع توثيق loadCachedCatalog/saveCatalogCache
        // أسفل الملف لسبب وجود هذا الكاش المنفصل عن كاش Firestore الداخلي).
        val CATALOG_HERBS_JSON = stringPreferencesKey("catalog_herbs_json")
        val CATALOG_CATEGORIES_JSON = stringPreferencesKey("catalog_categories_json")
        val CATALOG_BLENDS_JSON = stringPreferencesKey("catalog_blends_json")
        val CATALOG_LAST_SYNC_AT = longPreferencesKey("catalog_last_sync_at")
    }

    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map {
        it[Keys.FAVORITES] ?: emptySet()
    }

    /** هل وافق المستخدم على شاشة الترحيب (سياسة الخصوصية + التحذير الطبي +
     *  الشروط والأحكام)؟ تُقرأ مرة واحدة فقط عند أول تشغيل بعد التثبيت -
     *  محفوظة محلياً على الجهاز (DataStore)، وليست جزءاً من حساب أو
     *  Firestore، فتُمسَح فقط إذا حذف المستخدم بيانات التطبيق أو أزاله. */
    val termsAccepted: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.TERMS_ACCEPTED] ?: false
    }

    /** هل شاهد المستخدم شاشة ترحيب/شروط استخدام سيمو من قبل؟ انظر [Keys.SEMO_INTRO_SEEN]. */
    val semoIntroSeen: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.SEMO_INTRO_SEEN] ?: false
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map {
        it[Keys.DARK_MODE]
    }

    // الافتراضي false: قبل هذا التعديل كانت الألوان الديناميكية (Material You
    // المشتقة من خلفية الجهاز) مفعّلة افتراضياً، فتتجاوز أي لوحة ألوان يختارها
    // المستخدم من الإعدادات بالكامل — كانت تغيير اللوحة يبدو بلا أي أثر لأن
    // الشرط في HerbalEncyclopediaTheme يعطي أولوية لـ dynamicColor. الآن
    // يعتمد التطبيق افتراضياً على هويته البصرية الخاصة (اللوحة المختارة)
    // ويبقى بإمكان المستخدم تفعيل الألوان الديناميكية يدوياً إن أراد.
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.USE_DYNAMIC_COLOR] ?: false
    }

    val fontScale: Flow<Int> = context.dataStore.data.map { it[Keys.FONT_SCALE] ?: 0 }

    val themePalette: Flow<ThemePalette> = context.dataStore.data.map {
        ThemePalette.fromId(it[Keys.THEME_PALETTE])
    }

    /** وضع الأداء (عالي الجودة/اقتصادي) — يتحكم بالزجاج السائل والتمويه وثقل الحركات.
     *  إذا لم يختر المستخدم شيئاً بعد، يُستخدم وضع مقترَح تلقائياً حسب قدرة
     *  الجهاز (انظر [recommendedPerformanceMode]) بدل افتراض "أداء عالٍ" للجميع. */
    private val recommendedMode: PerformanceMode by lazy { recommendedPerformanceMode(context) }

    val performanceMode: Flow<PerformanceMode> = context.dataStore.data.map {
        PerformanceMode.fromId(it[Keys.PERFORMANCE_MODE], fallback = recommendedMode)
    }

    /** لغة واجهة التطبيق الحالية — راجع [AppLanguage] و[setAppLanguage]. */
    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map {
        AppLanguage.fromCode(it[Keys.APP_LANGUAGE])
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs -> prefs[Keys.APP_LANGUAGE] = language.code }
    }

    companion object {
        /**
         * أقصى عمر مسموح لكاش الموسوعة المحلي قبل اعتباره "قديماً" ووجوب
         * مزامنته من Firestore مجدداً - راجع [loadCachedCatalog] وAppViewModel.init.
         * القيمة الحالية: ٢٤ ساعة. طالما لم تمر هذه المدة على آخر مزامنة
         * ناجحة لهذا الجهاز تحديداً، يُعرض الكاش المحلي مباشرة بلا أي اتصال
         * شبكي - بصرف النظر عن عدد المستخدمين الآخرين الذين يفتحون التطبيق
         * بنفس اللحظة، لأن كلاً منهم يقرأ من تخزينه المحلي الخاص فقط.
         */
        const val CATALOG_MAX_AGE_MS: Long = 24L * 60 * 60 * 1000

        /**
         * قراءة متزامنة صريحة (وليست عبر Flow) للغة المحفوظة — تُستدعى فقط من
         * [android.app.Activity.attachBaseContext] (انظر MainActivity)، وهي
         * نقطة تُستدعى قبل أي تركيبة Compose أو viewModelScope، فيجب أن
         * يُغلَّف الـ Context باللغة الصحيحة قبل إنشاء أي مورد أو شاشة. تكلفة
         * هذه القراءة بسيطة (قيمة واحدة من ملف DataStore محلي صغير) وتحدث مرة
         * واحدة فقط عند كل إنشاء للنشاط.
         */
        fun getSavedLanguageCodeBlocking(context: Context): String = runBlocking {
            context.dataStore.data.first()[Keys.APP_LANGUAGE] ?: AppLanguage.ARABIC.code
        }
    }

    suspend fun toggleFavorite(herbId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (herbId in current) current - herbId else current + herbId
        }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { prefs ->
            if (enabled == null) prefs.remove(Keys.DARK_MODE) else prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setFontScale(level: Int) { context.dataStore.edit { it[Keys.FONT_SCALE] = level.coerceIn(0, 4) } }
    suspend fun setThemePalette(palette: ThemePalette) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_PALETTE] = palette.name }
    }
    suspend fun clearFavorites() { context.dataStore.edit { it[Keys.FAVORITES] = emptySet() } }

    suspend fun setPerformanceMode(mode: PerformanceMode) {
        context.dataStore.edit { prefs -> prefs[Keys.PERFORMANCE_MODE] = mode.name }
    }

    /** يُستدعى مرة واحدة فقط عند ضغط "أوافق" في شاشة الترحيب الأولى. */
    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.TERMS_ACCEPTED] = accepted }
    }

    /** يُستدعى مرة واحدة فقط عند ضغط "فهمت، لنبدأ" في شاشة ترحيب سيمو الأولى. */
    suspend fun setSemoIntroSeen(seen: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SEMO_INTRO_SEEN] = seen }
    }

    // ── إعدادات مساعد المقارنة الذكي (HerbAssistant) ────────────────────

    val aiSimilarityThreshold: Flow<Float> = context.dataStore.data.map {
        it[Keys.AI_SIMILARITY_THRESHOLD] ?: AiConfig.defaultSimilarityThreshold.toFloat()
    }
    val aiSearchThreshold: Flow<Float> = context.dataStore.data.map {
        it[Keys.AI_SEARCH_THRESHOLD] ?: AiConfig.defaultSearchThreshold.toFloat()
    }
    val aiExtraStopWords: Flow<Set<String>> = context.dataStore.data.map {
        it[Keys.AI_EXTRA_STOPWORDS] ?: emptySet()
    }

    suspend fun setAiSimilarityThreshold(value: Float) {
        context.dataStore.edit { it[Keys.AI_SIMILARITY_THRESHOLD] = value }
    }
    suspend fun setAiSearchThreshold(value: Float) {
        context.dataStore.edit { it[Keys.AI_SEARCH_THRESHOLD] = value }
    }
    suspend fun setAiExtraStopWords(words: Set<String>) {
        context.dataStore.edit { it[Keys.AI_EXTRA_STOPWORDS] = words }
    }
    suspend fun resetAiSettings() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AI_SIMILARITY_THRESHOLD)
            prefs.remove(Keys.AI_SEARCH_THRESHOLD)
            prefs.remove(Keys.AI_EXTRA_STOPWORDS)
            prefs.remove(Keys.AI_SYNONYMS)
            prefs.remove(Keys.AI_TRAINED_EXAMPLES)
            prefs.remove(Keys.AI_TRAINED_THRESHOLD)
            prefs.remove(Keys.AI_AUTO_LEARNED_EXAMPLES)
            prefs.remove(Keys.AI_AUTO_LEARN_ENABLED)
        }
    }

    // ── تدريب سيمو المخصّص: مرادفات وحالات (سؤال ← رد) يعلّمها المطوّر ───

    val aiSynonyms: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.AI_SYNONYMS] ?: emptySet()).mapNotNull { entry ->
            val parts = entry.split(AI_ENTRY_SEP, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
        }.toMap()
    }

    val aiTrainedExamples: Flow<List<TrainedExample>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.AI_TRAINED_EXAMPLES] ?: emptySet()).mapNotNull { entry ->
            val parts = entry.split(AI_ENTRY_SEP, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) TrainedExample(parts[0], parts[1]) else null
        }
    }

    val aiTrainedThreshold: Flow<Float> = context.dataStore.data.map {
        it[Keys.AI_TRAINED_THRESHOLD] ?: AiConfig.defaultTrainedThreshold.toFloat()
    }

    suspend fun setAiSynonyms(synonyms: Map<String, String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_SYNONYMS] = synonyms.map { (word, meaning) -> "$word$AI_ENTRY_SEP$meaning" }.toSet()
        }
    }

    suspend fun setAiTrainedExamples(examples: List<TrainedExample>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_TRAINED_EXAMPLES] = examples.map { "${it.pattern}$AI_ENTRY_SEP${it.response}" }.toSet()
        }
    }

    suspend fun setAiTrainedThreshold(value: Float) {
        context.dataStore.edit { it[Keys.AI_TRAINED_THRESHOLD] = value }
    }

    // ── التعلّم الذاتي: حالات يتعلّمها سيمو من تقييمات المستخدمين 👍/👎 ──

    val aiAutoLearnedExamples: Flow<List<TrainedExample>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.AI_AUTO_LEARNED_EXAMPLES] ?: emptySet()).mapNotNull { entry ->
            val parts = entry.split(AI_ENTRY_SEP, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) TrainedExample(parts[0], parts[1]) else null
        }
    }

    val aiAutoLearnEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AI_AUTO_LEARN_ENABLED] ?: AiConfig.defaultAutoLearnEnabled
    }

    suspend fun setAiAutoLearnedExamples(examples: List<TrainedExample>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_AUTO_LEARNED_EXAMPLES] = examples.map { "${it.pattern}$AI_ENTRY_SEP${it.response}" }.toSet()
        }
    }

    suspend fun setAiAutoLearnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_AUTO_LEARN_ENABLED] = enabled }
    }

    // ── كاش الموسوعة المحلي (أعشاب/تصنيفات/خلطات) ───────────────────────
    // هذا مستقل عن كاش Firestore الداخلي في HerbRepository (الذي يخزّن
    // مستندات خام لتفعيل العمل بلا إنترنت وإعادة الاستخدام بين المستمعين).
    // الهدف هنا مختلف: تفادي أي *اتصال* بـ Firestore عند بدء التطبيق طالما
    // آخر مزامنة كانت أحدث من [CATALOG_MAX_AGE_MS] - فحتى مع كاش Firestore
    // الداخلي، كل تسجيل جديد لمستمع (addSnapshotListener) أو طلب get() كان
    // يعني اتصالاً فعلياً يُحتسب على حصة القراءات المجانية اليومية. القوائم
    // هنا تُخزَّن كنص JSON خام (عبر org.json، المستخدمة أصلاً في restoreBackup)
    // بدل إضافة اعتمادية جديدة (Room/kotlinx.serialization) لتغيير صغير كهذا.

    /** يعيد آخر نسخة محفوظة محلياً، أو null إن لم تتم أي مزامنة بعد أو تلف الكاش. */
    suspend fun loadCachedCatalog(): CatalogSnapshot? {
        val prefs = context.dataStore.data.first()
        val herbsJson = prefs[Keys.CATALOG_HERBS_JSON] ?: return null
        val categoriesJson = prefs[Keys.CATALOG_CATEGORIES_JSON] ?: return null
        val blendsJson = prefs[Keys.CATALOG_BLENDS_JSON] ?: return null
        val lastSyncAt = prefs[Keys.CATALOG_LAST_SYNC_AT] ?: return null
        return runCatching {
            CatalogSnapshot(
                herbs = decodeHerbs(herbsJson),
                categories = decodeCategories(categoriesJson),
                blends = decodeBlends(blendsJson),
                lastSyncAt = lastSyncAt
            )
        }.getOrNull() // كاش تالف (مثلاً بعد تغيير شكل البيانات مستقبلاً) يُعامَل كغياب كاش، لا كخطأ يوقف التطبيق.
    }

    /** يستبدل الكاش المحلي بالكامل بأحدث نسخة، ويسجّل وقت هذه المزامنة كـ"الآن". */
    suspend fun saveCatalogCache(herbs: List<Herb>, categories: List<Category>, blends: List<Blend>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CATALOG_HERBS_JSON] = encodeHerbs(herbs)
            prefs[Keys.CATALOG_CATEGORIES_JSON] = encodeCategories(categories)
            prefs[Keys.CATALOG_BLENDS_JSON] = encodeBlends(blends)
            prefs[Keys.CATALOG_LAST_SYNC_AT] = System.currentTimeMillis()
        }
    }

    private fun encodeHerbs(herbs: List<Herb>): String {
        val array = JSONArray()
        herbs.forEach { herb ->
            array.put(
                JSONObject()
                    .put("id", herb.id)
                    .put("name", herb.name)
                    .put("category_id", herb.categoryId)
                    .put("benefits", herb.benefits)
                    .put("warnings", herb.warnings)
                    .put("harms", herb.harms)
                    .put("usage", herb.usage)
                    .put("notes", herb.notes)
                    .put("image_url", herb.imageUrl)
            )
        }
        return array.toString()
    }

    private fun decodeHerbs(json: String): List<Herb> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Herb(
                id = o.optString("id"),
                name = o.optString("name"),
                categoryId = o.optString("category_id").ifBlank { null },
                benefits = o.optString("benefits"),
                warnings = o.optString("warnings"),
                harms = o.optString("harms"),
                usage = o.optString("usage"),
                notes = o.optString("notes"),
                imageUrl = o.optString("image_url").ifBlank { null }
            )
        }
    }

    private fun encodeCategories(categories: List<Category>): String {
        val array = JSONArray()
        categories.forEach { category ->
            array.put(
                JSONObject()
                    .put("id", category.id)
                    .put("name", category.name)
                    .put("icon", category.icon)
            )
        }
        return array.toString()
    }

    private fun decodeCategories(json: String): List<Category> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Category(
                id = o.optString("id"),
                name = o.optString("name"),
                icon = o.optString("icon").ifBlank { null }
            )
        }
    }

    private fun encodeBlends(blends: List<Blend>): String {
        val array = JSONArray()
        blends.forEach { blend ->
            array.put(
                JSONObject()
                    .put("id", blend.id)
                    .put("name", blend.name)
                    .put("herb_ids", JSONArray(blend.herbIds))
                    .put("benefits", blend.benefits)
                    .put("usage", blend.usage)
                    .put("warnings", blend.warnings)
                    .put("notes", blend.notes)
                    .put("image_url", blend.imageUrl)
            )
        }
        return array.toString()
    }

    private fun decodeBlends(json: String): List<Blend> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val herbIdsArray = o.optJSONArray("herb_ids") ?: JSONArray()
            Blend(
                id = o.optString("id"),
                name = o.optString("name"),
                herbIds = (0 until herbIdsArray.length()).map { j -> herbIdsArray.getString(j) },
                benefits = o.optString("benefits"),
                usage = o.optString("usage"),
                warnings = o.optString("warnings"),
                notes = o.optString("notes"),
                imageUrl = o.optString("image_url").ifBlank { null }
            )
        }
    }
}
