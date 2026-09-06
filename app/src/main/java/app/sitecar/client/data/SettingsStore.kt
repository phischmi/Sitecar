package app.sitecar.client.data

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

enum class AccentColor {
    INDIGO, EMERALD, AMBER, ROSE, PAPRA
}

/** Aktion, die eine Wischgeste in der Dokumentenliste auslöst. */
enum class SwipeAction {
    NONE, DELETE, RENAME, EDIT_TAGS, DETAILS
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

    var smartInsightsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_INSIGHTS, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_INSIGHTS, value).apply()

    // Ergänzt die regelbasierten Vorschläge um on-device KI (Gemini Nano). Aus,
    // da ein einmaliger geräteweiter Modell-Download nötig ist und nicht jedes
    // Gerät Gemini Nano unterstützt.
    var onDeviceAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_ON_DEVICE_AI, false)
        set(value) = prefs.edit().putBoolean(KEY_ON_DEVICE_AI, value).apply()

    // Opt-in: Sitecar als Ziel im Android-Share-Sheet anderer Apps anbieten.
    var shareIntentEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHARE_INTENT, false)
        set(value) = prefs.edit().putBoolean(KEY_SHARE_INTENT, value).apply()

    var uploadCount: Int
        get() = prefs.getInt(KEY_UPLOAD_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_UPLOAD_COUNT, value).apply()

    var isSupporter: Boolean
        get() = prefs.getBoolean(KEY_IS_SUPPORTER, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SUPPORTER, value).apply()

    /** Erster App-Start (ms seit Epoch), 0 solange nie gesetzt — siehe [SitecarApp]. */
    var firstLaunchAtMillis: Long
        get() = prefs.getLong(KEY_FIRST_LAUNCH_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_FIRST_LAUNCH_AT, value).apply()

    /** Zeitpunkt der letzten Play-Bewertungsanfrage, 0 wenn noch nie gefragt. */
    var reviewPromptedAtMillis: Long
        get() = prefs.getLong(KEY_REVIEW_PROMPTED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_REVIEW_PROMPTED_AT, value).apply()

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

    /**
     * Ob der Einstieg (Begrüßung, Verbindung, Tour) durchlaufen wurde. Verhindert,
     * dass jemand nach dem Löschen seiner Zugangsdaten erneut begrüßt wird — dann
     * führt der Weg wieder direkt in die Einstellungen.
     */
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    /** Wischgeste von links nach rechts in der Dokumentenliste. */
    var swipeStartToEndAction: SwipeAction
        get() = SwipeAction.entries.firstOrNull { it.name == prefs.getString(KEY_SWIPE_START_TO_END, null) }
            ?: SwipeAction.EDIT_TAGS
        set(value) = prefs.edit().putString(KEY_SWIPE_START_TO_END, value.name).apply()

    /** Wischgeste von rechts nach links in der Dokumentenliste. */
    var swipeEndToStartAction: SwipeAction
        get() = SwipeAction.entries.firstOrNull { it.name == prefs.getString(KEY_SWIPE_END_TO_START, null) }
            ?: SwipeAction.DELETE
        set(value) = prefs.edit().putString(KEY_SWIPE_END_TO_START, value.name).apply()

    // Liest den Wert per [read] neu aus, sobald sich [watchedKey] ändert (oder
    // bei beliebiger Änderung, falls null).
    private fun <T> prefFlow(watchedKey: String?, read: () -> T): Flow<T> = callbackFlow {
        val send = { trySend(read()) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (watchedKey == null || key == watchedKey) send()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isConfigured: Flow<Boolean> = prefFlow(watchedKey = null) {
        serverUrl.isNotBlank() && apiKey.isNotBlank()
    }
    val themeModeFlow: Flow<ThemeMode> = prefFlow(KEY_THEME_MODE) { themeMode }
    val isSupporterFlow: Flow<Boolean> = prefFlow(KEY_IS_SUPPORTER) { isSupporter }
    val accentColorFlow: Flow<AccentColor> = prefFlow(KEY_ACCENT_COLOR) { accentColor }
    val swipeStartToEndActionFlow: Flow<SwipeAction> = prefFlow(KEY_SWIPE_START_TO_END) { swipeStartToEndAction }
    val swipeEndToStartActionFlow: Flow<SwipeAction> = prefFlow(KEY_SWIPE_END_TO_START) { swipeEndToStartAction }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_ORG = "default_org"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ON_DEVICE_OCR = "on_device_ocr_enabled"
        private const val KEY_SMART_INSIGHTS = "smart_insights_enabled"
        private const val KEY_ON_DEVICE_AI = "on_device_ai_enabled"
        private const val KEY_SHARE_INTENT = "share_intent_enabled"
        private const val KEY_UPLOAD_COUNT = "upload_count"
        private const val KEY_IS_SUPPORTER = "is_supporter"
        private const val KEY_FIRST_LAUNCH_AT = "first_launch_at"
        private const val KEY_REVIEW_PROMPTED_AT = "review_prompted_at"
        private const val KEY_FILENAME_TEMPLATE = "filename_template"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_SWIPE_START_TO_END = "swipe_start_to_end_action"
        private const val KEY_SWIPE_END_TO_START = "swipe_end_to_start_action"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
