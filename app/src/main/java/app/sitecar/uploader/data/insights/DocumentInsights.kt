package app.sitecar.uploader.data.insights

import java.time.LocalDate

/**
 * Ergebnis der on-device Textanalyse eines frisch gescannten/geteilten
 * Dokuments: Tag-Vorschläge, ein erkanntes Dokumentdatum (z. B. Rechnungsdatum)
 * und eine erkannte Frist (z. B. Kündigungsfrist, Garantie-Ablauf).
 */
data class DocumentInsights(
    val suggestedTagNames: List<String> = emptyList(),
    val documentDate: LocalDate? = null,
    val deadline: Deadline? = null,
) {
    companion object {
        val EMPTY = DocumentInsights()
    }
}

data class Deadline(val date: LocalDate, val label: String)
