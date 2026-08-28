package com.salman.herbalencyclopedia.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.salman.herbalencyclopedia.HerbalApp
import kotlinx.coroutines.tasks.await

data class AuthResult(val success: Boolean, val isAdmin: Boolean = false, val message: String? = null)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isAdmin: Boolean get() = auth.currentUser?.uid == HerbalApp.ADMIN_UID

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val admin = result.user?.uid == HerbalApp.ADMIN_UID
            AuthResult(success = true, isAdmin = admin)
        } catch (e: Exception) {
            AuthResult(success = false, message = e.localizedMessage)
        }
    }

    suspend fun register(email: String, password: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            AuthResult(success = true)
        } catch (e: Exception) {
            AuthResult(success = false, message = e.localizedMessage)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
