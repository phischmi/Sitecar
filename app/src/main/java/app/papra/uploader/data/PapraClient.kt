package app.papra.uploader.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File

class PapraClient(private val store: SettingsStore) {

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
        mimeType: String = "image/jpeg",
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
        }
        if (!res.status.isSuccess()) {
            throw ApiException(res.status, res.bodyAsText())
        }
        json.decodeFromString(DocumentsListResponse.serializer(), res.bodyAsText())
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
}

class ApiException(val status: HttpStatusCode, val responseBody: String) :
    RuntimeException("HTTP ${status.value}: ${responseBody.take(300)}")
