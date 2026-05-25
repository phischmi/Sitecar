package app.papra.uploader.ui.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Scan : Route
    @Serializable data object Upload : Route
    @Serializable data object Settings : Route
}
