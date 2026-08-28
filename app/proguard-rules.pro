# Keep data model classes used with Firestore reflection-based deserialization
-keepclassmembers class com.salman.herbalencyclopedia.data.model.** {
    *;
}
-keep class com.salman.herbalencyclopedia.data.model.** { *; }

# Firebase / gRPC
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
