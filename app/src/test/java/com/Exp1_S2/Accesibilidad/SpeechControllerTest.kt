package com.Exp1_S2.Accesibilidad

import com.Exp1_S2.Accesibilidad.speech.*
import org.junit.Assert.*
import org.junit.Test

class SpeechControllerTest {
    private class FakeEngine : SpeechEngine {
        lateinit var ready: (Boolean) -> Unit
        lateinit var progress: (String, SpeechState) -> Unit
        val requests = mutableListOf<Pair<String, String>>()
        var accepted = true
        var stops = 0
        var shutdowns = 0
        override fun initialize(ready: (Boolean) -> Unit, progress: (String, SpeechState) -> Unit) {
            this.ready = ready
            this.progress = progress
        }
        override fun speak(text: String, utteranceId: String): Boolean {
            requests += text to utteranceId
            return accepted
        }
        override fun stop() { stops++ }
        override fun shutdown() { shutdowns++ }
    }

    @Test fun initializationAndUnavailableBlockSpeech() {
        val engine = FakeEngine()
        val speech = SpeechController(engine)
        assertEquals(SpeechState.INITIALIZING, speech.state)
        speech.speak("Hola")
        engine.ready(false)
        assertEquals(SpeechState.UNAVAILABLE, speech.state)
        speech.speak("Hola")
        assertTrue(engine.requests.isEmpty())
    }

    @Test fun readyForwardsExactMessageAndBlocksBlankAndDuplicate() {
        val engine = FakeEngine()
        val speech = SpeechController(engine)
        engine.ready(true)
        assertEquals(SpeechState.READY, speech.state)
        speech.speak("  ")
        speech.speak("¡Necesito ayuda! ")
        speech.speak("Duplicado")
        assertEquals(listOf("¡Necesito ayuda! "), engine.requests.map { it.first })
        assertEquals(SpeechState.SPEAKING, speech.state)
        engine.progress(engine.requests.single().second, SpeechState.READY)
        assertEquals(SpeechState.READY, speech.state)
    }

    @Test fun immediateAndAsynchronousErrorsAllowRetry() {
        val engine = FakeEngine()
        val speech = SpeechController(engine)
        engine.ready(true)
        engine.accepted = false
        speech.speak("Hola")
        assertEquals(SpeechState.ERROR, speech.state)
        engine.accepted = true
        speech.speak("Frase personal")
        engine.progress(engine.requests.last().second, SpeechState.ERROR)
        assertEquals(SpeechState.ERROR, speech.state)
        speech.speak("Frase rápida")
        assertEquals(3, engine.requests.size)
    }

    @Test fun stoppedOrCompletedCallbacksCannotChangeNewPlayback() {
        val engine = FakeEngine()
        val speech = SpeechController(engine)
        engine.ready(true)
        speech.speak("Primero")
        val oldId = engine.requests.last().second
        speech.stop()
        assertEquals(1, engine.stops)
        assertEquals(SpeechState.READY, speech.state)
        engine.progress(oldId, SpeechState.SPEAKING)
        assertEquals(SpeechState.READY, speech.state)
        speech.speak("Segundo")
        engine.progress(oldId, SpeechState.READY)
        assertEquals(SpeechState.SPEAKING, speech.state)
        val id = engine.requests.last().second
        engine.progress(id, SpeechState.SPEAKING)
        engine.progress(id, SpeechState.READY)
        engine.progress(id, SpeechState.ERROR)
        assertEquals(SpeechState.READY, speech.state)
    }

    @Test fun closeStopsAndShutsDownOnceIgnoringLateCallbacks() {
        val engine = FakeEngine()
        val speech = SpeechController(engine)
        engine.ready(true)
        speech.speak("Hola")
        val id = engine.requests.last().second
        speech.close()
        speech.close()
        engine.progress(id, SpeechState.READY)
        speech.speak("No")
        assertEquals(1, engine.stops)
        assertEquals(1, engine.shutdowns)
        assertEquals(1, engine.requests.size)
        assertNull(speech.onChange)
    }

    @Test fun initializationAfterCloseDoesNotNotifyOrEnableSpeech() {
        val engine = FakeEngine()
        val speech = SpeechController(engine)
        var notifications = 0
        speech.onChange = { notifications++ }
        speech.close()
        engine.ready(true)
        speech.speak("Hola")
        assertEquals(SpeechState.INITIALIZING, speech.state)
        assertEquals(0, notifications)
        assertTrue(engine.requests.isEmpty())
    }
}
