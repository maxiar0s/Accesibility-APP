package com.Exp1_S2.Accesibilidad.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
    fun appRoot_displaysWelcomeHeading() {
        composeTestRule.setContent {
            AccesibilidadTheme {
                AccesibilidadApp()
            }
        }

        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
