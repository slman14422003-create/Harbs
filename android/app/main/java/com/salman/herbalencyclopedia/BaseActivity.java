package com.salman.herbalencyclopedia;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;

import com.salman.herbalencyclopedia.data.SettingsManager;

/**
 * تطبّق كل الشاشات حجم الخط المحفوظ محلياً (نفس مبدأ font-large/font-xlarge
 * على body في نسخة الويب القديمة)، عبر تعديل fontScale في Configuration.
 * كل الأنشطة يجب أن ترث من هذا الصف بدل AppCompatActivity مباشرة.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        SettingsManager settings = SettingsManager.getInstance(newBase);
        float scale = settings.fontScaleFor(settings.getFontSize());

        Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = scale;

        Context context = newBase.createConfigurationContext(configuration);
        super.attachBaseContext(context);
    }
}
