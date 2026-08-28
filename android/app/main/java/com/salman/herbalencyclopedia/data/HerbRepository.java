package com.salman.herbalencyclopedia.data;

import android.os.Handler;
import android.os.Looper;

import com.salman.herbalencyclopedia.model.Category;
import com.salman.herbalencyclopedia.model.Herb;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * طبقة وصول للبيانات: تجلب الأعشاب والتصنيفات من Firestore في خيط خلفي،
 * وتُعيد النتيجة للخيط الرئيسي عبر واجهة Callback. تحتفظ بنسخة مخزّنة
 * مؤقتاً في الذاكرة كي تستخدمها شاشات التفاصيل والمقارنة دون إعادة الجلب.
 */
public class HerbRepository {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private static final HerbRepository INSTANCE = new HerbRepository();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private List<Herb> cachedHerbs = Collections.emptyList();
    private List<Category> cachedCategories = Collections.emptyList();

    private HerbRepository() {}

    public static HerbRepository getInstance() {
        return INSTANCE;
    }

    public List<Herb> getCachedHerbs() {
        return cachedHerbs;
    }

    public List<Category> getCachedCategories() {
        return cachedCategories;
    }

    public String categoryNameFor(String categoryId) {
        if (categoryId == null) return "";
        for (Category category : cachedCategories) {
            if (category.getId().equals(categoryId)) {
                return category.getName();
            }
        }
        return "";
    }

    /** يجلب التصنيفات ثم الأعشاب معاً، ويعيدهما دفعة واحدة. */
    public void loadAll(Callback<List<Herb>> callback) {
        executor.execute(() -> {
            try {
                List<Category> categories = fetchCategories();
                List<Herb> herbs = fetchHerbs();

                cachedCategories = categories;
                cachedHerbs = herbs;

                mainHandler.post(() -> callback.onSuccess(herbs));
            } catch (IOException e) {
                String message = e.getMessage() != null ? e.getMessage() : "تعذّر تحميل البيانات";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    private List<Category> fetchCategories() throws IOException {
        List<JSONObject> docs = FirestoreRestClient.fetchCollection("categories");
        List<Category> result = new ArrayList<>();
        for (JSONObject doc : docs) {
            String id = FirestoreRestClient.extractDocumentId(doc);
            String name = FirestoreRestClient.getString(doc, "name");
            result.add(new Category(id, name));
        }
        return result;
    }

    private List<Herb> fetchHerbs() throws IOException {
        List<JSONObject> docs = FirestoreRestClient.fetchCollection("herbs");
        List<Herb> result = new ArrayList<>();
        for (JSONObject doc : docs) {
            String id = FirestoreRestClient.extractDocumentId(doc);
            result.add(new Herb(
                    id,
                    FirestoreRestClient.getString(doc, "name"),
                    FirestoreRestClient.getString(doc, "category_id"),
                    FirestoreRestClient.getString(doc, "benefits"),
                    FirestoreRestClient.getString(doc, "warnings"),
                    FirestoreRestClient.getString(doc, "harms"),
                    FirestoreRestClient.getString(doc, "usage"),
                    FirestoreRestClient.getString(doc, "notes"),
                    FirestoreRestClient.getString(doc, "image_url")
            ));
        }
        return result;
    }
}
