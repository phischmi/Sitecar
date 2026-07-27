package app.sitecar.client.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Rendert die erste Seite eines PDFs als Bitmap — für die Upload-Vorschau und
 * die Miniaturbilder der Dokumentenliste. [maxSize] begrenzt, falls gesetzt,
 * die längere Kante; ohne Angabe wird in Seitengröße gerendert.
 * Liefert null, wenn die Datei kein lesbares PDF ist.
 */
fun renderPdfFirstPage(file: File, maxSize: Int? = null): Bitmap? = runCatching<Bitmap?> {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount == 0) return@runCatching null
            renderer.openPage(0).use { page ->
                val scale = if (maxSize == null) 1f else maxSize.toFloat() / maxOf(page.width, page.height)
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
