import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    // "org.jetbrains.kotlin.android" أُزيل: AGP 9+ يوفّر دعم Kotlin مدمجاً
    // (built-in Kotlin) في محرّك البناء نفسه، وتطبيق الإضافة القديمة معه
    // يمنع البناء بدل تخصيصه. مترجم Compose يبقى إضافة منفصلة كالمعتاد.
    id("org.jetbrains.kotlin.plugin.compose")
}

// يقرأ إعدادات التوقيع أولاً من متغيرات بيئة CI (RELEASE_*)، وإذا لم
// تكن موجودة (بناء محلي) يرجع لملف app/keystore.properties إن وُجد.
val releaseStoreFile: String? = System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword: String? = System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("RELEASE_KEY_PASSWORD")

val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

val hasEnvSigning = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()
val hasLocalSigning = keystorePropertiesFile.exists()

// SHA-256 (hex, no colons) of the *release* signing certificate, used by
// SecurityUtils.kt at runtime to detect a repackaged/re-signed APK (a common
// result of apktool decompile → edit → resign → reinstall). Read from a CI
// env var first, then from keystore.properties for local builds, so nothing
// sensitive needs to be hardcoded in source. Get this value once with:
//   keytool -list -v -keystore <your.keystore> -alias <alias> | grep "SHA256:"
// (strip the colons). Leave it unset/blank and the check is skipped entirely
// - it only activates once you provide the real value.
val releaseSignatureSha256: String =
    (System.getenv("RELEASE_SIGNATURE_SHA256")
        ?: keystoreProperties["signatureSha256"] as String?
        ?: "").replace(":", "").uppercase()

android {
    namespace = "com.salman.herbalencyclopedia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.salman.herbalencyclopedia"
        minSdk = 24
        targetSdk = 36
        versionCode = (System.getenv("APP_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.0"

        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "EXPECTED_SIGNATURE_SHA256", "\"$releaseSignatureSha256\"")
    }

    signingConfigs {
        if (hasEnvSigning || hasLocalSigning) {
            create("release") {
                if (hasEnvSigning) {
                    storeFile = file(releaseStoreFile!!)
                    storePassword = releaseStorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                } else {
                    storeFile = rootProject.file("app/${keystoreProperties["storeFile"]}")
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasEnvSigning || hasLocalSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // قاعدة بيانات القاموس المحلي (assets/lexicon/ar_synonyms.db) تُستثنى من
    // ضغط الـ APK الداخلي عمداً: هي أصلاً ملف ثنائي (SQLite) لا يستفيد من
    // إعادة الضغط (لا يصغر تقريباً)، وإبقاؤها بلا ضغط داخل الـ APK يجعل
    // نسخها إلى تخزين التطبيق عند الإقلاع مجرد نسخ بايتات مباشر وأسرع بلا أي
    // خطوة فكّ ضغط إضافية في وقت التشغيل — أبسط وأكثر ضماناً.
    androidResources {
        noCompress += "db"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // --- Compose ---
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.core:core-splashscreen:1.2.0")

    // --- Storage / images / EXIF ---
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // --- Firebase (project configured manually in HerbalApp.kt, no google-services.json) ---
    // ملاحظة: وحدات KTX (مثل firebase-firestore-ktx) أُزيلت من BoM إصدار 34+
    // (توقفت Google عن إصدارها منذ يوليو 2025)، ودُمجت واجهات KTX داخل
    // الوحدات الرئيسية نفسها. لذلك نعتمد الآن على firebase-firestore و
    // firebase-auth مباشرة بدل الإصدارات المنتهية.
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")

    // --- Coroutines (Task.await() used with Firebase calls) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
