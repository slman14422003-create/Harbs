package com.salman.herbalencyclopedia.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * إدارة إعدادات العرض المحلية: الوضع الليلي/النهاري وحجم الخط.
 * إعادة تنفيذ أصلية (Java + SharedPreferences) لِـ setTheme() وزر
 * fontSizeToggleBtn (cycleFontSize) من نسخة الويب القديمة، واللتان كانتا
 * تعتمدان هناك على localStorage ('theme' و 'fontSize').
 */
public final class SettingsManager {

    private static final String PREFS_NAME = "herb_settings";
    private static final String KEY_THEME = "theme"; // "dark" | "light"
    private static final String KEY_FONT_SIZE = "fontSize"; // "normal" | "large" | "xlarge"

    public static final String FONT_NORMAL = "normal";
    public static final String FONT_LARGE = "large";
    public static final String FONT_XLARGE = "xlarge";

    private static SettingsManager instance;

    private final SharedPreferences prefs;

    private SettingsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    // ---------------------------------------------------------------
    // الوضع الليلي / النهاري (setTheme القديمة)
    // ---------------------------------------------------------------

    public boolean isDarkMode() {
        return "dark".equals(prefs.getString(KEY_THEME, "light"));
    }

    /** يبدّل بين الوضع الليلي والنهاري ويطبّقه فوراً على كل التطبيق. */
    public boolean toggleDarkMode() {
        boolean newValue = !isDarkMode();
        setDarkMode(newValue);
        return newValue;
    }

    public void setDarkMode(boolean dark) {
        prefs.edit().putString(KEY_THEME, dark ? "dark" : "light").apply();
        applyNightMode();
    }

    /** يُطبَّق عند إقلاع التطبيق (Application.onCreate) وعند كل تبديل. */
    public void applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // ---------------------------------------------------------------
    // حجم الخط (fontSizeToggleBtn / cycleFontSize القديمة)
    // ---------------------------------------------------------------

    public String getFontSize() {
        return prefs.getString(KEY_FONT_SIZE, FONT_NORMAL);
    }

    /** يبدّل: عادي → كبير → أكبر → عادي (نفس ترتيب المصفوفة الأصلية). */
    public String cycleFontSize() {
        String current = getFontSize();
        String next;
        switch (current) {
            case FONT_NORMAL:
                next = FONT_LARGE;
                break;
            case FONT_LARGE:
                next = FONT_XLARGE;
                break;
            default:
                next = FONT_NORMAL;
                break;
        }
        prefs.edit().putString(KEY_FONT_SIZE, next).apply();
        return next;
    }

    /** الاسم المعروض للمستخدم لحجم الخط الحالي (عادي/كبير/أكبر). */
    public String fontSizeLabel(String level) {
        switch (level) {
            case FONT_LARGE:
                return "كبير";
            case FONT_XLARGE:
                return "أكبر";
            default:
                return "عادي";
        }
    }

    /** مُعامِل تكبير النص المطابق لكل مستوى، يُستخدم في attachBaseContext. */
    public float fontScaleFor(String level) {
        switch (level) {
            case FONT_LARGE:
                return 1.15f;
            case FONT_XLARGE:
                return 1.3f;
            default:
                return 1.0f;
        }
    }
}
