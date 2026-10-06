package com.Exp1_S2.Accesibilidad

data class User(
    val name: String,
    val email: String,
    val uid: String = "",
    val communicationPreference: CommunicationPreference = CommunicationPreference.EMAIL,
    val primaryCommunicationMode: CommunicationMode = CommunicationMode.WRITTEN,
    val accessibilityPreferences: Set<AccessibilityPreference> = emptySet()
)

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
