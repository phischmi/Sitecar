package app.sitecar.client.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.sitecar.client.data.AccentColor

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
    private const val MANIFEST_PACKAGE = "app.sitecar.client"

    // Nur Akzentfarben mit eigener activity-alias/Icon-Ressource im Manifest.
    // Fehlt einer zukünftigen Akzentfarbe die Icon-Variante, fällt apply()
    // unten auf Indigo zurück, statt eine nicht existierende Alias-Komponente
    // anzusprechen (würde crashen) oder alle Aliase zu deaktivieren (App-Icon
    // würde aus dem Launcher verschwinden).
    private val ICON_ALIASES: Map<AccentColor, String> = mapOf(
        AccentColor.INDIGO to "LauncherIndigo",
        AccentColor.EMERALD to "LauncherEmerald",
        AccentColor.AMBER to "LauncherAmber",
        AccentColor.ROSE to "LauncherRose",
        AccentColor.PAPRA to "LauncherPapra",
    )

    fun apply(context: Context, accentColor: AccentColor) {
        val pm = context.packageManager
        val targetSuffix = ICON_ALIASES[accentColor] ?: ICON_ALIASES.getValue(AccentColor.INDIGO)
        ICON_ALIASES.forEach { (_, suffix) ->
            val alias = ComponentName(context.packageName, "$MANIFEST_PACKAGE.$suffix")
            val state = if (suffix == targetSuffix) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        }
    }
}
