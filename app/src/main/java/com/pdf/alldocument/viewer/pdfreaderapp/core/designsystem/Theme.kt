package com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * Purple brand, light + dark.
 *
 * Every surface role is explicitly set. The previous scheme left ~20 of the
 * ~35 Material 3 roles at their generated defaults, which is why container and
 * tonal surfaces came out mismatched against the old red brand.
 *
 * Dark is a purple-tinted neutral ramp rather than pure grey, so the two modes
 * still read as one brand.
 */

private val LightColorScheme = lightColorScheme(
    primary = BrandPurpleDeep,
    onPrimary = SurfaceLight,
    primaryContainer = BrandPurpleContainer,
    onPrimaryContainer = OnBrandPurpleContainer,
    inversePrimary = Color(0xFFC4B0FF),

    secondary = SecondaryPurple,
    onSecondary = SurfaceLight,
    secondaryContainer = SecondaryPurpleContainer,
    onSecondaryContainer = OnSecondaryPurpleContainer,

    tertiary = Color(0xFF9B4DD6),
    onTertiary = SurfaceLight,
    tertiaryContainer = Color(0xFFF0DBFF),
    onTertiaryContainer = Color(0xFF31005A),

    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = BrandPurpleDeep,

    inverseSurface = Color(0xFF2C2A3E),
    inverseOnSurface = Color(0xFFF3F0FA),

    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF8FF),
    surfaceContainer = Color(0xFFF4F1FB),
    surfaceContainerHigh = Color(0xFFEEEAF7),
    surfaceContainerHighest = Color(0xFFE7E3F2),

    outline = Color(0xFF7C7790),
    outlineVariant = Color(0xFFDCD8E8),

    error = ErrorLight,
    onError = SurfaceLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC4B0FF),
    onPrimary = Color(0xFF2A1B5E),
    primaryContainer = Color(0xFF4A2FB8),
    onPrimaryContainer = Color(0xFFE7DEFF),
    inversePrimary = BrandPurpleDeep,

    secondary = Color(0xFFC7BFFF),
    onSecondary = Color(0xFF2A1B5E),
    secondaryContainer = Color(0xFF41318F),
    onSecondaryContainer = Color(0xFFE6DEFF),

    tertiary = Color(0xFFDDB6FF),
    onTertiary = Color(0xFF3F0B6B),
    tertiaryContainer = Color(0xFF5A2A85),
    onTertiaryContainer = Color(0xFFF0DBFF),

    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = Color(0xFFC4B0FF),

    inverseSurface = Color(0xFFEDEBF5),
    inverseOnSurface = Color(0xFF1A1A2E),

    surfaceBright = Color(0xFF3A3651),
    surfaceContainerLowest = Color(0xFF0F0D18),
    surfaceContainerLow = Color(0xFF1C1930),
    surfaceContainer = Color(0xFF211D38),
    surfaceContainerHigh = Color(0xFF2C2742),
    surfaceContainerHighest = Color(0xFF37314D),

    outline = Color(0xFF938FAC),
    outlineVariant = Color(0xFF474366),

    error = ErrorDark,
    onError = Color(0xFF601410),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    scrim = Color(0xFF000000)
)

@Composable
fun DocumentReaderTheme(
    // Always force light theme — the design is white/light only.
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use the light color scheme regardless of system setting.
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // The app is light-only, so both bars get dark icons on a
                // transparent background. Do NOT set statusBarColor /
                // navigationBarColor here: that fights enableEdgeToEdge() and is
                // ignored outright on API 35+.
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
