package com.Exp1_S2.Accesibilidad

import com.Exp1_S2.Accesibilidad.communication.*
import org.junit.Assert.*
import org.junit.Test

class CommunicationHistoryTest {
    @Test fun firestoreMessageDocumentContainsOnlyTextAndTimestamp() {
        val document = FirestoreCommunicationHistoryGateway.newMessageData("Hola")

        assertEquals(setOf("text", "createdAt"), document.keys)
        assertEquals("Hola", document["text"])
        assertNotNull(document["createdAt"])
    }

    @Test fun bindsSavesListsAndDeletesOnlyOwnedHistory() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway)
        history.bind("uid-a")
        assertEquals("uid-a", gateway.listenUid)
        gateway.emit(Result.success(listOf(CommunicationMessage("one", "Hola", 1))))
        history.save("  Necesito ayuda  ")
        assertEquals("Necesito ayuda", gateway.savedText)
        assertEquals("uid-a", gateway.writeUid)
        gateway.saveDone(Result.success(Unit))
        history.delete("one")
        assertEquals("one", gateway.deletedId)
        gateway.deleteDone(Result.success(Unit))
        assertNotNull(history.state.notice)
    }

    @Test fun clearsAllOwnedHistoryAndReportsFailure() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway).apply { bind("uid-a") }

        history.clearAll()

        assertEquals("uid-a", gateway.clearUid)
        assertTrue(history.state.writing)
        gateway.clearDone(Result.failure(IllegalStateException()))
        assertFalse(history.state.writing)
        assertNotNull(history.state.error)
    }

    @Test fun rejectsBlankAndOverLimitMessages() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway).apply { bind("uid-a") }
        history.save("  ")
        assertNotNull(history.state.error)
        history.save("x".repeat(CommunicationHistory.MAX_MESSAGE_LENGTH + 1))
        assertNotNull(history.state.error)
        assertNull(gateway.savedText)
    }

    @Test fun saveReportsInvalidAndBusyRejectionsAndAsyncFailure() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway).apply { bind("uid-a") }
        var callbackResult: Result<Unit>? = null

        history.save("x".repeat(CommunicationHistory.MAX_MESSAGE_LENGTH + 1)) { callbackResult = it }
        assertTrue(callbackResult?.isFailure == true)
        assertNull(gateway.savedText)

        history.save("first")
        history.save("second") { callbackResult = it }
        assertTrue(callbackResult?.isFailure == true)
        assertEquals("first", gateway.savedText)

        callbackResult = null
        gateway.saveDone(Result.success(Unit))
        assertFalse(history.state.writing)

        history.save("first-success") { callbackResult = it }
        assertNull(callbackResult)
        gateway.saveDone(Result.success(Unit))
        assertTrue(callbackResult?.isSuccess == true)

        callbackResult = null
        history.save("failure") { callbackResult = it }
        assertNull(callbackResult)
        gateway.saveDone(Result.failure(IllegalStateException("offline")))
        assertTrue(callbackResult?.isFailure == true)
        assertFalse(history.state.writing)
        assertNotNull(history.state.error)
    }

    @Test fun staleSaveCompletionsAfterRebindOrCloseDoNotReportSuccess() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway).apply { bind("uid-a") }
        var callbackResult: Result<Unit>? = null

        history.save("rebind") { callbackResult = it }
        assertNull(callbackResult)
        val rebindCompletion = gateway.saveDone
        history.bind("uid-b")
        rebindCompletion(Result.success(Unit))
        assertNull(callbackResult)

        history.save("close") { callbackResult = it }
        assertNull(callbackResult)
        val closeCompletion = gateway.saveDone
        history.close()
        closeCompletion(Result.success(Unit))
        assertNull(callbackResult)
    }

    @Test fun saveReportsMissingUidAndClosedController() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway)
        var callbackResult: Result<Unit>? = null

        history.save("hello") { callbackResult = it }
        assertTrue(callbackResult?.isFailure == true)
        assertNull(gateway.savedText)

        history.bind("uid-a")
        history.close()
        callbackResult = null
        history.save("hello") { callbackResult = it }
        assertTrue(callbackResult?.isFailure == true)
        assertNull(gateway.savedText)
    }

    @Test fun reportsReadWriteErrorsAndIgnoresStaleCallbacks() {
        val gateway = FakeGateway()
        val history = CommunicationHistory(gateway)
        history.bind("uid-a")
        val stale = gateway.listener
        history.bind("uid-b")
        stale(Result.success(listOf(CommunicationMessage("stale", "stale", 0))))
        assertTrue(history.state.messages.isEmpty())
        gateway.emit(Result.failure(IllegalStateException()))
        assertNotNull(history.state.error)
        history.save("hola")
        gateway.saveDone(Result.failure(IllegalStateException()))
        assertNotNull(history.state.error)
        assertFalse(history.state.writing)
    }

    private class FakeGateway : CommunicationHistoryGateway {
        var listenUid: String? = null
        var writeUid: String? = null
        var savedText: String? = null
        var deletedId: String? = null
        var clearUid: String? = null
        lateinit var listener: (Result<List<CommunicationMessage>>) -> Unit
        lateinit var saveDone: (Result<Unit>) -> Unit
        lateinit var deleteDone: (Result<Unit>) -> Unit
        lateinit var clearDone: (Result<Unit>) -> Unit
        override fun listen(uid: String, changed: (Result<List<CommunicationMessage>>) -> Unit): HistorySubscription {
            listenUid = uid; listener = changed
            return HistorySubscription { }
        }
        override fun create(uid: String, text: String, done: (Result<Unit>) -> Unit) {
            writeUid = uid; savedText = text; saveDone = done
        }
        override fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit) {
            writeUid = uid; deletedId = id; deleteDone = done
        }
        override fun clearAll(uid: String, done: (Result<Unit>) -> Unit) {
            clearUid = uid; clearDone = done
        }
        fun emit(result: Result<List<CommunicationMessage>>) = listener(result)
    }
}
