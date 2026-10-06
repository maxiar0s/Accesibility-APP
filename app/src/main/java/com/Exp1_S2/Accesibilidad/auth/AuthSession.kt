package com.Exp1_S2.Accesibilidad.auth

import com.Exp1_S2.Accesibilidad.User

data class AuthIdentity(val uid: String, val email: String, val name: String?)
data class RegisteredAccount(val identity: AuthIdentity, val profileWarning: Boolean = false)

/** Callbacks run on the UI thread. Credentials are transient operation arguments only. */
interface AuthGateway {
    val currentIdentity: AuthIdentity?
    fun login(email: String, password: String, done: (Result<AuthIdentity>) -> Unit)
    /** Successful registration completes signed out, including display-name warning outcomes. */
    fun register(
        name: String, email: String, password: String,
        onCreated: (AuthIdentity) -> Unit, done: (Result<RegisteredAccount>) -> Unit
    )
    fun resetPassword(email: String, done: (Result<Unit>) -> Unit)
    fun signOut()
}

interface ProfileStore {
    fun profile(uid: String): User?
    fun saveProfile(user: User)
    fun saveCurrent(user: User)
    fun clearCurrent()
}

enum class AuthRoute { LOGIN, REGISTRATION, RECOVERY, HOME }
data class AuthState(
    val route: AuthRoute = AuthRoute.LOGIN,
    val user: User? = null,
    val busy: Boolean = false,
    val message: String? = null
)

/** Lifecycle-owned orchestration: no Android/Firebase construction or stored credentials. */
class AuthSession(private val auth: AuthGateway, private val profiles: ProfileStore) {
    var state = AuthState()
        private set
    var onChange: ((AuthState) -> Unit)? = null
    private var closed = false
    private var generation = 0

    init {
        val identity = auth.currentIdentity
        if (identity == null) profiles.clearCurrent() else accept(identity)
    }

    private fun publish(next: AuthState) {
        if (closed) return
        state = next
        onChange?.invoke(next)
    }

    private fun accept(identity: AuthIdentity) {
        val local = profiles.profile(identity.uid)?.takeIf { it.uid == identity.uid }
        val user = (local ?: User("", identity.email)).copy(
            uid = identity.uid,
            email = identity.email,
            name = identity.name?.takeIf { it.isNotBlank() } ?: local?.name.orEmpty()
        )
        profiles.saveProfile(user)
        profiles.saveCurrent(user)
        publish(AuthState(route = AuthRoute.HOME, user = user))
    }

    fun navigate(route: AuthRoute) {
        if (!closed && !state.busy && state.user == null && route != AuthRoute.HOME) {
            publish(AuthState(route = route))
        }
    }

    private fun begin(): Int? {
        if (closed || state.busy || state.user != null) return null
        publish(state.copy(busy = true, message = null))
        return ++generation
    }

    private fun active(operation: Int) = !closed && operation == generation

    fun login(email: String, password: String) {
        if (closed || state.busy) return
        if (!validEmail(email) || password.isEmpty()) {
            publish(state.copy(message = "Introduce un correo electrónico válido y tu contraseña."))
            return
        }
        val operation = begin() ?: return
        auth.login(email.trim(), password) { result ->
            if (active(operation)) result.fold(
                onSuccess = { identity -> accept(identity) },
                onFailure = { publish(state.copy(busy = false, message = it.message)) }
            )
        }
    }

    fun register(profile: User, password: String) {
        if (closed || state.busy) return
        val validationMessage = validateRegistration(profile.name, profile.email, password)
        if (validationMessage != null) {
            publish(state.copy(message = validationMessage))
            return
        }
        val operation = begin() ?: return
        auth.register(profile.name.trim(), profile.email.trim(), password, onCreated = { identity ->
            if (active(operation)) {
                profiles.saveProfile(profile.copy(
                    uid = identity.uid, email = identity.email, name = profile.name.trim()
                ))
                profiles.clearCurrent()
            }
        }) { result ->
            if (active(operation)) result.fold(
                onSuccess = { account ->
                    publish(AuthState(message = if (account.profileWarning) {
                        "Cuenta creada. Tu nombre se guardó en este dispositivo, pero no se pudo actualizar en línea. Inicia sesión para continuar."
                    } else "Cuenta creada. Inicia sesión para continuar."))
                },
                onFailure = { publish(state.copy(busy = false, message = it.message)) }
            )
        }
    }

    fun resetPassword(email: String) {
        if (closed || state.busy) return
        if (!validEmail(email)) {
            publish(state.copy(message = "Introduce un correo electrónico válido."))
            return
        }
        val operation = begin() ?: return
        auth.resetPassword(email.trim()) { result ->
            if (active(operation)) publish(state.copy(busy = false, message = result.fold(
                onSuccess = { "Si existe una cuenta con este correo, se enviarán instrucciones para recuperar la contraseña." },
                onFailure = { it.message }
            )))
        }
    }

    fun logout() {
        if (closed) return
        generation++
        auth.signOut()
        profiles.clearCurrent()
        publish(AuthState())
    }

    fun close() {
        closed = true
        generation++
        onChange = null
    }
}

fun validEmail(email: String): Boolean =
    email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
