package app.sitecar.uploader.data

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Wandelt einen über den Android-Share-Sheet empfangenen Intent
 * (ACTION_SEND / ACTION_SEND_MULTIPLE) in ein [PendingUpload] um, damit er
 * denselben Upload-Screen durchläuft wie ein Kamera-Scan.
 *
 * Sind alle geteilten Elemente Bilder, werden sie wie mehrseitige Scans
 * behandelt (zu einem PDF zusammengeführt, optional mit OCR). Alles andere
 * (z. B. ein per Mail geteiltes PDF) wird unverändert als fertiges Dokument
 * übernommen — dabei zählt nur das erste Element, weitere werden ignoriert
 * (Papras Upload-Endpunkt nimmt ohnehin nur eine Datei pro Aufruf entgegen).
 */
object ShareIntentHandler {

    fun parse(context: Context, intent: Intent?): PendingUpload? {
        if (intent == null) return null
        val uris = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { listOf(it) }
                    .orEmpty()
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    .orEmpty()
            else -> emptyList()
        }
        if (uris.isEmpty()) return null

        val resolver = context.contentResolver
        val mimeTypes = uris.map { resolver.getType(it) ?: intent.type.orEmpty() }

        return if (mimeTypes.all { it.startsWith("image/") }) {
            val pages = uris.mapNotNull { uri -> runCatching { copyAsJpeg(context, uri) }.getOrNull() }
            if (pages.isEmpty()) null else PendingUpload.Images(pages)
        } else {
            val uri = uris.first()
            val rawMimeType = mimeTypes.first().ifBlank { "application/octet-stream" }
            val displayName = queryDisplayName(context, uri)
            // Manche Apps deklarieren PDFs als generisches application/octet-stream
            // (z. B. wenn ihr FileProvider keinen expliziten Typ zuordnet) — verlässt
            // sich der Dateiname eindeutig auf .pdf, korrigieren wir den Typ, damit
            // Upload-Content-Type und PDF-Texterkennung (Smart Insights) trotzdem
            // greifen.
            val mimeType = if (rawMimeType == "application/octet-stream" &&
                displayName?.endsWith(".pdf", ignoreCase = true) == true
            ) {
                "application/pdf"
            } else {
                rawMimeType
            }
            val file = runCatching { copyToCache(context, uri, mimeType, displayName) }.getOrNull()
                ?: return null
            PendingUpload.ReadyDocument(
                file = file,
                mimeType = mimeType,
                suggestedName = displayName,
                ignoredCount = uris.size - 1,
            )
        }
    }

    /** Dekodiert und re-encodiert als JPEG, damit auch PNG/WEBP/HEIC-Freigaben im PdfBuilder funktionieren. */
    private fun copyAsJpeg(context: Context, uri: Uri): File {
        val bytes = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Konnte URI nicht öffnen: $uri" }.readBytes()
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        requireNotNull(bitmap) { "Kein gültiges Bild: $uri" }
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        val out = File(dir, "sitecar-shared-${timestamp()}-${sharedCounter++}.jpg")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        return out
    }

    private fun copyToCache(context: Context, uri: Uri, mimeType: String, displayName: String?): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val extension = displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: extensionForMimeType(mimeType)
        val out = File(dir, "sitecar-shared-${timestamp()}.$extension")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Konnte URI nicht öffnen: $uri" }
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun extensionForMimeType(mimeType: String): String = when (mimeType) {
        "application/pdf" -> "pdf"
        else -> "bin"
    }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    @Volatile
    private var sharedCounter = 0
}
