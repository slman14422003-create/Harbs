package com.salman.herbalencyclopedia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salman.herbalencyclopedia.data.ai.AiConfig
import com.salman.herbalencyclopedia.data.ai.TrainedExample
import com.salman.herbalencyclopedia.ui.theme.PerformanceMode
import com.salman.herbalencyclopedia.ui.theme.ThemePalette
import com.salman.herbalencyclopedia.ui.theme.recommendedPerformanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "herbal_prefs")

/** فاصل داخلي لتشفير أزواج (مرادف/حالة مدرَّبة) داخل قيمة نصية واحدة، لأن
 *  DataStore يخزّن Set<String> فقط دون بنية key-value متداخلة. حرف تحكم
 *  غير مرئي وشبه مستحيل ورودُه ضمن نص عربي طبيعي يكتبه المطوّر. */
private const val AI_ENTRY_SEP = "\u241F"

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

    suspend fun setFontScale(level: Int) { context.dataStore.edit { it[Keys.FONT_SCALE] = level.coerceIn(0, 2) } }
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
}
