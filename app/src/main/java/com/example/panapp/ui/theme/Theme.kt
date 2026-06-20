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
    primary              = Blue40,
    onPrimary            = Color.White,
    primaryContainer     = Blue90,
    onPrimaryContainer   = Blue10,
    secondary            = Slate40,
    onSecondary          = Color.White,
    secondaryContainer   = Slate90,
    onSecondaryContainer = Slate10,
    tertiary             = Indigo40,
    onTertiary           = Color.White,
    tertiaryContainer    = Indigo90,
    onTertiaryContainer  = Indigo10,
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    background           = CoolBg,
    onBackground         = Slate10,
    surface              = CoolSurface,
    onSurface            = Slate10,
    surfaceVariant       = CoolSurfVar,
    onSurfaceVariant     = Slate40,
    outline              = CoolOutline,
    outlineVariant       = CoolOutlineVar,
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
