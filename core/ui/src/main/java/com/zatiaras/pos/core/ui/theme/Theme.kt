package com.zatiaras.pos.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Brand500,
    onPrimary = Brand950,
    primaryContainer = Brand900,
    onPrimaryContainer = Brand100,
    secondary = Slate200,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate100,
    tertiary = Brand400,
    onTertiary = Brand100, // Fixed contrast
    background = Slate950,
    onBackground = Slate50,
    surface = Slate950,
    onSurface = Slate50,
    surfaceVariant = Slate900,
    onSurfaceVariant = Slate400,
    outline = Slate800,
    outlineVariant = Slate900,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Brand600,
    onPrimary = Color.White,
    primaryContainer = Brand50,
    onPrimaryContainer = Brand900,
    // ShadCN "Secondary" is usually a dark button in light mode, or a muted gray.
    // Here we map it to Slate for neutral actions.
    secondary = Slate900,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = Brand600,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Slate950,
    surface = Color.White,
    onSurface = Slate950,
    surfaceVariant = Slate50, // Subtle card backgrounds
    onSurfaceVariant = Slate500, // Muted text
    outline = Slate200, // Thin borders
    outlineVariant = Slate100,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ZatiarasPOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We default to FALSE for dynamic color to enforce our Brand Identity
    // Dynamic color (Material You) overrides our meticulously picked colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Clean look
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes, // Use our new ShadCN shapes
        content = content
    )
}
