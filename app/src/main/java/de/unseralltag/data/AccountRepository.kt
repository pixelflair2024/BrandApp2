package de.unseralltag.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/** Anmeldung (Google) und Haushalts-Verwaltung. */
object AccountRepository {
    private val auth get() = Firebase.auth
    private val db get() = Firebase.firestore

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = db.collection("users").document(uid).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            trySend(snap?.toObject(UserProfile::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun observeHousehold(id: String): Flow<Household?> = callbackFlow {
        val reg = db.collection("households").document(id).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(null); return@addSnapshotListener
            }
            trySend(snap?.toObject(Household::class.java))
        }
        awaitClose { reg.remove() }
    }

    private fun webClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else null
    }

    /** Muss mit einem Activity-Context aufgerufen werden. */
    suspend fun signInWithGoogle(activityContext: Context) {
        val clientId = webClientId(activityContext)
            ?: error("Firebase ist nicht eingerichtet (default_web_client_id fehlt). Siehe Anleitung im README.")
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val cred = result.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val google = GoogleIdTokenCredential.createFrom(cred.data)
            val firebaseCred = GoogleAuthProvider.getCredential(google.idToken, null)
            val user = auth.signInWithCredential(firebaseCred).await().user ?: error("Anmeldung fehlgeschlagen")
            ensureProfile(user)
        } else {
            error("Unerwarteter Anmeldetyp")
        }
    }

    private suspend fun ensureProfile(user: FirebaseUser) {
        val ref = db.collection("users").document(user.uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(
                UserProfile(
                    name = user.displayName?.substringBefore(' ')?.ifBlank { null } ?: "Ich",
                    email = user.email ?: "",
                )
            ).await()
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    suspend fun updateMyName(name: String, householdId: String?) {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).set(mapOf("name" to name), SetOptions.merge()).await()
        if (householdId != null) {
            db.collection("households").document(householdId).update("memberNames.$uid", name).await()
        }
    }

    private fun newCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        return (1..6).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun createHousehold(householdName: String, myName: String) {
        val uid = currentUser?.uid ?: error("Nicht angemeldet")
        val ref = db.collection("households").document()
        val code = newCode()
        ref.set(
            mapOf(
                "name" to householdName,
                "members" to listOf(uid),
                "memberNames" to mapOf(uid to myName),
                "inviteCode" to code,
                "lastJoinCode" to "",
            )
        ).await()
        db.collection("invites").document(code).set(mapOf("householdId" to ref.id)).await()
        db.collection("users").document(uid)
            .set(mapOf("householdId" to ref.id, "name" to myName), SetOptions.merge()).await()
        // Standard-Einkaufsliste anlegen
        ref.collection(Collections.SHOPPING_LISTS).add(ShoppingList(name = "Einkauf", order = 0))
    }

    suspend fun joinHousehold(rawCode: String, myName: String) {
        val uid = currentUser?.uid ?: error("Nicht angemeldet")
        val code = rawCode.trim().uppercase()
        val invite = db.collection("invites").document(code).get().await()
        val hid = invite.getString("householdId") ?: error("Code nicht gefunden")
        db.collection("households").document(hid).update(
            mapOf(
                "members" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to myName,
                "lastJoinCode" to code,
            )
        ).await()
        db.collection("users").document(uid)
            .set(mapOf("householdId" to hid, "name" to myName), SetOptions.merge()).await()
    }
}
