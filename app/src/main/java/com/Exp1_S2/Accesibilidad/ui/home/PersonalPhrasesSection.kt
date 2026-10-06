package com.Exp1_S2.Accesibilidad.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.Exp1_S2.Accesibilidad.phrases.PersonalPhrase
import com.Exp1_S2.Accesibilidad.phrases.PersonalPhrases

@Composable
fun PersonalPhrasesSection(controller: PersonalPhrases, onSelect: (String) -> Unit) {
    var state by remember(controller) { mutableStateOf(controller.state) }
    var deleting by remember { mutableStateOf<PersonalPhrase?>(null) }
    DisposableEffect(controller) {
        controller.onChange = { state = it }
        state = controller.state
        onDispose { controller.onChange = null }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Frases personales", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        if (state.loading) Text("Cargando frases personales…")
        state.listError?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = controller::retry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar carga") }
        }
        if (!state.loading && state.listError == null && state.phrases.isEmpty()) Text("Todavía no hay frases personales.")
        state.phrases.forEach { phrase ->
            Button(onClick = { onSelect(phrase.text) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(phrase.text)
            }
            TextButton(onClick = { controller.edit(phrase) }, enabled = !state.writing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Editar frase: ${phrase.text}")
            }
            TextButton(onClick = { deleting = phrase }, enabled = !state.writing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Eliminar frase: ${phrase.text}")
            }
        }
        OutlinedTextField(value = state.draft, onValueChange = controller::draft,
            label = { Text("Texto de la frase personal") }, supportingText = { Text("De 1 a 500 caracteres") },
            enabled = !state.writing, modifier = Modifier.fillMaxWidth())
        state.writeError?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Button(onClick = controller::save, enabled = !state.writing,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (state.writing) "Guardando frase…" else if (state.editingId == null) "Añadir frase" else "Guardar frase")
        }
        if (state.editingId != null) TextButton(onClick = controller::cancelEdit, enabled = !state.writing) {
            Text("Cancelar edición")
        }
    }
    deleting?.let { phrase ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("¿Eliminar la frase personal?") },
            text = { Text(phrase.text) }, confirmButton = {
                TextButton(onClick = { controller.delete(phrase.id); deleting = null }, enabled = !state.writing) {
                    Text("Confirmar eliminación")
                }
            }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar eliminación") } })
    }
}
