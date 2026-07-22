# Sitecar (Android)

Schlanke native Android-App zum Aufnehmen und Hochladen von Dokumenten an eine
selbst gehostete [Papra](https://github.com/papra-hq/papra)-Instanz — ein
eigenständiger Begleiter ("Sidecar") für Papra, vormals PapraCam.

- Inoffiziell, nicht von papra-hq
- Anbindung via API-Key (kein OAuth/Login)
- Kotlin + Jetpack Compose, ML Kit Document Scanner, Ktor

## Features (MVP)

- Server-URL und API-Key in den Einstellungen hinterlegen (verschlüsselt gespeichert).
- Verbindungstest gegen `GET /api/organizations`.
- Dokument scannen mit automatischer Kantenerkennung und Perspektivkorrektur
  (Google ML Kit Document Scanner, on-device).
- Vorschau, Auswahl der Ziel-Organisation, Anpassen des Dateinamens.
- Upload via `POST /api/organizations/:organizationId/documents` (Multipart, JPEG).
- Vorhandene Dokumente einer Organisation ansehen (`GET /api/organizations/:organizationId/documents`),
  Tippen lädt die Datei herunter und öffnet sie im System-Viewer.

## Geplant (Phase 2, In-App-Kauf)

- Mehrseitige Scans
- Durchsuchbares PDF (OCR-Layer per ML Kit Text Recognition)
- Upload als PDF an Papra

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

## Lizenz

MIT (TBD).
