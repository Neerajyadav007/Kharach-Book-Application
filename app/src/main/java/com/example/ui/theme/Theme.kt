package com.example.ui.theme

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

private val FinDarkColorScheme = darkColorScheme(
    primary = FinDarkPrimary,
    onPrimary = FinDarkOnPrimary,
    primaryContainer = FinDarkPrimaryContainer,
    onPrimaryContainer = FinDarkOnPrimaryContainer,
    secondary = FinDarkSecondary,
    onSecondary = FinDarkOnSecondary,
    secondaryContainer = FinDarkSecondaryContainer,
    onSecondaryContainer = FinDarkOnSecondaryContainer,
    tertiary = FinDarkTertiary,
    onTertiary = FinDarkOnTertiary,
    tertiaryContainer = FinDarkTertiaryContainer,
    onTertiaryContainer = FinDarkOnTertiaryContainer,
    error = FinDarkError,
    onError = FinDarkOnError,
    errorContainer = FinDarkErrorContainer,
    onErrorContainer = FinDarkOnErrorContainer,
    background = FinDarkSurface,
    onBackground = FinDarkOnSurface,
    surface = FinDarkSurface,
    onSurface = FinDarkOnSurface,
    surfaceVariant = FinDarkSurfaceContainerHighest,
    onSurfaceVariant = FinDarkOnSurfaceVariant,
    outline = FinDarkOutline,
    outlineVariant = FinDarkOutlineVariant,
    surfaceTint = FinDarkPrimaryContainer
)

private val FinLightColorScheme = lightColorScheme(
    primary = FinLightPrimary,
    onPrimary = FinLightOnPrimary,
    primaryContainer = FinLightPrimaryContainer,
    onPrimaryContainer = FinLightOnPrimaryContainer,
    secondary = FinLightSecondary,
    onSecondary = FinLightOnSecondary,
    secondaryContainer = FinLightSecondaryContainer,
    onSecondaryContainer = FinLightOnSecondaryContainer,
    tertiary = FinLightTertiary,
    onTertiary = FinLightOnTertiary,
    tertiaryContainer = FinLightTertiaryContainer,
    onTertiaryContainer = FinLightOnTertiaryContainer,
    error = FinLightError,
    onError = FinLightOnError,
    errorContainer = FinLightErrorContainer,
    onErrorContainer = FinLightOnErrorContainer,
    background = FinLightSurface,
    onBackground = FinLightOnSurface,
    surface = FinLightSurface,
    onSurface = FinLightOnSurface,
    surfaceVariant = FinLightSurfaceContainerHighest,
    onSurfaceVariant = FinLightOnSurfaceVariant,
    outline = FinLightOutline,
    outlineVariant = FinLightOutlineVariant,
    surfaceTint = FinLightPrimaryContainer
)

@Composable
fun FinPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FinDarkColorScheme else FinLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinPulseTypography,
        content = content
    )
}
