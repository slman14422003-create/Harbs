package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 * - Offline persistence is enabled with an unlimited cache size so the full
 *   catalog (including base64 image data) stays available offline and all
 *   local writes made offline queue up and sync automatically once the
 *   connection returns.
 */
class HerbRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
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

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    suspend fun addHerb(herb: Herb) {
        val data: HashMap<String, Any?> = hashMapOf(
            "name" to herb.name,
            "category_id" to herb.categoryId,
            "benefits" to herb.benefits.ifBlank { "—" },
            "warnings" to herb.warnings.ifBlank { "—" },
            "harms" to herb.harms.ifBlank { "—" },
            "usage" to herb.usage.ifBlank { "—" },
            "notes" to herb.notes.ifBlank { "—" },
            "image_url" to herb.imageUrl,
            "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("herbs").add(data).await()
    }

    suspend fun updateHerb(herb: Herb) {
        val data: HashMap<String, Any?> = hashMapOf(
            "name" to herb.name,
            "category_id" to herb.categoryId,
            "benefits" to herb.benefits.ifBlank { "—" },
            "warnings" to herb.warnings.ifBlank { "—" },
            "harms" to herb.harms.ifBlank { "—" },
            "usage" to herb.usage.ifBlank { "—" },
            "notes" to herb.notes.ifBlank { "—" },
            "image_url" to herb.imageUrl,
            "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("herbs").document(herb.id).update(data).await()
    }

    suspend fun deleteHerb(id: String) {
        db.collection("herbs").document(id).delete().await()
    }

    suspend fun addCategory(name: String) {
        db.collection("categories").add(
            hashMapOf(
                "name" to name,
                "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateCategory(id: String, name: String) {
        db.collection("categories").document(id).update("name", name).await()
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
