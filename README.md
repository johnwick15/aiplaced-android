# CSCAPrep Native Android

Native Kotlin and Jetpack Compose client for CSCAPrep.com. The app contains no WebView and connects securely to the WordPress CSCAPrep Core API.

## Included in v1.2.0

- Five-question guest Prep preview
- Shared CSCAPrep.com accounts, progress, language, plan and Mock history
- Email/password login and password reset
- Native Sign in with Google through Android Credential Manager
- Native Stripe PaymentSheet for one-time 1, 3 and 6 month Pro access passes
- Prep Mode and full-screen timed Mock Exams

## Backend

Install CSCAPrep Core 1.12.41 or newer on `https://cscaprep.com` before using this app. Configure the Google Web Client ID, Stripe keys, one-time Stripe Price IDs and Stripe webhook in CSCAPrep settings.

## Build

The project targets Android API 35 and builds with Java 17 and Gradle 8.9. Production distribution requires a private release signing key. The included automation uses a stable development test key for directly installable development APKs.
