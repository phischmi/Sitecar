package app.sitecar.client.ui.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Scan : Route
    @Serializable data object Documents : Route
    @Serializable data object Tags : Route
    @Serializable data object Trash : Route
    @Serializable data object Upload : Route
    @Serializable data object Settings : Route
}
