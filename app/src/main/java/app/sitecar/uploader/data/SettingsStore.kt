package app.sitecar.uploader.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/** INDIGO ist der kostenlose Standard, die übrigen sind Unterstützer-Akzente. */
enum class AccentColor {
    INDIGO, EMERALD, AMBER, ROSE
}

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "sitecar_settings",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var defaultOrgId: String
        get() = prefs.getString(KEY_DEFAULT_ORG, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_DEFAULT_ORG, value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME_MODE, null) }
            ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var onDeviceOcrEnabled: Boolean
        get() = prefs.getBoolean(KEY_ON_DEVICE_OCR, true)
        set(value) = prefs.edit().putBoolean(KEY_ON_DEVICE_OCR, value).apply()

    /** Tag-/Datums-/Fristen-Vorschläge aus dem OCR-Text beim Hochladen (regelbasiert, on-device). */
    var smartInsightsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_INSIGHTS, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_INSIGHTS, value).apply()

    /** Lebenszeit-Zähler erfolgreicher Uploads, steuert den Spenden-Hinweis. */
    var uploadCount: Int
        get() = prefs.getInt(KEY_UPLOAD_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_UPLOAD_COUNT, value).apply()

    /** True, sobald die einmalige Unterstützer-Freischaltung gekauft wurde. */
    var isSupporter: Boolean
        get() = prefs.getBoolean(KEY_IS_SUPPORTER, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SUPPORTER, value).apply()

    var filenameTemplate: String
        get() = prefs.getString(KEY_FILENAME_TEMPLATE, null)?.takeIf { it.isNotBlank() }
            ?: FilenameTemplate.DEFAULT
        set(value) = prefs.edit()
            .putString(KEY_FILENAME_TEMPLATE, value.ifBlank { FilenameTemplate.DEFAULT })
            .apply()

    var accentColor: AccentColor
        get() = AccentColor.entries.firstOrNull { it.name == prefs.getString(KEY_ACCENT_COLOR, null) }
            ?: AccentColor.INDIGO
        set(value) = prefs.edit().putString(KEY_ACCENT_COLOR, value.name).apply()

    val isConfigured: Flow<Boolean> = callbackFlow {
        val send = { trySend(serverUrl.isNotBlank() && apiKey.isNotBlank()) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> send() }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val themeModeFlow: Flow<ThemeMode> = callbackFlow {
        val send = { trySend(themeMode) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) send()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isSupporterFlow: Flow<Boolean> = callbackFlow {
        val send = { trySend(isSupporter) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_SUPPORTER) send()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val accentColorFlow: Flow<AccentColor> = callbackFlow {
        val send = { trySend(accentColor) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ACCENT_COLOR) send()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_ORG = "default_org"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ON_DEVICE_OCR = "on_device_ocr_enabled"
        private const val KEY_SMART_INSIGHTS = "smart_insights_enabled"
        private const val KEY_UPLOAD_COUNT = "upload_count"
        private const val KEY_IS_SUPPORTER = "is_supporter"
        private const val KEY_FILENAME_TEMPLATE = "filename_template"
        private const val KEY_ACCENT_COLOR = "accent_color"
    }
}
