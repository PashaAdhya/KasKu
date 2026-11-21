package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Neo-Brutalist Palette
val WarmPaper = Color(0xFFFFFDF5)
val CreamSurface = Color(0xFFFAF6E8)
val PureWhite = Color(0xFFFFFFFF)
val InkBlack = Color(0xFF111111)
val MutedInkLight = Color(0xFF525866)
val AcidLime = Color(0xFFD9FF00)
val ElectricCyan = Color(0xFF00E5FF)
val CoralRed = Color(0xFFFF5E5B)
val AmberGold = Color(0xFFFFB800)
val MintGreen = Color(0xFF00E676)
val VioletPop = Color(0xFFB388FF)
val Tangerine = Color(0xFFFF8A00)
val SkyBlue = Color(0xFF56CCF2)

// Dark Neo-Brutalist Palette (eye-comfortable obsidian with crisp chalk borders)
val ObsidianBg = Color(0xFF0E1015)
val CharcoalSurface = Color(0xFF171B24)
val ElevatedCharcoal = Color(0xFF202633)
val ChalkWhite = Color(0xFFF3F4F0)
val MutedInkDark = Color(0xFFA0A8B8)
val DarkBorder = Color(0xFFE5E7EB)
val DarkShadow = Color(0xFF000000)

val DarkLime = Color(0xFFC8EC00)
val DarkCyan = Color(0xFF22D3EE)
val DarkCoral = Color(0xFFFF6B6B)
val DarkAmber = Color(0xFFFBBF24)
val DarkMint = Color(0xFF34D399)
val DarkViolet = Color(0xFFC084FC)
val DarkTangerine = Color(0xFFFB923C)
val DarkSky = Color(0xFF38BDF8)

@Immutable
data class BrutalPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val shadow: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val secondaryAccent: Color,
    val onSecondaryAccent: Color,
    val expense: Color,
    val onExpense: Color,
    val income: Color,
    val onIncome: Color,
    val warning: Color,
    val onWarning: Color,
    val violet: Color,
    val tangerine: Color,
    val sky: Color,
)

val LightBrutalPalette = BrutalPalette(
    isDark = false,
    background = WarmPaper,
    surface = PureWhite,
    surfaceVariant = CreamSurface,
    textPrimary = InkBlack,
    textSecondary = MutedInkLight,
    border = InkBlack,
    shadow = InkBlack,
    primaryAccent = AcidLime,
    onPrimaryAccent = InkBlack,
    secondaryAccent = ElectricCyan,
    onSecondaryAccent = InkBlack,
    expense = CoralRed,
    onExpense = InkBlack,
    income = MintGreen,
    onIncome = InkBlack,
    warning = AmberGold,
    onWarning = InkBlack,
    violet = VioletPop,
    tangerine = Tangerine,
    sky = SkyBlue,
)

val DarkBrutalPalette = BrutalPalette(
    isDark = true,
    background = ObsidianBg,
    surface = CharcoalSurface,
    surfaceVariant = ElevatedCharcoal,
    textPrimary = ChalkWhite,
    textSecondary = MutedInkDark,
    border = DarkBorder,
    shadow = DarkShadow,
    primaryAccent = DarkLime,
    onPrimaryAccent = InkBlack,
    secondaryAccent = DarkCyan,
    onSecondaryAccent = InkBlack,
    expense = DarkCoral,
    onExpense = InkBlack,
    income = DarkMint,
    onIncome = InkBlack,
    warning = DarkAmber,
    onWarning = InkBlack,
    violet = DarkViolet,
    tangerine = DarkTangerine,
    sky = DarkSky,
)

val LocalBrutalPalette = staticCompositionLocalOf { LightBrutalPalette }
