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
- Darstellung wählbar: Hell / Dunkel / System.
- Schnellzugriff-Shortcut "Scannen" per Long-Press auf das App-Icon.
- Anpassbares Dateinamen-Format für Scans (Platzhalter `{date}` `{time}`
  `{org}` `{counter}`).
- Freiwilliger "Sitecar unterstützen"-Hinweis (einmalig 0,99 €, Play Billing),
  erscheint nach dem 5. erfolgreichen Upload, danach alle 10 weiteren — oder
  jederzeit manuell in den Einstellungen. Rein kosmetischer Dank: schaltet
  drei weitere Akzentfarben frei (Smaragd/Bernstein/Rosé statt nur Indigo),
  inklusive passend eingefärbtem App-Icon-Hintergrund. Kein bestehendes
  Feature wird Nicht-Unterstützern weggenommen oder vorenthalten.

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
  "Keine Daten erhoben" angeben. Der In-App-Kauf läuft vollständig über
  Google Play Billing; Zahlungsdaten sieht der Entwickler nie.
- **Play Billing Library aktuell halten**: Google erzwingt regelmäßig
  Mindestversionen für die Billing Library (aktuell `7.1.1` in
  `libs.versions.toml`), sonst wird der Play-Store-Upload irgendwann
  abgelehnt — vor dem Release-Build kurz gegen die aktuelle Version prüfen.
- **In-App-Produkt anlegen**: In der Play Console unter
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

MIT (TBD).
