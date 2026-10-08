package com.Exp1_S2.Accesibilidad.communication

data class CommunicationMessage(val id: String, val text: String, val createdAt: Long)
fun interface HistorySubscription { fun cancel() }

interface CommunicationHistoryGateway {
    fun listen(uid: String, changed: (Result<List<CommunicationMessage>>) -> Unit): HistorySubscription
    fun create(uid: String, text: String, done: (Result<Unit>) -> Unit)
    fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit)
}

data class CommunicationHistoryState(
    val messages: List<CommunicationMessage> = emptyList(),
    val loading: Boolean = false,
    val writing: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class CommunicationHistory(private val gateway: CommunicationHistoryGateway) {
    var state = CommunicationHistoryState()
        private set
    var onChange: ((CommunicationHistoryState) -> Unit)? = null
    private var uid: String? = null
    private var generation = 0
    private var listenerGeneration = 0
    private var subscription: HistorySubscription? = null
    private var closed = false

    private fun publish(next: CommunicationHistoryState) {
        state = next
        onChange?.invoke(next)
    }

    fun bind(owner: String?) {
        if (closed || uid == owner) return
        generation++
        listenerGeneration++
        subscription?.cancel()
        subscription = null
        uid = owner?.takeIf(String::isNotBlank)
        publish(CommunicationHistoryState())
        if (uid != null) retry()
    }

    fun retry() {
        val owner = uid ?: return
        if (closed) return
        val operation = generation
        val listener = ++listenerGeneration
        subscription?.cancel()
        publish(state.copy(loading = true, error = null))
        subscription = gateway.listen(owner) { result ->
            if (active(owner, operation) && listener == listenerGeneration) {
                publish(result.fold(
                    onSuccess = { state.copy(messages = it, loading = false, error = null) },
                    onFailure = { state.copy(loading = false, error = "No se pudo cargar el historial. Reintenta para conectar de nuevo.") }
                ))
            }
        }
    }

    fun save(text: String) {
        val owner = uid ?: return
        if (closed || state.writing) return
        val message = text.trim()
        if (message.isEmpty() || message.length > MAX_MESSAGE_LENGTH) {
            publish(state.copy(error = "El mensaje debe tener entre 1 y $MAX_MESSAGE_LENGTH caracteres.", notice = null))
            return
        }
        mutate(owner, "No se pudo guardar el mensaje. Inténtalo de nuevo.") { done -> gateway.create(owner, message, done) }
    }

    fun delete(id: String) {
        val owner = uid ?: return
        if (closed || state.writing || state.messages.none { it.id == id }) return
        mutate(owner, "No se pudo eliminar el mensaje. Inténtalo de nuevo.") { done -> gateway.delete(owner, id, done) }
    }

    private fun mutate(owner: String, failure: String, write: ((Result<Unit>) -> Unit) -> Unit) {
        val operation = generation
        publish(state.copy(writing = true, error = null, notice = null))
        write { result ->
            if (active(owner, operation)) publish(result.fold(
                onSuccess = { state.copy(writing = false, notice = "Cambio guardado.") },
                onFailure = { state.copy(writing = false, error = failure) }
            ))
        }
    }

    private fun active(owner: String, operation: Int) = !closed && uid == owner && generation == operation

    fun close() {
        bind(null)
        closed = true
        onChange = null
    }

    companion object { const val MAX_MESSAGE_LENGTH = 500 }
}
