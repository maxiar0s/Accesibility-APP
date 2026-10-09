# Accesibilidad

An Android application that demonstrates an accessible account and communication flow. It is intended for people who benefit from choosing communication and accessibility preferences, and for evaluators reviewing the required Android UI components.

## What is implemented

| Screen | Purpose |
|---|---|
| Login | Asynchronously signs in with Firebase Email/Password, with loading and understandable errors. |
| Registration | Creates a Firebase account and saves submitted profile/preferences locally per uid, then signs out and returns to Login. |
| Password recovery | Requests a Firebase password-reset email and displays a generic confirmation without exposing account existence. |
| Communication home | Perfil actual plegable, mensajes hablados transcritos en texto grande, frases rápidas personalizables, historial de mensajes en Firestore, aviso visual y cierre de sesión con Firebase. |

### Navigation flow

`Login → Registration → Login → Communication home`

From Login, **Recover password** opens Password recovery and returns with the back action. Only successful sign-in or a restored Firebase current identity opens Communication home. Signing out returns to Login; state-driven routing keeps no protected navigation history. While a request is pending, duplicate submissions and route changes are disabled. Registration retains the existing return-to-Login flow rather than leaving the newly created account signed in.

## Assignment UI-component coverage

| Required component | Implementation |
|---|---|
| Inputs | `OutlinedTextField` controls collect name, email, passwords, recovery email, and a communication message. |
| Buttons | Material `Button` controls register, sign in, request recovery, send messages, and select quick phrases. |
| Links | `TextButton` controls provide the create-account, password-recovery, return-to-login, and sign-out navigation actions. |
| Combo box | An `ExposedDropdownMenuBox` selects the communication preference. |
| Checkboxes | Registration selects zero or more accessibility preferences. |
| Radio buttons | Registration selects one primary communication mode. |
| Tabla | El perfil autenticado presenta filas de etiqueta y valor para nombre, correo, preferencia, modo y accesibilidad. Solo se muestra al expandirlo. |
| Acciones | Las tres acciones de comunicación ocupan el ancho disponible y crecen con su contenido, sin una cuadrícula de altura fija; cerrar sesión permanece en la barra superior. |

## Authentication and local profile storage

`AuthGateway` is injectable; production `FirebaseAuthGateway` uses the Firebase main Authentication module. Registration, sign-in, and password reset are asynchronous. Firebase creates and automatically signs in new accounts; the adapter attempts to set the display name, then explicitly signs out. A display-name failure is reported as **account created with a warning**, not a failed creation that invites a duplicate registration. The submitted name remains a local fallback.

`AuthSession` coordinates loading, results, and routes. An activity-owned `AuthSessionViewModel` preserves in-flight state during configuration changes. UI observers detach on disposal; closing the owning scope or logging out invalidates late callback UI mutations. Form passwords exist only transiently in ordinary Compose state/SDK call arguments, not in profiles, preferences, logs, or saved-state handles. Successful operations remove the credential form from composition.

Android SharedPreferences (`local_profile`) stores current uid/email/name metadata and per-uid profile selections through `LocalProfileStore`. Firebase `currentUser`, not cached metadata, controls restoration. On restoration the app reads only the matching uid's local profile, refreshes email/name from Firebase where available, and never adopts another uid's cached name. Logout calls Firebase sign-out and removes active metadata; local per-uid preferences remain for a later login. There is no five-account limit or public account directory.

Preferences are device-local, password-free, and **not an authorization mechanism**. They do not synchronize through Firestore or configure Android accessibility settings. A restored Firebase identity is not proof that a remotely revoked token is still valid: this academic scope does not add a backend-validation gate, MFA, email-verification requirement, or production hardening. Process termination during an unfinished remote operation is not a transactional recovery guarantee.

## Accessibility decisions

- Material 3 controls provide labeled text inputs and visible selected states.
- Registration and home content scroll vertically; interactive controls use full-width layouts where appropriate.
- Registration radio buttons and checkboxes use 48 dp containers; primary actions use 56 dp heights or minimum heights.
- The registration title, current-profile summary, and home welcome text are marked as headings.
- Login's password-visibility action has an explicit content description.
- Login errors use an assertive live region; home feedback uses a polite live region.
- Opening the message editor requests focus for its input.
- El aviso visual utiliza colores de error de Material; el tema verde azulado y azul marino respeta el modo oscuro del sistema. El color dinámico es opcional y está desactivado por defecto.

The “high contrast,” “visual alerts,” and “vibration” choices in registration are stored preferences; they do not configure device accessibility settings.

### Interfaz en español y requisitos de registro

La interfaz, los estados de carga, las confirmaciones, los errores propios y las etiquetas accesibles están en español. Las frases escritas por cada usuario no se traducen. El perfil inicia cerrado: **Mostrar perfil** permite consultar los datos de la cuenta actual y **Ocultar perfil** los retira del contenido desplazable.

- **Nombre:** al menos 3 caracteres después de quitar espacios iniciales y finales; se admiten acentos y espacios interiores.
- **Contraseña de registro:** al menos 7 caracteres, con mayúscula, minúscula, número y un símbolo que no sea espacio ni letra ni número. La confirmación debe coincidir exactamente. La contraseña no se recorta ni modifica.
- **Cuentas existentes:** iniciar sesión solo requiere correo válido y contraseña no vacía; la nueva complejidad no se aplica al acceso ni a la recuperación.

Estas reglas se comparten entre formulario y controlador y se aplican **solo en este cliente**. No modifican ni demuestran la política de Firebase. Las pruebas locales cubren sus límites; compilar las pruebas Compose no equivale a ejecutarlas ni a comprobar el aspecto con texto ampliado en un teléfono.

Para actualizar en el teléfono, instala desde Android Studio la configuración `app` sobre la aplicación existente con el mismo identificador y firma, sin desinstalar ni borrar sus datos. Si Android informa una firma incompatible, no borres la aplicación: utiliza la misma configuración de firma que la instalación anterior. Este cambio no elimina cuentas ni transforma frases guardadas; la instalación en un dispositivo queda fuera de esta comprobación local.

### Voz y mensajes rápidos

Al mostrar un mensaje escrito, una frase rápida o una frase personal, **Leer en voz alta** reproduce exactamente ese texto en español. Seleccionar o enviar un mensaje no inicia audio automáticamente. **Detener voz** cancela la lectura; cambiar el mensaje, salir de Home o cambiar de cuenta también detiene la voz y libera el motor cuando corresponde.

Se necesita un motor Android TextToSpeech con datos de español disponibles. La pantalla informa la preparación, la lectura, los errores y la falta de voz española; no sustituye silenciosamente el idioma. Si no está disponible, revisa la configuración de texto a voz del dispositivo.

**Dictar mensaje** abre el servicio de reconocimiento de voz configurado en Android en español. El servicio puede requerir conexión a Internet y disponibilidad del proveedor; Android puede solicitar permiso de micrófono dentro de esa interfaz. Si el servicio no existe, el resultado está vacío o se cancela, la aplicación lo informa y no guarda ningún mensaje. La aplicación no captura ni conserva audio: solo guarda el texto reconocido en el historial de la cuenta.

En **Frases rápidas** se mantienen las frases incluidas y también se pueden crear, editar, seleccionar y eliminar frases personales. Estas últimas usan la colección existente `users/{uid}/phrases`. En el historial, **Eliminar todo el historial** pide confirmación y elimina todos los documentos de `communicationHistory` del usuario en lotes, incluidos los que no están en la lista más reciente de 100; no elimina frases personales. Si una operación falla, la pantalla muestra un error en lugar de confirmar éxito.

Las pruebas locales usan un motor falso; compilar el adaptador comprueba las API del SDK, pero no demuestra audio audible, disponibilidad de español ni funcionamiento en un dispositivo. La generación de un APK firmado sigue pendiente: consulta la [guía de firma](docs/signing-apk.md).

## Technology stack

- Kotlin and Jetpack Compose with Material 3
- AndroidX Navigation Compose
- Android SDK: `minSdk 24`, `compileSdk 37`, `targetSdk 37`
- JUnit 4 local tests and Compose/AndroidX instrumentation tests

## Prerequisites

### Firebase SDK setup

Gradle declares Google Services plugin 4.5.0 and Firebase BoM 34.19.0 with
the main Authentication and Cloud Firestore modules (no Firebase KTX modules).
Authentication and personal phrase CRUD are wired to Firebase. Profile data stays local;
database creation and security-rule publication are not performed by the app or these checks.

Local verification requires the registered app's matching `app/google-services.json`,
Android SDK platform 37, the Java 25 daemon required by this project's Gradle
configuration, and access to dependency repositories. Run
`.\gradlew.bat :app:processDebugGoogleServices` before unit tests and APK assembly.
Email/Password enablement is user-confirmed but not remotely verified here.
Provider configuration and Firestore security rules remain separately authorized external work.

### Firestore setup: personal phrases and message history

1. In your own Firebase Console project, create a **Cloud Firestore Standard edition** database with the default database ID. Choose a location appropriate for your users and production/locked rules, not open test access.
2. Open its Rules tab. Review any existing rules before changing them: do not overwrite unrelated permissions blindly. For a new database, paste the root [`firestore.rules`](firestore.rules) contents and select **Publish** manually. Existing databases require merging the owner-only phrase and `communicationHistory` matches without broad rules that also grant access.
3. Sign in and add a personal phrase or send a written message. Firestore creates `users/{uid}/phrases/{id}` and `users/{uid}/communicationHistory/{id}` documents automatically; no manual collection creation is needed. Select a saved message to display it again or confirm its individual deletion.

Phrase documents contain only `text`. Message-history documents contain only `text` and a server-generated `createdAt` timestamp, including speech transcripts; raw audio is never stored. Message text is trimmed and limited to 1–500 characters in the client and rules; the live query displays at most the latest 100 messages. Written messages and quick or personal phrases selected for communication remain in the account's cloud history until individually or collectively deleted; replaying a history item does not create another entry. The app does not save passwords or authentication tokens. A live list can contain pending local snapshots; controls stay busy until a write completes. Offline writes can remain pending until reconnection. List errors offer retry.

UID ownership comes from the authenticated session, not profile preferences. Logout/account change immediately cancels subscriptions and discards late callbacks; account-keyed Home state clears private drafts and displayed selections. The activity-owned history controller survives configuration changes and closes with its ViewModel. Unit and Compose tests inject fake gateways and do not read or write a real Firestore project.

**External setup is still pending/unknown.** The rules file is a proposal, not deployed or emulator-tested, and does not protect a remote database until you publish it. Local tests/builds do not demonstrate live persistence or remote access denial. No deployment targeting files are added.

### Android build tools

- Android Studio with an installed Android SDK compatible with the project.
- A JDK compatible with the installed Android Gradle Plugin.
- An emulator or USB-connected Android device only for connected instrumentation tests.

## Build, run, and test

From the project root on Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
git diff --check
```

Open the project in Android Studio and run the `app` configuration to install the debug build. `connectedDebugAndroidTest` requires ADB plus a running emulator or connected, authorized device; it cannot be verified on a host with no connected Android target.

Unit tests use deterministic fake authentication, phrase/history gateways, and an in-memory `PreferenceValues` backing store. They cover loading/results, registration warnings, identity-based restoration, uid isolation, logout, recovery, serialization, phrase CRUD, communication-history validation/CRUD, listener cancellation, and stale callbacks. They do not exercise real Firebase or Android SharedPreferences runtime. Compose tests inject fake sessions and controllers, including phrase and message-history selection/deletion flows; compiling them is not executing them.

### Remaining runtime and distribution checks

With separate authorization, manually verify registration/sign-in, name-update feedback, restart restoration, logout/back behavior, and delivered password-reset links on a device against the configured service. Check Android preference persistence and accessible layouts on that device. Personal phrase persistence and owner-only access require external database/rules setup and live checks; profile synchronization and signed release distribution are not implemented. No service account, real account creation, reset email, device run, or release publication is part of this work unit.

## Delivery and Git guidance

- Keep source, Gradle wrapper/configuration, tests, and `docs/` in the delivery ZIP.
- Exclude generated/local material such as `.git/`, `.gradle/`, `build/`, `app/build/`, `.idea/`, and `local.properties`.
- Repository: [Accesibility-APP](https://github.com/maxiar0s/Accesibility-APP).
- The final report and delivery checklist are in [`docs/Exp1_S2/S2_ Formato de respuesta_A_Actividad_2.docx`](docs/Exp1_S2/S2_%20Formato%20de%20respuesta_A_Actividad_2.docx). Export the DOCX to PDF with Microsoft Word or LibreOffice before submission.
