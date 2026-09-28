# Architecture

## Input path

```text
Compose controls -> GamepadViewModel -> ControllerInput -> GamepadReport
                                                        |
                                                        v
HidService -> InputSender -> ReportQueue -> BluetoothHidDevice.sendReport
                    |                          generic HID or PcProtocol wrapper
                    +-> local timing/counter diagnostics
```

`GamepadViewModel` combines held buttons, both sticks, D-pad hat and triggers into complete reports. `ControllerGestures` owns each pointer until release/cancel and releases in `finally`, including disposal. A generation key discards existing gestures after a safety reset. Input is allowed only on the foreground, focused Controller screen with no drawer/profile dialog. Editor previews never send reports.

The source Java core lives under `com.example.bluetooth`: `GamepadReport`, `PcProtocol`, `InputSender`, `ReportQueue`, `TimingStats`, and `HidService`. It comes from source commit `ec3b596a8b787d23f4d01b516fbdec5a116ee63a`. Protocol/core implementations and transported tests retain the source behavior. Service integration changes are package/resources, host-address accessors, and a completion callback after stopping/closing the profile.

Generic HID uses report ID 1 and a nine-byte payload. Experimental Windows mode uses vendor report ID 0x66 and a 12-byte PG/version-1 payload wrapping the same state. Report IDs remain separate from Android sendReport payloads. The existing Windows companion remains external and unchanged.

Each controller-screen stick keeps its saved layout position as a home center. On an eligible down, `FloatingStickState` records the stable pointer ID and exact touch point as a temporary origin, emits neutral input, and calculates subsequent displacement relative to that fixed origin. Its circular activation radius is the base radius plus the configured reach (24 dp by default). Release, cancellation/disposal, backgrounding, or settings replacement clears live input without changing saved layout geometry. The layout editor explicitly uses fixed-center rendering so temporary origins cannot modify arrangements.

## Connection lifecycle

`MainActivity` owns system permission, Bluetooth enable, notification and discoverability prompts. The ViewModel-owned `BluetoothConnection` binds using application context, surviving Activity recreation. The foreground service serializes Bluetooth operations and callbacks on its worker thread. It provides the authoritative registration, connection and host state; the UI periodically reads that state and the OS bonded-host list. Neither a Connect tap nor a database entry marks a host connected.

Only the responsive foreground/focused UI supplies watchdog pulses. Lifecycle transitions, window changes, navigation, overlays and connection changes reset local state and request neutral output. The source sender retains ordered digital edges, analog coalescing, bounded/expiring queues, 250 ms idle heartbeat, 750 ms UI watchdog, and transport-failure disconnection. Reconnection starts neutral.

Stop waits for profile cleanup before unbinding and allowing a mode change. Mode preference is frozen per service instance. Changing the descriptor requires re-pairing both devices. The service is non-exported and declares the connectedDevice foreground type. Android 12+ uses Nearby devices permissions; notification permission is optional.

## UI and persistence

Compose styling, controller components, layout editor and Room layout repository remain in place. Presets reflow in portrait and landscape; saved custom placements remain user-controlled. The controller pauses if the window cannot fit controls. Holdable L3/R3 replace the old double-tap-only action.

`gamepad_db` stays at schema version 2. Layout tables and saved records are retained. Legacy simulated paired-device records are ignored; Android's bonded devices and HID callbacks now supply host state. No destructive migration fallback is configured. No source-app preferences or layouts are imported.

Host mode, floating-stick enablement, and activation reach persist in SharedPreferences. Floating sticks default to enabled with 24 dp of reach (adjustable from 0 to 48 dp). Deadzone and sender interval are session settings, initially 4% and 8 ms. Unsupported gyro, turbo, paddles, remapping and advanced calibration are labeled unavailable. Haptic effects use the default phone vibrator; labels do not establish multiple physical motors or remote rumble.

## Measurement boundary

Sender counters reflect local API acceptance/rejection, not receiver delivery. Timing summaries measure local scheduling, queuing and API calls. The Compose adapter does not supply original touch timestamps, so that source metric remains unsampled. RSSI, receiver loss and round-trip latency are unavailable; fabricated charts and ping tests were removed.

Protocol tests and emulator checks cannot establish Bluetooth radio behavior, physical multi-touch comfort, Windows driver behavior, or in-game latency. See [validation](VALIDATION.md).
