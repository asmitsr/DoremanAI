package com.sneha.ai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// v2.0: shared shapes — small 12dp, medium 16dp, large 20dp.
// Existing colour schemes and the SnehaTheme signature are unchanged.
private val DoremanShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9B7BFF),
    onPrimary = Color(0xFF0F0A24),
    primaryContainer = Color(0xFF281E54),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF06202E),
    secondaryContainer = Color(0xFF102C44),
    onSecondaryContainer = Color(0xFFC7EEFF),
    tertiary = Color(0xFFF472B6),
    onTertiary = Color(0xFF33081F),
    background = Color(0xFF070913),
    onBackground = Color(0xFFF0F2FC),
    surface = Color(0xFF101426),
    onSurface = Color(0xFFF0F2FC),
    surfaceVariant = Color(0xFF191F38),
    onSurfaceVariant = Color(0xFFB5BCD6),
    outline = Color(0xFF2E3658),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF2D040A),
    errorContainer = Color(0xFF3B1219),
    onErrorContainer = Color(0xFFFFD9DC)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B3FD9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E2FF),
    onPrimaryContainer = Color(0xFF1E0E62),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF07334B),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color.White,
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF111528),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111528),
    surfaceVariant = Color(0xFFE8ECF7),
    onSurfaceVariant = Color(0xFF454B66),
    outline = Color(0xFFC5CCE2),
    error = Color(0xFFD92D3E),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF5F0914)
)

private val MidnightScheme = darkColorScheme(
    // Midnight Indigo #282B4A background, Vanilla #EEEBDA surfaces/text.
    primary = Color(0xFFEEEBDA),
    onPrimary = Color(0xFF282B4A),
    primaryContainer = Color(0xFF4A4E7A),
    onPrimaryContainer = Color(0xFFEEEBDA),
    secondary = Color(0xFFAEB2D8),
    onSecondary = Color(0xFF282B4A),
    secondaryContainer = Color(0xFF4A4E7A),
    onSecondaryContainer = Color(0xFFEEEBDA),
    tertiary = Color(0xFFF2D8C9),
    onTertiary = Color(0xFF282B4A),
    background = Color(0xFF282B4A),
    onBackground = Color(0xFFEEEBDA),
    surface = Color(0xFF34385F),
    onSurface = Color(0xFFEEEBDA),
    surfaceVariant = Color(0xFF41456F),
    onSurfaceVariant = Color(0xFFD8D9E6),
    outline = Color(0xFF6B70A3),
    error = Color(0xFFFF8A95),
    onError = Color(0xFF2D040A),
    errorContainer = Color(0xFF5A1F2A),
    onErrorContainer = Color(0xFFFFD9DC)
)

@Composable
fun SnehaTheme(mode: String, fontScale: Float, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        "midnight" -> true
        else -> isSystemInDarkTheme()
    }
    // "midnight" is a named dark scheme, not the generic dark scheme.
    val scheme = when (mode) {
        "midnight" -> MidnightScheme
        "light" -> LightScheme
        "dark" -> DarkScheme
        else -> if (dark) DarkScheme else LightScheme
    }
    val d = LocalDensity.current
    val base = MaterialTheme.typography
    val typo = base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 32.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.5f.sp, lineHeight = 23.sp),
        bodySmall = base.bodySmall.copy(fontSize = 12.5f.sp, lineHeight = 19.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    )
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * fontScale)) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typo,
            shapes = DoremanShapes,
            content = content
        )
    }
}
