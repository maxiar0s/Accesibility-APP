package com.Exp1_S2.Accesibilidad.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Exp1_S2.Accesibilidad.auth.AuthRoute
import com.Exp1_S2.Accesibilidad.auth.AuthSession
import com.Exp1_S2.Accesibilidad.auth.AuthSessionViewModel
import com.Exp1_S2.Accesibilidad.phrases.PersonalPhrases
import com.Exp1_S2.Accesibilidad.ui.home.CommunicationHomeScreen
import com.Exp1_S2.Accesibilidad.ui.login.LoginScreen
import com.Exp1_S2.Accesibilidad.ui.recovery.PasswordRecoveryScreen
import com.Exp1_S2.Accesibilidad.ui.registration.RegistrationScreen

@Composable
fun AccesibilidadApp(session: AuthSession? = null, phrases: PersonalPhrases? = null) {
    val owner = if (session == null) viewModel<AuthSessionViewModel>() else null
    val controller = session ?: requireNotNull(owner).session
    val phraseController = phrases ?: requireNotNull(owner) { "Inject phrases with a fake session." }.phrases
    var state by remember(controller) { mutableStateOf(controller.state) }
    DisposableEffect(controller, phraseController) {
        controller.onChange = {
            phraseController.bind(it.user?.uid)
            state = it
        }
        phraseController.bind(controller.state.user?.uid)
        state = controller.state
        onDispose {
            controller.onChange = null
            if (session != null) controller.close()
            if (phrases != null) phraseController.close()
        }
    }
    BackHandler(state.route == AuthRoute.REGISTRATION || state.route == AuthRoute.RECOVERY) {
        controller.navigate(AuthRoute.LOGIN)
    }

    // There is no protected back stack: only an authenticated state can render Home.
    Surface(modifier = Modifier.fillMaxSize()) {
        when (state.route) {
            AuthRoute.LOGIN -> LoginScreen(
                onLogin = controller::login,
                busy = state.busy,
                message = state.message,
                onNavigateToRegistration = { controller.navigate(AuthRoute.REGISTRATION) },
                onNavigateToRecovery = { controller.navigate(AuthRoute.RECOVERY) }
            )
            AuthRoute.REGISTRATION -> RegistrationScreen(
                onRegister = controller::register, busy = state.busy, message = state.message
            )
            AuthRoute.RECOVERY -> PasswordRecoveryScreen(
                onResetPassword = controller::resetPassword,
                busy = state.busy,
                message = state.message,
                onReturnToLogin = { controller.navigate(AuthRoute.LOGIN) }
            )
            AuthRoute.HOME -> state.user?.let { user ->
                key(user.uid) {
                    CommunicationHomeScreen(
                        userName = user.name, onLogout = controller::logout,
                        phrases = phraseController, user = user
                    )
                }
            }
        }
    }
}
