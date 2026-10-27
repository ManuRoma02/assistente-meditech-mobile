package com.example.assistentemeditechmobile.theme

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
import androidx.compose.ui.platform.LocalContext
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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),

    secondary = MeditechSupsiBlue,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFE0F2FE),

    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),

    background = MeditechDarkBg,
    onBackground = MeditechDarkTextPrimary,

    surface = MeditechDarkSurface,
    onSurface = MeditechDarkTextPrimary,
    surfaceVariant = MeditechDarkGroupedBg,
    onSurfaceVariant = MeditechDarkTextSecondary,

    outline = MeditechDarkBorder,
    outlineVariant = MeditechDarkBorderCyan,
    error = Color(0xFFFB7185),
    errorContainer = Color(0xFF4C0519)
)

@Composable
fun AssistenteMeditechMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let { win ->
                WindowCompat.getInsetsController(win, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
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
