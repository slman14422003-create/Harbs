package com.salman.herbalencyclopedia.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.salman.herbalencyclopedia.model.Category;
import com.salman.herbalencyclopedia.model.Herb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * تخزين مؤقت محلي للأعشاب والتصنيفات على الجهاز، حتى يبقى التطبيق قابلاً
 * للاستخدام بدون إنترنت بعد أول تحميل ناجح.
 *
 * إعادة تنفيذ أصلية (Java + SharedPreferences/JSON) لِـ
 * loadDataFromLocalCache() / saveDataToLocalCache() / lastUpdateBadge
 * من نسخة الويب القديمة، التي كانت تعتمد هناك على localStorage.
 */
public final class LocalCacheStore {

    private static final String PREFS_NAME = "herb_local_cache";
    private static final String KEY_HERBS = "cached_herbs";
    private static final String KEY_CATEGORIES = "cached_categories";
    private static final String KEY_LAST_UPDATE = "last_update_millis";

    private static LocalCacheStore instance;

    private final SharedPreferences prefs;

    private LocalCacheStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LocalCacheStore getInstance(Context context) {
        if (instance == null) {
            instance = new LocalCacheStore(context);
        }
        return instance;
    }

    public long getLastUpdateMillis() {
        return prefs.getLong(KEY_LAST_UPDATE, 0L);
    }

    public boolean hasCache() {
        return getLastUpdateMillis() > 0L;
    }

    /** يحفظ آخر نسخة ناجحة من الأعشاب والتصنيفات محلياً مع طابع زمني. */
    public void save(List<Herb> herbs, List<Category> categories) {
        try {
            JSONArray herbsArray = new JSONArray();
            for (Herb h : herbs) {
                JSONObject o = new JSONObject();
                o.put("id", h.getId());
                o.put("name", h.getName());
                o.put("category_id", h.getCategoryId());
                o.put("benefits", h.getBenefits());
                o.put("warnings", h.getWarnings());
                o.put("harms", h.getHarms());
                o.put("usage", h.getUsage());
                o.put("notes", h.getNotes());
                o.put("image_url", h.getImageUrl());
                herbsArray.put(o);
            }

            JSONArray categoriesArray = new JSONArray();
            for (Category c : categories) {
                JSONObject o = new JSONObject();
                o.put("id", c.getId());
                o.put("name", c.getName());
                categoriesArray.put(o);
            }

            prefs.edit()
                    .putString(KEY_HERBS, herbsArray.toString())
                    .putString(KEY_CATEGORIES, categoriesArray.toString())
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply();
        } catch (JSONException ignored) {
            // في حال فشل التسلسل، ببساطة لا يُحدَّث الكاش المحلي
        }
    }

    public List<Herb> loadHerbs() {
        List<Herb> result = new ArrayList<>();
        String raw = prefs.getString(KEY_HERBS, null);
        if (raw == null) return result;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Herb(
                        o.optString("id"),
                        o.optString("name"),
                        o.optString("category_id"),
                        o.optString("benefits"),
                        o.optString("warnings"),
                        o.optString("harms"),
                        o.optString("usage"),
                        o.optString("notes"),
                        o.optString("image_url")
                ));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public List<Category> loadCategories() {
        List<Category> result = new ArrayList<>();
        String raw = prefs.getString(KEY_CATEGORIES, null);
        if (raw == null) return result;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Category(o.optString("id"), o.optString("name")));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
