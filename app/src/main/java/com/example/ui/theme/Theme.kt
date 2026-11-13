package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.preferences.ThemePreference

@Immutable
data class StatusColors(
  val isDark: Boolean,
  val up: Color,
  val upPartial: Color,
  val upContainer: Color,
  val down: Color,
  val downContainer: Color,
  val degraded: Color,
  val degradedContainer: Color,
  val maintenance: Color,
  val maintenanceContainer: Color,
  val noData: Color,
  val border: Color,
  val surfaceElevated: Color,
  val muted: Color,
)

private val DarkStatusColors =
  StatusColors(
    isDark = true,
    up = StatusUpDark,
    upPartial = StatusPartialUpDark,
    upContainer = StatusUpContainerDark,
    down = StatusDownDark,
    downContainer = StatusDownContainerDark,
    degraded = StatusDegradedDark,
    degradedContainer = StatusDegradedContainerDark,
    maintenance = StatusMaintDark,
    maintenanceContainer = StatusMaintContainerDark,
    noData = StatusNoDataDark,
    border = NocBorder,
    surfaceElevated = NocSurfaceElevated,
    muted = NocTextMuted,
  )

private val LightStatusColors =
  StatusColors(
    isDark = false,
    up = StatusUpLight,
    upPartial = StatusPartialUpLight,
    upContainer = StatusUpContainerLight,
    down = StatusDownLight,
    downContainer = StatusDownContainerLight,
    degraded = StatusDegradedLight,
    degradedContainer = StatusDegradedContainerLight,
    maintenance = StatusMaintLight,
    maintenanceContainer = StatusMaintContainerLight,
    noData = StatusNoDataLight,
    border = LightBorder,
    surfaceElevated = LightSurfaceElevated,
    muted = LightTextMuted,
  )

val LocalStatusColors = staticCompositionLocalOf { DarkStatusColors }

private val DarkColorScheme =
  darkColorScheme(
    primary = StatusUpDark,
    onPrimary = Color(0xFF0A1D12),
    primaryContainer = StatusUpContainerDark,
    onPrimaryContainer = StatusUpDark,
    secondary = StatusMaintDark,
    onSecondary = Color(0xFF08142B),
    secondaryContainer = StatusMaintContainerDark,
    onSecondaryContainer = StatusMaintDark,
    tertiary = StatusDegradedDark,
    background = NocBg,
    onBackground = NocTextPrimary,
    surface = NocSurface,
    onSurface = NocTextPrimary,
    surfaceVariant = NocSurfaceElevated,
    onSurfaceVariant = NocTextSecondary,
    outline = NocBorder,
    outlineVariant = NocBorder,
    error = StatusDownDark,
    onError = Color.White,
    errorContainer = StatusDownContainerDark,
    onErrorContainer = StatusDownDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StatusUpLight,
    onPrimary = Color.White,
    primaryContainer = StatusUpContainerLight,
    onPrimaryContainer = StatusUpLight,
    secondary = StatusMaintLight,
    onSecondary = Color.White,
    secondaryContainer = StatusMaintContainerLight,
    onSecondaryContainer = StatusMaintLight,
    tertiary = StatusDegradedLight,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = StatusDownLight,
    onError = Color.White,
    errorContainer = StatusDownContainerLight,
    onErrorContainer = StatusDownLight,
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun GjnWmStatusTheme(
  themePreference: ThemePreference = ThemePreference.DARK,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val useDark =
    when (themePreference) {
      ThemePreference.DARK -> true
      ThemePreference.LIGHT -> false
      ThemePreference.SYSTEM -> systemDark
    }

  val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
  val statusColors = if (useDark) DarkStatusColors else LightStatusColors

  CompositionLocalProvider(LocalStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
