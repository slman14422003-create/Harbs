package com.salman.herbalencyclopedia;

import android.app.Application;

import com.salman.herbalencyclopedia.data.SettingsManager;

/**
 * موسوعة الأعشاب الطبية - نقطة انطلاق التطبيق.
 * لم يعد هناك حاجة لأي إعداد خاص بـ WebView بعد التحويل إلى تطبيق أندرويد
 * أصلي (Java + Views)، فالبيانات تُجلب مباشرة عبر الشبكة (Firestore REST).
 */
public class HerbalApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // يطبّق الوضع الليلي/النهاري المحفوظ فور إقلاع التطبيق
        // (مطابقة لسطر setTheme(savedTheme) عند تحميل الصفحة في النسخة القديمة).
        SettingsManager.getInstance(this).applyNightMode();
    }
}
