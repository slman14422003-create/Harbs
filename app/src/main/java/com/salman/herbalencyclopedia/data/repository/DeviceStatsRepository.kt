package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * أداة إدمن جديدة بطلب صريح من المستخدم: "معرفة كم جهاز يستخدم الموسوعة".
 *
 * كل جهاز له هوية مجهولة ثابتة (AuthRepository.ensureAnonymousUid — نفس
 * الآلية المستخدمة أصلاً لحظر مُرسِلي الملاحظات المسيئين، راجع
 * FeedbackRepository)، فتُستخدَم نفسها هنا كمعرّف مستقر للجهاز بلا أي حاجة
 * لجمع أي بيانات تعريف حقيقية عن المستخدم. مستند واحد بمجموعة "devices"
 * لكل جهاز — [pingDevice] يُستدعى مرة عند كل بدء تشغيل للتطبيق (راجع
 * AppViewModel.init) فيُنشئ المستند عند أول مرة (first_seen) ويُحدِّث
 * last_seen في كل مرة بعدها. عدد الأجهزة الكلي إذن هو ببساطة عدد مستندات
 * هذه المجموعة، وعدد "الأجهزة النشطة" هو عدد ما last_seen فيه خلال آخر
 * N يوم.
 *
 * القراءة والكتابة اليدوية للمجموعة كاملة (list) مقصورة على الأدمن فقط حسب
 * firestore.rules (نفس نمط "feedback"/"blocked_users" الموجود مسبقاً) —
 * كل جهاز يملك صلاحية الكتابة فقط على مستنده الخاص (uid يطابق
 * request.auth.uid)، فلا يمكنه قراءة عدد الأجهزة الأخرى أو التلاعب بها.
 *
 * العدّ هنا عبر استعلامات التجميع count() بدل تحميل كل المستندات وعدّها
 * محلياً — قراءة واحدة فقط بصرف النظر عن عدد الأجهزة الفعلي (راجع تعليق
 * حصة القراءات المجانية في AppViewModel.init)، مهم خصوصاً مع نمو عدد
 * الأجهزة بمرور الوقت.
 */
class DeviceStatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun collection() = db.collection("devices")

    /** يُستدعى مرة عند كل بدء تشغيل — أفضل جهد، أي فشل (بلا إنترنت مثلاً) يُتجاهل بصمت. */
    suspend fun pingDevice(uid: String) {
        val doc = collection().document(uid)
        val alreadyExists = runCatching { doc.get().await().exists() }.getOrDefault(true)
        val data: HashMap<String, Any?> = hashMapOf("last_seen" to FieldValue.serverTimestamp())
        if (!alreadyExists) data["first_seen"] = FieldValue.serverTimestamp()
        doc.set(data, SetOptions.merge()).await()
    }

    /** إجمالي عدد الأجهزة (كل الوقت) المسجَّلة على الإطلاق. */
    suspend fun totalDeviceCount(): Long =
        collection().count().get(AggregateSource.SERVER).await().count

    /** عدد الأجهزة التي استُخدم فيها التطبيق خلال آخر [sinceDays] يوماً. */
    suspend fun activeDeviceCount(sinceDays: Int = 7): Long {
        val cutoff = Timestamp(Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(sinceDays.toLong())))
        return collection()
            .whereGreaterThanOrEqualTo("last_seen", cutoff)
            .count().get(AggregateSource.SERVER).await().count
    }
}
