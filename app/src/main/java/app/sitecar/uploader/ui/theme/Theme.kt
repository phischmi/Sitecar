package app.sitecar.uploader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.sitecar.uploader.data.AccentColor

private data class PrimaryTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

// INDIGO ist der kostenlose Standard, bewusst nicht Papras Markenfarbe, sondern
// jeweils eine nahezu komplementäre Farbe – harmoniert mit Papra, statt dessen
// Farbe zu duplizieren. EMERALD/AMBER/ROSE sind rein kosmetische
// Unterstützer-Akzente, gleiche Bau-Logik wie Indigo. Töne orientieren sich an
// gängigen modernen UI-Paletten (kräftiger, satter) statt an den blasseren
// Pastelltönen der Vorgängerversion; alle primary/onPrimary-Paarungen liegen
// bei Kontrast ≥ 5:1 (WCAG AA).
private val lightPrimaryTokens: Map<AccentColor, PrimaryTokens> = mapOf(
    AccentColor.INDIGO to PrimaryTokens(
        primary = Color(0xFF4F46E5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0E7FF),
        onPrimaryContainer = Color(0xFF312E81),
    ),
    AccentColor.EMERALD to PrimaryTokens(
        primary = Color(0xFF047857),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1FAE5),
        onPrimaryContainer = Color(0xFF022C22),
    ),
    AccentColor.AMBER to PrimaryTokens(
        primary = Color(0xFFB45309),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF78350F),
    ),
    AccentColor.ROSE to PrimaryTokens(
        primary = Color(0xFFBE123C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE4E6),
        onPrimaryContainer = Color(0xFF881337),
    ),
    // Papras eigenes Orange (Hellmodus), siehe Kommentar oben.
    AccentColor.PAPRA to PrimaryTokens(
        primary = Color(0xFFC2410C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE0CC),
        onPrimaryContainer = Color(0xFF431407),
    ),
)

private val darkPrimaryTokens: Map<AccentColor, PrimaryTokens> = mapOf(
    AccentColor.INDIGO to PrimaryTokens(
        primary = Color(0xFF7A90FF),
        onPrimary = Color(0xFF0F173D),
        primaryContainer = Color(0xFF273268),
        onPrimaryContainer = Color(0xFFD0D7FB),
    ),
    AccentColor.EMERALD to PrimaryTokens(
        primary = Color(0xFF6EE7B7),
        onPrimary = Color(0xFF033D2C),
        primaryContainer = Color(0xFF065F46),
        onPrimaryContainer = Color(0xFFD1FAE5),
    ),
    AccentColor.AMBER to PrimaryTokens(
        primary = Color(0xFFF6C177),
        onPrimary = Color(0xFF432B06),
        primaryContainer = Color(0xFF6B4210),
        onPrimaryContainer = Color(0xFFFDE9C8),
    ),
    AccentColor.ROSE to PrimaryTokens(
        primary = Color(0xFFF48FC0),
        onPrimary = Color(0xFF4A0826),
        primaryContainer = Color(0xFF7A1245),
        onPrimaryContainer = Color(0xFFFCE0EE),
    ),
    // Papras eigenes Neon-Lime/-Gelb (Dunkelmodus), siehe Kommentar oben —
    // bewusst neonig-knallig, im Gegensatz zu den sonst eher gedeckten
    // Pastelltönen der übrigen Akzentfarben im Dunkelmodus.
    AccentColor.PAPRA to PrimaryTokens(
        primary = Color(0xFFC6FF00),
        onPrimary = Color(0xFF1A2E05),
        primaryContainer = Color(0xFF3D4B0A),
        onPrimaryContainer = Color(0xFFECFCCB),
    ),
)

// Neutrale, Papra-inspirierte Töne (Grau-Hintergründe, Warn-Orange als Tertiär) –
// gelten unabhängig von der gewählten Akzentfarbe.
private fun lightScheme(tokens: PrimaryTokens): ColorScheme = lightColorScheme(
    primary = tokens.primary,
    onPrimary = tokens.onPrimary,
    primaryContainer = tokens.primaryContainer,
    onPrimaryContainer = tokens.onPrimaryContainer,
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

private fun darkScheme(tokens: PrimaryTokens): ColorScheme = darkColorScheme(
    primary = tokens.primary,
    onPrimary = tokens.onPrimary,
    primaryContainer = tokens.primaryContainer,
    onPrimaryContainer = tokens.onPrimaryContainer,
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

/** Für die Farb-Vorschau (Swatches) in den Einstellungen. */
fun accentPrimaryColor(accentColor: AccentColor, darkTheme: Boolean): Color =
    (if (darkTheme) darkPrimaryTokens else lightPrimaryTokens).getValue(accentColor).primary

@Composable
fun SitecarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.INDIGO,
    content: @Composable () -> Unit,
) {
    val tokens = (if (darkTheme) darkPrimaryTokens else lightPrimaryTokens).getValue(accentColor)
    val scheme = if (darkTheme) darkScheme(tokens) else lightScheme(tokens)
    MaterialTheme(colorScheme = scheme, content = content)
}
