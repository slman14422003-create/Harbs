# Firestore uses the model classes for reflection-based deserialization.
-keepclassmembers class com.salman.herbalencyclopedia.data.model.** {
    *;
}
-keep class com.salman.herbalencyclopedia.data.model.** { *; }

# Keep the Firebase App Check provider implementations discoverable after R8.
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.android.gms.safetynet.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
