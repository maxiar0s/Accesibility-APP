package com.Exp1_S2.Accesibilidad.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Exp1_S2.Accesibilidad.ui.theme.AccesibilidadTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccesibilidadAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appRoot_displaysRegistrationFormAndEmptyState() {
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp()
            }
        }

        composeTestRule.onNodeWithText("Registro accesible").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Todavía no hay cuentas registradas. Completá el formulario para agregar la primera."
        ).assertExists()
    }

    @Test
    fun registration_showsValidationMessageWhenRequiredFieldsAreBlank() {
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp()
            }
        }

        composeTestRule.onNodeWithText("Registrar cuenta").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Completá todos los campos obligatorios.")
            .assertIsDisplayed()
    }
}
