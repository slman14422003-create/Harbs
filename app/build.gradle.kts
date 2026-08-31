import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ─────────────────────────────────────────────────────────────────────────
// توقيع الإصدار (Release signing)
// ─────────────────────────────────────────────────────────────────────────
// مصدران للتوقيع، بالأولوية التالية:
//  1) ملف محلي app/keystore.properties (للبناء من Android Studio على جهازك
//     — أنشئه مرة واحدة فقط، انظر keystore.properties.example بجانبه، ولن
//     تحتاج لمس ملف gradle هذا مرة أخرى مهما تغيّر الإصدار).
//  2) متغيرات بيئة (تُستخدم في CI/GitHub Actions فقط، راجع
//     .github/workflows/android-release.yml).
// إن لم يتوفر أي منهما، يبقى "release" بلا توقيع (صالح فقط لتجربة محلية
// عبر assembleRelease بدون signingConfig، ولن يُقبل في متجر Play).
val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseStoreFile: String? =
    keystoreProperties.getProperty("storeFile") ?: System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword: String? =
    keystoreProperties.getProperty("storePassword") ?: System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? =
    keystoreProperties.getProperty("keyAlias") ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? =
    keystoreProperties.getProperty("keyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD")

android {
    namespace = "com.salman.herbalencyclopedia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.salman.herbalencyclopedia"
        minSdk = 24
        targetSdk = 35
        versionCode = (System.getenv("APP_VERSION_CODE")?.toIntOrNull()) ?: 2
        versionName = System.getenv("APP_VERSION_NAME") ?: "2.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // AAB (App Bundle) هو الصيغة التي يطلبها متجر Play الآن، لا APK مباشرة.
    // هذا الإعداد يجعل Gradle يولّد حزمة APK "مُحسَّنة الحجم" حسب كل جهاز
    // (تُدار تلقائياً عبر Play عند التوزيع كـ AAB) بدل APK واحد ضخم يحوي
    // كل موارد اللغات/الشاشات لكل الأجهزة.
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // خط عربي مخصص (Tajawal) عبر Google Fonts — يُحمَّل من جهاز المستخدم
    // وقت التشغيل عبر خدمات Google Play (Downloadable Fonts)، فلا يحتاج
    // ملف خط داخل المشروع. راجع ui/theme/Type.kt. بلا رقم إصدار مكتوب يدوياً
    // كي يُدار الإصدار عبر compose-bom أعلاه (نفس إصدار باقي مكتبات Compose)
    // ويُتفادى أي تعارض إصدارات يسبب أخطاء ترجمة غامضة.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Firebase (initialized manually via FirebaseOptions - no google-services.json required)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // قراءة اتجاه الصورة (EXIF) قبل الضغط - راجع data/image/ImageCompressor.kt
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Local persistence (favorites, theme preference, offline cache)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
