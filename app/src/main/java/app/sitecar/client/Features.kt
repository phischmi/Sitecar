package app.sitecar.client

/** Zentrale Feature-Schalter. */
object Features {
    const val SUPPORTER_ENABLED = false

    // Zeigt die sonst an SUPPORTER_ENABLED gebundenen UI-Teile ohne echten Kauf
    // an (Akzentfarben-Auswahl, Unterstützer-Bereich als freigeschaltetes Badge).
    // Aus für den geschlossenen Test: Tester sollen nur das Standard-Theme
    // sehen, ohne Unterstützer-UI.
    const val SUPPORTER_PREVIEW_ENABLED = false
}
