package app.sitecar.client.data.insights

import java.time.LocalDate

/**
 * Ergebnis der on-device Textanalyse eines frisch gescannten/geteilten
 * Dokuments: Tag-Vorschläge, ein erkanntes Dokumentdatum (z. B. Rechnungsdatum)
 * und ein erkannter Absendername (z. B. Firmenname im Briefkopf).
 */
data class DocumentInsights(
    val suggestedTagNames: List<String> = emptyList(),
    val documentDate: LocalDate? = null,
    val senderName: String? = null,
) {
    companion object {
        val EMPTY = DocumentInsights()
    }
}
