# Accesibilidad

An Android application that demonstrates an accessible account and communication flow. It is intended for people who benefit from choosing communication and accessibility preferences, and for evaluators reviewing the required Android UI components.

## What is implemented

| Screen | Purpose |
|---|---|
| Login | Authenticates a locally registered user; exposes account creation and password-recovery actions. |
| Registration | Collects account, communication, and accessibility preferences; shows the registered-user summary. |
| Password recovery | Accepts an email address and displays a recovery-request confirmation. |
| Communication home | Lets the signed-in user draft a message, select a quick phrase, toggle a visual notice, or sign out. |

### Navigation flow

`Login → Registration → Login → Communication home`

From Login, **Recover password** opens Password recovery and returns with the back action. A successful login opens Communication home; signing out returns to Login and removes the home screen from the back stack.

## Assignment UI-component coverage

| Required component | Implementation |
|---|---|
| Inputs | `OutlinedTextField` controls collect name, email, passwords, recovery email, and a communication message. |
| Buttons | Material `Button` controls register, sign in, request recovery, send messages, and select quick phrases. |
| Links | `TextButton` controls provide the create-account, password-recovery, return-to-login, and sign-out navigation actions. |
| Combo box | An `ExposedDropdownMenuBox` selects the communication preference. |
| Checkboxes | Registration selects zero or more accessibility preferences. |
| Radio buttons | Registration selects one primary communication mode. |
| Table | Registration renders a three-column registered-account summary: name, email, and communication preference. |
| Grid | Communication home uses a two-column `LazyVerticalGrid` for its four actions. |

## Local user storage

The app keeps users in an in-memory `Array<User?>(5)` owned by `AccesibilidadApp`. Registration writes to the first empty slot and rejects a sixth account. Login compares the entered email and password with the stored slots.

This is intentionally a demonstration-only storage model: users and session state disappear when the composition/activity is recreated, data is not persisted, and passwords are held as plain strings. It is not suitable for production authentication.

## Accessibility decisions

- Material 3 controls provide labeled text inputs and visible selected states.
- Registration and home content scroll vertically; interactive controls use full-width layouts where appropriate.
- Registration radio buttons and checkboxes use 48 dp containers; primary actions use 56 dp heights or minimum heights.
- The registration title, registered-account summary, and home welcome text are marked as headings.
- Login's password-visibility action has an explicit content description.
- Login errors use an assertive live region; home feedback uses a polite live region.
- Opening the message editor requests focus for its input.
- The home visual notice uses Material error-container colors, and the app theme supports system dark mode and dynamic color on Android 12+.

The “high contrast,” “visual alerts,” and “vibration” choices in registration are stored preferences; they do not configure device accessibility settings.

## Technology stack

- Kotlin and Jetpack Compose with Material 3
- AndroidX Navigation Compose
- Android SDK: `minSdk 24`, `compileSdk 37`, `targetSdk 37`
- JUnit 4 local tests and Compose/AndroidX instrumentation tests

## Prerequisites

- Android Studio with an installed Android SDK compatible with the project.
- A JDK compatible with the installed Android Gradle Plugin.
- An emulator or USB-connected Android device only for connected instrumentation tests.

## Build, run, and test

From the project root on Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

Open the project in Android Studio and run the `app` configuration to install the debug build. `connectedDebugAndroidTest` requires ADB plus a running emulator or connected, authorized device; it cannot be verified on a host with no connected Android target.

## Delivery and Git guidance

- Keep source, Gradle wrapper/configuration, tests, and `docs/` in the delivery ZIP.
- Exclude generated/local material such as `.git/`, `.gradle/`, `build/`, `app/build/`, `.idea/`, and `local.properties`.
- Create and push a remote Git repository before submission. The student must supply the repository URL and capture their own Git-history evidence; this documentation does not invent either.
- Capture the required application screenshots from a real emulator/device state. See [`docs/technical-report.md`](docs/technical-report.md) and [`docs/submission-checklist.md`](docs/submission-checklist.md).
