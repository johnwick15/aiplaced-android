# CSCAPrep Android

Android wrapper for `https://cscaprep.com/`. The WordPress plugin remains the source of the interface and business logic, so normal site updates appear in the app without rebuilding the APK.

## Included behavior

- Persistent WordPress login cookies and local storage
- JavaScript, exam timers, RTL languages and full-screen Mock Mode
- Android back navigation
- File chooser and HTTP downloads
- Native Android sharing for website share requests
- Internal CSCAPrep links stay in the app; unrelated links open in the appropriate Android app
- Connection error screen with retry
- No PWA or service worker dependency

## Build

GitHub Actions builds and signs the development APK. For Play Store distribution, replace the development signing key with a private release key and create an Android App Bundle.
