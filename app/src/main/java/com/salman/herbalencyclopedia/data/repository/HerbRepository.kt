package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await

/**
 * Talks to the same Firestore collections as the web app:
 * "herbs" and "categories".
 *
 * Sync model:
 * - [observeHerbs] / [observeCategories] keep a *live* Firestore listener open
 *   (`addSnapshotListener`) so any change made here, from another device, or
 *   from the web app's admin panel is pushed to this app in real time -
 *   including the app's own local writes, which are reflected instantly from
 *   Firestore's local cache before the server round-trip even completes
 *   (optimistic UI), then reconciled automatically when the server confirms.
 * - [fetchHerbs] / [fetchCategories] remain available as one-shot reads
 *   (used for the manual "retry/refresh" action and for the admin tools that
 *   need a definite snapshot, like [restoreBackup] verification).
 * - Offline persistence is enabled with a *bounded* cache size (see
 *   [PERSISTENT_CACHE_BYTES]) so the full catalog (including base64 image
 *   data) still stays available offline and all local writes made offline
 *   still queue up and sync automatically once the connection returns, but
 *   Firestore's own garbage collector now trims the least-recently-used
 *   documents once the cache grows past the cap instead of retaining every
 *   historical snapshot forever. An unlimited cache was the main reason the
 *   app's on-disk footprint kept climbing well past the actual catalog size
 *   the longer the app stayed installed and synced.
 * - [observeCollection] auto-retries with exponential backoff on listener
 *   errors instead of permanently ending the live sync (see its retryWhen).
 */
class HerbRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(PERSISTENT_CACHE_BYTES)
                    .build()
            )
            .build()
    }

    // ---------------------------------------------------------------------
    // Live (real-time) sync
    // ---------------------------------------------------------------------

    /** Emits the full herb list every time Firestore's data changes, locally or on the server. */
    fun observeHerbs(): Flow<List<Herb>> = observeCollection("herbs", Herb::class.java)

    /** Emits the full category list every time Firestore's data changes, locally or on the server. */
    fun observeCategories(): Flow<List<Category>> = observeCollection("categories", Category::class.java)

    /** Emits the full blends ("الخلطات") list every time Firestore's data changes, locally or on the server. */
    fun observeBlends(): Flow<List<Blend>> = observeCollection("blends", Blend::class.java)

    private fun <T> observeCollection(collection: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val registration = db.collection(collection)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(clazz))
                }
            }
        awaitClose { registration.remove() }
    }.retryWhen { _, attempt ->
        // كان أي خطأ بالمستمع (انقطاع لحظي، انتهاء صلاحية توكن، ...) يُنهي
        // المزامنة الحيّة نهائياً إلى أن يُعاد فتح التطبيق يدوياً. الآن
        // نعيد الاشتراك تلقائياً بتأخير تصاعدي (1s ثم 2s ثم 4s... سقف 30s)
        // بدل الاستسلام من أول خطأ - نفس المصدر (uiState.error) يبقى
        // يعرض الرسالة المناسبة للمستخدم أثناء إعادة المحاولة بالخلفية.
        val delayMs = (1000L shl attempt.toInt().coerceAtMost(5)).coerceAtMost(30_000L)
        delay(delayMs)
        true
    }

    // ---------------------------------------------------------------------
    // One-shot reads
    // ---------------------------------------------------------------------

    suspend fun fetchHerbs(fromServer: Boolean = false): List<Herb> =
        db.collection("herbs").get(if (fromServer) Source.SERVER else Source.DEFAULT)
            .await().toObjects(Herb::class.java)

    suspend fun fetchCategories(fromServer: Boolean = false): List<Category> =
        db.collection("categories").get(if (fromServer) Source.SERVER else Source.DEFAULT)
            .await().toObjects(Category::class.java)

    suspend fun fetchBlends(fromServer: Boolean = false): List<Blend> =
        db.collection("blends").get(if (fromServer) Source.SERVER else Source.DEFAULT)
            .await().toObjects(Blend::class.java)

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    suspend fun addHerb(herb: Herb) {
        val data: HashMap<String, Any?> = hashMapOf(
            "name" to herb.name.trim(),
            "category_id" to herb.categoryId,
            "benefits" to herb.benefits.trim().ifBlank { "—" },
            "warnings" to herb.warnings.trim().ifBlank { "—" },
            "harms" to herb.harms.trim().ifBlank { "—" },
            "usage" to herb.usage.trim().ifBlank { "—" },
            "notes" to herb.notes.trim().ifBlank { "—" },
            "image_url" to herb.imageUrl,
            "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("herbs").add(data).await()
    }

    suspend fun updateHerb(herb: Herb) {
        val data: HashMap<String, Any?> = hashMapOf(
            "name" to herb.name.trim(),
            "category_id" to herb.categoryId,
            "benefits" to herb.benefits.trim().ifBlank { "—" },
            "warnings" to herb.warnings.trim().ifBlank { "—" },
            "harms" to herb.harms.trim().ifBlank { "—" },
            "usage" to herb.usage.trim().ifBlank { "—" },
            "notes" to herb.notes.trim().ifBlank { "—" },
            "image_url" to herb.imageUrl,
            "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("herbs").document(herb.id).update(data).await()
    }

    suspend fun deleteHerb(id: String) {
        db.collection("herbs").document(id).delete().await()
    }

    // ---------------------------------------------------------------------
    // Blends ("الخلطات") — same admin-only write model as herbs above.
    // ---------------------------------------------------------------------

    suspend fun addBlend(blend: Blend) {
        val data: HashMap<String, Any?> = hashMapOf(
            "name" to blend.name.trim(),
            "herb_ids" to blend.herbIds,
            "benefits" to blend.benefits.trim().ifBlank { "—" },
            "usage" to blend.usage.trim().ifBlank { "—" },
            "warnings" to blend.warnings.trim().ifBlank { "—" },
            "notes" to blend.notes.trim().ifBlank { "—" },
            "image_url" to blend.imageUrl,
            "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("blends").add(data).await()
    }

    suspend fun updateBlend(blend: Blend) {
        val data: HashMap<String, Any?> = hashMapOf(
            "name" to blend.name.trim(),
            "herb_ids" to blend.herbIds,
            "benefits" to blend.benefits.trim().ifBlank { "—" },
            "usage" to blend.usage.trim().ifBlank { "—" },
            "warnings" to blend.warnings.trim().ifBlank { "—" },
            "notes" to blend.notes.trim().ifBlank { "—" },
            "image_url" to blend.imageUrl,
            "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("blends").document(blend.id).update(data).await()
    }

    suspend fun deleteBlend(id: String) {
        db.collection("blends").document(id).delete().await()
    }

    suspend fun addCategory(name: String) {
        db.collection("categories").add(
            hashMapOf(
                "name" to name.trim(),
                "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateCategory(id: String, name: String) {
        db.collection("categories").document(id).update("name", name.trim()).await()
    }

    suspend fun deleteCategory(id: String) {
        db.collection("categories").document(id).delete().await()
    }

    suspend fun deleteAllHerbs() {
        val docs = db.collection("herbs").get(Source.SERVER).await().documents
        docs.chunked(450).forEach { chunk ->
            val batch = db.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
        }
    }

    suspend fun deleteAllData() {
        deleteAllHerbs()
        val docs = db.collection("categories").get(Source.SERVER).await().documents
        docs.chunked(450).forEach { chunk ->
            val batch = db.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
        }
    }

    /** Forces a real round-trip to the server (bypassing the local cache) to verify connectivity. */
    suspend fun testConnection(): Boolean {
        db.collection("categories").limit(1).get(Source.SERVER).await()
        return true
    }

    suspend fun restoreBackup(json: String) {
        val root = org.json.JSONObject(json)
        val operations = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>()
        val categories = root.optJSONArray("categories") ?: org.json.JSONArray()
        for (i in 0 until categories.length()) {
            val o = categories.getJSONObject(i); val id = o.optString("id")
            if (id.isNotBlank()) operations += db.collection("categories").document(id) to hashMapOf("name" to o.optString("name"), "icon" to o.optString("icon").ifBlank { null })
        }
        val herbs = root.optJSONArray("herbs") ?: org.json.JSONArray()
        for (i in 0 until herbs.length()) {
            val o = herbs.getJSONObject(i); val id = o.optString("id")
            if (id.isNotBlank()) operations += db.collection("herbs").document(id) to hashMapOf("name" to o.optString("name"), "category_id" to o.optString("categoryId").ifBlank { null }, "benefits" to o.optString("benefits"), "warnings" to o.optString("warnings"), "harms" to o.optString("harms"), "usage" to o.optString("usage"), "notes" to o.optString("notes"), "image_url" to o.optString("imageUrl").ifBlank { null })
        }
        operations.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, data) -> batch.set(ref, data) }
            batch.commit().await()
        }
    }

    companion object {
        /**
         * سقف كاش Firestore المحلي (٤٠ ميجابايت). كانت القيمة السابقة
         * CACHE_SIZE_UNLIMITED تعني عدم وجود أي سقف إطلاقاً، فيستمر الكاش
         * بالتضخم بلا حدود مع كل مزامنة أو تحديث حتى لصور/مستندات لم تعد
         * تُعرض فعلياً — وهذا هو السبب الرئيسي لكون حجم التطبيق بعد التثبيت
         * والاستخدام أكبر بكثير من حجم بيانات الموسوعة الفعلي. بسقف محدود،
         * Firestore يشغّل تنظيفاً تلقائياً (LRU garbage collection) يحذف أقدم
         * المستندات غير المستخدمة عند تجاوز هذا الحد، مع إبقاء العمل بلا
         * إنترنت يعمل بشكل طبيعي تماماً (الموسوعة كاملة أصغر من هذا السقف
         * بمراحل، فتبقى كل البيانات الفعلية محفوظة محلياً دوماً).
         */
        private const val PERSISTENT_CACHE_BYTES: Long = 40L * 1024 * 1024

        /** Turns a Firestore/network exception into a short, user-facing Arabic message. */
        fun describeError(e: Throwable): String {
            val code = (e as? FirebaseFirestoreException)?.code
            return when (code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "لا يوجد اتصال بالإنترنت حالياً. سيتم عرض آخر بيانات محفوظة والمزامنة تلقائياً عند عودة الاتصال."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "ليست لديك صلاحية الوصول إلى هذه البيانات."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "يجب تسجيل الدخول أولاً."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "انتهت مهلة الاتصال بالخادم. حاول مرة أخرى."
                else -> e.localizedMessage ?: "حدث خطأ غير متوقع أثناء المزامنة."
            }
        }
    }
}
