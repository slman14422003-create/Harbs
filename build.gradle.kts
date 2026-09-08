// Top-level build file
//
// AGP 9 يوفر دعم Kotlin مدمجاً (built-in Kotlin) في محرّك البناء نفسه، لذا
// لم تعد إضافة "org.jetbrains.kotlin.android" منفصلة مطلوبة (بل تمنع البناء
// إن بقيت مع AGP 9+). لا يزال مكوّن مترجم Compose ("kotlin.plugin.compose")
// يُطبَّق صراحة كالمعتاد لأنه غير مرتبط بدعم Kotlin المدمج في AGP.
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
