package app.sitecar.client.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Dekodiert ein Bild höchstens so groß, dass seine längere Kante etwa [maxSize]
 * Pixel nicht überschreitet.
 *
 * Ein Kamera-Scan in voller Auflösung belegt als Bitmap schnell 30–50 MB
 * (3000 × 4000 × 4 Byte). Für eine Bildschirm-Vorschau oder ein Miniaturbild
 * wird davon nichts gebraucht, der Speicher bleibt dem Prozess aber als
 * gewachsener Heap erhalten — genug, damit Android die App im Hintergrund
 * (z. B. während der Scanner im Vordergrund läuft) als Erstes abräumt.
 */
fun decodeSampledBitmap(file: File, maxSize: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSize)
    }
    BitmapFactory.decodeFile(file.absolutePath, opts)
}.getOrNull()

/** Wie [decodeSampledBitmap], für bereits im Speicher liegende Bilddaten. */
fun decodeSampledBitmap(bytes: ByteArray, maxSize: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSize)
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}.getOrNull()

/** inSampleSize muss eine Zweierpotenz sein; die längere Kante landet damit in [maxSize, 2 × maxSize). */
private fun sampleSizeFor(width: Int, height: Int, maxSize: Int): Int {
    val longestEdge = maxOf(width, height)
    if (longestEdge <= 0 || maxSize <= 0) return 1
    var sample = 1
    while (longestEdge / (sample * 2) >= maxSize) {
        sample *= 2
    }
    return sample
}
