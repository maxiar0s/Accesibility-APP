package com.Exp1_S2.Accesibilidad.phrases

data class PersonalPhrase(val id: String, val text: String)
fun interface PhraseSubscription { fun cancel() }

/** Callbacks run on the UI thread; only write completion confirms persistence. */
interface PhraseGateway {
    fun listen(uid: String, changed: (Result<List<PersonalPhrase>>) -> Unit): PhraseSubscription
    fun create(uid: String, text: String, done: (Result<Unit>) -> Unit)
    fun update(uid: String, id: String, text: String, done: (Result<Unit>) -> Unit)
    fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit)
}

data class PhraseState(
    val phrases: List<PersonalPhrase> = emptyList(),
    val loading: Boolean = false,
    val writing: Boolean = false,
    val draft: String = "",
    val editingId: String? = null,
    val listError: String? = null,
    val writeError: String? = null
)

class PersonalPhrases(private val gateway: PhraseGateway) {
    var state = PhraseState()
        private set
    var onChange: ((PhraseState) -> Unit)? = null
    private var uid: String? = null
    private var generation = 0
    private var listenGeneration = 0
    private var subscription: PhraseSubscription? = null
    private var closed = false

    private fun publish(next: PhraseState) {
        state = next
        onChange?.invoke(next)
    }

    fun bind(owner: String?) {
        if (closed || uid == owner) return
        generation++
        listenGeneration++
        subscription?.cancel()
        subscription = null
        uid = owner?.takeIf { it.isNotBlank() }
        publish(PhraseState())
        if (uid != null) retry()
    }

    fun retry() {
        val owner = uid ?: return
        if (closed) return
        val operation = generation
        val listener = ++listenGeneration
        subscription?.cancel()
        publish(state.copy(loading = true, listError = null))
        subscription = gateway.listen(owner) { result ->
            if (active(owner, operation) && listener == listenGeneration) {
                publish(result.fold(
                    onSuccess = { state.copy(phrases = it, loading = false, listError = null) },
                    onFailure = { state.copy(loading = false, listError = "No se pudieron cargar las frases personales. Reintenta para conectar de nuevo.") }
                ))
            }
        }
    }

    fun draft(text: String) {
        if (!closed && uid != null && !state.writing) publish(state.copy(draft = text, writeError = null))
    }

    fun edit(phrase: PersonalPhrase) {
        if (!closed && !state.writing && state.phrases.any { it.id == phrase.id }) {
            publish(state.copy(draft = phrase.text, editingId = phrase.id, writeError = null))
        }
    }

    fun cancelEdit() {
        if (!closed && !state.writing) publish(state.copy(draft = "", editingId = null, writeError = null))
    }

    fun save() {
        if (closed || uid == null || state.writing) return
        val text = state.draft.trim()
        if (text.isEmpty() || text.length > 500) {
            publish(state.copy(writeError = "Introduce una frase de 1 a 500 caracteres."))
            return
        }
        val id = state.editingId
        mutate(clearEditor = true) { owner, done ->
            if (id == null) gateway.create(owner, text, done) else gateway.update(owner, id, text, done)
        }
    }

    fun delete(id: String) {
        if (state.phrases.none { it.id == id }) return
        mutate(clearEditor = state.editingId == id) { owner, done -> gateway.delete(owner, id, done) }
    }

    private fun mutate(clearEditor: Boolean, write: (String, (Result<Unit>) -> Unit) -> Unit) {
        val owner = uid ?: return
        if (closed || state.writing) return
        val operation = generation
        publish(state.copy(writing = true, writeError = null))
        write(owner) { result ->
            if (active(owner, operation)) publish(result.fold(
                onSuccess = { state.copy(writing = false, draft = if (clearEditor) "" else state.draft,
                    editingId = if (clearEditor) null else state.editingId, writeError = null) },
                onFailure = { state.copy(writing = false, writeError = "No se pudo guardar el cambio. Inténtalo de nuevo.") }
            ))
        }
    }

    private fun active(owner: String, operation: Int) = !closed && uid == owner && generation == operation

    fun close() {
        bind(null)
        closed = true
        onChange = null
    }
}
