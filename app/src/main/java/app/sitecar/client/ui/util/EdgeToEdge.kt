package app.sitecar.client.ui.util

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Ersatz für androidx' enableEdgeToEdge(): dessen Implementierung ruft auf jeder
 * API-Stufe Window.setStatusBarColor/setNavigationBarColor auf — seit Android 15
 * wirkungslos und von der Play Console als veraltet gemeldet (bis einschließlich
 * activity 1.13.0 unverändert enthalten, ein Upgrade hilft also nicht).
 *
 * Die App übergab dort ohnehin durchgehend transparente Scrims; die Leisten
 * bleiben unterhalb von API 35 über android:statusBarColor/navigationBarColor in
 * values/themes.xml transparent, ab API 35 zeichnet das System selbst nichts mehr.
 * Übrig bleiben damit das Abschalten des Insets-Fittings und der Icon-Kontrast.
 */
fun ComponentActivity.applyEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }
    // Auf API 28/29 bliebe nur LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES, das
    // seit Android 15 ebenfalls veraltet ist. Es wird bewusst nicht gesetzt:
    // dort bleibt im Querformat der Bereich neben der Notch ungenutzt.
}

/**
 * Icon-Farbe der System-Leisten. Richtet sich nach dem in der App gewählten
 * Theme, nicht nach dem des Systems — sonst stünden z. B. bei "Dunkel" auf einem
 * hellen System dunkle Icons auf dunklem Grund.
 */
fun ComponentActivity.applySystemBarIconContrast(darkTheme: Boolean) {
    WindowInsetsControllerCompat(window, window.decorView).run {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
    }
}
