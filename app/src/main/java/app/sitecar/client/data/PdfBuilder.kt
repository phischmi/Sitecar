package app.sitecar.client.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Baut aus einer Liste von JPEG-Scans ein PDF, optional mit unsichtbarem
 * OCR-Textlayer (ML Kit Latin Text Recognition) für Volltextsuche ohne
 * serverseitige OCR.
 */
class PdfBuilder(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Erkennt den Text aller Seiten per on-device OCR, unabhängig davon, ob der
     * unsichtbare Textlayer im PDF aktiviert ist — für lokale Analysen (Tag-/Datums-
     * /Absender-Vorschläge), die auch ohne PDF-Textlayer funktionieren sollen.
     */
    suspend fun recognizeText(imageFiles: List<File>): String = withContext(Dispatchers.IO) {
        // joinToString's transform lambda isn't inline, so it can't call a suspend
        // function directly — collect with map (which is inline) first, then join.
        imageFiles.map { imageFile ->
            runCatching {
                recognizer.process(InputImage.fromFilePath(context, Uri.fromFile(imageFile))).await().text
            }.getOrDefault("")
        }.joinToString("\n")
    }

    suspend fun build(
        imageFiles: List<File>,
        outputFile: File,
        ocrEnabled: Boolean = true,
        onProgress: (currentPage: Int, totalPages: Int) -> Unit = { _, _ -> },
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            outputFile.parentFile?.mkdirs()
            val doc = PDDocument(scratchFileSetting())
            try {
                imageFiles.forEachIndexed { index, imageFile ->
                    onProgress(index + 1, imageFiles.size)
                    addPageWithOcr(doc, imageFile, ocrEnabled)
                }
                doc.save(outputFile)
            } finally {
                doc.close()
            }
            outputFile
        }
    }

    private suspend fun addPageWithOcr(doc: PDDocument, imageFile: File, ocrEnabled: Boolean) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, bounds)
        val imageWidth = bounds.outWidth.toFloat()
        val imageHeight = bounds.outHeight.toFloat()
        if (imageWidth <= 0f || imageHeight <= 0f) return

        val page = PDPage(PDRectangle(imageWidth, imageHeight))
        doc.addPage(page)

        val pdImage = imageFile.inputStream().use { JPEGFactory.createFromStream(doc, it) }

        val ocrText = if (ocrEnabled) {
            runCatching {
                recognizer.process(InputImage.fromFilePath(context, Uri.fromFile(imageFile))).await()
            }.getOrNull()
        } else {
            null
        }

        PDPageContentStream(doc, page).use { stream ->
            stream.drawImage(pdImage, 0f, 0f, imageWidth, imageHeight)
            if (ocrText != null) {
                drawInvisibleText(stream, ocrText, imageHeight)
            }
        }
    }

    private fun drawInvisibleText(
        stream: PDPageContentStream,
        text: com.google.mlkit.vision.text.Text,
        pageHeight: Float,
    ) {
        stream.beginText()
        stream.setRenderingMode(RenderingMode.NEITHER)
        stream.setFont(PDType1Font.HELVETICA, 1f)
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                val raw = line.text.takeIf { it.isNotBlank() } ?: continue
                val sanitized = sanitizeForWinAnsi(raw).takeIf { it.isNotBlank() } ?: continue

                val fontSize = (box.height() * 0.75f).coerceAtLeast(1f)
                val pdfX = box.left.toFloat()
                val pdfY = pageHeight - box.bottom.toFloat()

                runCatching {
                    stream.setFont(PDType1Font.HELVETICA, fontSize)
                    stream.setTextMatrix(Matrix(1f, 0f, 0f, 1f, pdfX, pdfY))
                    stream.showText(sanitized)
                }
            }
        }
        stream.endText()
    }

    /**
     * Helvetica nutzt WinAnsiEncoding. Zeichen ausserhalb dieses Bereichs
     * werfen IllegalArgumentException; ersetze sie durch '?', damit der
     * Textlayer immerhin grob durchsuchbar bleibt.
     */
    private fun sanitizeForWinAnsi(s: String): String = buildString(s.length) {
        for (c in s) {
            append(if (isWinAnsi(c)) c else '?')
        }
    }

    private fun isWinAnsi(c: Char): Boolean {
        val code = c.code
        // ASCII (0x20–0x7E) + Latin-1 supplement (0xA0–0xFF) + häufige WinAnsi-Sonderzeichen
        if (code in 0x20..0x7E) return true
        if (code in 0xA0..0xFF) return true
        return c in WIN_ANSI_EXTRAS
    }

    /**
     * PDFBox hält die JPEG-Daten aller Seiten bis zum Speichern im Heap — bei
     * einem mehrseitigen Scan sind das schnell dreistellige MB, und der Prozess
     * wird abgeschossen. Nur die ersten Megabyte bleiben deshalb im RAM, alles
     * Weitere landet in einer Scratch-Datei im Cache-Verzeichnis.
     */
    private fun scratchFileSetting(): MemoryUsageSetting =
        MemoryUsageSetting.setupMixed(MAX_MAIN_MEMORY_BYTES).setTempDir(context.cacheDir)

    companion object {
        private const val WIN_ANSI_EXTRAS = "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ"
        private const val MAX_MAIN_MEMORY_BYTES = 8L * 1024 * 1024
    }
}
