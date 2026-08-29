package com.salman.herbalencyclopedia

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.salman.herbalencyclopedia.data.image.DataUriFetcher
import com.salman.herbalencyclopedia.data.repository.AppContainer

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

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        }
    }

    // بدون هذا، Coil (v2.x) لا يعرف كيف يقرأ روابط data: (base64) —
    // فيرجّع لكل AsyncImage بالتطبيق صورة فاشلة/فارغة لأي عشبة صورتها
    // مخزّنة كـ base64. Coil يستخدم هذا الـ ImageLoader تلقائياً لأن
    // Application هنا تُنفّذ ImageLoaderFactory.
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(DataUriFetcher.Factory()) }
            .build()
    }

    companion object {
        /** Same admin UID hardcoded in the web app's firebase-config.js */
        const val ADMIN_UID = "OWssFNrZDaZfeSlrLF8ReS8O6LM2"
    }
}
