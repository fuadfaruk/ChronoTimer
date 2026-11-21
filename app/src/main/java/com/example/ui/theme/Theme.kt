package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = Color(0xFF00363D),
  primaryContainer = Color(0xFF004F58),
  onPrimaryContainer = Color(0xFF8BF3FF),

  secondary = NeonGreen,
  onSecondary = Color(0xFF003919),
  secondaryContainer = Color(0xFF005327),
  onSecondaryContainer = Color(0xFF70FF9C),

  tertiary = NeonAmber,
  onTertiary = Color(0xFF492900),
  tertiaryContainer = Color(0xFF693C00),
  onTertiaryContainer = Color(0xFFFFDCBE),

  background = BackgroundDark,
  onBackground = TextPrimaryDark,

  surface = SurfaceDark,
  onSurface = TextPrimaryDark,

  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,

  outline = SurfaceBorderDark,
  outlineVariant = Color(0xFF1E2838),

  error = NeonCoral,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF00838F),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFB2EBF2),
  onPrimaryContainer = Color(0xFF004D40),

  secondary = Color(0xFF2E7D32),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFC8E6C9),
  onSecondaryContainer = Color(0xFF1B5E20),

  tertiary = Color(0xFFE65100),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFE0B2),
  onTertiaryContainer = Color(0xFFBF360C),

  background = BackgroundLight,
  onBackground = TextPrimaryLight,

  surface = SurfaceLight,
  onSurface = TextPrimaryLight,

  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,

  outline = SurfaceBorderLight,
  outlineVariant = Color(0xFFE2E8F0),

  error = Color(0xFFD32F2F),
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek dark OLED aesthetic for timer & stopwatch
  dynamicColor: Boolean = false, // Preserve crafted precision neon aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
