package app.sitecar.uploader.data

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Rendert das (für Supporter anpassbare) Dateinamen-Format. Platzhalter:
 * {date}, {time}, {org}, {counter}. Der Default reproduziert exakt das
 * bisherige feste Format "Scan-yyyyMMdd-HHmmss".
 */
object FilenameTemplate {
    const val DEFAULT = "Scan-{date}-{time}"

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

    /**
     * [documentDate] überschreibt, falls gesetzt, nur den {date}-Platzhalter
     * (z. B. mit dem im Dokument erkannten Rechnungsdatum statt dem Scan-Datum).
     * {time} bleibt die tatsächliche Scan-/Upload-Zeit, da das Dokumentdatum
     * keine Uhrzeit trägt.
     */
    fun render(
        template: String,
        organizationName: String?,
        counter: Int,
        documentDate: LocalDate? = null,
    ): String {
        val now = Date()
        val date = documentDate?.format(DATE_FORMATTER) ?: SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
        val time = SimpleDateFormat("HHmmss", Locale.US).format(now)
        val org = organizationName.orEmpty().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return template
            .replace("{date}", date)
            .replace("{time}", time)
            .replace("{org}", org)
            .replace("{counter}", counter.toString().padStart(3, '0'))
    }
}
