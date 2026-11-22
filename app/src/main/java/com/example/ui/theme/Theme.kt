package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark")
}

private val FallbackDarkColorScheme = darkColorScheme(
  primary = PrimaryDark,
  onPrimary = OnPrimaryDark,
  primaryContainer = PrimaryContainerDark,
  onPrimaryContainer = OnPrimaryContainerDark,

  secondary = SecondaryDark,
  onSecondary = OnSecondaryDark,
  secondaryContainer = SecondaryContainerDark,
  onSecondaryContainer = OnSecondaryContainerDark,

  tertiary = TertiaryDark,
  onTertiary = OnTertiaryDark,
  tertiaryContainer = TertiaryContainerDark,
  onTertiaryContainer = OnTertiaryContainerDark,

  background = BackgroundDark,
  onBackground = OnBackgroundDark,

  surface = SurfaceDark,
  onSurface = OnSurfaceDark,

  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,

  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,

  error = ErrorDark,
  onError = OnErrorDark,
  errorContainer = ErrorContainerDark,
  onErrorContainer = OnErrorContainerDark
)

private val FallbackLightColorScheme = lightColorScheme(
  primary = PrimaryLight,
  onPrimary = OnPrimaryLight,
  primaryContainer = PrimaryContainerLight,
  onPrimaryContainer = OnPrimaryContainerLight,

  secondary = SecondaryLight,
  onSecondary = OnSecondaryLight,
  secondaryContainer = SecondaryContainerLight,
  onSecondaryContainer = OnSecondaryContainerLight,

  tertiary = TertiaryLight,
  onTertiary = OnTertiaryLight,
  tertiaryContainer = TertiaryContainerLight,
  onTertiaryContainer = OnTertiaryContainerLight,

  background = BackgroundLight,
  onBackground = OnBackgroundLight,

  surface = SurfaceLight,
  onSurface = OnSurfaceLight,

  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,

  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,

  error = ErrorLight,
  onError = OnErrorLight,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> FallbackDarkColorScheme
    else -> FallbackLightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

/**
 * Harmonizes stored preset/step color slots with the user's live Material You / M3 ColorScheme.
 */
fun resolveThemeAccentColor(colorHex: Long, colorScheme: ColorScheme): Color {
  return when (colorHex) {
    0xFF00E5FFL -> colorScheme.primary
    0xFF00E676L -> colorScheme.secondary
    0xFFFF9100L -> colorScheme.tertiary
    0xFFB388FFL -> lerp(colorScheme.primary, colorScheme.tertiary, 0.5f)
    0xFFFF5252L -> colorScheme.error
    0xFFFFD600L -> lerp(colorScheme.secondary, colorScheme.tertiary, 0.5f)
    else -> Color(colorHex)
  }
}

/**
 * Returns an accessible, M3-aligned foreground color for a dynamic [containerColor].
 */
fun contrastingContentColor(containerColor: Color, colorScheme: ColorScheme): Color {
  return when (containerColor) {
    colorScheme.primary -> colorScheme.onPrimary
    colorScheme.secondary -> colorScheme.onSecondary
    colorScheme.tertiary -> colorScheme.onTertiary
    colorScheme.error -> colorScheme.onError
    colorScheme.primaryContainer -> colorScheme.onPrimaryContainer
    colorScheme.secondaryContainer -> colorScheme.onSecondaryContainer
    colorScheme.tertiaryContainer -> colorScheme.onTertiaryContainer
    colorScheme.errorContainer -> colorScheme.onErrorContainer
    else -> if (containerColor.luminance() > 0.45f) Color(0xFF141414) else Color(0xFFFDFDFD)
  }
}
