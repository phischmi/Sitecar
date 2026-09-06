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
    val notes: String? = null,
    val content: String? = null,
)

/**
 * Eigener Body nur für das Dokumentdatum: [documentDate] hat bewusst keinen
 * Default, damit ein null auch wirklich als `null` im JSON landet und das Datum
 * damit gelöscht wird — [UpdateDocumentBody] ließe das Feld schlicht weg.
 */
@Serializable
data class UpdateDocumentDateBody(
    val documentDate: String?,
)

@Serializable
data class UpdateDocumentResponse(
    val document: DocumentDto,
)

/**
 * Das einzelne Dokument samt Feldern, die die Listen-Antwort nicht mitliefert
 * (extrahierter Text, Notiz, Dokumentdatum) — siehe Detailansicht.
 */
@Serializable
data class DocumentDetailDto(
    val id: String,
    val name: String? = null,
    val originalName: String? = null,
    val mimeType: String? = null,
    val originalSize: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val documentDate: String? = null,
    val content: String? = null,
    val notes: String? = null,
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class DocumentResponse(
    val document: DocumentDetailDto,
)

/**
 * Ein Eintrag der Änderungshistorie. [event] ist einer der Papra-Werte
 * "created", "updated", "deleted", "restored", "tagged", "untagged"; Nutzer und
 * Tag liefert der Server nur, solange beide noch existieren, und alle Felder
 * darin können einzeln null sein.
 */
@Serializable
data class DocumentActivityDto(
    val id: String,
    val createdAt: String? = null,
    val event: String = "",
    val eventData: DocumentActivityEventData? = null,
    val user: DocumentActivityUserDto? = null,
    val tag: DocumentActivityTagDto? = null,
)

@Serializable
data class DocumentActivityEventData(
    val updatedFields: List<String> = emptyList(),
)

@Serializable
data class DocumentActivityUserDto(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class DocumentActivityTagDto(
    val id: String? = null,
    val name: String? = null,
    val color: String? = null,
)

@Serializable
data class DocumentActivitiesResponse(
    val activities: List<DocumentActivityDto> = emptyList(),
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
