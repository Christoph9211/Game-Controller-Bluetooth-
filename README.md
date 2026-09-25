# Bluetooth Gamepad

Bluetooth Gamepad is an Android controller interface built with Jetpack Compose. You can interact with an on-screen gamepad, arrange its controls, save layout profiles, and try haptic feedback on the phone.

**Current status:** this repository is a UI prototype. Device discovery, pairing, connection status, RSSI, latency, packet counts, and the 100-packet burst test use seeded or generated data. Controller input is kept in app state and is not sent to another device as Bluetooth HID reports. The app does not currently turn a phone into a working wireless gamepad.

## What you can use now

- An on-screen controller with sticks, D-pad, triggers, bumpers, and face buttons. Touch input updates local state and can vibrate the phone.
- A layout editor for moving, scaling, adding, and removing controls. It includes default, FPS, fighting, and southpaw arrangements.
- Saved layout profiles backed by a local Room database. The profile manager can load, duplicate, rename, and delete profiles.
- A quick actions drawer for local settings such as haptics, profile selection, and displayed polling rate.
- Discovery and diagnostics screens for exploring the planned interface. Their devices and link measurements are demo data, not readings from nearby hardware.

## Run the app

1. Open this folder as an Android project in Android Studio.
2. Install the Android SDK platform required by `compileSdk` in [`app/build.gradle.kts`](app/build.gradle.kts) (API 36 with minor API level 1), and use a JDK supported by the configured Android Gradle Plugin.
3. Configure Gradle to use a local installation compatible with the version in [`gradle/wrapper/gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties) (9.3.1). This checkout has a wrapper properties file but does **not** include `gradlew`, `gradlew.bat`, or `gradle-wrapper.jar`.
4. For a debug build, provide `debug.keystore` in the repository root. The debug signing configuration explicitly points there; the file is ignored by Git. If you prefer Android's standard generated debug key, update the debug signing configuration in `app/build.gradle.kts` first.
5. Sync the project, select an emulator or Android device running API 24 or newer, and run the `app` configuration.

With a suitable local Gradle installation, the equivalent command from the repository root is:

```powershell
gradle :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/` after a successful build. The app does not need an API key to display its current screens. `.env.example` describes an optional Gemini key convention, but the app source does not make Gemini calls.

## Explore the interface

Start on **Controller** and touch the controls to see local input and haptic feedback. Open **Customize** to change an arrangement, then save it or use the saved layouts manager to load a profile. **Discovery** shows sample targets and simulates connecting when you select one. **Diagnostics** shows generated link values and a simulated burst result. The quick actions drawer changes settings in the current app session. Saved layouts persist in Room; the simulated connect action also writes a paired-device record.

The Bluetooth permissions in `AndroidManifest.xml` describe intended functionality. They do not make the demo discovery or connection flow real, and this checkout has no runtime Bluetooth permission request or HID registration code.

## Project structure

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/example/MainActivity.kt` | Compose entry point and screen navigation |
| `app/src/main/java/com/example/ui/screens/` | Controller, layout editor, discovery, diagnostics, and quick actions UI |
| `app/src/main/java/com/example/ui/components/` | Reusable controller and dashboard components |
| `app/src/main/java/com/example/viewmodel/GamepadViewModel.kt` | Screen state, demo devices and telemetry, input callbacks, and layout operations |
| `app/src/main/java/com/example/data/local/` | Room entities, DAOs, database, and layout serialization |
| `app/src/main/java/com/example/util/HapticFeedbackManager.kt` | Android phone vibration effects |
| `app/src/test/`, `app/src/androidTest/` | Local and device test sources |

See [Architecture](docs/ARCHITECTURE.md) for the current data flow and the gap between the prototype and a Bluetooth HID controller.

## Tests

The project contains JUnit/Robolectric tests and one Android instrumented context test. With the local Gradle setup above:

```powershell
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

The connected test requires an emulator or attached device. Tests in this repository do not verify Bluetooth pairing, transmitted HID input, or measured radio latency.

## Release signing

The release build expects `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables. The key alias is `upload`. If `KEYSTORE_PATH` is unset, the build configuration points to `my-upload-key.jks` in the repository root. Keep signing keys and passwords outside Git. A successful release build produces an APK under `app/build/outputs/apk/release/`; it does not establish that the Bluetooth feature works.

## Current limitations

- There is no Bluetooth scanner, pairing API, HID device service, report descriptor, or report sender in the app source.
- The sample device list includes named products and a connected Windows host for UI demonstration only. No Windows bridge implementation is in this repository.
- The diagnostics loop generates values with `Random`; the burst action returns fabricated latency and zero packet loss.
- UI options such as gyro aim, turbo, deadzone, and polling rate update state or feedback, but do not change a remote controller stream.
- The Room database uses destructive migration fallback, so a future schema version change can erase saved local data unless migrations are added.

There is no license file in this checkout.
