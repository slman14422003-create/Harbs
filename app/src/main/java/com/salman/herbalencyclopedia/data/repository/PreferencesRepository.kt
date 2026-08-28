package com.salman.herbalencyclopedia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salman.herbalencyclopedia.ui.theme.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "herbal_prefs")

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
    }

    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map {
        it[Keys.FAVORITES] ?: emptySet()
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map {
        it[Keys.DARK_MODE]
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.USE_DYNAMIC_COLOR] ?: true
    }

    val fontScale: Flow<Int> = context.dataStore.data.map { it[Keys.FONT_SCALE] ?: 0 }

    val themePalette: Flow<ThemePalette> = context.dataStore.data.map {
        ThemePalette.fromId(it[Keys.THEME_PALETTE])
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
}
