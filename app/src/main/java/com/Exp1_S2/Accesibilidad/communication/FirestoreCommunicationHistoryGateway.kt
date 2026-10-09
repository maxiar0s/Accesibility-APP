package com.Exp1_S2.Accesibilidad.communication

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreCommunicationHistoryGateway(private val firestore: FirebaseFirestore) : CommunicationHistoryGateway {
    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection(COLLECTION)

    override fun listen(
        uid: String,
        changed: (Result<List<CommunicationMessage>>) -> Unit
    ): HistorySubscription {
        val registration = collection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(MAX_HISTORY_ITEMS)
            .addSnapshotListener { snapshot, error ->
            when {
                error != null -> changed(Result.failure(error))
                snapshot != null -> changed(Result.success(snapshot.documents.mapNotNull { document ->
                    val text = document.get("text") as? String
                    val createdAt = (document.get("createdAt") as? Timestamp)?.toDate()?.time
                    if (text != null && createdAt != null) CommunicationMessage(document.id, text, createdAt) else null
                }.sortedByDescending(CommunicationMessage::createdAt)))
            }
            }
        return HistorySubscription { registration.remove() }
    }

    override fun create(uid: String, text: String, done: (Result<Unit>) -> Unit) {
        collection(uid).add(newMessageData(text)).complete(done)
    }

    override fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit) {
        collection(uid).document(id).delete().complete(done)
    }

    override fun clearAll(uid: String, done: (Result<Unit>) -> Unit) {
        val messages = collection(uid)
        fun deleteNextBatch() {
            messages.limit(DELETE_BATCH_SIZE).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        done(Result.success(Unit))
                    } else {
                        val batch = firestore.batch()
                        snapshot.documents.forEach { batch.delete(it.reference) }
                        batch.commit()
                            .addOnSuccessListener { deleteNextBatch() }
                            .addOnFailureListener { done(Result.failure(it)) }
                    }
                }
                .addOnFailureListener { done(Result.failure(it)) }
        }
        deleteNextBatch()
    }

    private fun <T> Task<T>.complete(done: (Result<Unit>) -> Unit) {
        addOnSuccessListener { done(Result.success(Unit)) }
        addOnFailureListener { done(Result.failure(it)) }
    }

    companion object {
        const val COLLECTION = "communicationHistory"
        const val MAX_HISTORY_ITEMS = 100L
        private const val DELETE_BATCH_SIZE = 450L

        internal fun newMessageData(text: String): Map<String, Any> = mapOf(
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}
