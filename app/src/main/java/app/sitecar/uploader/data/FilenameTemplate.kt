package app.sitecar.uploader.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Rendert das (für Supporter anpassbare) Dateinamen-Format. Platzhalter:
 * {date}, {time}, {org}, {counter}. Der Default reproduziert exakt das
 * bisherige feste Format "Scan-yyyyMMdd-HHmmss".
 */
object FilenameTemplate {
    const val DEFAULT = "Scan-{date}-{time}"

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss", Locale.US)

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
        val now = LocalDateTime.now()
        val date = (documentDate ?: now.toLocalDate()).format(DATE_FORMATTER)
        val time = now.format(TIME_FORMATTER)
        val org = organizationName.orEmpty().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return template
            .replace("{date}", date)
            .replace("{time}", time)
            .replace("{org}", org)
            .replace("{counter}", counter.toString().padStart(3, '0'))
    }
}
