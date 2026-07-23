package app.sitecar.uploader.data.insights

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * On-device Prompt-Inferenz über Gemini Nano (Android AICore, via die ML Kit
 * GenAI Prompt API). Läuft komplett lokal auf dem Gerät — nur der einmalige,
 * geräteweite Modell-Download benötigt Netzwerk, die eigentliche Inferenz nicht.
 * Ergänzt [RuleBasedInsightsEngine] um echtes Sprachverständnis statt eines
 * festen Schlüsselwort-Vokabulars; siehe [HybridInsightsEngine] für die
 * Kombination beider.
 */
object GenAiInsightsEngine : SmartInsightsEngine {

    private val model by lazy { Generation.getClient() }
    private val json = Json { ignoreUnknownKeys = true }

    /** Aktueller Verfügbarkeitsstatus des Modells auf diesem Gerät, siehe [FeatureStatus]. */
    suspend fun checkStatus(): Int = model.checkStatus()

    /** Startet den einmaligen, geräteweiten Modell-Download; Fortschritt via [DownloadStatus]. */
    fun download(): Flow<DownloadStatus> = model.download()

    override suspend fun analyze(text: String, existingTagNames: List<String>): DocumentInsights {
        if (text.isBlank()) return DocumentInsights.EMPTY
        val available = runCatching { checkStatus() == FeatureStatus.AVAILABLE }.getOrDefault(false)
        if (!available) return DocumentInsights.EMPTY

        return runCatching {
            val response = model.generateContent(
                generateContentRequest(TextPart(buildPrompt(text, existingTagNames))) {
                    temperature = 0.2f
                    candidateCount = 1
                },
            )
            parse(response.candidates.firstOrNull()?.text.orEmpty())
        }.getOrDefault(DocumentInsights.EMPTY)
    }

    private fun parse(raw: String): DocumentInsights {
        val jsonText = JSON_OBJECT.find(raw)?.value ?: return DocumentInsights.EMPTY
        val result = runCatching { json.decodeFromString<Result>(jsonText) }.getOrNull()
            ?: return DocumentInsights.EMPTY

        val documentDate = result.documentDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val deadlineDate = result.deadlineDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val deadline = deadlineDate?.let {
            Deadline(it, result.deadlineLabel?.trim()?.takeIf { label -> label.isNotBlank() } ?: "Frist")
        }

        return DocumentInsights(
            suggestedTagNames = result.tags.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            documentDate = documentDate,
            deadline = deadline,
        )
    }

    private fun buildPrompt(text: String, existingTagNames: List<String>): String {
        val truncated = text.take(MAX_INPUT_CHARS)
        val tagsHint = if (existingTagNames.isNotEmpty()) {
            "Bereits vorhandene Tags in der Organisation, bevorzuge exakt passende davon: " +
                existingTagNames.joinToString(", ") + "."
        } else {
            "Es gibt noch keine Tags in der Organisation."
        }
        return """
            Du analysierst den Text eines gescannten Dokuments (OCR, kann Fehler enthalten) und
            schlägst Metadaten dafür vor. $tagsHint
            Schlage zusätzlich bis zu 2 weitere, kurze, prägnante Tag-Namen vor (ein bis zwei Wörter,
            gleiche Sprache wie das Dokument), falls sinnvoll.
            Erkenne das Datum des Dokuments selbst (z. B. Rechnungs-, Ausstellungs- oder Vertragsdatum;
            NICHT das heutige Datum) sowie eine etwaige Frist (z. B. Kündigungsfrist, Garantie-Ablauf,
            Zahlungsziel) mit kurzer Bezeichnung.
            Antworte AUSSCHLIESSLICH mit einem einzigen kompakten JSON-Objekt ohne jede weitere
            Erklärung, exakt in diesem Format (Datumsformat JJJJ-MM-TT, null falls unbekannt):
            {"tags": ["Tag1"], "documentDate": "2024-03-01", "deadlineDate": null, "deadlineLabel": null}

            Dokumenttext:
            ${'"'}${'"'}${'"'}
            $truncated
            ${'"'}${'"'}${'"'}
        """.trimIndent()
    }

    @Serializable
    private data class Result(
        val tags: List<String> = emptyList(),
        val documentDate: String? = null,
        val deadlineDate: String? = null,
        val deadlineLabel: String? = null,
    )

    private val JSON_OBJECT = Regex("""\{[\s\S]*\}""")
    private const val MAX_INPUT_CHARS = 6000
}
