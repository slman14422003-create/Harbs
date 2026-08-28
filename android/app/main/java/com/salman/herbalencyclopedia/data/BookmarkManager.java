package com.salman.herbalencyclopedia.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * إدارة "المفضلة" (Bookmarks) محلياً على الجهاز.
 * هذه إعادة تنفيذ أصلية (Java + SharedPreferences) لنفس ميزة المفضلة التي
 * كانت موجودة في نسخة الويب القديمة (toggleBookmark / isHerbBookmarked /
 * showBookmarksModal / clearBookmarks) والتي اعتمدت هناك على localStorage.
 */
public final class BookmarkManager {

    private static final String PREFS_NAME = "herb_bookmarks";
    private static final String KEY_IDS = "bookmarked_herb_ids";

    private static BookmarkManager instance;

    private final SharedPreferences prefs;

    private BookmarkManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized BookmarkManager getInstance(Context context) {
        if (instance == null) {
            instance = new BookmarkManager(context);
        }
        return instance;
    }

    /** يعيد نسخة عن مجموعة معرّفات الأعشاب المضافة للمفضلة. */
    public Set<String> getBookmarkedIds() {
        return new HashSet<>(prefs.getStringSet(KEY_IDS, Collections.emptySet()));
    }

    public boolean isBookmarked(String herbId) {
        if (herbId == null) return false;
        return getBookmarkedIds().contains(herbId);
    }

    /** يبدّل حالة عشبة بين مضافة/غير مضافة للمفضلة، ويعيد الحالة الجديدة. */
    public boolean toggle(String herbId) {
        if (herbId == null) return false;
        Set<String> ids = getBookmarkedIds();
        boolean nowBookmarked;
        if (ids.contains(herbId)) {
            ids.remove(herbId);
            nowBookmarked = false;
        } else {
            ids.add(herbId);
            nowBookmarked = true;
        }
        prefs.edit().putStringSet(KEY_IDS, ids).apply();
        return nowBookmarked;
    }

    public int count() {
        return getBookmarkedIds().size();
    }

    /** يحذف كل الأعشاب من المفضلة (مطابق لزر "مسح المفضلة" في نسخة الويب). */
    public void clearAll() {
        prefs.edit().remove(KEY_IDS).apply();
    }
}
