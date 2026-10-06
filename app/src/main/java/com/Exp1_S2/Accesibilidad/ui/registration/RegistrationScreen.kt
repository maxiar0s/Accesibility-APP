package com.Exp1_S2.Accesibilidad.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.Exp1_S2.Accesibilidad.AccessibilityPreference
import com.Exp1_S2.Accesibilidad.CommunicationMode
import com.Exp1_S2.Accesibilidad.CommunicationPreference
import com.Exp1_S2.Accesibilidad.User
import com.Exp1_S2.Accesibilidad.auth.REGISTRATION_PASSWORD_HINT
import com.Exp1_S2.Accesibilidad.auth.validateRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegister: (User, String) -> Unit,
    busy: Boolean,
    message: String?,
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
            text = "Completa tus datos para crear una cuenta. Puedes elegir cómo recibir comunicaciones.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre completo") },
            supportingText = { Text("Al menos 3 caracteres sin espacios iniciales o finales") },
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
            supportingText = { Text(REGISTRATION_PASSWORD_HINT) },
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

        (validationMessage ?: message)?.let { message ->
            Text(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = {
                validationMessage = validateRegistration(name, email, password, confirmPassword)
                if (validationMessage == null) {
                    onRegister(
                        User(
                            name = name.trim(),
                            email = email.trim(),
                            communicationPreference = communicationPreference,
                            primaryCommunicationMode = primaryMode,
                            accessibilityPreferences = accessibilityPreferences
                        ), password
                    )
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(if (busy) "Creando cuenta…" else "Registrar cuenta")
        }
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
fun UserSummary(user: User) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text(
            text = "Perfil actual",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge
        )
        SummaryRow("Nombre", user.name)
        SummaryRow("Correo", user.email)
        SummaryRow("Preferencia", user.communicationPreference.label())
        SummaryRow("Modo de comunicación", user.primaryCommunicationMode.label())
        SummaryRow("Accesibilidad", user.accessibilityPreferences.joinToString { it.label() }.ifEmpty { "Ninguna seleccionada" })
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
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
