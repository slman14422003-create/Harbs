package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.salman.herbalencyclopedia.data.model.Feedback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Talks to the "feedback" Firestore collection: users report wrong/missing
 * info on a herb or blend here. Sending is open to everyone (see
 * firestore.rules — no login required), but only the admin account can
 * list/read this collection, which is what backs [observeFeedback] used by
 * the admin-only inbox in Settings.
 */
class FeedbackRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun submitFeedback(
        targetType: String,
        targetId: String,
        targetName: String,
        message: String,
        senderName: String?
    ) {
        val data: HashMap<String, Any?> = hashMapOf(
            "target_type" to targetType,
            "target_id" to targetId,
            "target_name" to targetName,
            "message" to message.trim(),
            "sender_name" to senderName?.trim()?.ifBlank { null },
            "created_at" to FieldValue.serverTimestamp()
        )
        db.collection("feedback").add(data).await()
    }

    /** Live listener, admin-only per firestore.rules — newest feedback first. */
    fun observeFeedback(): Flow<List<Feedback>> = callbackFlow {
        val registration = db.collection("feedback")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Feedback::class.java))
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun deleteFeedback(id: String) {
        db.collection("feedback").document(id).delete().await()
    }
}
