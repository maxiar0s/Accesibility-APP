package com.Exp1_S2.Accesibilidad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.Exp1_S2.Accesibilidad.ui.AccesibilidadApp
import com.Exp1_S2.Accesibilidad.ui.theme.AccesibilidadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccesibilidadTheme {
                AccesibilidadApp()
            }
        }
    }
}
