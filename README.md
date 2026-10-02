# Tanami

Tanami is an offline-first Android guide for growing quick-harvest vegetables at home across Southeast Asia. The Android application ID is `fi.dev.tanami`.

## Current stage

The first development stage establishes the native Android project, Compose UI foundation, app identity, and Firebase AI Logic dependency. Plant guides and garden tracking are local-first. Photo diagnosis uses Gemini through Firebase AI Logic when a Firebase project is configured; the app remains usable without Firebase configuration.

## Build

Open this directory in Android Studio or run `./gradlew :app:assembleDebug` with Android SDK 36 installed.

## Firebase image analysis setup

1. Register an Android app with application ID `fi.dev.tanami` in Firebase.
2. Enable Firebase AI Logic and select the Gemini Developer API to start with its no-cost tier.
3. Download `google-services.json` into `app/` (the file is ignored by Git).
4. For local debug builds, configure Firebase App Check's debug provider in Firebase Console. Production uses Play Integrity.

Planting timelines and care guidance in this prototype are starter estimates. The catalog labels the start point for each harvest range; verify the seed variety and local conditions before expanding these into definitive regional advice. The home screen stores a selected Southeast Asian country locally. City, altitude, and seasonal adaptation are not implemented yet.
