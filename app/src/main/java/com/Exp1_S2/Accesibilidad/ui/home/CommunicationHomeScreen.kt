package com.Exp1_S2.Accesibilidad.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.Exp1_S2.Accesibilidad.User
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.Exp1_S2.Accesibilidad.speech.AndroidSpeechEngine
import com.Exp1_S2.Accesibilidad.speech.SpeechController
import com.Exp1_S2.Accesibilidad.speech.SpeechState
import com.Exp1_S2.Accesibilidad.ui.registration.UserSummary
import com.Exp1_S2.Accesibilidad.communication.CommunicationHistory

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
    modifier: Modifier = Modifier,
    phrases: com.Exp1_S2.Accesibilidad.phrases.PersonalPhrases? = null,
    user: User? = null,
    speechFactory: (() -> SpeechController)? = null,
    history: CommunicationHistory? = null
) {
    val context = LocalContext.current.applicationContext
    val historyController = history
    var historyState by remember(historyController) {
        mutableStateOf(historyController?.state ?: com.Exp1_S2.Accesibilidad.communication.CommunicationHistoryState())
    }
    DisposableEffect(historyController, user?.uid) {
        historyController?.let { controller ->
            controller.onChange = { historyState = it }
            controller.bind(user?.uid)
            historyState = controller.state
        }
        onDispose {
            historyController?.let { controller ->
                controller.onChange = null
                controller.bind(null)
            }
        }
    }
    val speech = remember(user?.uid, speechFactory) {
        speechFactory?.invoke() ?: SpeechController(AndroidSpeechEngine(context))
    }
    var speechState by remember(speech) { mutableStateOf(speech.state) }
    DisposableEffect(speech) {
        speech.onChange = { speechState = it }
        speechState = speech.state
        onDispose { speech.close() }
    }
    var profileExpanded by remember(user?.uid) { mutableStateOf(false) }
    var messageDraft by remember { mutableStateOf("") }
    var displayedMessage by remember { mutableStateOf<String?>(null) }
    var showMessageEditor by remember { mutableStateOf(false) }
    var showQuickPhrases by remember { mutableStateOf(false) }
    var visualNoticeEnabled by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    fun display(message: String) {
        if (message != displayedMessage) speech.stop()
        displayedMessage = message
    }

    fun communicate(message: String) {
        historyController?.save(message)
        display(message)
    }

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
            user?.let { currentUser ->
                TextButton(
                    onClick = { profileExpanded = !profileExpanded },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                        stateDescription = if (profileExpanded) "Expandido" else "Contraído"
                    }
                ) {
                    Text(if (profileExpanded) "Ocultar perfil" else "Mostrar perfil")
                }
                if (profileExpanded) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) { UserSummary(currentUser) }
                    }
                }
            }
            Text(
                text = "Elige una forma de expresar lo que necesitas.",
                style = MaterialTheme.typography.bodyLarge
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                homeActions.forEach { action ->
                    HomeActionCard(
                        label = action,
                        onClick = when (action) {
                            "Escribir mensaje" -> ({
                                showMessageEditor = true
                                showQuickPhrases = false
                                feedback = "Escribe tu mensaje y luego selecciona Enviar mensaje."
                            })
                            "Frases rápidas" -> ({
                                showQuickPhrases = !showQuickPhrases
                                showMessageEditor = false
                                feedback = if (showQuickPhrases) {
                                    "Elige una frase rápida para mostrarla."
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
                            else -> ({ feedback = "Acción no disponible." })
                        }
                    )
                }
            }

            if (showMessageEditor) {
                OutlinedTextField(
                    value = messageDraft,
                    onValueChange = { messageDraft = it.take(CommunicationHistory.MAX_MESSAGE_LENGTH) },
                    label = { Text("Mensaje") },
                    supportingText = { Text("${messageDraft.length}/${CommunicationHistory.MAX_MESSAGE_LENGTH} caracteres") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    minLines = 3
                )
                Button(
                    onClick = {
                        if (messageDraft.isBlank()) {
                            feedback = "Escribe un mensaje antes de enviarlo."
                        } else {
                            val message = messageDraft.trim()
                            communicate(message)
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
                                communicate(phrase)
                                feedback = "Frase rápida seleccionada."
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            Text(phrase)
                        }
                    }
                }
            }

            phrases?.let { controller ->
                PersonalPhrasesSection(controller, ::communicate)
            }

            if (user != null && historyController != null) {
                CommunicationHistorySection(historyState, historyController, ::display)
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
                        Button(
                            onClick = { speech.speak(message) },
                            enabled = speechState == SpeechState.READY || speechState == SpeechState.ERROR,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) { Text("Leer en voz alta") }
                        Button(
                            onClick = speech::stop,
                            enabled = speechState == SpeechState.SPEAKING,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) { Text("Detener voz") }
                    }
                }
            }

            Text(
                speechState.message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )

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
    "Aviso visual"
)

@Composable
private fun HomeActionCard(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
