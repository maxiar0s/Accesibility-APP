package com.Exp1_S2.Accesibilidad.speech

import com.Exp1_S2.Accesibilidad.communication.CommunicationHistory

/** Returns the first non-blank recognized alternative, if any. */
internal fun normalizeSpeechResult(alternatives: List<String>?): String? =
    alternatives?.firstNotNullOfOrNull { alternative ->
        alternative.trim().takeIf { it.isNotEmpty() && it.length <= CommunicationHistory.MAX_MESSAGE_LENGTH }
    }
