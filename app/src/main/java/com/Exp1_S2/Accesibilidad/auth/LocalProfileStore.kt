package com.Exp1_S2.Accesibilidad.auth

import android.content.SharedPreferences
import com.Exp1_S2.Accesibilidad.AccessibilityPreference
import com.Exp1_S2.Accesibilidad.CommunicationMode
import com.Exp1_S2.Accesibilidad.CommunicationPreference
import com.Exp1_S2.Accesibilidad.User

/** String-only backing store permits honest JVM testing without simulating Android execution. */
interface PreferenceValues {
    fun get(key: String): String?
    fun write(values: Map<String, String?>)
}

class AndroidPreferenceValues(private val preferences: SharedPreferences) : PreferenceValues {
    override fun get(key: String): String? = preferences.getString(key, null)
    override fun write(values: Map<String, String?>) {
        preferences.edit().also { editor ->
            values.forEach { (key, value) ->
                if (value == null) editor.remove(key) else editor.putString(key, value)
            }
        }.apply()
    }
}

class LocalProfileStore(private val values: PreferenceValues) : ProfileStore {
    override fun profile(uid: String): User? {
        val prefix = "profile.$uid."
        val name = values.get(prefix + "name") ?: return null
        return User(
            uid = uid,
            name = name,
            email = values.get(prefix + "email").orEmpty(),
            communicationPreference = enumValue(values.get(prefix + "communication"), CommunicationPreference.EMAIL),
            primaryCommunicationMode = enumValue(values.get(prefix + "mode"), CommunicationMode.WRITTEN),
            accessibilityPreferences = values.get(prefix + "accessibility").orEmpty().split(",")
                .mapNotNull { value -> AccessibilityPreference.entries.firstOrNull { it.name == value } }.toSet()
        )
    }

    override fun saveProfile(user: User) {
        val prefix = "profile.${user.uid}."
        values.write(mapOf(
            prefix + "name" to user.name,
            prefix + "email" to user.email,
            prefix + "communication" to user.communicationPreference.name,
            prefix + "mode" to user.primaryCommunicationMode.name,
            prefix + "accessibility" to user.accessibilityPreferences.joinToString(",") { it.name }
        ))
    }

    override fun saveCurrent(user: User) = values.write(mapOf(
        "current.uid" to user.uid, "current.email" to user.email, "current.name" to user.name
    ))

    override fun clearCurrent() = values.write(mapOf(
        "current.uid" to null, "current.email" to null, "current.name" to null
    ))

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
