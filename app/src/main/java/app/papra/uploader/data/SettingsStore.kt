package app.papra.uploader.data

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

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "papra_settings",
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

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_ORG = "default_org"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
