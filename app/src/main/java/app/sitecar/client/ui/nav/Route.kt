package app.sitecar.client.ui.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Scan : Route
    @Serializable data object Documents : Route
    @Serializable data object Tags : Route
    @Serializable data object Trash : Route
    @Serializable data object Upload : Route
    @Serializable data object Settings : Route
    @Serializable data object Onboarding : Route

    @Serializable
    data class DocumentDetails(
        val organizationId: String,
        val documentId: String,
        val documentName: String? = null,
    ) : Route
}
