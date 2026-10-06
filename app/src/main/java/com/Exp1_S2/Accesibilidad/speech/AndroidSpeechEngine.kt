package com.Exp1_S2.Accesibilidad.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidSpeechEngine(context: Context) : SpeechEngine {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var closed = false

    override fun initialize(ready: (Boolean) -> Unit, progress: (String, SpeechState) -> Unit) {
        // Post initialization so the constructor assignment completes before using the engine.
        tts = TextToSpeech(context) { status ->
            main.post {
                if (!closed) {
                    val engine = tts
                    val spanish = Locale("es")
                    val supported = status == TextToSpeech.SUCCESS && engine != null &&
                        engine.isLanguageAvailable(spanish) >= TextToSpeech.LANG_AVAILABLE &&
                        engine.setLanguage(spanish) >= TextToSpeech.LANG_AVAILABLE
                    val listening = engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        private fun report(id: String, state: SpeechState) {
                            main.post { if (!closed) progress(id, state) }
                        }
                        override fun onStart(utteranceId: String) = report(utteranceId, SpeechState.SPEAKING)
                        override fun onDone(utteranceId: String) = report(utteranceId, SpeechState.READY)
                        @Deprecated("Required legacy callback")
                        override fun onError(utteranceId: String) = report(utteranceId, SpeechState.ERROR)
                        override fun onError(utteranceId: String, errorCode: Int) = report(utteranceId, SpeechState.ERROR)
                    }) == TextToSpeech.SUCCESS
                    ready(supported && listening)
                }
            }
        }
    }

    override fun speak(text: String, utteranceId: String): Boolean =
        !closed && tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS

    override fun stop() { tts?.stop() }

    override fun shutdown() {
        if (closed) return
        closed = true
        tts?.shutdown()
        tts = null
    }
}
