package app.sitecar.client.ui.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.sitecar.client.R
import app.sitecar.client.data.DocumentDto
import app.sitecar.client.data.SitecarApiClient
import app.sitecar.client.ui.util.ApiErrorMessages
import app.sitecar.client.ui.util.friendlyErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lädt ein Dokument in den Cache und öffnet es in der passenden Fremd-App
 * (Dokumentenliste und Papierkorb tun beide genau das). Gibt null zurück, wenn
 * es geklappt hat, sonst die anzuzeigende Fehlermeldung.
 */
suspend fun openDocumentExternally(
    context: Context,
    client: SitecarApiClient,
    organizationId: String,
    doc: DocumentDto,
    errorMessages: ApiErrorMessages,
): String? {
    val result = withContext(Dispatchers.IO) {
        client.downloadDocumentFile(organizationId = organizationId, documentId = doc.id)
            .mapCatching { bytes ->
                val dir = File(context.cacheDir, "documents").apply { mkdirs() }
                // Der Dokumentname kommt vom Server und darf den Cache-Pfad nicht
                // verlassen (z. B. "../" oder ein Slash im Namen).
                val safeName = (doc.name ?: doc.id).replace(Regex("""[/\\]"""), "_")
                val file = File(dir, "${doc.id}-$safeName")
                file.writeBytes(bytes)
                file
            }
    }
    val file = result.getOrElse { return friendlyErrorMessage(it, errorMessages) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, doc.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(intent) }.fold(
        onSuccess = { null },
        onFailure = {
            if (it is ActivityNotFoundException) {
                context.getString(R.string.documents_no_viewer)
            } else {
                friendlyErrorMessage(it, errorMessages)
            }
        },
    )
}
