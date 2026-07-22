package app.sitecar.uploader

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
}
