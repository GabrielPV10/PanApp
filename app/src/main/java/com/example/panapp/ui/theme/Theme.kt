package com.example.panapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PanLightColors = lightColorScheme(
    primary              = Amber40,
    onPrimary            = Color.White,
    primaryContainer     = Amber80,
    onPrimaryContainer   = Amber10,
    secondary            = Wood40,
    onSecondary          = Color.White,
    secondaryContainer   = Wood80,
    onSecondaryContainer = Wood10,
    tertiary             = Herb40,
    onTertiary           = Color.White,
    tertiaryContainer    = Herb80,
    onTertiaryContainer  = Herb10,
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    background           = WarmBg,
    onBackground         = Amber10,
    surface              = WarmSurface,
    onSurface            = Amber10,
    surfaceVariant       = WarmSurfVar,
    onSurfaceVariant     = Wood40,
    outline              = WarmOutline,
    outlineVariant       = WarmOutlineVar,
)

@Composable
fun PanAppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PanLightColors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = PanLightColors,
        typography = Typography,
        content = content
    )
}
