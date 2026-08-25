package com.Exp1_S2.Accesibilidad.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.Exp1_S2.Accesibilidad.AccessibilityPreference
import com.Exp1_S2.Accesibilidad.CommunicationMode
import com.Exp1_S2.Accesibilidad.CommunicationPreference
import com.Exp1_S2.Accesibilidad.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    users: List<User>,
    onRegister: (User) -> Boolean,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var communicationPreference by remember { mutableStateOf(CommunicationPreference.EMAIL) }
    var primaryMode by remember { mutableStateOf(CommunicationMode.WRITTEN) }
    var accessibilityPreferences by remember { mutableStateOf(emptySet<AccessibilityPreference>()) }
    var preferenceExpanded by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var registrationSucceeded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registro accesible",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Completá tus datos para crear una cuenta. Podés elegir cómo preferís recibir comunicaciones.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        ExposedDropdownMenuBox(
            expanded = preferenceExpanded,
            onExpandedChange = { preferenceExpanded = !preferenceExpanded }
        ) {
            OutlinedTextField(
                value = communicationPreference.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferencia de comunicación") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = preferenceExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = preferenceExpanded,
                onDismissRequest = { preferenceExpanded = false }
            ) {
                CommunicationPreference.entries.forEach { preference ->
                    DropdownMenuItem(
                        text = { Text(preference.label()) },
                        onClick = {
                            communicationPreference = preference
                            preferenceExpanded = false
                        }
                    )
                }
            }
        }

        SelectionSection(title = "Modo principal de comunicación") {
            CommunicationMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = primaryMode == mode,
                        onClick = { primaryMode = mode },
                        modifier = Modifier.size(48.dp)
                    )
                    Text(text = mode.label(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        SelectionSection(title = "Preferencias de accesibilidad") {
            AccessibilityPreference.entries.forEach { preference ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = preference in accessibilityPreferences,
                        onCheckedChange = { checked ->
                            accessibilityPreferences = if (checked) {
                                accessibilityPreferences + preference
                            } else {
                                accessibilityPreferences - preference
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    Text(text = preference.label(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        validationMessage?.let { message ->
            Text(
                text = message,
                color = if (registrationSucceeded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = {
                validationMessage = validateRegistration(name, email, password, confirmPassword)
                registrationSucceeded = false
                if (validationMessage == null) {
                    val registered = onRegister(
                        User(
                            name = name.trim(),
                            email = email.trim(),
                            password = password,
                            communicationPreference = communicationPreference,
                            primaryCommunicationMode = primaryMode,
                            accessibilityPreferences = accessibilityPreferences
                        )
                    )
                    registrationSucceeded = registered
                    validationMessage = if (registered) {
                        name = ""
                        email = ""
                        password = ""
                        confirmPassword = ""
                        "Cuenta registrada correctamente."
                    } else {
                        "No se pueden registrar más cuentas: se alcanzó el límite de cinco."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Registrar cuenta")
        }

        UserSummary(users = users)
    }
}

@Composable
private fun SelectionSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun UserSummary(users: List<User>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text(
            text = "Cuentas registradas (${users.size} de 5)",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge
        )
        if (users.isEmpty()) {
            Text(
                text = "Todavía no hay cuentas registradas. Completá el formulario para agregar la primera.",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryCell("Nombre", Modifier.weight(1f), isHeader = true)
                SummaryCell("Correo", Modifier.weight(1f), isHeader = true)
                SummaryCell("Preferencia", Modifier.weight(1f), isHeader = true)
            }
            users.forEach { user ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCell(user.name, Modifier.weight(1f))
                    SummaryCell(user.email, Modifier.weight(1f))
                    SummaryCell(user.communicationPreference.label(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(text: String, modifier: Modifier, isHeader: Boolean = false) {
    Text(
        text = text,
        modifier = modifier.padding(4.dp),
        style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    )
}

private fun validateRegistration(
    name: String,
    email: String,
    password: String,
    confirmPassword: String
): String? = when {
    name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
        "Completá todos los campos obligatorios."
    !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ->
        "Ingresá un correo electrónico válido."
    password != confirmPassword -> "Las contraseñas no coinciden."
    else -> null
}

private fun CommunicationPreference.label(): String = when (this) {
    CommunicationPreference.EMAIL -> "Correo electrónico"
    CommunicationPreference.PHONE -> "Llamada telefónica"
    CommunicationPreference.TEXT_MESSAGE -> "Mensaje de texto"
}

private fun CommunicationMode.label(): String = when (this) {
    CommunicationMode.WRITTEN -> "Escrita"
    CommunicationMode.VOICE -> "Voz"
    CommunicationMode.VISUAL -> "Visual"
}

private fun AccessibilityPreference.label(): String = when (this) {
    AccessibilityPreference.HIGH_CONTRAST -> "Alto contraste"
    AccessibilityPreference.VISUAL_ALERTS -> "Alertas visuales"
    AccessibilityPreference.VIBRATION -> "Vibración"
}
