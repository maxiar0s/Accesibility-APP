package com.Exp1_S2.Accesibilidad

import com.Exp1_S2.Accesibilidad.auth.validateRegistration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationValidationTest {
    private fun validate(name: String = "Ana", password: String = "Abcde1!") =
        validateRegistration(name, "ana@example.com", password)

    @Test
    fun name_trimsEdgesAndAcceptsThreeOrMoreCharactersIncludingAccentsAndSpaces() {
        assertNotNull(validate(name = " Án "))
        assertNull(validate(name = " Ána "))
        assertNull(validate(name = " José "))
        assertNull(validate(name = "A B"))
    }

    @Test
    fun password_lengthBoundaryIsSeven() {
        assertNotNull(validate(password = "Abcd1!"))
        assertNull(validate(password = "Abcde1!"))
        assertNull(validate(password = "Abcdef1!"))
    }

    @Test
    fun password_requiresEachCharacterClass() {
        listOf("abcde1!", "ABCDE1!", "Abcdef!", "Abcdef1").forEach {
            assertNotNull("Missing class in $it", validate(password = it))
        }
        assertNull(validate(password = "Abcde1!"))
    }

    @Test
    fun whitespaceDoesNotSatisfySymbolButDoesNotMutateValidPassword() {
        listOf("Abcde1 ", "Abcde1\t", "Abcde1\n").forEach {
            assertNotNull(validate(password = it))
        }
        assertNull(validateRegistration("Ana", "ana@example.com", " Abcde1! ", " Abcde1! "))
        assertNotNull(validateRegistration("Ana", "ana@example.com", " Abcde1! ", "Abcde1!"))
    }

    @Test
    fun confirmationMustMatchExactlyWhenSupplied() {
        assertEquals("Las contraseñas no coinciden.",
            validateRegistration("Ana", "ana@example.com", "Abcde1!", "Abcde2!"))
        assertNotNull(validateRegistration("Ana", "ana@example.com", "Abcde1!", ""))
        assertNull(validateRegistration("Ana", "ana@example.com", "Abcde1!", "Abcde1!"))
    }

    @Test
    fun emailMustBeValidAndEdgesAreTrimmed() {
        assertNotNull(validateRegistration("Ana", "invalid", "Abcde1!"))
        assertNull(validateRegistration("Ana", " ana@example.com ", "Abcde1!"))
    }
}
