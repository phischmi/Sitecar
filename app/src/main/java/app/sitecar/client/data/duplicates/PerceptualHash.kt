package app.sitecar.client.data.duplicates

import android.graphics.Bitmap

/**
 * Einfacher Average-Hash (aHash) für einen schnellen, rein lokalen
 * Ähnlichkeitsvergleich zweier Seiten-Bilder — genug, um "das habe ich schon
 * mal gescannt" zu erkennen, ohne ein echtes Bild-Diff oder Cloud-Vergleich.
 */
object PerceptualHash {
    private const val SIZE = 8

    fun compute(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
        val luminance = DoubleArray(SIZE * SIZE)
        var sum = 0.0
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val value = 0.299 * r + 0.587 * g + 0.114 * b
                luminance[y * SIZE + x] = value
                sum += value
            }
        }
        if (scaled !== bitmap) scaled.recycle()

        val average = sum / luminance.size
        var hash = 0L
        for (i in luminance.indices) {
            if (luminance[i] > average) hash = hash or (1L shl i)
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
