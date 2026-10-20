package com.fit15.app

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Signaalkleur: alleen voor acties en "werk". Tekst erop is altijd inkt-zwart. */
val Signal = Color(0xFFFF4B1F)
val Ink = Color(0xFF16191C)
val Chalk = Color(0xFFF1F2F0)

/** Smalle, zware letters als op een sporthal-klok. Zit op elke Android (Roboto Condensed). */
val Display = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

object Type {
    val hero = TextStyle(fontFamily = Display, fontSize = 46.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp)
    val title = TextStyle(fontFamily = Display, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp)
    val section = TextStyle(fontFamily = Display, fontSize = 24.sp, lineHeight = 28.sp)
    val number = TextStyle(fontFamily = Display, fontSize = 34.sp, lineHeight = 34.sp, fontFeatureSettings = "tnum")
    val timer = TextStyle(fontFamily = Display, fontSize = 72.sp, lineHeight = 72.sp, fontFeatureSettings = "tnum")
    val row = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val small = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
}

/** Voor tekst op de achtergrond: de signaalkleur is te licht voor kleine letters op beton, dus daar een donkerdere variant. */
@Composable
fun accentText(): Color = if (isSystemInDarkTheme()) Signal else Color(0xFFB8300A)

@Composable
fun Fit15Theme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Signal, onPrimary = Ink,
            primaryContainer = Color(0xFF4A1F12), onPrimaryContainer = Color(0xFFFFD9CF),
            tertiary = Color(0xFF5FD0B0), onTertiary = Color(0xFF00201B),
            background = Color(0xFF0F1113), onBackground = Color(0xFFECEEEE),
            surface = Color(0xFF171A1C), onSurface = Color(0xFFECEEEE),
            surfaceVariant = Color(0xFF24292C), onSurfaceVariant = Color(0xFFB4BCC0),
            secondaryContainer = Color(0xFF4A1F12), onSecondaryContainer = Color(0xFFFFD9CF),
            outline = Color(0xFF7A8388), outlineVariant = Color(0xFF30363A),
        )
    } else {
        lightColorScheme(
            primary = Signal, onPrimary = Ink,
            primaryContainer = Color(0xFFFFD9CF), onPrimaryContainer = Color(0xFF3A0E03),
            tertiary = Color(0xFF1D5B4B), onTertiary = Color.White,
            background = Color(0xFFE8EAEA), onBackground = Ink,
            surface = Color(0xFFF5F6F6), onSurface = Ink,
            surfaceVariant = Color(0xFFDADDDD), onSurfaceVariant = Color(0xFF4A5256),
            secondaryContainer = Color(0xFFFFD9CF), onSecondaryContainer = Color(0xFF3A0E03),
            outline = Color(0xFF788085), outlineVariant = Color(0xFFC4C9CA),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
