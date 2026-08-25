package com.Exp1_S2.Accesibilidad.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val quickPhrases = listOf(
    "Necesito ayuda, por favor.",
    "Quiero comunicarme por escrito.",
    "Gracias por tu paciencia."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationHomeScreen(
    userName: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageDraft by remember { mutableStateOf("") }
    var displayedMessage by remember { mutableStateOf<String?>(null) }
    var showMessageEditor by remember { mutableStateOf(false) }
    var showQuickPhrases by remember { mutableStateOf(false) }
    var visualNoticeEnabled by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showMessageEditor) {
        if (showMessageEditor) focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Comunicación") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Cerrar sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (userName.isNullOrBlank()) "Bienvenido/a" else "Bienvenido/a, $userName",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Elegí una forma de expresar lo que necesitás.",
                style = MaterialTheme.typography.bodyLarge
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homeActions) { action ->
                    HomeActionCard(
                        label = action,
                        onClick = when (action) {
                            "Escribir mensaje" -> ({
                                showMessageEditor = true
                                showQuickPhrases = false
                                feedback = "Escribí tu mensaje y luego seleccioná Enviar."
                            })
                            "Frases rápidas" -> ({
                                showQuickPhrases = !showQuickPhrases
                                showMessageEditor = false
                                feedback = if (showQuickPhrases) {
                                    "Elegí una frase rápida para mostrarla."
                                } else {
                                    "Frases rápidas ocultas."
                                }
                            })
                            "Aviso visual" -> ({
                                visualNoticeEnabled = !visualNoticeEnabled
                                feedback = if (visualNoticeEnabled) {
                                    "Aviso visual urgente activado."
                                } else {
                                    "Aviso visual urgente desactivado."
                                }
                            })
                            else -> ({ feedback = "La configuración estará disponible en una próxima versión." })
                        }
                    )
                }
            }

            if (showMessageEditor) {
                OutlinedTextField(
                    value = messageDraft,
                    onValueChange = { messageDraft = it },
                    label = { Text("Mensaje") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    minLines = 3
                )
                Button(
                    onClick = {
                        if (messageDraft.isBlank()) {
                            feedback = "Escribí un mensaje antes de enviarlo."
                        } else {
                            displayedMessage = messageDraft.trim()
                            messageDraft = ""
                            feedback = "Mensaje preparado para comunicar."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text("Enviar mensaje")
                }
            }

            if (showQuickPhrases) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Frases rápidas", style = MaterialTheme.typography.titleLarge)
                    quickPhrases.forEach { phrase ->
                        Button(
                            onClick = {
                                displayedMessage = phrase
                                feedback = "Frase rápida seleccionada."
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            Text(phrase)
                        }
                    }
                }
            }

            if (visualNoticeEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "AVISO VISUAL URGENTE ACTIVADO",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            displayedMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mensaje para comunicar", style = MaterialTheme.typography.titleMedium)
                        Text(message, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            feedback?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private val homeActions = listOf(
    "Escribir mensaje",
    "Frases rápidas",
    "Aviso visual",
    "Configuración"
)

@Composable
private fun HomeActionCard(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 144.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
