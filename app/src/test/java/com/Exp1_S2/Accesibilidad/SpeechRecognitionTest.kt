package com.Exp1_S2.Accesibilidad

import com.Exp1_S2.Accesibilidad.speech.normalizeSpeechResult
import com.Exp1_S2.Accesibilidad.communication.CommunicationHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechRecognitionTest {
    @Test fun selectsFirstNonBlankAlternativeAndTrimsIt() {
        assertEquals("Hola", normalizeSpeechResult(listOf("  ", " Hola ", "Adiós")))
    }

    @Test fun returnsNullWhenNoAlternativeIsUsable() {
        assertNull(normalizeSpeechResult(null))
        assertNull(normalizeSpeechResult(listOf(" ", "\n")))
    }

    @Test fun rejectsTranscriptOverHistoryLimit() {
        assertNull(normalizeSpeechResult(listOf("x".repeat(CommunicationHistory.MAX_MESSAGE_LENGTH + 1))))
        assertEquals("x".repeat(CommunicationHistory.MAX_MESSAGE_LENGTH),
            normalizeSpeechResult(listOf("x".repeat(CommunicationHistory.MAX_MESSAGE_LENGTH))))
    }
}
