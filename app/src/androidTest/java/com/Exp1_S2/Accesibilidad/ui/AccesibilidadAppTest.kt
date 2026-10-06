package com.Exp1_S2.Accesibilidad.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Exp1_S2.Accesibilidad.ui.home.CommunicationHomeScreen
import com.Exp1_S2.Accesibilidad.ui.theme.AccesibilidadTheme
import com.Exp1_S2.Accesibilidad.auth.*
import com.Exp1_S2.Accesibilidad.phrases.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccesibilidadAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appRoot_displaysLoginScreen() {
        val session = fakeSession()
        val phrases = fakePhrases()
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp(session, phrases)
            }
        }

        composeTestRule.onNodeWithText("Crear una cuenta").assertIsDisplayed()
    }

    @Test
    fun login_navigatesToRegistration() {
        val session = fakeSession()
        val phrases = fakePhrases()
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp(session, phrases)
            }
        }

        composeTestRule.onNodeWithText("Crear una cuenta").performClick()

        composeTestRule.onNodeWithText("Registro accesible")
            .assertIsDisplayed()
    }

    @Test
    fun communicationHome_displaysSelectedQuickPhrase() {
        composeTestRule.setContent {
            AccesibilidadTheme {
                CommunicationHomeScreen(userName = "Ana", onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Frases rápidas").performClick()
        composeTestRule.onNodeWithText("Necesito ayuda, por favor.").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Mensaje para comunicar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun login_waitsForFakeSuccessThenLogoutRemovesProtectedContent() {
        val auth = FakeAuth()
        val session = fakeSession(auth)
        val phrases = fakePhrases()
        composeTestRule.setContent { AccesibilidadTheme { AccesibilidadApp(session, phrases) } }
        composeTestRule.onNodeWithText("Correo electrónico").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Contraseña").performTextInput("secret")
        composeTestRule.onNode(hasText("Iniciar sesión") and hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Iniciando sesión…").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Perfil actual").assertDoesNotExist()
        composeTestRule.runOnIdle { auth.loginDone(Result.success(AuthIdentity("uid", "ana@example.com", "Ana"))) }
        composeTestRule.onNodeWithText("Perfil actual").assertDoesNotExist()
        composeTestRule.onNodeWithText("Mostrar perfil").performClick()
        composeTestRule.onNodeWithText("Perfil actual").assertIsDisplayed()
        composeTestRule.onNodeWithText("ana@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ocultar perfil").performClick()
        composeTestRule.onNodeWithText("Perfil actual").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cerrar sesión").performClick()
        composeTestRule.onNodeWithText("Crear una cuenta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil actual").assertDoesNotExist()
    }

    @Test
    fun registration_fakeSuccessReturnsToLoginWithConfirmation() {
        val auth = FakeAuth()
        val session = fakeSession(auth)
        val phrases = fakePhrases()
        composeTestRule.setContent { AccesibilidadTheme { AccesibilidadApp(session, phrases) } }
        composeTestRule.onNodeWithText("Crear una cuenta").performClick()
        composeTestRule.onNodeWithText("Nombre completo").performTextInput("Ana")
        composeTestRule.onNodeWithText("Correo electrónico").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Contraseña").performScrollTo().performTextInput("Secret1!")
        composeTestRule.onNodeWithText("Confirmar contraseña").performScrollTo().performTextInput("Secret1!")
        composeTestRule.onNodeWithText("Registrar cuenta").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Creando cuenta…").assertIsNotEnabled()
        composeTestRule.runOnIdle {
            auth.created(AuthIdentity("uid", "ana@example.com", "Ana"))
            auth.registerDone(Result.success(RegisteredAccount(AuthIdentity("uid", "ana@example.com", "Ana"))))
        }
        composeTestRule.onNodeWithText("Cuenta creada. Inicia sesión para continuar.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crear una cuenta").assertIsDisplayed()
    }

    @Test
    fun personalPhrases_createSelectEditAndConfirmDelete() {
        val phrases = fakePhrases().apply { bind("uid") }
        composeTestRule.setContent {
            AccesibilidadTheme { CommunicationHomeScreen("Ana", {}, phrases = phrases) }
        }
        composeTestRule.onNodeWithText("Texto de la frase personal").performScrollTo().performTextInput("Hello")
        composeTestRule.onNodeWithText("Añadir frase").performScrollTo().performClick()
        composeTestRule.onNode(hasText("Hello") and hasClickAction()).performScrollTo().performClick()
        composeTestRule.onNodeWithText("Mensaje para comunicar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Editar frase: Hello").performScrollTo().performClick()
        composeTestRule.runOnIdle { phrases.draft("Changed") }
        composeTestRule.onNodeWithText("Guardar frase").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Eliminar frase: Changed").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Confirmar eliminación").performClick()
        composeTestRule.onNodeWithText("Todavía no hay frases personales.").performScrollTo().assertIsDisplayed()
    }

    private fun fakePhrases(): PersonalPhrases = PersonalPhrases(object : PhraseGateway {
        private var changed: ((Result<List<PersonalPhrase>>) -> Unit)? = null
        private var items = emptyList<PersonalPhrase>()
        override fun listen(uid: String, changed: (Result<List<PersonalPhrase>>) -> Unit): PhraseSubscription {
            this.changed = changed
            changed(Result.success(items))
            return PhraseSubscription { this.changed = null }
        }
        override fun create(uid: String, text: String, done: (Result<Unit>) -> Unit) {
            items = items + PersonalPhrase("id", text)
            changed?.invoke(Result.success(items))
            done(Result.success(Unit))
        }
        override fun update(uid: String, id: String, text: String, done: (Result<Unit>) -> Unit) {
            items = items.map { if (it.id == id) it.copy(text = text) else it }
            changed?.invoke(Result.success(items))
            done(Result.success(Unit))
        }
        override fun delete(uid: String, id: String, done: (Result<Unit>) -> Unit) {
            items = items.filterNot { it.id == id }
            changed?.invoke(Result.success(items))
            done(Result.success(Unit))
        }
    })

    private fun fakeSession(auth: AuthGateway = FakeAuth()): AuthSession {
        val values = object : PreferenceValues {
            private val data = mutableMapOf<String, String>()
            override fun get(key: String) = data[key]
            override fun write(values: Map<String, String?>) {
                values.forEach { (key, value) -> if (value == null) data.remove(key) else data[key] = value }
            }
        }
        return AuthSession(auth, LocalProfileStore(values))
    }

    private class FakeAuth : AuthGateway {
        override val currentIdentity: AuthIdentity? = null
        lateinit var loginDone: (Result<AuthIdentity>) -> Unit
        lateinit var registerDone: (Result<RegisteredAccount>) -> Unit
        lateinit var created: (AuthIdentity) -> Unit
        override fun login(email: String, password: String, done: (Result<AuthIdentity>) -> Unit) { loginDone = done }
        override fun register(
            name: String, email: String, password: String,
            onCreated: (AuthIdentity) -> Unit, done: (Result<RegisteredAccount>) -> Unit
        ) {
            registerDone = done
            created = onCreated
        }
        override fun resetPassword(email: String, done: (Result<Unit>) -> Unit) { done(Result.success(Unit)) }
        override fun signOut() = Unit
    }
}
