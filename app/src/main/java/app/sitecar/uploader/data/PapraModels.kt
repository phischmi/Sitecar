package app.sitecar.uploader.data

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
    val mimeType: String? = null,
    val originalSize: Long = 0,
    val createdAt: String? = null,
)

@Serializable
data class CreateDocumentResponse(
    val document: DocumentDto,
)

@Serializable
data class DocumentsListResponse(
    val documents: List<DocumentDto> = emptyList(),
    val documentsCount: Int = 0,
)
