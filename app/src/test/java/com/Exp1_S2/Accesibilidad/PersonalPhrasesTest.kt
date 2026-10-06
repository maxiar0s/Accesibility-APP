package com.Exp1_S2.Accesibilidad

import com.Exp1_S2.Accesibilidad.phrases.*
import org.junit.Assert.*
import org.junit.Test

class PersonalPhrasesTest {
    private val gateway = FakeGateway()
    private val controller = PersonalPhrases(gateway).apply { bind("owner") }
    private val phrase = PersonalPhrase("id", "Original")

    @Test fun createTrimsAndWaitsForWriteCompletion() {
        controller.draft("  Hello  ")
        controller.save()
        assertEquals(listOf("create", "owner", "Hello"), gateway.writes.single())
        gateway.emit(listOf(PersonalPhrase("new", "Hello")))
        assertTrue(controller.state.writing)
        assertEquals("  Hello  ", controller.state.draft)
        gateway.complete(Result.success(Unit))
        assertFalse(controller.state.writing)
        assertEquals("", controller.state.draft)
    }

    @Test fun listIsListenerDrivenNotOptimisticallyMutated() {
        assertTrue(controller.state.loading)
        gateway.emit(listOf(phrase))
        assertEquals(listOf(phrase), controller.state.phrases)
        assertFalse(controller.state.loading)
        controller.delete(phrase.id)
        gateway.complete(Result.success(Unit))
        assertEquals(listOf(phrase), controller.state.phrases)
        gateway.emit(emptyList())
        assertTrue(controller.state.phrases.isEmpty())
    }

    @Test fun editAndDeleteUseOwnerAndDocumentId() {
        gateway.emit(listOf(phrase))
        controller.edit(phrase)
        controller.draft(" Changed ")
        controller.save()
        assertEquals(listOf("update", "owner", "id", "Changed"), gateway.writes.last())
        gateway.complete(Result.success(Unit))
        assertNull(controller.state.editingId)
        controller.delete("id")
        assertEquals(listOf("delete", "owner", "id"), gateway.writes.last())
        gateway.complete(Result.success(Unit))
        assertFalse(controller.state.writing)
    }

    @Test fun emptyAndTooLongTextNeverWrite() {
        for (text in listOf("   ", "x".repeat(501))) {
            controller.draft(text)
            controller.save()
            assertNotNull(controller.state.writeError)
            assertEquals(text, controller.state.draft)
        }
        assertTrue(gateway.writes.isEmpty())
        controller.draft("x".repeat(500))
        controller.save()
        assertEquals(1, gateway.writes.size)
    }

    @Test fun failedWritePreservesEditorAndAllowsRetry() {
        gateway.emit(listOf(phrase))
        controller.edit(phrase)
        controller.draft("Replacement")
        controller.save()
        gateway.complete(Result.failure(IllegalStateException()))
        assertEquals("Replacement", controller.state.draft)
        assertEquals("id", controller.state.editingId)
        assertEquals(listOf(phrase), controller.state.phrases)
        assertNotNull(controller.state.writeError)
        assertFalse(controller.state.writing)
        controller.save()
        assertEquals(2, gateway.writes.size)
    }

    @Test fun pendingWriteBlocksAllDuplicateMutationsAndEditorChanges() {
        gateway.emit(listOf(phrase))
        controller.draft("New")
        controller.save()
        controller.save()
        controller.delete("id")
        controller.edit(phrase)
        controller.draft("Ignored")
        controller.cancelEdit()
        assertEquals(1, gateway.writes.size)
        assertEquals("New", controller.state.draft)
    }

    @Test fun accountChangeCancelsAndDiscardsOldListAndWrite() {
        gateway.emit(listOf(phrase))
        controller.draft("Private")
        controller.save()
        val oldWrite = gateway.complete
        controller.bind("second")
        assertTrue(gateway.listeners[0].cancelled)
        assertEquals("second", gateway.listeners[1].uid)
        gateway.emit(listOf(phrase), 0)
        oldWrite(Result.failure(IllegalStateException()))
        assertEquals(PhraseState(loading = true), controller.state)
        controller.draft("Second")
        controller.save()
        oldWrite(Result.success(Unit))
        assertTrue(controller.state.writing)
        assertEquals("Second", controller.state.draft)
        assertEquals("second", gateway.writes.last()[1])
    }

    @Test fun logoutClearsPrivateStateAndRejectsCallbacksAndWrites() {
        gateway.emit(listOf(phrase))
        controller.edit(phrase)
        controller.save()
        controller.bind(null)
        assertTrue(gateway.listeners.single().cancelled)
        gateway.emit(listOf(phrase))
        gateway.complete(Result.success(Unit))
        controller.draft("Ignored")
        controller.save()
        assertEquals(PhraseState(), controller.state)
        assertEquals(1, gateway.writes.size)
    }

    @Test fun retryReplacesListenerAndIgnoresOldListener() {
        gateway.listeners[0].changed(Result.failure(IllegalStateException()))
        assertNotNull(controller.state.listError)
        controller.retry()
        assertTrue(gateway.listeners[0].cancelled)
        gateway.emit(listOf(phrase), 0)
        assertTrue(controller.state.loading)
        gateway.emit(listOf(phrase), 1)
        assertNull(controller.state.listError)
        assertEquals(listOf(phrase), controller.state.phrases)
        controller.bind("owner")
        assertEquals(2, gateway.listeners.size)
    }

    @Test fun closeCancelsAndCannotRestart() {
        controller.close()
        controller.bind("second")
        controller.retry()
        gateway.emit(listOf(phrase))
        assertTrue(gateway.listeners.single().cancelled)
        assertEquals(PhraseState(), controller.state)
    }

    private class FakeGateway : PhraseGateway {
        data class Listener(val uid: String, val changed: (Result<List<PersonalPhrase>>) -> Unit,
            var cancelled: Boolean = false)
        val listeners = mutableListOf<Listener>()
        val writes = mutableListOf<List<String>>()
        lateinit var complete: (Result<Unit>) -> Unit
        override fun listen(uid: String, changed: (Result<List<PersonalPhrase>>) -> Unit): PhraseSubscription {
            val listener = Listener(uid, changed)
            listeners += listener
            return PhraseSubscription { listener.cancelled = true }
        }
        fun emit(phrases: List<PersonalPhrase>, index: Int = listeners.lastIndex) {
            listeners[index].changed(Result.success(phrases))
        }
        override fun create(uid: String, text: String, done: (Result<Unit>) -> Unit) {
            writes += listOf("create", uid, text)
            complete = done
        }
        override fun update(uid: String, id: String, text: String, done: (Result<Unit>) -> Unit) {
            writes += listOf("update", uid, id, text)
            complete = done
        }
        override fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit) {
            writes += listOf("delete", uid, id)
            complete = done
        }
    }
}
