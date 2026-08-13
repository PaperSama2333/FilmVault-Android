package com.papersama.filmvault.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

val KodakYellow = Color(0xFFF6C700)
val Ink = Color(0xFF1A1A1A)
val Sub = Color(0xFF8A8A8E)
val Weak = Color(0xFFB5B5B9)
val Line = Color(0xFFF0F0F2)
val Surface = Color.White
val Gray = Color(0xFFF0F0F2)
val ArchiveBg = Color(0xFFFCF6E0)
val ArchiveLine = Color(0xFFF5DB66)
val TagBg = Color(0xFFF6F6F4)
val TagInk = Color(0xFF6B6B70)
val Danger = Color(0xFFE5484D)

private val FilmVaultColors = lightColorScheme(
    primary = KodakYellow,
    onPrimary = Ink,
    primaryContainer = KodakYellow,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = Surface,
    background = Surface,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = TagBg,
    onSurfaceVariant = TagInk,
    outline = Line,
    error = Danger,
)

private val FilmVaultTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val FilmVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun FilmVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FilmVaultColors,
        typography = FilmVaultTypography,
        shapes = FilmVaultShapes,
        content = content,
    )
}
