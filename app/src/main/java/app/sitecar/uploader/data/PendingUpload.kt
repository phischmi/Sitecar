package app.sitecar.uploader.data

import java.io.File

/**
 * Was der Upload-Screen als Nächstes verarbeiten soll: entweder frisch
 * gescannte/geteilte Bilder, die erst noch zu einem PDF zusammengeführt
 * werden (optional mit OCR-Textlayer), oder eine bereits fertige Datei
 * (z. B. ein per "Teilen an Sitecar" empfangenes PDF), die unverändert
 * hochgeladen wird.
 */
sealed interface PendingUpload {
    data class Images(val pages: List<File>) : PendingUpload

    data class ReadyDocument(
        val file: File,
        val mimeType: String,
        val suggestedName: String?,
        /** >0, falls beim Teilen mehrerer Dateien nur die erste übernommen wurde. */
        val ignoredCount: Int = 0,
    ) : PendingUpload
}
