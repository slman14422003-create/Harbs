package com.salman.herbalencyclopedia.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Mirrors a "feedback" document in Firestore: a note from any user (no
 * login required to send) reporting wrong/missing info on a herb or blend.
 * Only the admin account can read this collection (see firestore.rules),
 * and it shows up formatted in [com.salman.herbalencyclopedia.ui.screens.admin.AdminFeedbackScreen].
 */
data class Feedback @JvmOverloads constructor(
    @DocumentId
    val id: String = "",

    /** "herb" or "blend" — which collection [targetId] belongs to. */
    @get:PropertyName("target_type")
    @set:PropertyName("target_type")
    var targetType: String = "herb",

    @get:PropertyName("target_id")
    @set:PropertyName("target_id")
    var targetId: String = "",

    /** Snapshotted at send time so the report still reads fine even if the item is later renamed/deleted. */
    @get:PropertyName("target_name")
    @set:PropertyName("target_name")
    var targetName: String = "",

    @get:PropertyName("message")
    @set:PropertyName("message")
    var message: String = "",

    /** Optional — the sender may leave this blank and stay anonymous. */
    @get:PropertyName("sender_name")
    @set:PropertyName("sender_name")
    var senderName: String? = null,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Timestamp? = null
)
