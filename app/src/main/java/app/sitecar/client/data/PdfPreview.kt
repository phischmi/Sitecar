package app.sitecar.client.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Rendert die erste Seite eines PDFs als Bitmap — für die Upload-Vorschau und
 * die Miniaturbilder der Dokumentenliste. [maxSize] begrenzt die längere Kante
 * und ist bewusst Pflicht: die Seitengröße eines aus einem Scan gebauten PDFs
 * entspricht der Pixelgröße der Aufnahme, ungebremst also einer Bitmap von
 * mehreren Dutzend MB. Liefert null, wenn die Datei kein lesbares PDF ist.
 */
fun renderPdfFirstPage(file: File, maxSize: Int): Bitmap? = runCatching<Bitmap?> {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount == 0) return@runCatching null
            renderer.openPage(0).use { page ->
                val scale = (maxSize.toFloat() / maxOf(page.width, page.height)).coerceAtMost(1f)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }
}.getOrNull()
