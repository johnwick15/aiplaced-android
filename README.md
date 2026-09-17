# CSCAPrep Native Android

Native Kotlin and Jetpack Compose client for CSCAPrep.com. The app contains no WebView and connects securely to the WordPress CSCAPrep Core API.

## Included in v1.2.3

- Five-question guest Prep preview
- Shared CSCAPrep.com accounts, progress, language, plan and Mock history
- Email/password login and password reset
- Native Sign in with Google through Android Credential Manager
- Signed, stateless Google sign-in challenges using the Web application client as the ID-token audience
- Native Stripe PaymentSheet for one-time 1, 3 and 6 month Pro access passes
- Direct server confirmation after native payment, with Stripe webhook fallback
- Prep Mode and full-screen timed Mock Exams

## Backend

Install CSCAPrep Core 1.12.45 or newer on `https://cscaprep.com` before using this app. Configure the Android OAuth client for package `com.cscaprep.app` and the signing SHA-1, and use the Web application client ID as Android Credential Manager's server client ID. Stripe keys, one-time Stripe Price IDs and the Stripe webhook remain configured in CSCAPrep settings.

## Build

The project targets Android API 35 and builds with Java 17 and Gradle 8.9. Production distribution requires a private release signing key. The included automation uses a stable development test key for directly installable development APKs.
