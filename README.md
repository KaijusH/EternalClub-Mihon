# EternalClub Mihon extension

Standalone English Mihon source for **The Eternal Club** at https://eternalclubmanga.com.

## Build an APK

This repository is a lightweight project wrapper. GitHub Actions checks out the current
Keiyoushi extension framework, overlays the EternalClub module, and builds a standalone
debug APK.

1. Open the **Actions** tab.
2. Select **Build EternalClub APK**.
3. Choose **Run workflow**.
4. Open the completed run and download the `EternalClub-debug-apk` artifact.
5. Extract the ZIP and install the APK on Android.

## Extension details

- Source: Eternal Club
- Language: English (`en`)
- Website: https://eternalclubmanga.com/
- Package: `eu.kanade.tachiyomi.extension.en.eternalclub`

The adapter does not host or redistribute images. It reads chapter metadata and image
URLs from the website at runtime. Website markup or CDN changes may require selector
updates. This is an initial implementation and must pass the GitHub Actions build and
device testing before it can be considered working.
