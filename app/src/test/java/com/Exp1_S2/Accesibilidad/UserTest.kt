package com.Exp1_S2.Accesibilidad

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {
    @Test
    fun authenticateUser_matchesOnlyAnExistingSlotWithCorrectCredentials() {
        val users = arrayOfNulls<User>(5)
        users[2] = testUser(email = "ana@example.com", password = "secret")

        assertTrue(authenticateUser(users, "ana@example.com", "secret"))
        assertFalse(authenticateUser(users, "ana@example.com", "incorrect"))
        assertFalse(authenticateUser(users, "missing@example.com", "secret"))
    }

    private fun testUser(email: String, password: String) = User(
        name = "Ana",
        email = email,
        password = password,
        communicationPreference = CommunicationPreference.EMAIL,
        primaryCommunicationMode = CommunicationMode.WRITTEN,
        accessibilityPreferences = emptySet()
    )
}
