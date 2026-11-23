package com.pentax.k1sync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkroomBg = Color(0xFF121212)
val DarkroomSurface = Color(0xFF1C1C1E)
val DarkroomCard = Color(0xFF242427)
val DarkroomBorder = Color(0xFF323236)
val PentaxGreen = Color(0xFF2EBF6D)
val PentaxGreenDim = Color(0xFF1E4D33)
val PentaxRed = Color(0xFFD91E2A)
val TextPrimary = Color(0xFFF3F3F5)
val TextSecondary = Color(0xFF9E9EA6)

private val DarkroomColorScheme = darkColorScheme(
    primary = PentaxGreen,
    onPrimary = Color(0xFF061A0E),
    primaryContainer = PentaxGreenDim,
    onPrimaryContainer = Color(0xFFBFF5D3),
    secondary = PentaxRed,
    onSecondary = Color.White,
    background = DarkroomBg,
    onBackground = TextPrimary,
    surface = DarkroomSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkroomCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkroomBorder
)

@Composable
fun PentaxK1Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkroomColorScheme,
        content = content
    )
}
