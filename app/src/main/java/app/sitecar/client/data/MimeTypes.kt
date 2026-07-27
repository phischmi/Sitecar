package app.sitecar.client.data

/** Dateiendung für einen MIME-Typ; `bin` als Rückfall für alles Unbekannte. */
fun extensionForMimeType(mimeType: String): String = when {
    mimeType == "application/pdf" -> "pdf"
    mimeType.startsWith("image/") -> mimeType.substringAfter('/').ifBlank { "jpg" }
    else -> "bin"
}
