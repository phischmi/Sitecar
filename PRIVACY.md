# Privacy Policy – Sitecar for Papra

Last updated: 2026-07-22

_Deutsche Fassung: [`PRIVACY.de.md`](./PRIVACY.de.md)_

Sitecar is an unofficial, standalone companion app for self-hosted
[Papra](https://github.com/papra-hq/papra) instances. Sitecar is developed by
an individual and is not affiliated with papra-hq.

## What data is processed?

- **Server URL and API key** of your Papra instance: stored exclusively
  locally on your device, encrypted via `EncryptedSharedPreferences` (AES-256,
  Android Keystore). They only leave the device as part of the direct
  HTTPS/HTTP requests to the Papra server address you provide. The API key
  field on the settings screen is additionally masked as a password field. The
  app never writes the server URL or API key to logs
  (`android:allowBackup="false"` additionally ensures the encrypted settings
  cannot leave the device via Android backups).
- **Scanned documents**: cached on the device (app cache) and uploaded solely
  to the Papra instance you configured. The app developer has no access to
  these contents.
- **On-device processing**: edge detection/perspective correction (ML Kit
  Document Scanner), optional text recognition (ML Kit Text Recognition) and
  PDF creation run entirely locally on the device. No image or text data is
  transmitted to Google or third parties.

## What is NOT collected?

- No analytics, no tracking, no ads.
- No transmission of usage data to the app developer.
- No sharing of data with third parties by the app itself.

## Permissions

- **Internet**: for communication with your Papra instance.
- **Camera**: not requested directly by Sitecar — the ML Kit scanner runs as a
  separate Google Play Services activity and requests the camera permission
  itself.

## Responsibility for the Papra server

Sitecar only sends data to the Papra instance you configured yourself. The
operator of that instance — usually you — is responsible for handling this
data on the server (storage, any server-side OCR processing, backups, etc.).

## Contact

Questions about this privacy policy or the app: via the issues of the GitHub
repository.
