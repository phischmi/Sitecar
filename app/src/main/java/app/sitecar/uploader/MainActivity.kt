package app.sitecar.uploader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.sitecar.uploader.data.ThemeMode
import app.sitecar.uploader.ui.documents.DocumentsScreen
import app.sitecar.uploader.ui.nav.AppBottomBar
import app.sitecar.uploader.ui.nav.Route
import app.sitecar.uploader.ui.scan.ScanScreen
import app.sitecar.uploader.ui.settings.SettingsScreen
import app.sitecar.uploader.ui.theme.PapraTheme
import app.sitecar.uploader.ui.upload.UploadScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PapraApp
        val startFromScanShortcut = intent?.getStringExtra(EXTRA_SHORTCUT_ROUTE) == SHORTCUT_ROUTE_SCAN

        setContent {
            val themeMode by app.settingsStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            PapraTheme(darkTheme = darkTheme) {
                val nav = rememberNavController()
                val configured by app.settingsStore.isConfigured.collectAsState(initial = null)

                val start = when (configured) {
                    null -> null
                    true -> if (startFromScanShortcut) Route.Scan else Route.Documents
                    false -> Route.Settings
                }

                if (start != null) {
                    NavHost(navController = nav, startDestination = start) {
                        composable<Route.Settings> {
                            SettingsScreen(
                                store = app.settingsStore,
                                client = app.papraClient,
                                billing = app.billingManager,
                                onSaved = {
                                    nav.navigate(Route.Documents) {
                                        popUpTo<Route.Settings> { inclusive = true }
                                    }
                                },
                                onBack = { if (!nav.popBackStack()) finish() },
                            )
                        }
                        composable<Route.Scan> {
                            ScanScreen(
                                onScanned = { pages ->
                                    app.currentScanPages = pages
                                    nav.navigate(Route.Upload)
                                },
                                onOpenSettings = { nav.navigate(Route.Settings) },
                                bottomBar = { AppBottomBar(nav) },
                            )
                        }
                        composable<Route.Documents> {
                            DocumentsScreen(
                                client = app.papraClient,
                                store = app.settingsStore,
                                onOpenSettings = { nav.navigate(Route.Settings) },
                                bottomBar = { AppBottomBar(nav) },
                            )
                        }
                        composable<Route.Upload> {
                            UploadScreen(
                                pages = app.currentScanPages,
                                client = app.papraClient,
                                pdfBuilder = app.pdfBuilder,
                                store = app.settingsStore,
                                billing = app.billingManager,
                                onDone = {
                                    app.currentScanPages = emptyList()
                                    nav.popBackStack(Route.Scan, inclusive = false)
                                },
                                onBack = { nav.popBackStack() },
                                onOpenSettings = { nav.navigate(Route.Settings) },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SHORTCUT_ROUTE = "shortcut_route"
        const val SHORTCUT_ROUTE_SCAN = "scan"
    }
}
