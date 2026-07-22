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
  "Keine Daten erhoben" angeben.
- **Store-Assets**: Screenshots, Feature-Grafik, kurze/lange Beschreibung
  fehlen noch.
- **`applicationId`**: aktuell weiterhin `app.papra.uploader` (historisch, vor
  der Umbenennung zu Sitecar). Lässt sich nach der ersten Veröffentlichung
  praktisch nicht mehr ändern, ohne den Store-Eintrag/die Bewertungen zu
  verlieren — letzte Gelegenheit für eine Umbenennung ist jetzt.
- **Versionierung**: `versionCode`/`versionName` vor dem ersten Upload final
  festlegen (aktuell `1` / `0.1.0`).

## Lizenz

MIT (TBD).
