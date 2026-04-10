package com.example.jaxgamepad

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(
    private val context: Context
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            val googleIdTokenCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                return Result.failure(e)
            }

            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )

            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: return Result.failure(
                IllegalStateException("Firebase user was null")
            )

            // Sync Google profile data to Firestore
            try {
                val db = FirebaseFirestore.getInstance()
                val profileRef = db.collection("users").document(user.uid)
                val snapshot = profileRef.get().await()

                // Only overwrite or create if display name is missing
                if (!snapshot.exists() || snapshot.getString("displayName").isNullOrBlank()) {
                    val updates = hashMapOf<String, Any>(
                        "uid" to user.uid,
                        "email" to (user.email ?: ""),
                        "displayName" to (user.displayName ?: ""),
                        "photoPath" to (user.photoUrl?.toString() ?: "")
                    )
                    profileRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                // Non-critical error, don't fail the sign-in
                android.util.Log.e("GOOGLE_AUTH", "Failed to sync profile to Firestore", e)
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun currentUser() = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}

