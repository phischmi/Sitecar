package app.sitecar.uploader.data.insights

/** Analysiert den (OCR-)Text eines Dokuments und schlägt Tags, Datum und Fristen vor. */
fun interface SmartInsightsEngine {
    suspend fun analyze(text: String, existingTagNames: List<String>): DocumentInsights
}
