<p align="center">
  <img src="docs/logo.svg" width="96" height="96" alt="Sitecar Logo">
</p>

# Sitecar (Android)

Schlanke native Android-App zum Aufnehmen und Hochladen von Dokumenten an eine
selbst gehostete [Papra](https://github.com/papra-hq/papra)-Instanz — ein
eigenständiger Begleiter ("Sidecar") für Papra, vormals PapraCam.

- Inoffiziell, nicht von papra-hq
- Anbindung via API-Key (kein OAuth/Login)
- Kotlin + Jetpack Compose, ML Kit Document Scanner, Ktor

## Features

- Server-URL und API-Key in den Einstellungen hinterlegen (verschlüsselt gespeichert).
- Verbindungstest gegen `GET /api/organizations`.
- Mehrseitiges Dokument scannen mit automatischer Kantenerkennung und
  Perspektivkorrektur (Google ML Kit Document Scanner, on-device).
- Vorschau, Auswahl der Ziel-Organisation, Anpassen des Dateinamens.
- Zusammenführung zu einem PDF, optional mit unsichtbarem OCR-Textlayer
  (ML Kit Text Recognition, on-device) für Volltextsuche ohne serverseitige
  OCR — abschaltbar in den Einstellungen, falls die Papra-Instanz bereits
  selbst OCR durchführt (z. B. Mistral OCR).
- Upload via `POST /api/organizations/:organizationId/documents` (Multipart, PDF).
- Vorhandene Dokumente einer Organisation ansehen (`GET /api/organizations/:organizationId/documents`),
  Tippen lädt die Datei herunter und öffnet sie im System-Viewer. Miniatur-Vorschau
  (erste PDF-Seite bzw. Bild) statt generischem Icon, lokal gerendert und gecacht.
- Dokumente durchsuchen (serverseitige Volltextsuche über `searchQuery`) und
  sortieren (Datum/Name, auf-/absteigend).
- Dokument umbenennen (`PATCH .../documents/:documentId`), löschen
  (`DELETE .../documents/:documentId`, wandert in Papras Papierkorb) und im
  Papierkorb-Tab wiederherstellen oder einzeln bzw. komplett endgültig löschen
  (`POST .../restore`, `DELETE .../documents/trash/:documentId`,
  `DELETE .../documents/trash`).
- Tags-Tab: Tags anlegen, bearbeiten und löschen (`POST`/`PUT`/`DELETE
  .../tags`). Tippen auf einen Tag springt in den Dokumente-Tab und filtert
  dort per Suchsyntax `tag:Name` — derselbe Suchparser wie in der
  Papra-Weboberfläche.
- Tags einem Dokument zuweisen/entfernen über das Kontextmenü im Dokumente-Tab
  (`POST`/`DELETE .../documents/:documentId/tags`). Zugewiesene Tags werden
  als farbige Chips in der Liste angezeigt, Tippen auf einen Chip filtert
  ebenfalls danach.
- "Teilen an Sitecar": Bilder und PDFs aus anderen Apps (Mail-Anhänge,
  Galerie, Chat) lassen sich per Android-Share-Sheet direkt an Sitecar
  senden und durchlaufen denselben Upload-Screen wie ein Kamera-Scan.
  Geteilte Bilder werden wie mehrseitige Scans behandelt (zu einem PDF
  zusammengeführt, optional mit OCR); ein geteiltes PDF wird unverändert
  hochgeladen. Bei mehreren geteilten Dateien zählt nur die erste.
- Intelligente Vorschläge beim Hochladen (regelbasiert, komplett on-device,
  abschaltbar in den Einstellungen): erkennt Tags aus einem kleinen
  Schlüsselwort-Vokabular und schlägt bereits in der Organisation vorhandene
  Tags vor; erkennt das Dokumentdatum (z. B. Rechnungsdatum) als Alternative
  zum Scan-Datum im Dateinamen; erkennt Fristen (z. B. Kündigungsfrist,
  Garantie-Ablauf) und bietet nach dem Upload eine lokale Erinnerung an
  (Android-Notification via WorkManager, kein Server-/Kalenderzugriff).
- Duplikat-Warnung: vergleicht einen Perceptual-Hash der ersten Seite mit den
  letzten Uploads derselben Organisation (rein lokal, 14 Tage Gedächtnis,
  kein Download bestehender Dokumente) und warnt unaufdringlich, falls ein
  ähnliches Dokument kürzlich schon hochgeladen wurde — blockiert den Upload
  nicht, ist nur ein Hinweis.
- Pull-to-refresh zum Aktualisieren der Dokumentenliste.
- Darstellung wählbar: Hell / Dunkel / System.
- Schnellzugriff-Shortcut "Scannen" per Long-Press auf das App-Icon.
- Anpassbares Dateinamen-Format für Scans (Platzhalter `{date}` `{time}`
  `{org}` `{counter}`).
- _(Derzeit deaktiviert — die App wird zunächst vollständig kostenfrei
  ausgeliefert, siehe [Bezahlfunktion](#bezahlfunktion-vorerst-deaktiviert).)_
  Freiwilliger "Sitecar unterstützen"-Hinweis (einmalig 0,99 €, Play Billing),
  erscheint nach dem 5. erfolgreichen Upload, danach alle 10 weiteren — oder
  jederzeit manuell in den Einstellungen. Rein kosmetischer Dank: schaltet
  drei weitere Akzentfarben frei (Smaragd/Bernstein/Rosé statt nur Indigo),
  inklusive passend eingefärbtem App-Icon-Hintergrund. Kein bestehendes
  Feature wird Nicht-Unterstützern weggenommen oder vorenthalten.

## Bezahlfunktion (vorerst deaktiviert)

Die freiwillige Unterstützer-Bezahlfunktion ist über den zentralen Schalter
`Features.SUPPORTER_ENABLED` (in `app/src/main/java/app/sitecar/uploader/Features.kt`)
**abgeschaltet** (`false`). Solange sie aus ist:

- kein In-App-Kauf, kein Spenden-Dialog, keine Kauf-Aufforderung nach Uploads;
- der Akzentfarben- und Unterstützer-Bereich in den Einstellungen ist
  ausgeblendet (nur das Standard-Indigo ist aktiv);
- die App baut keine Play-Billing-Verbindung auf und deklariert im finalen
  Manifest **keine** `com.android.vending.BILLING`-Permission (per
  `tools:node="remove"` entfernt).

So bleibt die App rein kostenfrei und benötigt vorerst keine Gewerbeanmeldung.
Der komplette Bezahl-Code bleibt erhalten und lässt sich per Update
reaktivieren:

1. `Features.SUPPORTER_ENABLED = true` setzen,
2. in `AndroidManifest.xml` den `tools:node="remove"`-Eintrag für die
   BILLING-Permission entfernen,
3. das verwaltete In-App-Produkt `sitecar_supporter_tip` in der Play Console
   anlegen (siehe [Play-Store-Release](#play-store-release)).

## Lokalisierung

Standardsprache ist Englisch (`res/values/strings.xml`), Deutsch liegt als
Override in `res/values-de/strings.xml`. Android wählt automatisch anhand der
Systemsprache; auf Android 13+ kann der Nutzer die App-Sprache zusätzlich
unabhängig vom System setzen (Einstellungen → Apps → Sitecar → Sprache) —
das kommt allein durch `android:localeConfig` in der Manifest, ganz ohne
eigene In-App-UI.

Neue Sprache hinzufügen (z. B. Französisch):
1. `res/values-fr/strings.xml` anlegen und alle Keys aus `values/strings.xml`
   übersetzen (`app_name` unverändert lassen).
2. In `res/xml/locales_config.xml` einen `<locale android:name="fr" />`-Eintrag
   ergänzen.
3. In `app/build.gradle.kts` bei `defaultConfig.resourceConfigurations` `"fr"`
   ergänzen — sonst wird die neue Sprache beim Release-Build aus der APK
   herausgefiltert.

Kein Code muss angefasst werden; alle UI-Texte laufen bereits über
`stringResource(R.string....)`.

## Geplant

- Netzwerksicherheitskonfiguration statt globalem Cleartext-Traffic
- Automatisierte Tests (aktuell keine vorhanden)

## Build

Voraussetzungen: Android Studio (Ladybug/Meerkat oder neuer), JDK 17.

### Mit Android Studio
1. `File → Open …` und dieses Verzeichnis öffnen.
2. Android Studio synchronisiert Gradle und generiert ggf. den Gradle Wrapper.
3. `Run` (▶) auf einem angeschlossenen Gerät oder Emulator.

### CLI
Wenn lokal `gradle` installiert ist (z. B. via `brew install gradle`):

```sh
gradle wrapper
./gradlew assembleDebug
./gradlew installDebug   # auf verbundenes Gerät installieren
```

## API-Key in Papra anlegen

Im Papra-Web-UI unter **Settings → API Keys** einen Key mit der Berechtigung
`documents:create` (und idealerweise `organizations:read` für die Org-Liste)
erzeugen. Der Key beginnt mit `ppapi_`.

## Hinweise

- `usesCleartextTraffic="true"` ist gesetzt, damit auch HTTP-Self-Hosted-Instanzen
  funktionieren. Für reine HTTPS-Setups später per `networkSecurityConfig` einengen.
- API-Key und Server-URL liegen in `EncryptedSharedPreferences` (AES-256, Android Keystore).
- Min-SDK 26 (Android 8), Target-SDK 35.
- Der ML Kit Document Scanner lädt sein Modell beim ersten Start on-demand
  über Google Play Services nach (einmalig ~ein paar MB). Setzt Play Services voraus.
- Die App selbst hat keine `CAMERA`-Permission — der Scanner läuft als
  separate Activity in Play Services und fordert seine Permissions selbst an.
- Texterkennung (OCR) auf dem Gerät lässt sich in den Einstellungen abschalten,
  falls die Papra-Instanz bereits serverseitig OCR durchführt (z. B. Mistral OCR).
- Die Akzentfarben-Icons werden über vier `activity-alias`-Einträge im Manifest
  realisiert (eine pro Farbe, nur eine ist jeweils `enabled`); `LauncherIcon.apply()`
  schaltet zur Laufzeit um. Je nach Launcher kann die Aktualisierung des
  Home-Screen-Icons ein bis zwei Sekunden dauern — normales Verhalten dieser
  Technik, nicht spezifisch für Sitecar.

## Play-Store-Release

### Signing
Release-Builds werden signiert, wenn `keystore.properties` im Projektroot
existiert (siehe `keystore.properties.example` für das Format). Die Datei
selbst sowie `*.keystore`/`*.jks` sind über `.gitignore` ausgeschlossen und
dürfen nie eingecheckt werden.

```sh
cp keystore.properties.example keystore.properties
# Werte anpassen, dann:
./gradlew bundleRelease   # erzeugt das signierte .aab für die Play Console
```

Ohne `keystore.properties` bauen `assembleRelease`/`bundleRelease` weiterhin,
nur unsigniert.

### Vor der Veröffentlichung offen
- **Datenschutzerklärung**: Entwurf liegt in [`PRIVACY.md`](./PRIVACY.md),
  muss noch unter einer öffentlichen URL gehostet werden (z. B. GitHub Pages
  oder die gerenderte GitHub-Blob-Ansicht) und in der Play Console verlinkt
  werden.
- **Data-Safety-Formular**: Die App erhebt/übermittelt keine Daten an den
  Entwickler oder Dritte (siehe `PRIVACY.md`) — im Formular entsprechend
  "Keine Daten erhoben" angeben. Aktuell bietet die App keine In-App-Käufe an
  (Bezahlfunktion deaktiviert). Wird sie später aktiviert, läuft der Kauf
  vollständig über Google Play Billing; Zahlungsdaten sieht der Entwickler nie.
- **Play Billing Library aktuell halten** _(erst relevant, wenn die
  [Bezahlfunktion](#bezahlfunktion-vorerst-deaktiviert) wieder aktiviert
  wird)_: Google erzwingt regelmäßig Mindestversionen für die Billing Library
  (aktuell `7.1.1` in `libs.versions.toml`), sonst wird der Play-Store-Upload
  irgendwann abgelehnt — vor dem Release-Build kurz gegen die aktuelle Version
  prüfen.
- **In-App-Produkt anlegen** _(nur beim Reaktivieren der Bezahlfunktion nötig;
  aktuell ist `Features.SUPPORTER_ENABLED = false`)_: In der Play Console unter
  Monetarisierung → Produkte → In-App-Produkte ein **verwaltetes Produkt**
  (kein Abo) mit der ID `sitecar_supporter_tip` und Preis 0,99 € anlegen —
  diese ID ist im Client fest hinterlegt (`BillingManager.SUPPORTER_PRODUCT_ID`).
  Ohne dieses Produkt lädt `SupportDialog` keine Preisdaten und der
  Kauf-Button bleibt wirkungslos (`launchPurchaseFlow` bricht früh ab).
  Erst ab dem ersten Internal-Testing-Release testbar (Play Billing
  funktioniert nicht mit reinen Debug-Builds ohne Play-Console-Release-Track).
- **Store-Assets**: Screenshots, Feature-Grafik, kurze/lange Beschreibung
  fehlen noch.
- **Versionierung**: `versionCode`/`versionName` vor dem ersten Upload final
  festlegen (aktuell `1` / `0.1.0`).

## Lizenz

Sitecar-Code: **MIT** – siehe [`LICENSE`](./LICENSE).

### Verhältnis zu Papras AGPL-3.0

[Papra](https://github.com/papra-hq/papra) selbst steht unter der
**AGPL-3.0**. Sitecar ist davon nicht betroffen und muss die AGPL **nicht**
übernehmen:

- Sitecar ist eine **eigenständige, unabhängig geschriebene** Android-App.
  Sie enthält **keinen** Quellcode, keine Ressourcen und keine kompilierten
  Bestandteile aus Papra.
- Die Kommunikation läuft ausschließlich über Papras **öffentliche HTTP-API**
  (`/api/...`) über das Netzwerk. Der bloße Aufruf einer Netzwerk-API macht
  einen Client nach GPL/AGPL-Verständnis **nicht** zu einem abgeleiteten Werk
  ("derivative work") des Servers — die AGPL-Copyleft-Pflichten greifen daher
  nicht auf Sitecar über.
- Der Name **„Papra"** wird im Code und in der UI nur **nominativ** verwendet,
  um den kompatiblen Zielserver zu benennen (z. B. „Send to Papra",
  URL-Platzhalter, API-Kommentare). Eigene Klassen/Identifier tragen den Namen
  **Sitecar** (z. B. `SitecarApp`, `SitecarApiClient`, `SitecarTheme`), damit
  es keine Namens- oder Herkunftsverwechslung mit Papras Codebasis gibt.
- Sitecar ist **inoffiziell** und stammt **nicht** von papra-hq (siehe oben).

Kurz: Weil Sitecar Papra nur als API-Client anspricht und keinen AGPL-Code
einbindet, verursacht die AGPL-3.0 von Papra keine Lizenzpflichten für dieses
Projekt.
