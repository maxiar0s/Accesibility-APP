package com.Exp1_S2.Accesibilidad.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Exp1_S2.Accesibilidad.User
import com.Exp1_S2.Accesibilidad.authenticateUser
import com.Exp1_S2.Accesibilidad.ui.login.LoginScreen
import com.Exp1_S2.Accesibilidad.ui.recovery.PasswordRecoveryScreen
import com.Exp1_S2.Accesibilidad.ui.registration.RegistrationScreen

private object AppRoute {
    const val LOGIN = "login"
    const val REGISTRATION = "registration"
    const val RECOVERY = "recovery"
    const val SUCCESS = "success"
}

@Composable
fun AccesibilidadApp() {
    val users = remember { arrayOfNulls<User>(5) }
    var userVersion by remember { mutableIntStateOf(0) }
    val registeredUsers = remember(userVersion) { users.filterNotNull() }
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = AppRoute.LOGIN) {
            composable(AppRoute.LOGIN) {
                LoginScreen(
                    onLogin = { email, password -> authenticateUser(users, email, password) },
                    onNavigateToRegistration = {
                        navController.navigate(AppRoute.REGISTRATION) { launchSingleTop = true }
                    },
                    onNavigateToRecovery = {
                        navController.navigate(AppRoute.RECOVERY) { launchSingleTop = true }
                    },
                    onLoginSuccess = {
                        navController.navigate(AppRoute.SUCCESS) {
                            popUpTo(AppRoute.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoute.REGISTRATION) {
                RegistrationScreen(
                    users = registeredUsers,
                    onRegister = { user ->
                        val availableIndex = users.indexOfFirst { it == null }
                        if (availableIndex == -1) {
                            false
                        } else {
                            users[availableIndex] = user
                            userVersion++
                            true
                        }
                    },
                    onRegistrationSuccess = {
                        navController.navigate(AppRoute.LOGIN) {
                            popUpTo(AppRoute.LOGIN) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoute.RECOVERY) {
                PasswordRecoveryScreen(
                    onReturnToLogin = { navController.popBackStack() }
                )
            }
            composable(AppRoute.SUCCESS) {
                TemporarySuccessScreen()
            }
        }
    }
}

@Composable
private fun TemporarySuccessScreen() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text("Inicio de sesión exitoso")
        androidx.compose.material3.Text("La pantalla principal de comunicación estará disponible próximamente.")
    }
}
