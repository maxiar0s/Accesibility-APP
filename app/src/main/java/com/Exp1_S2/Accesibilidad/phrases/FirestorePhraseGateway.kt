package com.Exp1_S2.Accesibilidad.phrases

import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task

class FirestorePhraseGateway(private val firestore: FirebaseFirestore) : PhraseGateway {
    private fun collection(uid: String) = firestore.collection("users").document(uid).collection("phrases")

    override fun listen(uid: String, changed: (Result<List<PersonalPhrase>>) -> Unit): PhraseSubscription {
        val registration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) changed(Result.failure(error))
            else if (snapshot != null) changed(Result.success(snapshot.documents.mapNotNull { document ->
                (document.get("text") as? String)?.let { PersonalPhrase(document.id, it) }
            }.sortedWith(compareBy<PersonalPhrase> { it.text }.thenBy { it.id })))
        }
        return PhraseSubscription { registration.remove() }
    }

    override fun create(uid: String, text: String, done: (Result<Unit>) -> Unit) {
        collection(uid).add(mapOf("text" to text)).complete(done)
    }

    override fun update(uid: String, id: String, text: String, done: (Result<Unit>) -> Unit) {
        collection(uid).document(id).update("text", text).complete(done)
    }

    override fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit) {
        collection(uid).document(id).delete().complete(done)
    }

    private fun <T> Task<T>.complete(done: (Result<Unit>) -> Unit) {
        addOnSuccessListener { done(Result.success(Unit)) }
        addOnFailureListener { done(Result.failure(it)) }
    }
}
