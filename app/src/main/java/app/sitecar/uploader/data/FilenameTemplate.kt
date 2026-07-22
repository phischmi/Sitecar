package app.sitecar.uploader.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Rendert das (für Supporter anpassbare) Dateinamen-Format. Platzhalter:
 * {date}, {time}, {org}, {counter}. Der Default reproduziert exakt das
 * bisherige feste Format "Scan-yyyyMMdd-HHmmss".
 */
object FilenameTemplate {
    const val DEFAULT = "Scan-{date}-{time}"

    fun render(template: String, organizationName: String?, counter: Int): String {
        val now = Date()
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
        val time = SimpleDateFormat("HHmmss", Locale.US).format(now)
        val org = organizationName.orEmpty().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return template
            .replace("{date}", date)
            .replace("{time}", time)
            .replace("{org}", org)
            .replace("{counter}", counter.toString().padStart(3, '0'))
    }
}
