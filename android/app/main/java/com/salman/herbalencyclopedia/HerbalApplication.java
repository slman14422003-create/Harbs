package com.salman.herbalencyclopedia;

import android.app.Application;

/**
 * موسوعة الأعشاب الطبية - نقطة انطلاق التطبيق.
 * لم يعد هناك حاجة لأي إعداد خاص بـ WebView بعد التحويل إلى تطبيق أندرويد
 * أصلي (Java + Views)، فالبيانات تُجلب مباشرة عبر الشبكة (Firestore REST).
 */
public class HerbalApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
