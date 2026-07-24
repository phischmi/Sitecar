<p align="center">
  <img src="docs/logo.svg" width="96" height="96" alt="Sitecar Logo">
</p>

# Sitecar (for Papra)

Schlanke native Android-App zum Scannen und Hochladen von Dokumenten an eine
selbst gehostete [Papra](https://github.com/papra-hq/papra)-Instanz — ein
eigenständiger Begleiter ("Sidecar") für Papra.

Inoffiziell, nicht von papra-hq. Anbindung ausschließlich über Papras
öffentliche HTTP-API per API-Key (kein OAuth/Login).

## Features

- **Server-Anbindung**: Server-URL und API-Key in den Einstellungen
  (verschlüsselt gespeichert), Verbindungstest gegen `GET /api/organizations`.
- **Scannen**: Mehrseitige Dokumente mit automatischer Kantenerkennung und
  Perspektivkorrektur (Google ML Kit Document Scanner, on-device),
  Zusammenführung zu einem PDF.
- **On-device OCR** (optional): unsichtbarer OCR-Textlayer für Volltextsuche
  ohne serverseitige OCR (ML Kit Text Recognition), abschaltbar.
- **Teilen an Sitecar** (Opt-in): Bilder und PDFs aus anderen Apps per
  Android-Share-Sheet direkt an Sitecar senden.
- **Dokumente verwalten**: Liste mit Miniatur-Vorschau, Volltextsuche und
  Sortierung, Herunterladen/Öffnen, Umbenennen, Löschen (Papierkorb) sowie
  Wiederherstellen und endgültiges Löschen im Papierkorb-Tab.
- **Tags**: Anlegen, Bearbeiten, Löschen und Zuweisen von Tags; Filtern per
  Suchsyntax `tag:Name` (derselbe Parser wie die Papra-Weboberfläche).
- **Mehrfachauswahl**: langes Drücken aktiviert den Auswahlmodus für
  gemeinsames Löschen oder Tag-Bearbeiten.
- **Intelligente Vorschläge** (optional, on-device): regelbasierte Erkennung
  von Tags, Dokumentdatum und Absender aus dem OCR-Text; optional ergänzt um
  on-device KI (Gemini Nano via ML Kit GenAI Prompt API).
- **Duplikat-Warnung**: lokaler Perceptual-Hash-Abgleich der letzten Uploads
  (kein Server-Download), warnt unaufdringlich bei Ähnlichkeit.
- **Anpassbar**: Theme (Hell/Dunkel/System), konfigurierbare Wischgesten,
  anpassbares Dateinamen-Format (`{date}` `{time}` `{org}` `{sender}`
  `{counter}`), Schnellzugriff-Shortcut "Scannen" per Long-Press aufs Icon.

## Technik

- Kotlin + Jetpack Compose (Material 3)
- [Ktor](https://ktor.io/) HTTP-Client, kotlinx.serialization
- Google ML Kit (Document Scanner, Text Recognition, GenAI Prompt)
- PDFBox-Android für PDF-Erzeugung und -Textextraktion
- Min-SDK 26 (Android 8), Target-SDK 35
- Standardsprache Englisch (`res/values/strings.xml`), Deutsch als Override
  (`res/values-de/strings.xml`); alle UI-Texte laufen über `stringResource`.

## Build

Voraussetzungen: Android Studio (Ladybug/Meerkat oder neuer), JDK 17.

**Android Studio**: Verzeichnis öffnen, Gradle synchronisieren, `Run` (▶) auf
einem Gerät oder Emulator.

**CLI** (mit lokal installiertem `gradle`):

```sh
gradle wrapper
./gradlew assembleDebug
./gradlew installDebug   # auf verbundenes Gerät installieren
```

Debug-Builds werden mit dem eingecheckten `debug.keystore` signiert (Androids
öffentlich bekannter Standard-Debug-Key), sodass lokal und per GitHub Action
gebaute Debug-APKs untereinander installierbar bleiben.

Über **Actions → Build APK → Run workflow** lässt sich ein Build des jeweiligen
Branches anstoßen; die APK steht danach als Artifact zum Download bereit.

## API-Key in Papra anlegen

Im Papra-Web-UI unter **Settings → API Keys** einen Key mit der Berechtigung
`documents:create` (und `organizations:read` für die Org-Liste) erzeugen. Der
Key beginnt mit `ppapi_`.

> Papierkorb-Aktionen (Wiederherstellen, endgültig löschen, leeren)
> funktionieren aktuell mit keinem API-Key — ein serverseitiger Bug in Papra
> lehnt API-Key-Auth für diese drei Routen unabhängig von den Scopes ab. Als
> Workaround bleibt dafür vorerst die Papra-Weboberfläche.

## Hinweise

- `usesCleartextTraffic="true"` ist gesetzt, damit auch HTTP-Self-Hosted-
  Instanzen funktionieren.
- API-Key und Server-URL liegen in `EncryptedSharedPreferences` (AES-256,
  Android Keystore) und werden nie geloggt.
- Der ML Kit Document Scanner lädt sein Modell beim ersten Start on-demand über
  Google Play Services nach (setzt Play Services voraus). Die App selbst hat
  keine `CAMERA`-Permission — der Scanner läuft als separate Play-Services-
  Activity.
- Die optionale on-device KI (Gemini Nano) benötigt ein kompatibles Gerät;
  ohne Unterstützung bleibt der Schalter inaktiv. Modell-Download ist geräteweit
  und einmalig.

## Datenschutz

Die App erhebt keine Daten für den Entwickler oder Dritte. Details in
[`PRIVACY.md`](./PRIVACY.md); dieselbe Erklärung liegt als Webseite unter
[`docs/privacy.html`](./docs/privacy.html) und lässt sich per GitHub Pages
(Settings → Pages → Source: `main` / `/docs`) öffentlich veröffentlichen.

## Lizenz

Sitecar-Code: **MIT** – siehe [`LICENSE`](./LICENSE).

[Papra](https://github.com/papra-hq/papra) selbst steht unter **AGPL-3.0**.
Sitecar ist davon nicht betroffen: Es ist eine eigenständige, unabhängig
geschriebene App, enthält keinen Papra-Code und spricht Papra ausschließlich
über dessen öffentliche HTTP-API an. Der bloße Aufruf einer Netzwerk-API macht
einen Client nach GPL/AGPL-Verständnis nicht zu einem abgeleiteten Werk, sodass
die AGPL-Copyleft-Pflichten nicht auf Sitecar übergreifen. Der Name „Papra"
wird nur nominativ verwendet, um den kompatiblen Zielserver zu benennen.
