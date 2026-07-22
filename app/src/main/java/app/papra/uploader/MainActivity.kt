package app.papra.uploader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.papra.uploader.data.ThemeMode
import app.papra.uploader.ui.documents.DocumentsScreen
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

@Composable
private fun AppBottomBar(nav: NavHostController) {
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = listOf(
        Triple(Route.Scan, Icons.Default.DocumentScanner, R.string.nav_scan),
        Triple(Route.Documents, Icons.Default.Description, R.string.nav_documents),
    )

    NavigationBar {
        tabs.forEach { (route, icon, label) ->
            val routeName = route::class.qualifiedName
            NavigationBarItem(
                selected = currentRoute == routeName,
                onClick = {
                    if (currentRoute != routeName) {
                        nav.navigate(route)
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(stringResource(label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
