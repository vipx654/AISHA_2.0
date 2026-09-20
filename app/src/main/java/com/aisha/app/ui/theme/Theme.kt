package com.aisha.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AishaBlue = Color(0xFF7C4DFF)
private val AishaPink = Color(0xFFFF6EA9)
private val AishaDark = Color(0xFF141320)

private val LightColors = lightColorScheme(primary = AishaBlue, secondary = AishaPink)
private val DarkColors = darkColorScheme(primary = AishaBlue, secondary = AishaPink, background = AishaDark)

/** Theme setting is user-controlled (spec §3 Settings: theme). */
@Composable
fun AishaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
