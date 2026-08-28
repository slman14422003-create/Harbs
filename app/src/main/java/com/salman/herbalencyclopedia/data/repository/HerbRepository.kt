package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb
import kotlinx.coroutines.tasks.await

/**
 * Talks to the same Firestore collections as the web app:
 * "herbs" and "categories".
 */
class HerbRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
            .build()
    }

    suspend fun fetchHerbs(): List<Herb> =
        db.collection("herbs").get().await().toObjects(Herb::class.java)

    suspend fun fetchCategories(): List<Category> =
        db.collection("categories").get().await().toObjects(Category::class.java)

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
            "created_at" to com.google.firebase.Timestamp.now()
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
            "updated_at" to com.google.firebase.Timestamp.now()
        )
        db.collection("herbs").document(herb.id).update(data).await()
    }

    suspend fun deleteHerb(id: String) {
        db.collection("herbs").document(id).delete().await()
    }

    suspend fun addCategory(name: String) {
        db.collection("categories").add(hashMapOf("name" to name)).await()
    }

    suspend fun updateCategory(id: String, name: String) {
        db.collection("categories").document(id).update("name", name).await()
    }

    suspend fun deleteCategory(id: String) {
        db.collection("categories").document(id).delete().await()
    }

    suspend fun deleteAllHerbs() {
        val docs = db.collection("herbs").get().await().documents
        docs.chunked(450).forEach { chunk ->
            val batch = db.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
        }
    }

    suspend fun deleteAllData() {
        deleteAllHerbs()
        val docs = db.collection("categories").get().await().documents
        docs.chunked(450).forEach { chunk ->
            val batch = db.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
        }
    }

    suspend fun testConnection(): Boolean {
        db.collection("categories").limit(1).get().await()
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

}
