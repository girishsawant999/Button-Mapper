package com.example.buttonmapper.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = PastelPrimaryDark,
  secondary = PastelSecondaryDark,
  tertiary = Pink80,
  background = PastelBgStartDark,
  surface = PastelCardDark,
  error = PastelDangerDark,
  onPrimary = PastelOnPrimaryDark,
  onBackground = PastelSubtitleDark,
  onSurface = PastelSubtitleDark
)

private val LightColorScheme =
  lightColorScheme(
    primary = PastelPrimaryLight,
    secondary = PastelSecondaryLight,
    tertiary = Pink40,
    background = PastelBgStartLight,
    surface = PastelCardLight,
    error = PastelDangerLight,
    onPrimary = PastelOnPrimaryLight,
    onBackground = PastelSubtitleLight,
    onSurface = PastelSubtitleLight
  )

@Composable
fun ButtonMapperTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
