package app.papra.uploader

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
import app.papra.uploader.data.ThemeMode
import app.papra.uploader.ui.documents.DocumentsScreen
import app.papra.uploader.ui.nav.AppBottomBar
import app.papra.uploader.ui.nav.Route
import app.papra.uploader.ui.scan.ScanScreen
import app.papra.uploader.ui.settings.SettingsScreen
import app.papra.uploader.ui.theme.PapraTheme
import app.papra.uploader.ui.upload.UploadScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PapraApp

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
                    true -> Route.Documents
                    false -> Route.Settings
                }

                if (start != null) {
                    NavHost(navController = nav, startDestination = start) {
                        composable<Route.Settings> {
                            SettingsScreen(
                                store = app.settingsStore,
                                client = app.papraClient,
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
}
