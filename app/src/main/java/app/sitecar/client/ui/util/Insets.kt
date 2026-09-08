package app.sitecar.client.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Randlos heißt seit Android 15 auch, dass das Fenster im Querformat bis unter
 * die Notch reicht (LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, siehe EdgeToEdge.kt).
 * Scaffold, TopAppBar und NavigationBar berücksichtigen von Haus aus nur
 * systemBars — ohne den Cutout säße Inhalt sonst neben der Notch unter ihr.
 */
val scaffoldContentInsets: WindowInsets
    @Composable get() = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.displayCutout)

private val horizontalCutout: WindowInsets
    @Composable get() = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)

val appBarInsets: WindowInsets
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable get() = TopAppBarDefaults.windowInsets.union(horizontalCutout)

val bottomBarInsets: WindowInsets
    @Composable get() = NavigationBarDefaults.windowInsets.union(horizontalCutout)
