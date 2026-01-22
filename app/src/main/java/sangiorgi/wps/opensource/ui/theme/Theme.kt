package sangiorgi.wps.opensource.ui.theme

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

/**
 * Dark Color Scheme - Cybersecurity focused theme
 * Uses deep dark backgrounds with cyan accents for a professional security tool feel
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors - Cyber Cyan
    primary = CyberCyan80,
    onPrimary = CyberCyan20,
    primaryContainer = CyberCyan30,
    onPrimaryContainer = CyberCyan90,

    // Secondary colors - Slate Blue
    secondary = SlateBlue80,
    onSecondary = SlateBlue20,
    secondaryContainer = SlateBlue30,
    onSecondaryContainer = SlateBlue90,

    // Tertiary colors - Electric Green
    tertiary = ElectricGreen80,
    onTertiary = ElectricGreen20,
    tertiaryContainer = ElectricGreen30,
    onTertiaryContainer = ElectricGreen90,

    // Error colors
    error = ErrorRed80,
    onError = ErrorRed20,
    errorContainer = ErrorRed30,
    onErrorContainer = ErrorRed90,

    // Background and Surface
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,

    // Outline and inverse
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = CyberCyan40,

    // Scrim
    scrim = Color.Black,
)

/**
 * Light Color Scheme - Clean professional look
 * For users who prefer light mode while maintaining the security tool aesthetic
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors - Cyber Cyan
    primary = CyberCyan40,
    onPrimary = CyberCyan100,
    primaryContainer = CyberCyan90,
    onPrimaryContainer = CyberCyan10,

    // Secondary colors - Slate Blue
    secondary = SlateBlue40,
    onSecondary = SlateBlue100,
    secondaryContainer = SlateBlue90,
    onSecondaryContainer = SlateBlue10,

    // Tertiary colors - Electric Green
    tertiary = ElectricGreen40,
    onTertiary = ElectricGreen100,
    tertiaryContainer = ElectricGreen90,
    onTertiaryContainer = ElectricGreen10,

    // Error colors
    error = ErrorRed40,
    onError = ErrorRed100,
    errorContainer = ErrorRed90,
    onErrorContainer = ErrorRed10,

    // Background and Surface
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,

    // Outline and inverse
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = CyberCyan80,

    // Scrim
    scrim = Color.Black,
)

@Composable
fun WIFIWPSWPATESTEROPENSOURCETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Set to false to use our custom cybersecurity theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update status bar color to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
