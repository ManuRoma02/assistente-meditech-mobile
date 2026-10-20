package com.example.assistentemeditechmobile.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MeditechCyanPrimary,
    onPrimary = MeditechSurface,
    primaryContainer = MeditechCyanSoft,
    onPrimaryContainer = MeditechCyanDark,

    secondary = MeditechSupsiBlue,
    onSecondary = MeditechSurface,
    secondaryContainer = MeditechCyanUltraLight,
    onSecondaryContainer = MeditechTextPrimary,

    tertiary = MeditechTealActive,
    onTertiary = MeditechSurface,

    background = MeditechBg,
    onBackground = MeditechTextPrimary,

    surface = MeditechSurface,
    onSurface = MeditechTextPrimary,
    surfaceVariant = MeditechGroupedBg,
    onSurfaceVariant = MeditechTextSecondary,

    outline = MeditechBorder,
    outlineVariant = MeditechBorderCyan,
    error = MeditechUrgentRed,
    errorContainer = MeditechUrgentSoft
)

@Composable
fun AssistenteMeditechMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let { win ->
                WindowCompat.getInsetsController(win, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
