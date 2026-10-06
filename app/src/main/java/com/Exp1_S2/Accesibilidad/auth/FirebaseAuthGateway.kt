package com.Exp1_S2.Accesibilidad.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class FirebaseAuthGateway(private val auth: FirebaseAuth) : AuthGateway {
    override val currentIdentity: AuthIdentity?
        get() = auth.currentUser?.identity()

    override fun login(email: String, password: String, done: (Result<AuthIdentity>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            val user = auth.currentUser
            done(if (task.isSuccessful && user != null) Result.success(user.identity()) else failure(task.exception))
        }
    }

    override fun register(
        name: String, email: String, password: String,
        onCreated: (AuthIdentity) -> Unit, done: (Result<RegisteredAccount>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            val user = if (task.isSuccessful) task.result?.user else null
            if (user == null) {
                done(failure(task.exception))
            } else {
                val identity = user.identity()
                onCreated(identity)
                val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user.updateProfile(request).addOnCompleteListener { update ->
                    // Creation already succeeded. Always return to Login, even when name update fails.
                    auth.signOut()
                    done(Result.success(RegisteredAccount(identity, profileWarning = !update.isSuccessful)))
                }
            }
        }
    }

    override fun resetPassword(email: String, done: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            // Preserve a generic response even when enumeration protection is disabled remotely.
            val missing = (task.exception as? FirebaseAuthInvalidUserException)?.errorCode == "ERROR_USER_NOT_FOUND"
            done(if (task.isSuccessful || missing) Result.success(Unit) else failure(task.exception))
        }
    }

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.identity() = AuthIdentity(uid, email.orEmpty(), displayName)

    private fun <T> failure(error: Exception?): Result<T> {
        val message = when {
            error is FirebaseNetworkException -> "No hay conexión. Comprueba tu red e inténtalo de nuevo."
            error is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera e inténtalo de nuevo."
            error is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_WEAK_PASSWORD" -> "El servicio rechazó la contraseña. $REGISTRATION_PASSWORD_HINT"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado. Inicia sesión o recupera tu contraseña."
                "ERROR_OPERATION_NOT_ALLOWED" -> "El acceso con correo y contraseña no está disponible. Contacta al administrador de la aplicación."
                else -> "No se pudo completar la autenticación. Comprueba tus datos e inténtalo de nuevo."
            }
            else -> "No se pudo completar la solicitud. Inténtalo de nuevo."
        }
        return Result.failure(IllegalStateException(message))
    }
}
