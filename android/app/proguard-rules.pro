# قواعد الحماية العامة. minifyEnabled معطّل حالياً في التطبيق (انظر build.gradle)،
# لذا لا تُطبَّق هذه القواعد فعلياً إلا إذا تم تفعيل التصغير مستقبلاً.
-keepattributes *Annotation*

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
