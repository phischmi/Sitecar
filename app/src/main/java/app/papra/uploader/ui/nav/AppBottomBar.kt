package app.papra.uploader.ui.nav

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.papra.uploader.R

@Composable
fun AppBottomBar(nav: NavHostController) {
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
