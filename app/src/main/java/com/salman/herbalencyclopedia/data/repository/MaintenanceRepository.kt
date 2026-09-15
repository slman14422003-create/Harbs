package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

data class MaintenanceConfig(
    val enabled: Boolean = false,
    val message: String = ""
)

/**
 * أداة إدمن جديدة بطلب صريح: "تفعيل/تعطيل الصيانة" — علَم واحد بسيط بـ
 * Firestore يوقف نسخة public مؤقتاً برسالة للمستخدمين (مثلاً أثناء ترحيل
 * بيانات كبير أو صيانة السيرفر)، بدل الاضطرار لتوزيع تحديث كامل لذلك.
 *
 * مقصورة على نسخة public فقط بالتصميم — راجع MaintenanceGate بAppViewModel:
 * نسخة full (الأدمن نفسه) يجب أن تبقى قادرة على الدخول دوماً لتعطيل
 * الصيانة لاحقاً، فلا يجوز لهذا العلَم حجبها عن نفسها بالخطأ.
 */
class MaintenanceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun doc() = db.collection("app_config").document("maintenance")

    /** تُقرأ مرة عند كل بدء تشغيل (راجع AppViewModel.init). أي فشل (بلا إنترنت) يُعامَل كـ"غير مفعّلة" بدل حجب المستخدم بلا داعٍ. */
    suspend fun fetch(): MaintenanceConfig = runCatching {
        val snap = doc().get(Source.SERVER).await()
        if (snap.exists()) {
            MaintenanceConfig(
                enabled = snap.getBoolean("enabled") ?: false,
                message = snap.getString("message")?.trim().orEmpty()
            )
        } else MaintenanceConfig()
    }.getOrDefault(MaintenanceConfig())

    suspend fun save(config: MaintenanceConfig) {
        doc().set(
            hashMapOf(
                "enabled" to config.enabled,
                "message" to config.message.trim(),
                "updated_at" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
