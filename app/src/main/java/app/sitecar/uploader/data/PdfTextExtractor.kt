package app.sitecar.uploader.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Liest den vorhandenen Textlayer eines PDFs aus (z. B. ein per "Teilen an
 * Sitecar" empfangenes, bereits digitales PDF). Liefert einen leeren String,
 * falls das PDF keinen Textlayer hat (reine Scans ohne OCR) — es wird bewusst
 * keine eigene OCR auf PDF-Seiten gerastert, um das schlank zu halten.
 */
object PdfTextExtractor {
    suspend fun extractText(file: File): String = withContext(Dispatchers.IO) {
        runCatching {
            val doc = PDDocument.load(file)
            try {
                PDFTextStripper().getText(doc)
            } finally {
                doc.close()
            }
        }.getOrDefault("")
    }
}
