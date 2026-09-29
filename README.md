# Bluetooth Gamepad — Compose controller with Bluetooth transport

**Project label: COMPOSE CONTROLLER — HID TRANSPORT + EXPERIMENTAL WINDOWS BRIDGE**

This is a separate Jetpack Compose controller app with real Android Bluetooth HID transport, a Room-backed layout editor, saved profiles, floating sticks, and phone haptics. It also contains the experimental Windows Xbox bridge in `windows/`. Host state comes from Android Bluetooth callbacks and bonded devices; diagnostics measure the local sender. **Implementation is present; physical Bluetooth, Windows driver, gameplay, and end-to-end latency acceptance remain unverified.**

## Related controller projects

| Repository | Role |
| --- | --- |
| [Android-Bluetooth-Gamepad](https://github.com/Christoph9211/Android-Bluetooth-Gamepad) | Main controller project: native HID and experimental Windows bridge |
| [Bluetooth-Gamepad-Studio-](https://github.com/Christoph9211/Bluetooth-Gamepad-Studio-) | Related controller copy with its own build/signing configuration; may diverge from the main project |
| [Game-Controller-Bluetooth-](https://github.com/Christoph9211/Game-Controller-Bluetooth-) | Separate Compose controller and Room layouts; ported HID transport and in-repo experimental Windows bridge |

Android 9+ Bluetooth Classic HID controller with a Jetpack Compose interface, two sticks, eight-way D-pad, analog triggers, holdable buttons, phone haptics, and saved layout profiles.

The real controller transport was ported from `Android-Bluetooth-Gamepad` commit `ec3b596a8b787d23f4d01b516fbdec5a116ee63a`. Both generic HID and the experimental Windows companion protocol are preserved. This is a local test candidate; successful builds and emulator checks do not establish physical Bluetooth/gameplay compatibility.

The main project uses Canvas `PadView` gameplay controls with Compose navigation; this app uses Compose controller components and Room layouts. Studio is a related copy, not a guarantee of current source parity: its build configuration differs and the main project continues to evolve. Repository roles do not imply hardware validation.

## Implementation and evidence

| Area | Current status |
| --- | --- |
| Android transport | HID registration, bonded-host connection, generic reports and Windows vendor reports implemented in `app/src/main/java/com/example/bluetooth/` |
| Windows companion | Raw Input, protocol decoding, virtual Xbox output, safety gate, diagnostics and driver packaging code added in `4d8920b` on September 28, 2026 |
| Automated/emulator evidence | Historical Android port results are recorded in [validation](docs/VALIDATION.md); they are not a fresh validation of current main or the Windows addition |
| Windows build/distribution | Build/test/smoke scripts exist; this checkout has no GitHub Actions workflow or recorded Windows acceptance result |
| Hardware acceptance | Physical Bluetooth delivery, driver installation, gameplay, reconnect reliability and latency still require device testing |

## Connect

1. Open **Connect**, choose **Android / generic HID** (default), and tap **Start Bluetooth**.
2. Grant Nearby devices access and allow Bluetooth to turn on. Notifications are optional; the notification provides a Stop action.
3. Wait for HID registration, then tap **Make phone discoverable**.
4. From the receiving phone or PC's Bluetooth settings, pair with the controller phone. Confirm the system prompts.
5. Tap **Refresh paired hosts**, then **Connect** beside the receiver. Paired is different from HID connected. Receiving games must support controllers.
6. Return to the controller and keep this app visible. **Stop** unregisters HID; **Disconnect host** only ends the host connection.

Some phones do not support the Android HID Device profile. Registration errors are shown in connection setup. The phone is a controller, not a scanner for external gamepads.

### Windows bridge (experimental)

With Bluetooth stopped, switch Host mode to **Windows bridge (experimental)**. Forget the old pairing on both devices and pair again whenever changing modes; the descriptors differ.

Use the [in-repository PhoneGamepadBridge build and setup instructions](windows/README.md). Start the bridge after connecting the phone. Virtual Xbox output requires its separate Windows virtual-controller driver setup (retired ViGEmBus), with explicit consent and UAC. The companion implementation is included; a validated downloadable package is not established by its presence. Bluetooth connection alone does not prove virtual Xbox input or game compatibility.

## Controls and customization

- Floating analog sticks recenter their visible base at the initial touch and remain neutral until the thumb moves. In **Quick Actions**, “Recenter sticks on touch” is enabled by default and “Extra activation reach” defaults to 24 dp (adjustable from 0–48 dp); disabling it restores fixed-center sticks.

- Buttons send press and release; L3/R3 have dedicated holdable buttons. Sticks, buttons, D-pad and triggers accept simultaneous fingers. Trigger position controls 0-255 pressure; the source digital trigger bits engage at 128.
- The D-pad accepts eight directions. Sticks use a radial deadzone, initially 4%, adjustable from 0-25%.
- Quick actions select a 4 ms experimental, 8 ms default, or 16 ms compatibility analog send interval. These are application scheduling settings, not guaranteed radio rates.
- **Customize** retains layout editing and Room-backed save/load/duplicate/rename/delete. Presets reflow in portrait and landscape; custom profiles retain their saved placements. Undersized controller windows show an enlarge/rotate prompt.
- Navigation, overlays, focus loss, backgrounding, resizing, connection changes, and cancellation release input. Lift and touch again after a reset. The source 750 ms UI watchdog provides an additional neutral safeguard.
- Haptics operate this phone's vibrator only. Gyro, turbo, rear paddles, remapping, and advanced calibration are unavailable. Existing layout records containing unsupported elements remain stored.
- The editor preview is local and does not transmit input.

## Diagnostics

Diagnostics displays actual local sender counters and timing summaries: accepted/rejected API submissions, coalescing, queue age, scheduling lateness, API-call duration, and safety resets. API acceptance does not prove receiver delivery. Touch-event age has no samples unless an original event timestamp is supplied; this Compose adapter currently submits without one.

RSSI, receiver packet loss, and round-trip/game latency are **Unavailable**. There is no simulated device catalog, random telemetry, or fake ping burst. Copy and Reset operate on real sender diagnostics.

## Build and test

Use JDK 17, Android SDK Platform 36.1, and the included Gradle 9.3.1 wrapper. Configure `ANDROID_HOME` or an ignored `local.properties` for your installed SDK.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug signing uses Android's standard debug key. Application ID remains `com.aistudio.btgamepad.zqxrvk`. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Release signing retains `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`, and alias `upload`. No release is published by this port.

Tests include the source protocol/sender regressions, combined input/deadzone checks, Compose press/release/cancel/disposal, callback-driven connection state, permission denial, and saved-layout persistence. JVM Android tests use APIs 28/35 with JDK 17; device tests require an attached target.

See [architecture](docs/ARCHITECTURE.md) and [validation](docs/VALIDATION.md). There is no license file in this checkout.
