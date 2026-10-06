package com.Exp1_S2.Accesibilidad.auth

const val REGISTRATION_PASSWORD_HINT =
    "Usa al menos 7 caracteres, con mayúscula, minúscula, número y símbolo. Los espacios no cuentan como símbolo."

fun validateRegistration(
    name: String,
    email: String,
    password: String,
    confirmation: String? = null
): String? = when {
    name.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres sin contar espacios iniciales o finales."
    !validEmail(email) -> "Introduce un correo electrónico válido."
    password.length < 7 -> "La contraseña debe tener al menos 7 caracteres."
    password.none { it.isUpperCase() } -> "La contraseña debe incluir una mayúscula."
    password.none { it.isLowerCase() } -> "La contraseña debe incluir una minúscula."
    password.none { it.isDigit() } -> "La contraseña debe incluir un número."
    password.none { !it.isWhitespace() && !it.isLetterOrDigit() } ->
        "La contraseña debe incluir un símbolo que no sea un espacio."
    confirmation != null && password != confirmation -> "Las contraseñas no coinciden."
    else -> null
}
