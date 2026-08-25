package com.Exp1_S2.Accesibilidad

data class User(
    val name: String,
    val email: String,
    val password: String,
    val communicationPreference: CommunicationPreference,
    val primaryCommunicationMode: CommunicationMode,
    val accessibilityPreferences: Set<AccessibilityPreference>
)

fun authenticateUser(users: Array<User?>, email: String, password: String): Boolean =
    users.any { user -> user?.email == email && user?.password == password }

enum class CommunicationPreference {
    EMAIL,
    PHONE,
    TEXT_MESSAGE
}

enum class CommunicationMode {
    WRITTEN,
    VOICE,
    VISUAL
}

enum class AccessibilityPreference {
    HIGH_CONTRAST,
    VISUAL_ALERTS,
    VIBRATION
}
