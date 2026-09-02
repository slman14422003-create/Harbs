package com.salman.herbalencyclopedia

import android.app.ActivityManager
import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.salman.herbalencyclopedia.data.ai.DictionaryLexicon
import com.salman.herbalencyclopedia.data.image.DataUriFetcher
import com.salman.herbalencyclopedia.data.repository.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase is initialized manually with the same project configuration the
 * web app already uses (js/firebase-config.js), so the app talks to the
 * exact same "herbs" / "categories" Firestore collections — no
 * google-services.json required for this to compile and run.
 *
 * For production hardening (Play Integrity, FCM, Google Sign-In, or to lock
 * the Firebase API key to this app's package + SHA-1), register an Android
 * app for this package name in the Firebase console and swap this manual
 * setup for the generated google-services.json + the
 * com.google.gms.google-services plugin.
 */
class HerbalApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    /** نطاق حياة التطبيق الكامل، يُستخدم فقط لمهام خلفية عمرها التطبيق نفسه (مثل تحميل القاموس المحلي). */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        val options = FirebaseOptions.Builder()
            .setApiKey("AIzaSyAkVYaspguYs6gXAOaV7xoiesa38nqgm10")
            .setApplicationId("1:497780761661:web:95ae225c648814c0ed7654")
            .setProjectId("semoharbs")
            .setStorageBucket("semoharbs.firebasestorage.app")
            .setGcmSenderId("497780761661")
            .build()

        val firebaseApp = if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        } else {
            FirebaseApp.getInstance()
        }

        // App Check is installed immediately after Firebase initialization so every
        // subsequent Firestore/Auth request can carry an attestation token. Release
        // builds use Play Integrity; debug builds use Firebase's debug provider.
        if (firebaseApp != null) FirebaseSecurity.install()

        // ينسخ قاعدة بيانات القاموس المحلي (SQLite، مُدمَجة كجزء أساسي من
        // التطبيق) مرة واحدة إلى تخزين التطبيق الداخلي ثم يفتحها — انظر توثيق
        // DictionaryLexicon لتفصيل لماذا SQLite بدل تحميل ملف JSON ضخم بالكامل
        // في الذاكرة (كان يسبب OutOfMemoryError صامتاً على الأجهزة محدودة
        // الرام فيبقى القاموس عالقاً على "جارٍ التحميل…" للأبد). يعمل على خيط
        // IO في الخلفية فلا يؤخر بدء التطبيق، وأي فشل يُعامَل بهدوء تام كما هو
        // موثّق في DictionaryLexicon.
        applicationScope.launch { DictionaryLexicon.preload(this@HerbalApp) }
    }

    // بدون هذا، Coil (v2.x) لا يعرف كيف يقرأ روابط data: (base64) —
    // فيرجّع لكل AsyncImage بالتطبيق صورة فاشلة/فارغة لأي عشبة صورتها
    // مخزّنة كـ base64. Coil يستخدم هذا الـ ImageLoader تلقائياً لأن
    // Application هنا تُنفّذ ImageLoaderFactory.
    //
    // على الأجهزة منخفضة الرام: كاش الصور بالذاكرة أُنقص لنسبة أصغر من
    // الرام المتاح، والصور تُفكّ بعمق ألوان أخفض (RGB_565 بدل ARGB_8888
    // الافتراضي) — يقلّل استهلاك كل صورة بالذاكرة للنصف تقريباً بأثر بصري
    // ضئيل جداً على صور صغيرة كصور الأعشاب. على الأجهزة القوية يبقى كل شيء
    // بأعلى جودة كما كان.
    override fun newImageLoader(): ImageLoader {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = activityManager?.isLowRamDevice == true

        return ImageLoader.Builder(this)
            .components { add(DataUriFetcher.Factory()) }
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(if (isLowRam) 0.15 else 0.25)
                    .build()
            }
            .apply { if (isLowRam) bitmapConfig(Bitmap.Config.RGB_565) }
            .build()
    }

    companion object {
        /** Same admin UID hardcoded in the web app's firebase-config.js */
        const val ADMIN_UID = "OWssFNrZDaZfeSlrLF8ReS8O6LM2"
    }
}
