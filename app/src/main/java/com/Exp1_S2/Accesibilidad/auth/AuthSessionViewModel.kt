package com.Exp1_S2.Accesibilidad.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.Exp1_S2.Accesibilidad.communication.CommunicationHistory
import com.Exp1_S2.Accesibilidad.communication.FirestoreCommunicationHistoryGateway
import com.Exp1_S2.Accesibilidad.phrases.FirestorePhraseGateway
import com.Exp1_S2.Accesibilidad.phrases.PersonalPhrases

/** Keeps an in-flight registration from becoming a restored Home during configuration changes. */
class AuthSessionViewModel(application: Application) : AndroidViewModel(application) {
    val phrases = PersonalPhrases(FirestorePhraseGateway(FirebaseFirestore.getInstance()))
    val communicationHistory = CommunicationHistory(
        FirestoreCommunicationHistoryGateway(FirebaseFirestore.getInstance())
    )
    val session = AuthSession(
        FirebaseAuthGateway(FirebaseAuth.getInstance()),
        LocalProfileStore(AndroidPreferenceValues(application.getSharedPreferences("local_profile", Context.MODE_PRIVATE)))
    )

    override fun onCleared() {
        phrases.close()
        communicationHistory.close()
        session.close()
    }
}
