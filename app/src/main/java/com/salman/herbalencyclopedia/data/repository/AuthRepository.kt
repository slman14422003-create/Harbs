package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.salman.herbalencyclopedia.HerbalApp
import kotlinx.coroutines.tasks.await

data class AuthResult(
    val success: Boolean,
    val isAdmin: Boolean = false,
    val message: String? = null
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser get() = auth.currentUser

    /** The only account that may access the administration session. */
    val isAdmin: Boolean
        get() = auth.currentUser?.uid == HerbalApp.ADMIN_UID

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user?.uid != HerbalApp.ADMIN_UID) {
                // A valid Firebase account is not enough: this app is admin-only.
                auth.signOut()
                AuthResult(
                    success = false,
                    message = "هذا الحساب غير مخوّل بالدخول إلى لوحة الإدارة."
                )
            } else {
                AuthResult(success = true, isAdmin = true)
            }
        } catch (e: Exception) {
            AuthResult(
                success = false,
                message = e.localizedMessage ?: "تعذّر تسجيل الدخول. تحقق من البيانات."
            )
        }
    }

    fun logout() {
        auth.signOut()
    }
}
