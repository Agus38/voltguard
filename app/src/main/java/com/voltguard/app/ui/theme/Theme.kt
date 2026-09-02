package com.voltguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Palette --------------------------------------------------------------
val Bg = Color(0xFF0B111E)
val BgElevated = Color(0xFF111B2E)
val Card = Color(0xFF16233A)
val CardStroke = Color(0x2EFFFFFF)
val TextPrimary = Color(0xFFF2F5FB)
val TextSecondary = Color(0x9FB0C9)
val TextMuted = Color(0xFF6A7A94)

val Amber = Color(0xFFF6C453)
val AmberSoft = Color(0xFFB98A1E)
val Cyan = Color(0xFF4FD1E0)
val CyanSoft = Color(0xFF2FA3B0)
val Green = Color(0xFF43D17A)
val Red = Color(0xFFFF5A5F)
val Orange = Color(0xFFFF9F43)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Bg,
    primaryContainer = Color(0xFF3A2E10),
    onPrimaryContainer = Amber,
    secondary = Cyan,
    onSecondary = Bg,
    secondaryContainer = Color(0xFF0F2B30),
    onSecondaryContainer = Cyan,
    tertiary = Green,
    background = Bg,
    onBackground = TextPrimary,
    surface = Bg,
    onSurface = TextPrimary,
    surfaceVariant = Card,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2C3B54),
    error = Red,
    onError = Color(0xFF2A0B0C),
)

val DisplayMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 44.sp,
    lineHeight = 50.sp,
    letterSpacing = (-0.5).sp,
)
val StatNumber = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 26.sp,
    letterSpacing = (-0.3).sp,
)
val StatLabel = TextStyle(
    color = TextSecondary,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.3.sp,
)
val Caption = TextStyle(
    color = TextMuted,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
)

@Composable
fun VoltGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}

/** Soft radial backdrop used behind the hero gauge. */
fun heroBrush(state: Color): Brush = Brush.radialGradient(
    colors = listOf(state.copy(alpha = 0.28f), state.copy(alpha = 0.05f), Bg),
    center = androidx.compose.ui.geometry.Offset(x = 360f, y = 220f),
    radius = 700f,
)
