package com.example.artsan_finder.data.repository

import android.content.Context
import com.example.artsan_finder.data.local.LocalStorageManager
import com.example.artsan_finder.data.model.User
import com.example.artsan_finder.data.model.UserRole
import com.example.artsan_finder.data.remote.FirebaseModule
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Cloud authentication backed by Firebase Auth (email/password) with
 * user profiles stored in the Firestore "users" collection.
 * Works across any device, anywhere in the world.
 */
class AuthRepository(
    private val context: Context,
    private val storageManager: LocalStorageManager
) {
    private val auth: FirebaseAuth get() = FirebaseModule.auth
    private val usersCollection get() = FirebaseModule.firestore.collection("users")

    /** The address the user actually logs in with — owned by Firebase Auth, not the profile doc. */
    val currentLoginEmail: String? get() = auth.currentUser?.email

    /** When the Firebase Auth account was created — the true registration date. */
    val accountCreatedAt: Long? get() = auth.currentUser?.metadata?.creationTimestamp

    /** Emits the logged-in user's id in real time (null when signed out). */
    val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Checks the existing session on startup.
     * If 'Remember Me' was not checked, it clears the session to force a fresh login.
     */
    suspend fun checkSessionPersistence() {
        val rememberMe = storageManager.rememberMeEnabled.first()
        if (!rememberMe && auth.currentUser != null) {
            auth.signOut()
            storageManager.clearUserSession()
        }
    }

    suspend fun login(email: String, password: String, remember: Boolean = false): Result<User> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Invalid email or password"))
        }
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Login failed. Please try again."))

            val snapshot = usersCollection.document(uid).get().await()
            // The document KEY is the Firebase Auth uid — the identity security
            // rules verify. Never trust the stored `id` field: a profile edited
            // or created by hand can carry a stale value, and every write signed
            // with it gets PERMISSION_DENIED.
            val user = snapshot.toObject(User::class.java)?.copy(id = uid)

            if (user == null) {
                auth.signOut()
                return Result.failure(Exception("Account profile not found. Please contact support."))
            }

            storageManager.saveUserSession(user.id, user.email, user.name, remember)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            // Secondary instance: account is created but the user still logs in manually afterwards
            val secondary = FirebaseModule.secondaryAuth(context)
            val result = secondary.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Registration failed. Please try again."))

            // Write the profile through the secondary app so the request is authenticated
            // as the new user — security rules reject unauthenticated profile writes.
            val user = User(id = uid, email = email.trim(), name = name.trim(), role = UserRole.CUSTOMER)
            FirebaseModule.secondaryFirestore(context).collection("users")
                .document(uid).set(user).await()
            secondary.signOut()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    /**
     * Sends a password-reset email for the given address. Firebase hides whether the
     * account exists (email enumeration protection), so success only means the request
     * was accepted — the message shown to the user must stay neutral.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return Result.failure(Exception("Enter your email address first"))
        }
        return try {
            auth.sendPasswordResetEmail(trimmed).await()
            Result.success(trimmed)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    /**
     * Changes the signed-in user's own password. Firebase requires a recent login,
     * so the current password is used to re-authenticate first.
     */
    suspend fun changeOwnPassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            val email = user.email ?: return Result.failure(Exception("This account has no login email"))
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    /**
     * Changes the signed-in user's own login email. Firebase sends a verification
     * link to the new address; the login email switches once it is confirmed.
     */
    suspend fun changeOwnLoginEmail(currentPassword: String, newEmail: String): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            val email = user.email ?: return Result.failure(Exception("This account has no login email"))
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            user.verifyBeforeUpdateEmail(newEmail.trim()).await()
            Result.success("Verification link sent to ${newEmail.trim()} — the login email changes after you confirm it there.")
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun logout() {
        auth.signOut()
        storageManager.clearUserSession()
    }

    suspend fun deleteAccount(userId: String) {
        try {
            usersCollection.document(userId).delete().await()
            auth.currentUser?.takeIf { it.uid == userId }?.delete()?.await()
        } catch (_: Exception) {
            // Best effort — profile removal is what blocks future logins
        }
        storageManager.clearUserSession()
    }

    /** Observes a user profile in real time from Firestore. */
    fun getUser(userId: String): Flow<User?> = callbackFlow {
        val registration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                // A transient listener error (network blip, auth token refresh)
                // must not emit null — a null here reads as "signed out" upstream
                // and kicks the user back to the Login screen mid-session.
                if (error != null) return@addSnapshotListener
                // Doc key = auth uid = the identity rules check; the stored
                // `id` field may be stale on hand-edited profiles.
                trySend(snapshot?.toObject(User::class.java)?.copy(id = snapshot.id))
            }
        awaitClose { registration.remove() }
    }

    private fun friendlyAuthError(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
        is FirebaseAuthInvalidUserException -> "No account found for that email"
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException -> "Password is too weak — use at least 6 characters"
        is FirebaseNetworkException -> "No internet connection. Please check your network and try again."
        else -> e.message ?: "Something went wrong. Please try again."
    }
}
