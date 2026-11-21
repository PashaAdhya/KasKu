package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

private val KasKuLightColorScheme = lightColorScheme(
    primary = AcidLime,
    onPrimary = InkBlack,
    primaryContainer = AcidLime,
    onPrimaryContainer = InkBlack,
    secondary = ElectricCyan,
    onSecondary = InkBlack,
    secondaryContainer = CreamSurface,
    onSecondaryContainer = InkBlack,
    tertiary = CoralRed,
    onTertiary = InkBlack,
    background = WarmPaper,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = MutedInkLight,
    outline = InkBlack,
    error = CoralRed,
    onError = InkBlack,
)

private val KasKuDarkColorScheme = darkColorScheme(
    primary = DarkLime,
    onPrimary = InkBlack,
    primaryContainer = DarkLime,
    onPrimaryContainer = InkBlack,
    secondary = DarkCyan,
    onSecondary = InkBlack,
    secondaryContainer = ElevatedCharcoal,
    onSecondaryContainer = ChalkWhite,
    tertiary = DarkCoral,
    onTertiary = InkBlack,
    background = ObsidianBg,
    onBackground = ChalkWhite,
    surface = CharcoalSurface,
    onSurface = ChalkWhite,
    surfaceVariant = ElevatedCharcoal,
    onSurfaceVariant = MutedInkDark,
    outline = DarkBorder,
    error = DarkCoral,
    onError = InkBlack,
)

val KasKuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun KasKuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDark) KasKuDarkColorScheme else KasKuLightColorScheme
    val brutalPalette = if (useDark) DarkBrutalPalette else LightBrutalPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalBrutalPalette provides brutalPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KasKuTypography,
            shapes = KasKuShapes,
            content = content,
        )
    }
}
