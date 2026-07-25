package app.sitecar.client

/**
 * Zentrale Feature-Schalter.
 *
 * [SUPPORTER_ENABLED] steuert die freiwillige "Sitecar unterstützen"-Bezahlfunktion
 * (Play Billing, einmaliger 0,99-€-Kauf) samt der rein kosmetischen
 * Akzentfarben-Belohnung. Bewusst auf `false`, damit die App zunächst vollständig
 * kostenfrei ausgeliefert wird – ohne In-App-Kauf und damit vorerst ohne
 * Gewerbeanmeldung.
 *
 * Der gesamte Bezahl-Code bleibt erhalten und lässt sich später per Update
 * reaktivieren:
 *  1. hier auf `true` setzen,
 *  2. in `AndroidManifest.xml` die per `tools:node="remove"` entfernte
 *     `com.android.vending.BILLING`-Permission wieder zulassen,
 *  3. das verwaltete In-App-Produkt `sitecar_supporter_tip` in der Play Console
 *     anlegen (siehe README).
 */
object Features {
    const val SUPPORTER_ENABLED = false

    /**
     * Zeigt zwei sonst an [SUPPORTER_ENABLED] gebundene UI-Teile unabhängig
     * davon und ohne echten Kauf an — zum Testen/Vorschauen, bevor die
     * Bezahlfunktion selbst aktiviert wird:
     *  - Akzentfarben-Auswahl in den Einstellungen, alle Farben ohne Sperre
     *    wählbar (siehe `unlocked` in SettingsScreen).
     *  - Unterstützer-Bereich in den Einstellungen, dargestellt als bereits
     *    freigeschaltetes Badge (keine Kauf-CTA, da noch kein echtes
     *    In-App-Produkt existiert).
     * Sobald [SUPPORTER_ENABLED] wieder `true` ist, greifen automatisch
     * wieder die echte Unterstützer-Sperre und der echte Kauf-Flow — dieses
     * Flag wird dann für beide Stellen automatisch wirkungslos.
     */
    const val SUPPORTER_PREVIEW_ENABLED = false
}
