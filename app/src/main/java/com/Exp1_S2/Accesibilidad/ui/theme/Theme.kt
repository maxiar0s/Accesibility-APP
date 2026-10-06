package com.Exp1_S2.Accesibilidad.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF003832),
    secondary = NavyDark,
    onSecondary = Color(0xFF123248),
    tertiary = TealDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = CanvasDark,
    onSurface = InkDark,
    surfaceVariant = Color(0xFF293D44),
    onSurfaceVariant = Color(0xFFC2D5DA),
    secondaryContainer = Color(0xFF254452),
    onSecondaryContainer = InkDark,
    primaryContainer = Color(0xFF164B44),
    onPrimaryContainer = Color(0xFFB2F1E5)
)

private val LightColorScheme = lightColorScheme(
    primary = TealLight,
    onPrimary = Color.White,
    secondary = NavyLight,
    onSecondary = Color.White,
    tertiary = TealLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = CanvasLight,
    onSurface = InkLight,
    surfaceVariant = Color(0xFFE0ECEB),
    onSurfaceVariant = Color(0xFF3A5055),
    secondaryContainer = Color(0xFFDDECF2),
    onSecondaryContainer = InkLight,
    primaryContainer = Color(0xFFD0EDE6),
    onPrimaryContainer = Color(0xFF003832)
)

@Composable
fun AccesibilidadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Opt in explicitly; the default palette is consistent across devices.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
