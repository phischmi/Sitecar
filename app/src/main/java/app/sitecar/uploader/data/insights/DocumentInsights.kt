package app.sitecar.uploader.data.insights

import java.time.LocalDate

/**
 * Ergebnis der on-device Textanalyse eines frisch gescannten/geteilten
 * Dokuments: Tag-Vorschläge, ein erkanntes Dokumentdatum (z. B. Rechnungsdatum),
 * ein erkannter Absendername (z. B. Firmenname im Briefkopf) und eine erkannte
 * Frist (z. B. Kündigungsfrist, Garantie-Ablauf).
 */
data class DocumentInsights(
    val suggestedTagNames: List<String> = emptyList(),
    val documentDate: LocalDate? = null,
    val senderName: String? = null,
    val deadline: Deadline? = null,
) {
    companion object {
        val EMPTY = DocumentInsights()
    }
}

data class Deadline(val date: LocalDate, val label: String)
