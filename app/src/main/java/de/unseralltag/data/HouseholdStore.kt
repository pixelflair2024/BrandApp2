package de.unseralltag.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object Collections {
    const val EVENTS = "events"
    const val SPECIAL_DAYS = "specialDays"
    const val SHOPPING_LISTS = "shoppingLists"
    const val SHOPPING_ITEMS = "shoppingItems"
    const val WISHES = "wishes"
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val MEALS = "meals"
    const val CONTRACTS = "contracts"
}

/** Zugriff auf alle Daten eines Haushalts (households/{id}/...). */
class HouseholdStore(val householdId: String) {
    private val db = Firebase.firestore

    fun col(name: String): CollectionReference =
        db.collection("households").document(householdId).collection(name)

    fun <T : Any> observe(name: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val reg = col(name).addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.toObjects(clazz))
        }
        awaitClose { reg.remove() }
    }

    suspend fun <T : Any> getAll(name: String, clazz: Class<T>): List<T> =
        col(name).get().await().toObjects(clazz)

    /** Neu anlegen (id leer) oder überschreiben. Schreibt asynchron – funktioniert auch offline. */
    fun save(name: String, id: String, data: Any) {
        if (id.isBlank()) col(name).add(data) else col(name).document(id).set(data)
    }

    fun update(name: String, id: String, fields: Map<String, Any?>) {
        col(name).document(id).set(fields, SetOptions.merge())
    }

    fun delete(name: String, id: String) {
        col(name).document(id).delete()
    }

    fun renameMember(uid: String, newName: String) {
        db.collection("households").document(householdId)
            .update("memberNames.$uid", newName)
    }

    fun renameHousehold(newName: String) {
        db.collection("households").document(householdId).update("name", newName)
    }

    companion object {
        fun serverNow() = FieldValue.serverTimestamp()
    }
}

inline fun <reified T : Any> HouseholdStore.observe(name: String): Flow<List<T>> =
    observe(name, T::class.java)
