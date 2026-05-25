package app.papra.uploader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F172A),
    onPrimary = Color.White,
    secondary = Color(0xFF334155),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF94A3B8),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
)

@Composable
fun PapraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
