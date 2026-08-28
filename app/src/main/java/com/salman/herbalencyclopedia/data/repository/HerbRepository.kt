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
        val data = hashMapOf(
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
        val data = hashMapOf(
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
}
