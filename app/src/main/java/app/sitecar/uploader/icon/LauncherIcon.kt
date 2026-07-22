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
    fun apply(context: Context, accentColor: AccentColor) {
        val pm = context.packageManager
        AccentColor.entries.forEach { candidate ->
            val alias = ComponentName(context, aliasClassName(context, candidate))
            val state = if (candidate == accentColor) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        }
    }

    private fun aliasClassName(context: Context, accentColor: AccentColor): String {
        val pkg = context.packageName
        val suffix = when (accentColor) {
            AccentColor.INDIGO -> "LauncherIndigo"
            AccentColor.EMERALD -> "LauncherEmerald"
            AccentColor.AMBER -> "LauncherAmber"
            AccentColor.ROSE -> "LauncherRose"
        }
        return "$pkg.$suffix"
    }
}
