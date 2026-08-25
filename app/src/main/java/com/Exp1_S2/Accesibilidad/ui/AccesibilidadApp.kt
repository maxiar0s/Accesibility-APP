package com.Exp1_S2.Accesibilidad.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.Exp1_S2.Accesibilidad.User
import com.Exp1_S2.Accesibilidad.ui.registration.RegistrationScreen

@Composable
fun AccesibilidadApp() {
    val users = remember { arrayOfNulls<User>(5) }
    var userVersion by remember { mutableIntStateOf(0) }
    val registeredUsers = remember(userVersion) { users.filterNotNull() }

    Surface(modifier = Modifier.fillMaxSize()) {
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
            }
        )
    }
}
