package app.sitecar.uploader.share

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Schaltet die Sichtbarkeit von Sitecar im Android-Share-Sheet anderer Apps
 * per Component-Alias an/aus (siehe AndroidManifest.xml, `.ShareReceiver`) —
 * Opt-in, siehe SettingsStore.shareIntentEnabled. DONT_KILL_APP verhindert,
 * dass die laufende App dabei beendet wird.
 */
object ShareReceiver {
    // Siehe LauncherIcon.MANIFEST_PACKAGE: darf nicht aus context.packageName
    // gebildet werden, weicht im Debug-Build wegen applicationIdSuffix ab.
    private const val MANIFEST_PACKAGE = "app.sitecar.uploader"
    private const val ALIAS_NAME = "$MANIFEST_PACKAGE.ShareReceiver"

    fun setEnabled(context: Context, enabled: Boolean) {
        val alias = ComponentName(context.packageName, ALIAS_NAME)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
    }
}
