package app.papra.uploader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette an Papra angelehnt (neutrale Grau-Hintergründe, Warn-Orange als Tertiär).
// Primärfarbe bewusst nicht Papras Markenfarbe, sondern jeweils eine nahezu komplementäre Farbe –
// harmoniert mit Papra, statt dessen Farbe zu duplizieren:
//  - Dark Mode: Papras Neon-Lime (hue 77°) -> Indigo-Blau (hue 230°)
//  - Light Mode: Papras Orange (hue ~30°) -> Azurblau (hue 201°)
private val LightColors = lightColorScheme(
    primary = Color(0xFF0369A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF082F49),
    secondary = Color(0xFF505362),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF22232B),
    tertiary = Color(0xFFBD690F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE1C5),
    onTertiaryContainer = Color(0xFF3D270F),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFF999999),
    error = Color(0xFFB81E1E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE3E3),
    onErrorContainer = Color(0xFF410B0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7A90FF),
    onPrimary = Color(0xFF0F173D),
    primaryContainer = Color(0xFF273268),
    onPrimaryContainer = Color(0xFFD0D7FB),
    secondary = Color(0xFFC6C8D2),
    onSecondary = Color(0xFF252837),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFE6E6E6),
    tertiary = Color(0xFFF6A855),
    onTertiary = Color(0xFF41270B),
    tertiaryContainer = Color(0xFF604120),
    onTertiaryContainer = Color(0xFFFCE1C5),
    background = Color(0xFF18181B),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF141415),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF737373),
    error = Color(0xFFF28C8C),
    onError = Color(0xFF3D0F0F),
    errorContainer = Color(0xFF732626),
    onErrorContainer = Color(0xFFFAD1D1),
)

@Composable
fun PapraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = scheme, content = content)
}
