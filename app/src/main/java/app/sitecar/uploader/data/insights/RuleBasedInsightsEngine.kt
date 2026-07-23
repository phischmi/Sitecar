package app.sitecar.uploader.data.insights

import java.time.LocalDate

/**
 * Regelbasierte (rein lokale, keyword-/regex-basierte) Standard-Implementierung
 * von [SmartInsightsEngine]. Erkennt deutsche und englische Datumsformate sowie
 * ein kleines, festes Vokabular an Dokumentarten- und Fristen-Schlüsselwörtern.
 * Läuft komplett offline, ohne zusätzliches Modell.
 */
object RuleBasedInsightsEngine : SmartInsightsEngine {

    override fun analyze(text: String): DocumentInsights {
        if (text.isBlank()) return DocumentInsights.EMPTY

        val dates = findDates(text)
        val documentDate = dates.minByOrNull { it.first }?.second
        val deadline = findDeadline(text)
        val tags = TAG_KEYWORDS
            .filter { (keyword, _) -> containsWord(text, keyword) }
            .map { (_, tagName) -> tagName }
            .distinct()

        return DocumentInsights(
            suggestedTagNames = tags,
            documentDate = documentDate,
            deadline = deadline,
        )
    }

    private fun findDeadline(text: String): Deadline? {
        val lines = text.lines()
        lines.forEachIndexed { index, line ->
            val keyword = DEADLINE_KEYWORDS.firstOrNull { containsWord(line, it) } ?: return@forEachIndexed
            val window = if (index + 1 < lines.size) "$line ${lines[index + 1]}" else line
            val date = findDates(window).minByOrNull { it.first }?.second
            if (date != null) return Deadline(date, keyword.replaceFirstChar { it.uppercase() })
        }
        return null
    }

    /** Findet alle erkennbaren Datumsangaben im Text, sortiert nach Position im Text. */
    private fun findDates(text: String): List<Pair<Int, LocalDate>> {
        val found = mutableListOf<Pair<Int, LocalDate>>()

        for (match in NUMERIC_DATE.findAll(text)) {
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val month = match.groupValues[2].toIntOrNull() ?: continue
            val yearRaw = match.groupValues[3]
            val year = when (yearRaw.length) {
                4 -> yearRaw.toIntOrNull()
                2 -> yearRaw.toIntOrNull()?.let { if (it >= 70) 1900 + it else 2000 + it }
                else -> null
            } ?: continue
            runCatching { LocalDate.of(year, month, day) }
                .onSuccess { found += match.range.first to it }
        }

        for (match in ISO_DATE.findAll(text)) {
            val year = match.groupValues[1].toIntOrNull() ?: continue
            val month = match.groupValues[2].toIntOrNull() ?: continue
            val day = match.groupValues[3].toIntOrNull() ?: continue
            runCatching { LocalDate.of(year, month, day) }
                .onSuccess { found += match.range.first to it }
        }

        for (match in WRITTEN_DATE.findAll(text)) {
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val month = MONTHS[match.groupValues[2].lowercase()] ?: continue
            val year = match.groupValues[3].toIntOrNull() ?: continue
            runCatching { LocalDate.of(year, month, day) }
                .onSuccess { found += match.range.first to it }
        }

        return found.sortedBy { it.first }
    }

    /** Ganzwort-Suche (case-insensitive, unicode-sicher) ohne die Java-`\b`-Falle bei Umlauten. */
    private fun containsWord(text: String, word: String): Boolean {
        var from = 0
        while (true) {
            val idx = text.indexOf(word, from, ignoreCase = true)
            if (idx < 0) return false
            val before = idx - 1
            val after = idx + word.length
            val boundaryBefore = before < 0 || !text[before].isLetterOrDigit()
            val boundaryAfter = after >= text.length || !text[after].isLetterOrDigit()
            if (boundaryBefore && boundaryAfter) return true
            from = idx + 1
        }
    }

    private val NUMERIC_DATE = Regex("""\b(\d{1,2})\.(\d{1,2})\.(\d{2}|\d{4})\b""")
    private val ISO_DATE = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
    private val WRITTEN_DATE = Regex(
        """\b(\d{1,2})\.?\s+(Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)\s+(\d{4})\b""",
        RegexOption.IGNORE_CASE,
    )

    private val MONTHS = mapOf(
        "januar" to 1, "februar" to 2, "märz" to 3, "april" to 4, "mai" to 5, "juni" to 6,
        "juli" to 7, "august" to 8, "september" to 9, "oktober" to 10, "november" to 11, "dezember" to 12,
    )

    /** Schlüsselwort -> vorzuschlagender Tag-Name. Bewusst klein gehalten (deutsch + englisch). */
    private val TAG_KEYWORDS = listOf(
        "Rechnung" to "Rechnung",
        "Invoice" to "Rechnung",
        "Vertrag" to "Vertrag",
        "Contract" to "Vertrag",
        "Kündigung" to "Kündigung",
        "Kontoauszug" to "Kontoauszug",
        "Bank Statement" to "Kontoauszug",
        "Versicherung" to "Versicherung",
        "Insurance" to "Versicherung",
        "Garantie" to "Garantie",
        "Warranty" to "Garantie",
        "Gehaltsabrechnung" to "Gehaltsabrechnung",
        "Lohnabrechnung" to "Gehaltsabrechnung",
        "Payslip" to "Gehaltsabrechnung",
        "Steuerbescheid" to "Steuer",
        "Quittung" to "Quittung",
        "Beleg" to "Quittung",
        "Receipt" to "Quittung",
        "Angebot" to "Angebot",
        "Quote" to "Angebot",
        "Bestellung" to "Bestellung",
        "Mahnung" to "Mahnung",
    )

    /** Schlüsselwörter, die auf eine relevante Frist hindeuten (Kündigung, Garantie-Ablauf, Fälligkeit). */
    private val DEADLINE_KEYWORDS = listOf(
        "gültig bis",
        "kündigen bis",
        "Kündigungsfrist",
        "Garantie bis",
        "zahlbar bis",
        "fällig am",
        "Ablaufdatum",
        "expires",
        "valid until",
        "cancel by",
        "due date",
    )
}
