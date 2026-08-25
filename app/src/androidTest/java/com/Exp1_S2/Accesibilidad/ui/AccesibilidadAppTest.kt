package com.Exp1_S2.Accesibilidad.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Exp1_S2.Accesibilidad.ui.home.CommunicationHomeScreen
import com.Exp1_S2.Accesibilidad.ui.theme.AccesibilidadTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccesibilidadAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appRoot_displaysLoginScreen() {
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp()
            }
        }

        composeTestRule.onNodeWithText("Crear una cuenta").assertIsDisplayed()
    }

    @Test
    fun login_navigatesToRegistration() {
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp()
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
        composeTestRule.onNodeWithText("Necesito ayuda, por favor.").performClick()

        composeTestRule.onNodeWithText("Mensaje para comunicar").assertIsDisplayed()
    }
}
