package app.papra.uploader.data

import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val id: String,
    val name: String,
)

@Serializable
data class OrganizationsResponse(
    val organizations: List<Organization> = emptyList(),
)

@Serializable
data class DocumentDto(
    val id: String,
    val name: String? = null,
)

@Serializable
data class CreateDocumentResponse(
    val document: DocumentDto,
)
