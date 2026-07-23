package app.sitecar.uploader.data.insights

import kotlinx.coroutines.withTimeoutOrNull

/**
 * Kombiniert die regelbasierte Analyse mit der On-Device-KI ([GenAiInsightsEngine]):
 * die KI-Ergebnisse ergänzen bzw. überschreiben die regelbasierten nur dort, wo sie
 * tatsächlich etwas liefern. Bei Timeout, Fehler oder wenn Gemini Nano auf dem Gerät
 * nicht verfügbar ist, bleibt die zuverlässige regelbasierte Analyse allein maßgeblich —
 * [GenAiInsightsEngine] wirft dafür nie, sondern liefert in solchen Fällen leere Insights.
 */
object HybridInsightsEngine : SmartInsightsEngine {

    private const val TIMEOUT_MS = 8_000L

    override suspend fun analyze(text: String, existingTagNames: List<String>): DocumentInsights {
        val ruleBased = RuleBasedInsightsEngine.analyze(text, existingTagNames)
        val genAi = withTimeoutOrNull(TIMEOUT_MS) {
            GenAiInsightsEngine.analyze(text, existingTagNames)
        }
        if (genAi == null || genAi == DocumentInsights.EMPTY) return ruleBased

        return DocumentInsights(
            suggestedTagNames = (genAi.suggestedTagNames + ruleBased.suggestedTagNames).distinct(),
            documentDate = genAi.documentDate ?: ruleBased.documentDate,
            deadline = genAi.deadline ?: ruleBased.deadline,
        )
    }
}
