package app.sitecar.client

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.sitecar.client.data.AccentColor
import app.sitecar.client.data.PendingUpload
import app.sitecar.client.data.ShareIntentHandler
import app.sitecar.client.data.ThemeMode
import app.sitecar.client.ui.documents.DocumentsScreen
import app.sitecar.client.ui.documents.TagsScreen
import app.sitecar.client.ui.documents.TrashScreen
import app.sitecar.client.ui.nav.AppBottomBar
import app.sitecar.client.ui.nav.Route
import app.sitecar.client.ui.scan.ScanScreen
import app.sitecar.client.ui.settings.SettingsScreen
import app.sitecar.client.ui.theme.SitecarTheme
import app.sitecar.client.ui.upload.UploadScreen

class MainActivity : ComponentActivity() {
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SitecarApp
        val startFromScanShortcut = intent?.getStringExtra(EXTRA_SHORTCUT_ROUTE) == SHORTCUT_ROUTE_SCAN
        val startFromShare = consumeShareIntent(intent, app)

        setContent {
            val themeMode by app.settingsStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val accentColor by app.settingsStore.accentColorFlow.collectAsState(initial = AccentColor.INDIGO)

            // Die Icon-Farbe der System-Leisten richtet sich nach dem in der App
            // gewählten Theme, nicht nach dem des Systems — sonst stünden z. B. bei
            // "Dunkel" auf einem hellen System dunkle Icons auf dunklem Grund.
            SideEffect {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            SitecarTheme(darkTheme = darkTheme, accentColor = accentColor) {
                val nav = rememberNavController()
                navController = nav
                val configured by app.settingsStore.isConfigured.collectAsState(initial = null)

                val start = when (configured) {
                    null -> null
                    true -> when {
                        startFromShare -> Route.Upload
                        startFromScanShortcut -> Route.Scan
                        else -> Route.Documents
                    }
                    false -> Route.Settings
                }

                if (start != null) {
                    NavHost(navController = nav, startDestination = start) {
                        composable<Route.Settings> {
                            SettingsScreen(
                                store = app.settingsStore,
                                client = app.apiClient,
                                billing = app.billingManager,
                                onSaved = {
                                    val target = if (app.pendingUpload != null) Route.Upload else Route.Documents
                                    nav.navigate(target) {
                                        popUpTo<Route.Settings> { inclusive = true }
                                    }
                                },
                                onBack = { if (!nav.popBackStack()) finish() },
                            )
                        }
                        composable<Route.Scan> {
                            ScanScreen(
                                onScanned = { pages ->
                                    app.pendingUpload = PendingUpload.Images(pages)
                                    nav.navigate(Route.Upload)
                                },
                                onOpenSettings = { nav.navigate(Route.Settings) },
                                bottomBar = { AppBottomBar(nav) },
                            )
                        }
                        composable<Route.Documents> {
                            DocumentsScreen(
                                client = app.apiClient,
                                store = app.settingsStore,
                                onOpenSettings = { nav.navigate(Route.Settings) },
                                bottomBar = { AppBottomBar(nav) },
                                initialSearchQuery = app.pendingDocumentSearchQuery,
                                onConsumeInitialSearchQuery = { app.pendingDocumentSearchQuery = null },
                            )
                        }
                        composable<Route.Tags> {
                            TagsScreen(
                                client = app.apiClient,
                                store = app.settingsStore,
                                onOpenSettings = { nav.navigate(Route.Settings) },
                                onFilterByTag = { query ->
                                    app.pendingDocumentSearchQuery = query
                                    nav.navigate(Route.Documents)
                                },
                                bottomBar = { AppBottomBar(nav) },
                            )
                        }
                        composable<Route.Trash> {
                            TrashScreen(
                                client = app.apiClient,
                                store = app.settingsStore,
                                onOpenSettings = { nav.navigate(Route.Settings) },
                                bottomBar = { AppBottomBar(nav) },
                            )
                        }
                        composable<Route.Upload> {
                            UploadScreen(
                                pendingUpload = app.pendingUpload,
                                client = app.apiClient,
                                pdfBuilder = app.pdfBuilder,
                                store = app.settingsStore,
                                billing = app.billingManager,
                                recentUploads = app.recentUploadsStore,
                                onDone = {
                                    app.pendingUpload = null
                                    if (!nav.popBackStack(Route.Scan, inclusive = false)) {
                                        nav.navigate(Route.Documents) {
                                            popUpTo(nav.graph.id) { inclusive = true }
                                        }
                                    }
                                },
                                onBack = {
                                    app.pendingUpload = null
                                    if (!nav.popBackStack()) finish()
                                },
                                onOpenSettings = { nav.navigate(Route.Settings) },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as SitecarApp
        if (consumeShareIntent(intent, app)) {
            // Ist der Server noch nicht konfiguriert, bleibt der Screen auf
            // Settings; SettingsScreen.onSaved navigiert dann selbst zu Upload,
            // sobald app.pendingUpload gesetzt ist.
            val isConfigured = app.settingsStore.serverUrl.isNotBlank() && app.settingsStore.apiKey.isNotBlank()
            if (isConfigured) {
                navController?.navigate(Route.Upload) { launchSingleTop = true }
            }
            return
        }
        if (intent.getStringExtra(EXTRA_SHORTCUT_ROUTE) == SHORTCUT_ROUTE_SCAN) {
            navController?.navigate(Route.Scan) { launchSingleTop = true }
        }
    }

    /** Übernimmt einen per Android-Share-Sheet empfangenen Intent als [PendingUpload]. */
    private fun consumeShareIntent(intent: Intent?, app: SitecarApp): Boolean {
        val pending = ShareIntentHandler.parse(this, intent) ?: return false
        app.pendingUpload = pending
        return true
    }

    companion object {
        const val EXTRA_SHORTCUT_ROUTE = "shortcut_route"
        const val SHORTCUT_ROUTE_SCAN = "scan"
    }
}
