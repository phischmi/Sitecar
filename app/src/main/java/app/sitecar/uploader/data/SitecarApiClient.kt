package app.sitecar.uploader.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File

class SitecarApiClient(private val store: SettingsStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val http = HttpClient(Android) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    suspend fun listOrganizations(
        overrideUrl: String? = null,
        overrideKey: String? = null,
    ): Result<List<Organization>> = runCatching {
        val url = (overrideUrl ?: store.serverUrl).trimEnd('/')
        val key = overrideKey ?: store.apiKey
        val res = http.get("$url/api/organizations") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $key")
                append(HttpHeaders.Accept, "application/json")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(OrganizationsResponse.serializer(), res.bodyAsText()).organizations
    }

    suspend fun uploadDocument(
        organizationId: String,
        file: File,
        fileName: String,
        mimeType: String,
    ): Result<DocumentDto> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents"
        val res = http.post(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = file.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"$fileName\"",
                                )
                            },
                        )
                    },
                ),
            )
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(CreateDocumentResponse.serializer(), res.bodyAsText()).document
    }

    suspend fun listDocuments(
        organizationId: String,
        searchQuery: String? = null,
        sortField: DocumentSortField? = null,
        sortOrder: DocumentSortOrder? = null,
        pageIndex: Int = 0,
        pageSize: Int = 50,
    ): Result<DocumentsListResponse> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents"
        val res = http.get(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
            parameter("pageIndex", pageIndex)
            parameter("pageSize", pageSize)
            if (!searchQuery.isNullOrBlank()) {
                parameter("searchQuery", searchQuery)
            }
            if (sortField != null) {
                parameter("sortField", sortField.apiValue)
            }
            if (sortOrder != null) {
                parameter("sortOrder", sortOrder.apiValue)
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(DocumentsListResponse.serializer(), res.bodyAsText())
    }

    suspend fun listDeletedDocuments(
        organizationId: String,
        pageIndex: Int = 0,
        pageSize: Int = 100,
    ): Result<DocumentsListResponse> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/deleted"
        val res = http.get(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
            parameter("pageIndex", pageIndex)
            parameter("pageSize", pageSize)
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(DocumentsListResponse.serializer(), res.bodyAsText())
    }

    suspend fun updateDocumentName(
        organizationId: String,
        documentId: String,
        name: String,
    ): Result<DocumentDto> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/$documentId"
        val res = http.patch(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(UpdateDocumentBody(name = name))
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(UpdateDocumentResponse.serializer(), res.bodyAsText()).document
    }

    suspend fun downloadDocumentFile(
        organizationId: String,
        documentId: String,
    ): Result<ByteArray> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/$documentId/file"
        val res = http.get(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        res.readBytes()
    }

    /** Papra löscht weich (Papierkorb) — die Instanz behält das Dokument dort für eine gewisse Zeit. */
    suspend fun deleteDocument(
        organizationId: String,
        documentId: String,
    ): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/$documentId"
        val res = http.delete(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }

    suspend fun restoreDocument(
        organizationId: String,
        documentId: String,
    ): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/$documentId/restore"
        val res = http.post(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }

    /** Endgültiges Löschen aus dem Papierkorb — nicht mehr rückgängig zu machen. */
    suspend fun deleteTrashDocument(
        organizationId: String,
        documentId: String,
    ): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/trash/$documentId"
        val res = http.delete(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }

    /** Leert den kompletten Papierkorb der Organisation — nicht mehr rückgängig zu machen. */
    suspend fun emptyTrash(organizationId: String): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/trash"
        val res = http.delete(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }

    suspend fun listTags(organizationId: String): Result<List<TagDto>> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/tags"
        val res = http.get(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(TagsResponse.serializer(), res.bodyAsText()).tags
    }

    suspend fun createTag(
        organizationId: String,
        name: String,
        color: String,
        description: String? = null,
    ): Result<TagDto> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/tags"
        val res = http.post(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(CreateTagBody(name = name, color = color, description = description?.takeIf { it.isNotBlank() }))
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(CreateTagResponse.serializer(), res.bodyAsText()).tag
    }

    suspend fun updateTag(
        organizationId: String,
        tagId: String,
        name: String,
        color: String,
        description: String? = null,
    ): Result<TagDto> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/tags/$tagId"
        val res = http.put(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
                append(HttpHeaders.Accept, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(UpdateTagBody(name = name, color = color, description = description?.takeIf { it.isNotBlank() }))
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(UpdateTagResponse.serializer(), res.bodyAsText()).tag
    }

    suspend fun deleteTag(organizationId: String, tagId: String): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/tags/$tagId"
        val res = http.delete(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }

    suspend fun addTagToDocument(
        organizationId: String,
        documentId: String,
        tagId: String,
    ): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/$documentId/tags"
        val res = http.post(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
            contentType(ContentType.Application.Json)
            setBody(AddTagToDocumentBody(tagId = tagId))
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }

    suspend fun removeTagFromDocument(
        organizationId: String,
        documentId: String,
        tagId: String,
    ): Result<Unit> = runCatching {
        val url = "${store.serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents/$documentId/tags/$tagId"
        val res = http.delete(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${store.apiKey}")
            }
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
    }
}

class ApiException(val status: HttpStatusCode, val responseBody: String) :
    RuntimeException("HTTP ${status.value}: ${responseBody.take(300)}")
