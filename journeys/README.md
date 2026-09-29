# Core Android journeys

Agent-evaluated XML journeys for this checkout, following [Android CLI Journeys](https://developer.android.com/tools/agents/android-cli/journeys). An agent reads each action and drives the app with Android CLI inspection and ADB input; there is no `android journey run` command in the installed CLI.

| File | Coverage | Requirement |
| --- | --- | --- |
| `01-local-controls.xml` | Local controls, release values, navigation | Emulator or phone |
| `02-stick-settings.xml` | Recenter, reach, defaults, scheduling setting | Emulator or phone; changes preferences |
| `03-customize-save-load.xml` | Edit position, save named profile, reload | Dedicated test profile |
| `04-profile-duplicate-delete.xml` | Duplicate, cancel deletion, delete copy | Run 03 first |
| `05-permission-denied.xml` | Denial stays disconnected | Android 12+, permission prompt available |
| `06-diagnostics.xml` | Unavailable remote metrics, stopped sender, haptic UI | Emulator or phone |
| `07-hid-connect-and-release.xml` | Real host connection, simultaneous input, neutral on overlay, disconnect/stop | HID phone, paired receiver, receiver tester, concurrent touch |

Start each journey on Controller and satisfy its description first. Use an isolated test device/profile, retain existing user layouts, and record the selected device, APK hash, orientation, and prerequisites. Resolve optional notification prompts before continuing a Bluetooth assertion; notifications are not required for registration. If readiness or a screen transition does not finish within 15 seconds, capture evidence and stop that journey. Do not skip failed steps to obtain a pass.

For 07, record the approved receiver name/address before execution. Pairing is a prerequisite, not an inferred success from a paired-host list. Windows bridge validation additionally needs its companion and virtual-controller driver and is not covered by the generic HID journey.

## Run with Android CLI

Example agent request:

> Use $android-cli to evaluate journeys/01-local-controls.xml on the selected test device. Execute actions in order, inspect after each action, and save per-step results and screenshots under build/journeys/01-local-controls. Stop on the first failure. Distinguish blocked prerequisites from app failures.

From the repository root, build an APK if needed with `./gradlew.bat :app:assembleDebug`. Inspect `android run --help` before deployment. Select an actual serial from `adb devices -l`; never default to a different attached device. Example PowerShell inspection after setting `$serial` to that selected serial:

```powershell
android --version
android info
android layout --device="$serial" --full --output="build/journeys/layout.json"
android screen capture --device="$serial" --output="build/journeys/screen.png"
```

Create the output directory first. Open screenshots before making visual assertions; use fresh layout bounds for input. Canvas sticks and the layout editor may require screenshot-based targeting. Compose test tags are source hints, not guaranteed Android CLI selectors. Scroll to locate offscreen action targets; never scroll during a visibility assertion to make it pass. Ordinary ADB taps/swipes cannot prove simultaneous held input: use a capable tool or an operator for 07 and report a block when unavailable.

After 03, optionally verify durable storage by force-stopping and relaunching the same installed package (`com.aistudio.btgamepad.zqxrvk`), reopening Layouts, searching for Journey-Core-Layout, loading it, and checking X: 50% in Customize. Do not clear app data. Record this as a separate persistence check. Delete only profiles created by this run if cleanup is requested.

Each report should include each action, PASS/FAIL/BLOCKED/NOT RUN, actual commands, observed values, and evidence paths. UI labels, local counters, and emulator registration do not establish Bluetooth delivery or felt vibration.

## Creation validation

Created 2026-09-28 against current Compose screens, GamepadViewModel, and BluetoothConnection. Older prototype notes and the README's opening prototype label conflict with the current transport implementation; these journeys follow current source behavior.

Android CLI `1.0.16406183`, `android info`, and layout/screen help were checked. `adb devices -l` returned no attached devices. XML structure is validated; **none of these journeys has been executed on a device in this task**. No application code or device data was changed.

## Offline receiver tester

`gamepad-tester.html` displays browser gamepad buttons, axes, and observed button presses. Check its renderer with `node journeys/check-gamepad-tester.cjs`. Serve this directory on localhost, then forward that port using `adb -s RECEIVER_SERIAL reverse tcp:8766 tcp:8766` and open `http://127.0.0.1:8766/gamepad-tester.html` in receiver Chrome. Keep USB connected and the server running. Button numbers are device-specific; do not infer standard mapping.

On 2026-09-28 the tester was opened on Note9 over USB, detected the S22, recorded B0 pressed, and showed B0 released afterward. Local server process ID is saved in ignored `build/journeys/device-run/tester-server.pid`. The controller APK was also installed on Note9 as requested, but was not launched as a sender; Chrome remains the receiving tester.
