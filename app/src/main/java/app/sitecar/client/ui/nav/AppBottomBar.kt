package app.sitecar.client.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.sitecar.client.R

@Composable
fun AppBottomBar(nav: NavHostController) {
    val currentDestination = nav.currentBackStackEntryAsState().value?.destination
    val tabs = listOf(
        Triple(Route.Scan, Icons.Default.DocumentScanner, R.string.nav_scan),
        Triple(Route.Documents, Icons.Default.Description, R.string.nav_documents),
        Triple(Route.Tags, Icons.Default.Sell, R.string.nav_tags),
        Triple(Route.Trash, Icons.Default.Delete, R.string.nav_trash),
    )

    NavigationBar {
        tabs.forEach { (route, icon, label) ->
            // Über den Serializer-Hash vergleichen, nicht über Routen-Strings:
            // destination.route ist der von kotlinx.serialization erzeugte
            // serialName (ein konstanter String), qualifiedName dagegen der
            // Laufzeitname der Klasse — den R8 im Release-Build umbenennt.
            val selected = currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
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
