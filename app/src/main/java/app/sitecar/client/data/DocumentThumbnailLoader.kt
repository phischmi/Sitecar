package app.sitecar.client.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lädt eine Miniatur-Vorschau für ein Dokument (erste PDF-Seite bzw.
 * downgesampeltes Bild). Papras API liefert keinen Thumbnail-Endpunkt,
 * daher wird die volle Datei einmalig heruntergeladen, ein kleines
 * Vorschaubild gerendert und auf dem Gerät zwischengespeichert.
 */
class DocumentThumbnailLoader(
    private val client: SitecarApiClient,
    private val cacheDir: File,
) {
    suspend fun load(organizationId: String, doc: DocumentDto): Bitmap? = withContext(Dispatchers.IO) {
        val mime = doc.mimeType ?: return@withContext null
        if (!mime.startsWith("image/") && mime != "application/pdf") return@withContext null

        val cacheFile = File(cacheDir, "${doc.id}.jpg")
        if (cacheFile.exists()) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { return@withContext it }
        }

        val bytes = client.downloadDocumentFile(organizationId, doc.id).getOrNull() ?: return@withContext null
        val bitmap = if (mime.startsWith("image/")) {
            decodeSampledBitmap(bytes)
        } else {
            renderPdfThumbnail(bytes)
        } ?: return@withContext null

        runCatching {
            cacheDir.mkdirs()
            cacheFile.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out) }
        }
        bitmap
    }

    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= THUMBNAIL_SIZE && bounds.outHeight / (sample * 2) >= THUMBNAIL_SIZE) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /** PdfRenderer braucht einen Dateideskriptor, die API liefert aber nur Bytes. */
    private fun renderPdfThumbnail(bytes: ByteArray): Bitmap? {
        cacheDir.mkdirs()
        val tmpFile = File.createTempFile("thumb", ".pdf", cacheDir)
        return try {
            tmpFile.writeBytes(bytes)
            renderPdfFirstPage(tmpFile, maxSize = THUMBNAIL_SIZE)
        } catch (_: Exception) {
            null
        } finally {
            tmpFile.delete()
        }
    }

    companion object {
        private const val THUMBNAIL_SIZE = 160
    }
}
