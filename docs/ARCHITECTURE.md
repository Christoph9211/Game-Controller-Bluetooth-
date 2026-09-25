# Architecture and implementation status

This document describes the code currently in this repository. The product name and on-screen copy refer to Bluetooth HID, but there is no transport implementation here yet.

## Data flow

```text
Touch input in Compose screens
           |
           v
GamepadViewModel StateFlows ----> Compose UI updates
           |
           +----> HapticFeedbackManager ----> phone vibrator
           |
           +----> CustomLayoutRepository ----> Room gamepad_db
```

`MainActivity` creates the Compose UI and chooses among Controller, Customize Layout, Device Discovery, and Diagnostics. `GamepadViewModel` owns navigation, controller input, editor state, quick settings, the sample device list, and diagnostics values. Compose components read its `StateFlow` values and call its event methods.

Stick, trigger, and button callbacks update local ViewModel state. `HapticFeedbackManager` uses Android's vibrator service for feedback on the **phone**. It does not command motors in a remote gamepad. The haptic channel labels in the diagnostics UI are presentation labels; the manager uses the default phone vibrator.

## Saved data

Room database `gamepad_db` has tables for saved controller layouts, legacy element configurations, and paired-device records. Saved layout profiles are seeded when the layout table is empty, and the layout editor can save and load them through `CustomLayoutRepository`. The paired-device table receives records when the simulated connect action runs; those records are not proof of Bluetooth pairing. The discovery list itself is initialized from hard-coded `DeviceTarget` objects on each ViewModel creation. Quick action settings live in ViewModel memory.

The database is at version 2 with `fallbackToDestructiveMigration()`. Add explicit migrations before changing the schema if existing layouts must survive upgrades.

## Bluetooth and diagnostics boundary

`GamepadViewModel.initInitialData()` seeds the device catalog and initial signal history. `initiatePairAndConnect()` waits briefly, changes local device flags, writes a Room record, and updates the displayed host. It never calls Android Bluetooth APIs. `toggleDiscoveryScan()` only toggles a UI flag.

`startTelemetryLoop()` changes RSSI, latency, integrity, and packet totals with random values every 1.2 seconds. `runPingBurstTest()` animates progress and creates a synthetic result. The values shown in the dashboard are not radio measurements or a performance claim.

The manifest declares legacy and Android 12+ Bluetooth permissions. There is no runtime permission flow, device scan, HID registration, connection callback, report queue, or host-side bridge. Any future transport should keep its actual connection state separate from the demo data and only present measured values as diagnostics.

## Build and test notes

`app/build.gradle.kts` configures API 24 minimum, API 36 target, Compose, Room/KSP, and debug and release signing. The catalog includes Firebase and network dependencies, but the current app flow does not use them. `metadata.json` describes intended capabilities; it is not evidence that they are implemented.

The Gradle distribution URL is recorded in `gradle/wrapper/gradle-wrapper.properties`; executable wrapper files are absent. Debug signing points to a root `debug.keystore` that is also absent from Git. See the [README](../README.md) for local setup and commands.

Unit tests cover portions of ViewModel state and Room behavior. The instrumented test checks the application context. None exercise a real Bluetooth link. The screenshot test captures the scaffold `Greeting` composable rather than a controller screen.
