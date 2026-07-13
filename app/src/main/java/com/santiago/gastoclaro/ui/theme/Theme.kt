package com.santiago.gastoclaro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = AccentGold,
    background = LightBackground,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE3ECE7),
    error = androidx.compose.ui.graphics.Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF07382B),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF255D4E),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE0FFF3),
    secondary = DarkSecondary,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF393004),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF514717),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF2B0),
    tertiary = androidx.compose.ui.graphics.Color(0xFF8FCBFF),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF003352),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF164D6F),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFD3ECFF),
    background = DarkBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE4ECE7),
    surface = DarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE7EEE9),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC1CEC7),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF17211D),
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF2C3933),
    outline = androidx.compose.ui.graphics.Color(0xFF85958D),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF3E4D46),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6)
)

@Composable
fun GastoClaroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
