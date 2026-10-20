# Kharach Book Application

An Android personal-finance application built with Kotlin and Jetpack Compose.

## Run locally

**Prerequisite:** [Android Studio](https://developer.android.com/studio)

1. Open this project directory in Android Studio and allow Gradle sync to finish.
2. For Firebase-backed features, download `google-services.json` for your Firebase Android app and place it in `app/google-services.json`. This project-specific file is intentionally not tracked.
3. If using Gemini features, create a `.env` file in the project root and set `GEMINI_API_KEY` (see `.env.example`).
4. Run the app on an emulator or a connected Android device.

The Firestore security rules are in `firestore.rules`.
