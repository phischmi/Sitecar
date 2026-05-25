package app.papra.uploader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modernes Pistazien-Grün, generiert aus Source-Hue ~95° via Material-3-Tonal-Palette.
private val LightColors = lightColorScheme(
    primary = Color(0xFF4D6731),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCEEFAA),
    onPrimaryContainer = Color(0xFF102000),
    secondary = Color(0xFF586249),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C8),
    onSecondaryContainer = Color(0xFF161E0B),
    tertiary = Color(0xFF386663),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBECE7),
    onTertiaryContainer = Color(0xFF00201E),
    background = Color(0xFFFBFCF1),
    onBackground = Color(0xFF1B1C14),
    surface = Color(0xFFFBFCF1),
    onSurface = Color(0xFF1B1C14),
    surfaceVariant = Color(0xFFE0E4D0),
    onSurfaceVariant = Color(0xFF44483B),
    outline = Color(0xFF75786A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4D292),
    onPrimary = Color(0xFF203600),
    primaryContainer = Color(0xFF364F1B),
    onPrimaryContainer = Color(0xFFCEEFAA),
    secondary = Color(0xFFC0CBAD),
    onSecondary = Color(0xFF2B341E),
    secondaryContainer = Color(0xFF414A33),
    onSecondaryContainer = Color(0xFFDCE7C8),
    tertiary = Color(0xFFA0D0CB),
    onTertiary = Color(0xFF003734),
    tertiaryContainer = Color(0xFF1E4E4B),
    onTertiaryContainer = Color(0xFFBBECE7),
    background = Color(0xFF13140E),
    onBackground = Color(0xFFE3E3D7),
    surface = Color(0xFF13140E),
    onSurface = Color(0xFFE3E3D7),
    surfaceVariant = Color(0xFF44483B),
    onSurfaceVariant = Color(0xFFC4C8B5),
    outline = Color(0xFF8E9281),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun PapraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = scheme, content = content)
}
