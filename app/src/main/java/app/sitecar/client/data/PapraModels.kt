package app.sitecar.client.data

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
    val tags: List<TagDto> = emptyList(),
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

@Serializable
data class UpdateDocumentBody(
    val name: String? = null,
)

@Serializable
data class UpdateDocumentResponse(
    val document: DocumentDto,
)

enum class DocumentSortField(val apiValue: String) {
    CREATED_AT("createdAt"),
    NAME("name"),
}

enum class DocumentSortOrder(val apiValue: String) {
    ASC("asc"),
    DESC("desc"),
}

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val color: String,
    val description: String? = null,
    val documentsCount: Int = 0,
)

@Serializable
data class TagsResponse(
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class CreateTagBody(
    val name: String,
    val color: String,
    val description: String? = null,
)

@Serializable
data class CreateTagResponse(
    val tag: TagDto,
)

@Serializable
data class UpdateTagBody(
    val name: String,
    val color: String,
    val description: String? = null,
)

@Serializable
data class UpdateTagResponse(
    val tag: TagDto,
)

@Serializable
data class AddTagToDocumentBody(
    val tagId: String,
)
