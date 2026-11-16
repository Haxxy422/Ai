package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPreset

@Immutable
data class RoseAccentColors(
    val primaryGlow: Color,
    val secondaryGlow: Color,
    val tertiaryGlow: Color,
    val subtleBg: Color
)

val LocalRoseAccents = staticCompositionLocalOf {
    RoseAccentColors(
        primaryGlow = RosePrimary,
        secondaryGlow = CyanAccent,
        tertiaryGlow = VioletAccent,
        subtleBg = SpaceSurface
    )
}

val RoseShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

fun accentColorsFor(preset: AccentPreset): RoseAccentColors = when (preset) {
    AccentPreset.ROSE_CYAN -> RoseAccentColors(
        primaryGlow = RosePrimary,
        secondaryGlow = CyanAccent,
        tertiaryGlow = VioletAccent,
        subtleBg = Color(0xFF151830)
    )
    AccentPreset.ASTRAL_VIOLET -> RoseAccentColors(
        primaryGlow = VioletAccent,
        secondaryGlow = RoseSoft,
        tertiaryGlow = CyanAccent,
        subtleBg = Color(0xFF171433)
    )
    AccentPreset.EMERALD_AURORA -> RoseAccentColors(
        primaryGlow = EmeraldAurora,
        secondaryGlow = CyanAccent,
        tertiaryGlow = RosePrimary,
        subtleBg = Color(0xFF0E1D2A)
    )
    AccentPreset.SOLAR_ROSE -> RoseAccentColors(
        primaryGlow = RosePrimary,
        secondaryGlow = SolarGold,
        tertiaryGlow = CyanSoft,
        subtleBg = Color(0xFF1F1627)
    )
}

@Composable
fun RoseAiTheme(
    accentPreset: AccentPreset = AccentPreset.ROSE_CYAN,
    content: @Composable () -> Unit
) {
    val accents = accentColorsFor(accentPreset)
    val colorScheme = darkColorScheme(
        primary = accents.primaryGlow,
        onPrimary = Color.White,
        primaryContainer = accents.primaryGlow.copy(alpha = 0.2f),
        onPrimaryContainer = TextPrimary,
        secondary = accents.secondaryGlow,
        onSecondary = SpaceObsidian,
        secondaryContainer = accents.secondaryGlow.copy(alpha = 0.16f),
        onSecondaryContainer = TextPrimary,
        tertiary = accents.tertiaryGlow,
        onTertiary = Color.White,
        background = SpaceObsidian,
        onBackground = TextPrimary,
        surface = SpaceDeepNavy,
        onSurface = TextPrimary,
        surfaceVariant = SpaceSurface,
        onSurfaceVariant = TextSecondary,
        outline = SpaceBorder,
        outlineVariant = SpaceBorder.copy(alpha = 0.5f),
        error = StatusError,
        onError = Color.White,
        errorContainer = StatusError.copy(alpha = 0.18f),
        onErrorContainer = StatusError
    )

    CompositionLocalProvider(LocalRoseAccents provides accents) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RoseTypography,
            shapes = RoseShapes,
            content = content
        )
    }
}
