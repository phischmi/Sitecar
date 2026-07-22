package app.sitecar.uploader.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.sitecar.uploader.data.AccentColor

/**
 * Schaltet zwischen den Launcher-Icon-Varianten um (eine activity-alias pro
 * Akzentfarbe, siehe AndroidManifest.xml) — nur eine ist je Zeitpunkt aktiv.
 * DONT_KILL_APP verhindert, dass die laufende App dabei beendet wird.
 */
object LauncherIcon {
    // Fester Namespace der activity-alias-Einträge im Manifest (siehe
    // AndroidManifest.xml und namespace in build.gradle.kts). Darf NICHT aus
    // context.packageName gebildet werden: im Debug-Build weicht das wegen
    // applicationIdSuffix ".debug" vom Namespace ab, gegen den relative
    // android:name-Angaben im Manifest aufgelöst werden.
    private const val MANIFEST_PACKAGE = "app.sitecar.uploader"

    fun apply(context: Context, accentColor: AccentColor) {
        val pm = context.packageManager
        AccentColor.entries.forEach { candidate ->
            val alias = ComponentName(context.packageName, aliasClassName(candidate))
            val state = if (candidate == accentColor) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        }
    }

    private fun aliasClassName(accentColor: AccentColor): String {
        val suffix = when (accentColor) {
            AccentColor.INDIGO -> "LauncherIndigo"
            AccentColor.EMERALD -> "LauncherEmerald"
            AccentColor.AMBER -> "LauncherAmber"
            AccentColor.ROSE -> "LauncherRose"
        }
        return "$MANIFEST_PACKAGE.$suffix"
    }
}
