package com.Exp1_S2.Accesibilidad.speech

enum class SpeechState(val message: String) {
    INITIALIZING("Preparando voz en español…"),
    READY("Voz en español lista."),
    SPEAKING("Leyendo mensaje…"),
    ERROR("No se pudo reproducir el mensaje. Intenta nuevamente."),
    UNAVAILABLE("Voz en español no disponible. Revisa el motor y los datos de voz en Ajustes.")
}

interface SpeechEngine {
    fun initialize(ready: (Boolean) -> Unit, progress: (String, SpeechState) -> Unit)
    fun speak(text: String, utteranceId: String): Boolean
    fun stop()
    fun shutdown()
}

class SpeechController(private val engine: SpeechEngine) {
    var state = SpeechState.INITIALIZING
        private set
    var onChange: ((SpeechState) -> Unit)? = null
    private var closed = false
    private var available = false
    private var sequence = 0
    private var activeId: String? = null

    init {
        engine.initialize({ ready ->
            if (!closed) {
                available = ready
                update(if (ready) SpeechState.READY else SpeechState.UNAVAILABLE)
            }
        }, { id, next ->
            if (!closed && id == activeId) {
                if (next != SpeechState.SPEAKING) activeId = null
                update(next)
            }
        })
    }

    fun speak(text: String) {
        if (closed || !available || activeId != null || text.isBlank()) return
        val id = "message-${++sequence}"
        activeId = id
        update(SpeechState.SPEAKING)
        if (!engine.speak(text, id) && activeId == id) {
            activeId = null
            update(SpeechState.ERROR)
        }
    }

    fun stop() {
        if (closed || activeId == null) return
        activeId = null
        engine.stop()
        update(SpeechState.READY)
    }

    fun close() {
        if (closed) return
        closed = true
        activeId = null
        onChange = null
        engine.stop()
        engine.shutdown()
    }

    private fun update(next: SpeechState) {
        state = next
        onChange?.invoke(next)
    }
}
