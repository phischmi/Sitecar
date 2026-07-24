package app.sitecar.client.data.duplicates

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecentUpload(
    val organizationId: String,
    val hash: Long,
    val fileName: String,
    val uploadedAtEpochMillis: Long,
)

/**
 * Merkt sich lokal den Perceptual-Hash der letzten Uploads pro Organisation,
 * um vor einem erneuten Hochladen zu warnen, falls dasselbe Dokument aus
 * Versehen ein zweites Mal gescannt/geteilt wird. Rein lokal, kein
 * Server-Abgleich gegen die komplette Dokumentenbibliothek — das wäre für
 * eine schlanke App zu teuer (jedes bestehende Dokument müsste dafür
 * heruntergeladen werden).
 */
class RecentUploadsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun findSimilar(organizationId: String, hash: Long): RecentUpload? {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS
        return loadAll()
            .filter { it.organizationId == organizationId && it.uploadedAtEpochMillis >= cutoff }
            .firstOrNull { PerceptualHash.hammingDistance(it.hash, hash) <= MAX_HAMMING_DISTANCE }
    }

    fun record(upload: RecentUpload) {
        val updated = (loadAll() + upload)
            .sortedByDescending { it.uploadedAtEpochMillis }
            .take(MAX_ENTRIES)
        prefs.edit().putString(KEY_ENTRIES, json.encodeToString(updated)).apply()
    }

    private fun loadAll(): List<RecentUpload> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<RecentUpload>>(raw) }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "sitecar_recent_uploads"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_ENTRIES = 30
        private const val MAX_HAMMING_DISTANCE = 6
        private const val MAX_AGE_MILLIS = 14L * 24 * 60 * 60 * 1000
    }
}
