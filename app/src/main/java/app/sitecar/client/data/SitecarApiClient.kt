package app.sitecar.client.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
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
import io.ktor.client.statement.HttpResponse
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

    private val baseUrl: String get() = store.serverUrl.trimEnd('/')

    /** Setzt Bearer-Auth (und optional den JSON-Accept-Header) für eine Anfrage. */
    private fun HttpRequestBuilder.authHeaders(key: String = store.apiKey, acceptJson: Boolean = true) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $key")
            if (acceptJson) append(HttpHeaders.Accept, "application/json")
        }
    }

    /** Wirft [ApiException] bei nicht-erfolgreichem Status; der Body wird nur im Fehlerfall gelesen. */
    private suspend fun HttpResponse.ensureSuccess() {
        if (!status.isSuccess()) throw ApiException(status, bodyAsText())
    }

    /** Wie [ensureSuccess], gibt aber den Response-Body zurück (einmaliges Lesen für die JSON-Deserialisierung). */
    private suspend fun HttpResponse.textOrThrow(): String {
        val body = bodyAsText()
        if (!status.isSuccess()) throw ApiException(status, body)
        return body
    }

    suspend fun listOrganizations(
        overrideUrl: String? = null,
        overrideKey: String? = null,
    ): Result<List<Organization>> = runCatching {
        val url = (overrideUrl ?: store.serverUrl).trimEnd('/')
        val res = http.get("$url/api/organizations") {
            authHeaders(key = overrideKey ?: store.apiKey)
        }
        json.decodeFromString(OrganizationsResponse.serializer(), res.textOrThrow()).organizations
    }

    suspend fun uploadDocument(
        organizationId: String,
        file: File,
        fileName: String,
        mimeType: String,
    ): Result<DocumentDto> = runCatching {
        val res = http.post("$baseUrl/api/organizations/$organizationId/documents") {
            authHeaders()
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
        json.decodeFromString(CreateDocumentResponse.serializer(), res.textOrThrow()).document
    }

    suspend fun listDocuments(
        organizationId: String,
        searchQuery: String? = null,
        sortField: DocumentSortField? = null,
        sortOrder: DocumentSortOrder? = null,
        pageIndex: Int = 0,
        pageSize: Int = 50,
    ): Result<DocumentsListResponse> = runCatching {
        val res = http.get("$baseUrl/api/organizations/$organizationId/documents") {
            authHeaders()
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
        json.decodeFromString(DocumentsListResponse.serializer(), res.textOrThrow())
    }

    suspend fun listDeletedDocuments(
        organizationId: String,
        pageIndex: Int = 0,
        pageSize: Int = 100,
    ): Result<DocumentsListResponse> = runCatching {
        val res = http.get("$baseUrl/api/organizations/$organizationId/documents/deleted") {
            authHeaders()
            parameter("pageIndex", pageIndex)
            parameter("pageSize", pageSize)
        }
        json.decodeFromString(DocumentsListResponse.serializer(), res.textOrThrow())
    }

    suspend fun updateDocumentName(
        organizationId: String,
        documentId: String,
        name: String,
    ): Result<DocumentDto> = runCatching {
        val res = http.patch("$baseUrl/api/organizations/$organizationId/documents/$documentId") {
            authHeaders()
            contentType(ContentType.Application.Json)
            setBody(UpdateDocumentBody(name = name))
        }
        json.decodeFromString(UpdateDocumentResponse.serializer(), res.textOrThrow()).document
    }

    suspend fun downloadDocumentFile(
        organizationId: String,
        documentId: String,
    ): Result<ByteArray> = runCatching {
        val res = http.get("$baseUrl/api/organizations/$organizationId/documents/$documentId/file") {
            authHeaders(acceptJson = false)
        }
        res.ensureSuccess()
        res.readBytes()
    }

    /** Papra löscht weich (Papierkorb) — die Instanz behält das Dokument dort für eine gewisse Zeit. */
    suspend fun deleteDocument(
        organizationId: String,
        documentId: String,
    ): Result<Unit> = runCatching {
        http.delete("$baseUrl/api/organizations/$organizationId/documents/$documentId") {
            authHeaders(acceptJson = false)
        }.ensureSuccess()
    }

    suspend fun restoreDocument(
        organizationId: String,
        documentId: String,
    ): Result<Unit> = runCatching {
        http.post("$baseUrl/api/organizations/$organizationId/documents/$documentId/restore") {
            authHeaders(acceptJson = false)
        }.ensureSuccess()
    }

    /** Endgültiges Löschen aus dem Papierkorb — nicht mehr rückgängig zu machen. */
    suspend fun deleteTrashDocument(
        organizationId: String,
        documentId: String,
    ): Result<Unit> = runCatching {
        http.delete("$baseUrl/api/organizations/$organizationId/documents/trash/$documentId") {
            authHeaders(acceptJson = false)
        }.ensureSuccess()
    }

    /** Leert den kompletten Papierkorb der Organisation — nicht mehr rückgängig zu machen. */
    suspend fun emptyTrash(organizationId: String): Result<Unit> = runCatching {
        http.delete("$baseUrl/api/organizations/$organizationId/documents/trash") {
            authHeaders(acceptJson = false)
        }.ensureSuccess()
    }

    suspend fun listTags(organizationId: String): Result<List<TagDto>> = runCatching {
        val res = http.get("$baseUrl/api/organizations/$organizationId/tags") {
            authHeaders()
        }
        json.decodeFromString(TagsResponse.serializer(), res.textOrThrow()).tags
    }

    suspend fun createTag(
        organizationId: String,
        name: String,
        color: String,
        description: String? = null,
    ): Result<TagDto> = runCatching {
        val res = http.post("$baseUrl/api/organizations/$organizationId/tags") {
            authHeaders()
            contentType(ContentType.Application.Json)
            setBody(CreateTagBody(name = name, color = color, description = description?.takeIf { it.isNotBlank() }))
        }
        json.decodeFromString(CreateTagResponse.serializer(), res.textOrThrow()).tag
    }

    suspend fun updateTag(
        organizationId: String,
        tagId: String,
        name: String,
        color: String,
        description: String? = null,
    ): Result<TagDto> = runCatching {
        val res = http.put("$baseUrl/api/organizations/$organizationId/tags/$tagId") {
            authHeaders()
            contentType(ContentType.Application.Json)
            setBody(UpdateTagBody(name = name, color = color, description = description?.takeIf { it.isNotBlank() }))
        }
        json.decodeFromString(UpdateTagResponse.serializer(), res.textOrThrow()).tag
    }

    suspend fun deleteTag(organizationId: String, tagId: String): Result<Unit> = runCatching {
        http.delete("$baseUrl/api/organizations/$organizationId/tags/$tagId") {
            authHeaders(acceptJson = false)
        }.ensureSuccess()
    }

    suspend fun addTagToDocument(
        organizationId: String,
        documentId: String,
        tagId: String,
    ): Result<Unit> = runCatching {
        http.post("$baseUrl/api/organizations/$organizationId/documents/$documentId/tags") {
            authHeaders(acceptJson = false)
            contentType(ContentType.Application.Json)
            setBody(AddTagToDocumentBody(tagId = tagId))
        }.ensureSuccess()
    }

    suspend fun removeTagFromDocument(
        organizationId: String,
        documentId: String,
        tagId: String,
    ): Result<Unit> = runCatching {
        http.delete("$baseUrl/api/organizations/$organizationId/documents/$documentId/tags/$tagId") {
            authHeaders(acceptJson = false)
        }.ensureSuccess()
    }
}

class ApiException(val status: HttpStatusCode, val responseBody: String) :
    RuntimeException("HTTP ${status.value}: ${responseBody.take(300)}")
