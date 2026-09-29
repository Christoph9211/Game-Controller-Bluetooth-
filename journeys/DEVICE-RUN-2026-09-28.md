# Device journey run - 2026-09-28

Device: Samsung SM-S908U1 (S22 Ultra), serial R3CT309WF3J. Android CLI 1.0.16406183.

APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `45592098D68C74CC4A1F022FFC80B4314E89AA6530D17AAD01CD1D4D22BC568F`. Installed/launched through `android run`; existing application data retained.

Display: 1440 x 3088 portrait / 3088 x 1440 landscape; font scale 1.1, physical density 600, existing override 560. Automatic rotation was temporarily locked to portrait for the editor inspection and restored to free.

Evidence is local under [build/journeys/device-run](../build/journeys/device-run/). Full actual ADB input commands: [commands.log](../build/journeys/device-run/commands.log). Each JSON was captured using `android layout --device=R3CT309WF3J --full --output=PATH`. PNGs used `android screen capture --device=R3CT309WF3J --output=PATH` and were visually inspected.

## Summary

| Journey | Result | Finding |
| --- | --- | --- |
| 01 Local controls | PARTIAL / observed checks passed | Retried after rotation invalidated the first attempt. Checks ran on existing layout; default-preset prerequisite was not established. |
| 02 Stick settings | FAIL at step 9 | Maximum gesture produced 46 dp instead of 48 dp. Endpoint recheck later produced 0 dp. Cause not established; do not equate this with a confirmed slider implementation bug. |
| 03 Customize/save/load | FAIL at step 3 | Portrait editor canvas unusable; left stick cannot be selected. No profile created. Default-layout prerequisite also not established. |
| 04 Duplicate/delete | BLOCKED | Required Journey-Core-Layout was not created by 03. |
| 05 Permission denial | BLOCKED | Nearby devices permissions were already granted; no permissions revoked. |
| 06 Diagnostics | PARTIAL / observed checks passed | Sender assertion required an extra scroll before step 3. Record as an adapted run, not an exact XML pass. |
| 07 Physical HID | PARTIAL - receiver delivery verified | Generic HID paired to the Note9. Receiver input log confirms A DOWN and UP. Simultaneous-touch / overlay neutral / disconnect-stop steps remain pending. |

Settings after local tests: recenter on, activation reach 24 dp (started at 26 dp), sender interval 8 ms. Existing saved profiles were not changed or deleted. Bluetooth stopped; Windows bridge preference retained pending setup decision. Haptic event label changed to Dual Motor Stereo Impulse; physical sensation remains unverified.

## Visual findings

- [Portrait editor](../build/journeys/device-run/editor.png): Reset to Default wraps vertically; header and tab row consume most height; the editor canvas is outside the usable area. This blocks selecting the left stick.
- [Initial portrait controller](../build/journeys/device-run/start.png): controls overlap in the existing loaded layout. Its provenance was not established; this is not a default-preset regression claim.
- [Slider endpoint recheck](../build/journeys/device-run/settings.png): value displayed 0 dp after endpoint tap; restored to 24 dp afterward.

## Per-action results

### 01-local-controls.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Verify that the controller shows Not connected and Local controls. | Observed PASS on existing layout; `restart-controller.json` |
| 2 | Drag the left stick away from its touch origin, then release it. | Observed PASS on existing layout; `01-retry-03.json` |
| 3 | Verify that LS displays (0%, 0%) after release. | Observed PASS on existing layout; `01-retry-03.json` |
| 4 | Drag the right stick away from its touch origin, then release it. | Observed PASS on existing layout; `01-retry-05.json` |
| 5 | Verify that RS displays (0%, 0%) after release. | Observed PASS on existing layout; `01-retry-05.json` |
| 6 | Press and release the A button. | Observed PASS on existing layout; `01-retry-06.json` |
| 7 | Press and release the left trigger. | Observed PASS on existing layout; `01-retry-07.json` |
| 8 | Press and release the right trigger. | Observed PASS on existing layout; `01-retry-09.json` |
| 9 | Verify that LT: 0 and RT: 0 are displayed. | Observed PASS on existing layout; `01-retry-09.json` |
| 10 | Tap Quick actions. | Observed PASS on existing layout; `01-retry-10.json` |
| 11 | Press Android Back. | Observed PASS on existing layout; `01-retry-12.json` |
| 12 | Verify that the drawer is closed and the controller is visible. | Observed PASS on existing layout; `01-retry-12.json` |
| 13 | Tap Connect. | Observed PASS on existing layout; `01-retry-14.json` |
| 14 | Verify that Bluetooth connection is visible. | Observed PASS on existing layout; `01-retry-14.json` |
| 15 | Tap Back to controller. | Observed PASS on existing layout; `01-retry-16.json` |
| 16 | Verify that both sticks and both triggers display neutral values. | Observed PASS on existing layout; `01-retry-16.json` |

### 02-stick-settings.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Tap Quick actions. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 2 | Scroll the drawer until Stick defaults is visible. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 3 | Tap Stick defaults. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 4 | Verify that Recenter sticks on touch is on and EXTRA ACTIVATION REACH shows 24 dp. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 5 | Tap the Recenter sticks on touch switch. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 6 | Verify that the switch is off and the extra activation reach slider is disabled. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 7 | Tap the Recenter sticks on touch switch. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 8 | Move the EXTRA ACTIVATION REACH slider to its maximum. | Performed / observed; `02-01.json` through `02-07.json`, commands.log |
| 9 | Verify that EXTRA ACTIVATION REACH shows 48 dp. | FAIL: 46 dp; `02-09.json`. Later endpoint recheck: `02-recheck-maximum.json` (0 dp). |
| 10 | Tap Stick defaults. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 11 | Verify that Recenter sticks on touch is on and EXTRA ACTIVATION REACH shows 24 dp. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 12 | Scroll the drawer to the scheduling interval choices. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 13 | Tap 16 ms. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 14 | Press Android Back. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 15 | Tap Diagnostics. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 16 | Verify that Local sender measurements describes a 16 ms analog scheduling interval. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 17 | Tap Back to controller. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 18 | Tap Quick actions. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 19 | Scroll the drawer to the scheduling interval choices. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 20 | Tap 8 ms. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |
| 21 | Press Android Back. | NOT counted after failure. Subsequent exploration/restoration recorded in commands.log; 16 ms observed in `02-16.json`, 8 ms in `06-03.json`. |

### 03-customize-save-load.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Tap Customize. | PASS: `03-02.json` |
| 2 | Verify that Customize Layout is visible. | PASS: `03-02.json` |
| 3 | Tap the left stick in the editor canvas to select it. | FAIL: no usable visible canvas / left stick; `editor.png`, `03-02.json` |
| 4 | Tap Center X in the selected control inspector. | NOT RUN |
| 5 | Verify that the selected control inspector shows X: 50%. | NOT RUN |
| 6 | Tap Profiles in the editor toolbar. | NOT RUN |
| 7 | Tap Save Current As New. | NOT RUN |
| 8 | Tap Layout Name and enter Journey-Core-Layout. | NOT RUN |
| 9 | Tap Save layout. | NOT RUN |
| 10 | Tap Search layouts and enter Journey-Core-Layout. | NOT RUN |
| 11 | Verify that the Journey-Core-Layout card is visible and marked Active. | NOT RUN |
| 12 | Tap Close. | NOT RUN |
| 13 | Press Android Back. | NOT RUN |
| 14 | Tap Layouts. | NOT RUN |
| 15 | Tap Search layouts and enter Journey-Core-Layout. | NOT RUN |
| 16 | Tap Active or Load on the Journey-Core-Layout card. | NOT RUN |
| 17 | Tap Close. | NOT RUN |
| 18 | Tap Customize. | NOT RUN |
| 19 | Tap the left stick in the editor canvas. | NOT RUN |
| 20 | Verify that its inspector still shows X: 50%. | NOT RUN |
| 21 | Press Android Back. | NOT RUN |

### 04-profile-duplicate-delete.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Tap Layouts. | BLOCKED prerequisite; no action performed |
| 2 | Tap Search layouts and enter Journey-Core-Layout. | BLOCKED prerequisite; no action performed |
| 3 | Tap Duplicate Journey-Core-Layout. | BLOCKED prerequisite; no action performed |
| 4 | Verify that Journey-Core-Layout and Journey-Core-Layout (Copy) are listed. | BLOCKED prerequisite; no action performed |
| 5 | Tap Delete Journey-Core-Layout (Copy). | BLOCKED prerequisite; no action performed |
| 6 | Verify that Delete Layout Configuration? names Journey-Core-Layout (Copy). | BLOCKED prerequisite; no action performed |
| 7 | Tap Cancel. | BLOCKED prerequisite; no action performed |
| 8 | Verify that Journey-Core-Layout (Copy) is still listed. | BLOCKED prerequisite; no action performed |
| 9 | Tap Delete Journey-Core-Layout (Copy). | BLOCKED prerequisite; no action performed |
| 10 | Tap Delete in the confirmation dialog. | BLOCKED prerequisite; no action performed |
| 11 | Verify that the copy is absent and Journey-Core-Layout remains listed. | BLOCKED prerequisite; no action performed |
| 12 | Tap Close. | BLOCKED prerequisite; no action performed |

### 05-permission-denied.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Tap Connect. | BLOCKED prerequisite; no action performed |
| 2 | Tap Start Bluetooth. | BLOCKED prerequisite; no action performed |
| 3 | Tap the system permission dialog option that denies Nearby devices access. | BLOCKED prerequisite; no action performed |
| 4 | Verify that Controller status reports Nearby devices permission denied. Allow it in app settings to connect. | BLOCKED prerequisite; no action performed |
| 5 | Verify that Make phone discoverable is disabled and no host is marked HID connected. | BLOCKED prerequisite; no action performed |
| 6 | Tap Back to controller. | BLOCKED prerequisite; no action performed |
| 7 | Verify that Not connected and Local controls are displayed. | BLOCKED prerequisite; no action performed |

### 06-diagnostics.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Tap Diagnostics. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 2 | Verify that RSSI, Round-trip latency, and Receiver packet loss each show Unavailable. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 3 | Verify that Local sender measurements shows No sender session. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 4 | Scroll until Reset is visible. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 5 | Tap Reset. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 6 | Verify that No sender session. remains displayed. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 7 | Scroll until Test phone vibration is visible. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 8 | Verify that Phone haptics explains that only this phone is tested and remote rumble is not reported. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 9 | Tap Test phone vibration. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 10 | Verify that the app remains on Diagnostics without crashing. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 11 | Scroll to the top. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |
| 12 | Tap Back to controller. | Observed PASS with extra scroll before step 3; `06-02.json`, `06-03.json`, `06-06.json`, `06-08.json`, `06-10.json`, `06-11b.json`, `06-12.json` |

### 07-hid-connect-and-release.xml

| Step | Action | Result / evidence |
| --- | --- | --- |
| 1 | Tap Connect. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 2 | Tap Start Bluetooth. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 3 | Verify that Controller status reports Ready and Make phone discoverable is enabled. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 4 | Verify that the Windows bridge mode switch is disabled while Bluetooth is active. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 5 | Tap Refresh paired hosts. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 6 | Scroll to the approved receiver card. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 7 | Tap Connect on that receiver card. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 8 | Verify that the receiver card reports HID connected. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 9 | Scroll to the top. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 10 | Tap Back to controller. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 11 | Verify that the controller header names the connected receiver. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 12 | Press and hold A on the controller phone. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 13 | Verify on the receiver gamepad tester that A is held. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 14 | Release A on the controller phone. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 15 | Verify on the receiver tester that A is released. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 16 | Hold the left stick away from neutral and hold the right trigger with another finger. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 17 | Verify on the receiver tester that both inputs are active simultaneously. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 18 | While keeping those contacts down, tap Quick actions with another finger. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 19 | Verify on the receiver tester that all inputs have returned to neutral. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 20 | Release all contacts. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 21 | Press Android Back. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 22 | Verify on the receiver tester that inputs remain neutral until a fresh touch. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 23 | Tap Connect. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 24 | Tap Disconnect host. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 25 | Verify that no receiver card reports HID connected. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 26 | Tap Stop. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |
| 27 | Verify that Controller status reports Stopped and Start Bluetooth is enabled. | NOT RUN as generic-HID journey; wrong-mode setup stopped. `07-03.json`, `07-mode.json`. |


## Physical setup continuation

User approved generic HID and fresh pairing. The receiving device is **Chris's Galaxy Note9**, model SM-N960U1, serial `291154396f3f7ece` (initially called S9 in conversation). Controller remains S22 Ultra `R3CT309WF3J`.

- Switched the stopped controller from Windows bridge to Android / generic HID. HID registration reached Ready.
- Removed only the receiver's old S22 pairing, discovered the S22 again, compared matching system passkeys, and confirmed pairing on both phones. No unrelated pairings were removed.
- Connection was established automatically after pairing; the journey's manual Connect action was unnecessary and was not claimed as exercised.
- Controller reports `HID connected: Chris's Galaxy Note9` in `07-controller-ready.json`.
- Receiver enumerates `Chris's S22 Ultra`, bus `0x0005`, controller number 1, sources `0x01000511`, with sticks, hat, and trigger ranges. Evidence: `receiver-input.txt`, `receiver-events.txt`.
- Complete receiver capture `receiver-a-repeat.txt` records `BTN_GAMEPAD DOWN` at 584.899086 and `BTN_GAMEPAD UP` at 586.699169 (receiver monotonic seconds). The controller gesture was `adb -s R3CT309WF3J shell input swipe 2307 977 2307 977 1800`. Receiver capture was `adb -s 291154396f3f7ece shell timeout 12 getevent -lt /dev/input/event14`. This verifies physical Bluetooth delivery of press and release to the receiver kernel; it does not verify application/game mapping or latency.
- The earlier `receiver-a-events.txt` recording timed out after DOWN; it is incomplete and superseded by the complete repeat capture.
- Receiver layout capture immediately after pairing failed to obtain idle; controller confirmation and receiver input-device/event records provide subsequent evidence. No missing receiver-paired JSON is treated as a pass.

**Current state:** generic HID connected to Note9; automatic rotation remains enabled. Waiting for the user to hold left stick and right trigger concurrently to check received axes and overlay neutral safety. Do not count the original journey's remaining steps as passed. The historical stopped/Windows-mode state above describes the earlier local-test phase, not the current setup.

## Requested Note9 APK installation and tester

Installed this checkout's debug APK on Note9 (`291154396f3f7ece`), package `com.aistudio.btgamepad.zqxrvk`, versionName 1.0/versionCode 1; package readback confirmed installation. Existing apps/data retained. The APK is the controller app, not a receiver tester.

External tester could not load because the Note9 had no internet connection. Created the local `journeys/gamepad-tester.html`, served only on host 127.0.0.1:8766, and opened it in Note9 Chrome using ADB reverse over USB. Renderer check passed. `offline-tester-verified.json` shows the S22 device, B0 released at 0.000, and observed pressed history `0:0` after an 800 ms A hold. This adds browser-level button delivery verification to the earlier receiver-kernel evidence. Multi-touch/overlay safety still pending. Leave USB and the server running to keep the tester available.
