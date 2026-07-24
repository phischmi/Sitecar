<p align="center">
  <img src="docs/logo.svg" width="96" height="96" alt="Sitecar Logo">
</p>

# Sitecar (for Papra)

Schlanke native Android-App zum Aufnehmen und Hochladen von Dokumenten an eine
selbst gehostete [Papra](https://github.com/papra-hq/papra)-Instanz — ein
eigenständiger Begleiter ("Sidecar") für Papra, vormals PapraCam.

- Inoffiziell, nicht von papra-hq
- Anbindung via API-Key (kein OAuth/Login)
- Kotlin + Jetpack Compose, ML Kit Document Scanner, Ktor

"Sitecar (for Papra)" ist der volle Name (z. B. für den Play-Store-Eintrag);
auf dem Gerät selbst (Launcher-Icon, Kürzel, App-Info) bleibt es bewusst
kurz **Sitecar** — `R.string.app_name`/`android:label` sind unverändert
"Sitecar". Der volle Store-Titel wird in der Play Console gepflegt (Store-
Eintrag → App-Name), nicht im Code.

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
- Mehrfachauswahl: langes Drücken auf ein Dokument aktiviert den
  Auswahlmodus (weitere Dokumente per Tippen dazu-/abwählen), die Titelleiste
  wechselt zu "N ausgewählt" mit Aktionen zum gemeinsamen Löschen oder
  Tags-Bearbeiten. Wischgesten sind während der Auswahl deaktiviert, System-
  Zurück verlässt den Auswahlmodus statt den Bildschirm zu verlassen.
- "Teilen an Sitecar" (Opt-in, standardmäßig aus — Einstellungen → "Teilen
  an Sitecar"): Bilder und PDFs aus anderen Apps (Mail-Anhänge, Galerie,
  Chat, Rechnungs-/Anbieter-Apps) lassen sich per Android-Share-Sheet direkt
  an Sitecar senden und durchlaufen denselben Upload-Screen wie ein
  Kamera-Scan. Geteilte Bilder werden wie mehrseitige Scans behandelt (zu
  einem PDF zusammengeführt, optional mit OCR); ein geteiltes PDF wird
  unverändert hochgeladen. Bei mehreren geteilten Dateien zählt nur die
  erste. Erkannte MIME-Typen: `image/*`, `application/pdf` sowie
  `application/octet-stream` (manche Apps deklarieren PDFs darüber, z. B.
  wenn ihr FileProvider keinen expliziten Typ zuordnet — Sitecar korrigiert
  den Typ dann anhand der `.pdf`-Dateiendung). Taucht Sitecar in einer
  bestimmten App trotz aktiviertem Opt-in nicht im Share-Sheet auf, teilt
  diese App vermutlich über einen anderen Mechanismus als Androids
  Standard-`ACTION_SEND` (z. B. einen eigenen "Exportieren"-Button, der
  direkt ins Dateisystem speichert) — das kann keine Ziel-App abfangen.
  Technisch per activity-alias umgesetzt (`.ShareReceiver`), damit sich die
  Sichtbarkeit zur Laufzeit umschalten lässt, ohne dass Sitecar sonst
  irgendwo im Share-Sheet auftaucht.
- Intelligente Vorschläge beim Hochladen (regelbasiert, komplett on-device,
  abschaltbar in den Einstellungen): erkennt Tags aus einem kleinen
  Schlüsselwort-Vokabular. Existiert der erkannte Tag bereits in der
  Organisation, ist er vorausgewählt; existiert er noch nicht, wird er nur
  als Vorschlag angezeigt und erst beim manuellen Auswählen tatsächlich
  angelegt. Erkennt außerdem das Dokumentdatum (z. B. Rechnungsdatum) und
  den Absender (Briefkopf-Firmenname oder ein "Von:"/"Absender:"-Label) als
  Alternative zum Scan-Datum bzw. zur Organisation im Dateinamen.
- Optional: KI auf dem Gerät (Gemini Nano via Android AICore / ML Kit GenAI
  Prompt API, in den Einstellungen separat aktivierbar) ergänzt die
  regelbasierten Vorschläge um echtes Sprachverständnis statt eines festen
  Schlüsselwort-Vokabulars — läuft komplett on-device, es verlassen keine
  Daten das Gerät, nur der einmalige geräteweite Modell-Download braucht
  Netzwerk. Benötigt ein Gemini-Nano-kompatibles Gerät; ist die KI nicht
  verfügbar, fehlgeschlagen oder zu langsam (Timeout), greifen automatisch
  weiterhin die regelbasierten Vorschläge.
- Duplikat-Warnung: vergleicht einen Perceptual-Hash der ersten Seite mit den
  letzten Uploads derselben Organisation (rein lokal, 14 Tage Gedächtnis,
  kein Download bestehender Dokumente) und warnt unaufdringlich, falls ein
  ähnliches Dokument kürzlich schon hochgeladen wurde — blockiert den Upload
  nicht, ist nur ein Hinweis.
- Pull-to-refresh zum Aktualisieren der Dokumentenliste.
- Darstellung wählbar: Hell / Dunkel / System.
- Schnellzugriff-Shortcut "Scannen" per Long-Press auf das App-Icon.
- Anpassbares Dateinamen-Format für Scans (Platzhalter `{date}` `{time}`
  `{org}` `{sender}` `{counter}`), mit In-App-Hilfe (Info-Icon neben dem Feld
  in den Einstellungen) zur Erklärung der Platzhalter.
- Konfigurierbare Wischgesten in der Dokumentenliste (links/rechts getrennt
  einstellbar in den Einstellungen): Löschen, Umbenennen oder Tags
  bearbeiten. Löst nie direkt die Aktion aus, sondern öffnet den jeweiligen
  Dialog wie im "..."-Kontextmenü — kein versehentliches Löschen durch
  Wischen. Standard: rechts wischen → Tags, links wischen → Löschen.
- _(Derzeit deaktiviert — die App wird zunächst vollständig kostenfrei
  ausgeliefert, siehe [Bezahlfunktion](#bezahlfunktion-vorerst-deaktiviert).)_
  Freiwilliger "Sitecar unterstützen"-Hinweis (einmalig 0,99 €, Play Billing),
  erscheint nach dem 5. erfolgreichen Upload, danach alle 10 weiteren — oder
  jederzeit manuell in den Einstellungen. Rein kosmetischer Dank: schaltet
  vier weitere Akzentfarben frei (Smaragd/Bernstein/Rosé/Papra statt nur
  Indigo). "Papra" reproduziert bewusst Papras eigenes Original-Farbschema
  (Orange im Hellmodus, Neon-Lime/-Gelb im Dunkelmodus) statt der für Indigo
  gewählten Abkehr davon. Jeder Akzent-Kreis in den Einstellungen zeigt
  Hell- und Dunkelmodus-Farbe diagonal geteilt in einem Kreis (oben links /
  unten rechts), unabhängig vom aktuell aktiven App-Theme. Jede Akzentfarbe
  hat ein eigenfarbenes App-Icon (activity-alias); beim Icon wird bewusst
  immer die (dunklere, kontrastreichere) Hellmodus-Akzentfarbe verwendet
  (z. B. Papras Orange statt des helleren Neon-Lime). Kein bestehendes
  Feature wird Nicht-Unterstützern weggenommen oder vorenthalten.

## Bezahlfunktion (vorerst deaktiviert)

Die freiwillige Unterstützer-Bezahlfunktion ist über den zentralen Schalter
`Features.SUPPORTER_ENABLED` (in `app/src/main/java/app/sitecar/client/Features.kt`)
**abgeschaltet** (`false`). Akzentfarben-Auswahl und Unterstützer-Bereich in
den Einstellungen sind davon unabhängig über `Features.SUPPORTER_PREVIEW_ENABLED`
(aktuell `true`) zum Testen/Vorschauen freigeschaltet: alle fünf Farben sind
ohne Sperre wählbar, und der Unterstützer-Bereich zeigt den "bereits
Unterstützer"-Zustand (Badge) statt einer Kauf-CTA, da ohne echte
Bezahlfunktion kein In-App-Produkt zum Kaufen existiert. Sobald
`SUPPORTER_ENABLED` wieder aktiviert wird, greifen an beiden Stellen
automatisch wieder die echte Unterstützer-Sperre bzw. der echte Kauf-Flow,
ganz ohne weitere Code-Änderung. Solange die Bezahlfunktion aus ist:

- kein In-App-Kauf, kein Spenden-Dialog, keine Kauf-Aufforderung nach Uploads;
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

Debug-Builds werden immer mit dem im Repo eingecheckten `debug.keystore`
signiert (Androids öffentlich bekannter, nicht-geheimer Standard-Debug-Key —
siehe `app/build.gradle.kts`), egal ob lokal oder per GitHub Action gebaut.
So lässt sich eine lokal gebaute Debug-APK jederzeit über eine per Action
gebaute installieren (und umgekehrt), ohne
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Kam eine ältere Debug-APK vor dieser
Änderung mit einem anderen (automatisch generierten) Debug-Key auf das
Gerät, hilft einmalig `adb uninstall app.sitecar.client.debug` vor der
nächsten Installation.

### Debug-APK per GitHub Action

Unter **Actions → Build APK → Run workflow** lässt sich manuell ein Build der
aktuellsten Version des jeweiligen Branches anstoßen. Nach Abschluss steht die
APK als Artifact (`sitecar-<version>-debug-<sha>`) zum Download bereit. Lokal
installieren:

```sh
adb install sitecar-*.apk
```

## API-Key in Papra anlegen

Im Papra-Web-UI unter **Settings → API Keys** einen Key mit der Berechtigung
`documents:create` (und idealerweise `organizations:read` für die Org-Liste)
erzeugen. Der Key beginnt mit `ppapi_`.

**Papierkorb-Aktionen (Wiederherstellen, endgültig löschen, Papierkorb
leeren) funktionieren aktuell mit keinem API-Key** — das ist kein
Scope-/Berechtigungsproblem des eigenen Keys, sondern ein serverseitiger Bug
in Papra selbst: Die drei zugehörigen Routen (`POST .../restore`,
`DELETE .../documents/trash/:documentId`, `DELETE .../documents/trash`)
registrieren ihre `requireAuthentication()`-Middleware ohne
`apiKeyPermissions`-Angabe. In `isAuthenticationValid()`
(`auth.models.ts`) führt das im API-Key-Zweig zu `if (!requiredApiKeyPermissions)
return false` — API-Key-Auth wird für diese drei Routen also unabhängig von
den Scopes des Keys grundsätzlich abgelehnt (nur eine eingeloggte
Browser-Session kommt durch). Sitecar zeigt dafür einen eigenen Hinweistext
statt der irreführenden "API-Key prüfen"-Meldung; als Workaround bleibt
vorerst nur die Papra-Weboberfläche für diese drei Aktionen.

## Hinweise

- `usesCleartextTraffic="true"` ist gesetzt, damit auch HTTP-Self-Hosted-Instanzen
  funktionieren. Für reine HTTPS-Setups später per `networkSecurityConfig` einengen.
- API-Key und Server-URL liegen in `EncryptedSharedPreferences` (AES-256, Android
  Keystore) und werden nie geloggt; das API-Key-Feld ist zusätzlich als
  Passwortfeld maskiert.
- Min-SDK 26 (Android 8), Target-SDK 35.
- Der ML Kit Document Scanner lädt sein Modell beim ersten Start on-demand
  über Google Play Services nach (einmalig ~ein paar MB). Setzt Play Services voraus.
- Die App selbst hat keine `CAMERA`-Permission — der Scanner läuft als
  separate Activity in Play Services und fordert seine Permissions selbst an.
- Texterkennung (OCR) auf dem Gerät lässt sich in den Einstellungen abschalten,
  falls die Papra-Instanz bereits serverseitig OCR durchführt (z. B. Mistral OCR).
- Die optionale on-device KI (Gemini Nano) läuft über Android AICore und die
  `com.google.mlkit:genai-prompt`-Bibliothek (aktuell Beta). Unterstützte Geräte
  siehe [ML-Kit-GenAI-Übersicht](https://developers.google.com/ml-kit/genai#device-support);
  ohne Unterstützung bleibt der Schalter in den Einstellungen inaktiv und eine
  Meldung erklärt das. Der Modell-Download ist geräteweit und einmalig (wird
  ggf. auch von anderen Apps mitgenutzt, die dieselbe ML-Kit-GenAI-API
  verwenden).
- Die Akzentfarben-Icons werden über fünf `activity-alias`-Einträge im Manifest
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
- **Datenschutzerklärung**: Inhalt liegt in [`PRIVACY.md`](./PRIVACY.md) sowie
  als eigenständige HTML-Seite im separaten, öffentlichen Repo
  [`phischmi/sitecar-privacy`](https://github.com/phischmi/sitecar-privacy)
  (getrennt vom privaten App-Repo, da GitHub Pages aus privaten Repos ohne
  GitHub Pro nicht öffentlich veröffentlicht werden kann). Sobald dort unter
  Settings → Pages die Veröffentlichung aktiviert ist (Source: `main` /
  `/ (root)`), muss die resultierende URL noch in der Play Console verlinkt
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
- **Store-Assets**:
  - Hi-Res-Icon (512×512, 32-Bit-PNG mit Alpha-Kanal) liegt als
    [`docs/play-icon-512.png`](./docs/play-icon-512.png) bereit (Quelle:
    [`docs/play-icon-512.svg`](./docs/play-icon-512.svg) — volles
    quadratisches Bleed ohne eigene Formmaskierung, da Play Store das Icon
    je nach Gerät selbst maskiert).
  - Vorstellungsgrafik (1024×500, 24-Bit-PNG ohne Alpha-Kanal) liegt in
    Englisch ([`docs/play-feature-graphic-1024x500-en.png`](./docs/play-feature-graphic-1024x500-en.png))
    und Deutsch ([`docs/play-feature-graphic-1024x500-de.png`](./docs/play-feature-graphic-1024x500-de.png))
    bereit, jeweils mit SVG-Quelle daneben.
  - Screenshots fehlen noch — dafür wird ein echtes Gerät/Emulator benötigt.
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
