# Datenschutzerklärung – Sitecar for Papra

Stand: 2026-07-22

Sitecar ist eine inoffizielle, eigenständige Begleiter-App für selbst
gehostete [Papra](https://github.com/papra-hq/papra)-Instanzen. Sitecar wird
von einer Einzelperson entwickelt und steht in keiner Verbindung zu papra-hq.

## Welche Daten werden verarbeitet?

- **Server-URL und API-Key** deiner Papra-Instanz: werden ausschließlich
  lokal auf deinem Gerät gespeichert, verschlüsselt via
  `EncryptedSharedPreferences` (AES-256, Android Keystore). Sie verlassen das
  Gerät nur in Form der direkten HTTPS/HTTP-Anfragen an die von dir
  angegebene Papra-Server-Adresse.
- **Gescannte Dokumente**: werden auf dem Gerät zwischengespeichert
  (App-Cache) und ausschließlich an die von dir konfigurierte Papra-Instanz
  hochgeladen. Der App-Entwickler hat keinen Zugriff auf diese Inhalte.
- **On-Device-Verarbeitung**: Kantenerkennung/Perspektivkorrektur (ML Kit
  Document Scanner), optionale Texterkennung (ML Kit Text Recognition) und
  PDF-Erstellung laufen vollständig lokal auf dem Gerät. Dabei werden keine
  Bild- oder Textdaten an Google oder Dritte übertragen.
- **Freiwillige Unterstützung (0,99 €, einmalig)** — _in der aktuellen Version
  deaktiviert; die App ist vollständig kostenfrei und bietet keine In-App-Käufe
  an._ Sollte die Funktion später per Update aktiviert werden, läuft der Kauf
  vollständig über Google Play Billing ab. Sitecar erhält dabei nur die
  Information, ob der Kauf erfolgreich war — Zahlungsdaten (Zahlungsmittel,
  Rechnungsadresse etc.) laufen ausschließlich über Google und sind für den
  App-Entwickler nicht einsehbar.

## Was wird NICHT erhoben?

- Keine Analytics, kein Tracking, keine Werbung.
- Keine Übermittlung von Nutzungsdaten an den App-Entwickler.
- Keine Weitergabe von Daten an Dritte durch die App selbst.

## Berechtigungen

- **Internet**: für die Kommunikation mit deiner Papra-Instanz.
- **Kamera**: wird nicht direkt von Sitecar angefordert — der ML-Kit-Scanner
  läuft als separate Google-Play-Services-Activity und fordert die
  Kamera-Berechtigung selbst an.

## Verantwortlichkeit für den Papra-Server

Sitecar sendet Daten ausschließlich an die von dir selbst konfigurierte
Papra-Instanz. Für den Umgang mit diesen Daten auf dem Server (Speicherung,
etwaige serverseitige OCR-Verarbeitung, Backups etc.) ist der Betreiber
dieser Instanz verantwortlich — im Regelfall du selbst.

## Kontakt

Fragen zu dieser Datenschutzerklärung oder zur App: über die Issues des
GitHub-Repositories.
