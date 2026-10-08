package com.Exp1_S2.Accesibilidad.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.Exp1_S2.Accesibilidad.communication.CommunicationHistory
import com.Exp1_S2.Accesibilidad.communication.CommunicationHistoryState
import com.Exp1_S2.Accesibilidad.communication.CommunicationMessage

@Composable
fun CommunicationHistorySection(
    state: CommunicationHistoryState,
    controller: CommunicationHistory,
    onSelect: (String) -> Unit
) {
    var deleting by remember { mutableStateOf<CommunicationMessage?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Historial de mensajes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        Text("Tus mensajes se guardan en la nube en tu cuenta. Puedes eliminar cada mensaje desde esta lista.")
        if (state.loading) Text("Cargando historial…")
        state.error?.let { error ->
            Text(error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = controller::retry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Reintentar carga del historial")
            }
        }
        state.notice?.let { notice ->
            Text(notice, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (!state.loading && state.error == null && state.messages.isEmpty()) {
            Text("Todavía no hay mensajes guardados.")
        }
        state.messages.forEach { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onSelect(message.text) },
                        enabled = !state.writing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) { Text(message.text) }
                    TextButton(
                        onClick = { deleting = message },
                        enabled = !state.writing,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Eliminar mensaje: ${message.text}") }
                }
            }
        }
        if (state.writing) Text("Guardando cambio…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    deleting?.let { message ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar este mensaje del historial?") },
            text = { Text(message.text) },
            confirmButton = {
                TextButton(
                    onClick = { controller.delete(message.id); deleting = null },
                    enabled = !state.writing
                ) { Text("Confirmar eliminación del historial") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancelar eliminación") }
            }
        )
    }
}
