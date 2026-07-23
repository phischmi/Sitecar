package app.sitecar.uploader.ui.util

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Setzt FLAG_SECURE, solange die aufrufende Composable sichtbar ist — verhindert
 * Screenshots/Bildschirmaufnahmen und blendet den Inhalt im Übersichts-
 * ("Letzte Apps")-Vorschaubild aus. Für Screens mit sensiblen Klartext-Daten
 * (Server-URL, API-Key) statt global für die ganze App, damit z. B. Screenshots
 * des Dokumente-Tabs weiterhin möglich bleiben.
 */
@Composable
fun SecureScreen() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
