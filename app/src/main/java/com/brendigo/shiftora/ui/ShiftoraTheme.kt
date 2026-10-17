package com.brendigo.shiftora.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Navy = Color(0xFF0B1F3B)
    val DeepNavy = Color(0xFF061831)
    val Blue = Color(0xFF2563EB)
    val Mint = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val Purple = Color(0xFF6D43E9)
    val Coral = Color(0xFFEF5350)
    val Ink = Color(0xFF0F172A)
    val Secondary = Color(0xFF64748B)
    val Bg = Color(0xFFF8FAFC)
    val BlueLight = Color(0xFFE8F1FF)
    val MintLight = Color(0xFFDEF8EE)
    val AmberLight = Color(0xFFFFF4DD)
    val PurpleLight = Color(0xFFF1EAFE)
    val CoralLight = Color(0xFFFFEAEA)
}

private val light = lightColorScheme(
    primary = Palette.Blue,
    onPrimary = Color.White,
    primaryContainer = Palette.BlueLight,
    onPrimaryContainer = Palette.Navy,
    secondary = Palette.Mint,
    onSecondary = Color.White,
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Color.White,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.BlueLight,
    onSurfaceVariant = Palette.Secondary,
    outline = Color(0xFFDCE5F2)
)
private val dark = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Palette.DeepNavy,
    background = Color(0xFF071426),
    onBackground = Color(0xFFF2F6FF),
    surface = Color(0xFF12243B),
    onSurface = Color(0xFFF2F6FF),
    surfaceVariant = Color(0xFF233957),
    onSurfaceVariant = Color(0xFFBBCEE6),
    outline = Color(0xFF47617D)
)

@Composable
fun ShiftoraTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val base = Typography()
    val typography = Typography(
        headlineLarge = TextStyle(fontSize = 31.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
        headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = base.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    )
    MaterialTheme(colorScheme = if (darkMode) dark else light, typography = typography, content = content)
}
