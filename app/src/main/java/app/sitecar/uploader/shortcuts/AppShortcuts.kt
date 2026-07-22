package app.sitecar.uploader.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.sitecar.uploader.MainActivity
import app.sitecar.uploader.R

/**
 * Dynamischer Long-Press-Shortcut "Scannen starten" — Unterstützer-Perk.
 * Braucht keine Manifest-Deklaration (nur statische Shortcuts bräuchten das).
 */
object AppShortcuts {
    private const val SHORTCUT_ID_SCAN = "quick_scan"

    fun sync(context: Context, enabled: Boolean) {
        if (!enabled) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(SHORTCUT_ID_SCAN))
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_SHORTCUT_ROUTE, MainActivity.SHORTCUT_ROUTE_SCAN)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID_SCAN)
            .setShortLabel(context.getString(R.string.shortcut_scan_short_label))
            .setLongLabel(context.getString(R.string.shortcut_scan_long_label))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_scan))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }
}
