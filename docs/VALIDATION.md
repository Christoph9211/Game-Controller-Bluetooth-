# Port validation

Validated locally on 2026-09-28. Source: `ec3b596a8b787d23f4d01b516fbdec5a116ee63a` in the named source checkout; that checkout was left unchanged. Its pre-existing dirty `.worktrees/ui-improvements` entry was not modified.

## Build and automated checks

JDK 17, Gradle wrapper 9.3.1, Android SDK 36.1:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- Debug APK assembly passed with standard debug signing.
- 34 JUnit/Robolectric/Compose tests passed; zero failures or errors. Robolectric targets APIs 28 and 35 where applicable.
- Transported source checks include 20,093 core assertions and 3,212 sender checks.
- Coverage includes protocol bytes, trigger threshold, combined inputs, radial deadzone, independent held buttons, cancellation/disposal, diagonal D-pad, neutral reset/fresh touches, permission denial, HID callback-driven connection state, layout editing/persistence, and narrow profile cards.
- Lint: zero errors, 57 warnings. Warnings remain; this is not a warning-free release certification.
- Five protocol/sender Java files match the pinned source after package-name normalization. HidService has the documented Android integration changes.

APK: `app/build/outputs/apk/debug/app-debug.apk`

SHA-256: `3D0988B7FB032F8177CF2B2927D914EE87604A85DCE79EF613B706D84C8D9237`

Local reports: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html`, and `build/validation/gradle-checks.log`.

## Android CLI emulator checks

Installed and launched with Android CLI on `medium_phone`, API 36 (`emulator-5554`). Screenshots and UI hierarchy snapshots are saved locally under ignored `build/validation/`.

- Denied Nearby devices permission: stayed disconnected with no invented host.
- Granted Nearby devices permission: generic HID registered and reported Ready.
- Denied optional notification permission: service still registered.
- Stopped generic HID, switched modes, started Windows bridge: experimental profile registered. Mode selection stayed locked while active; Stop completed cleanup.
- Visually inspected Controller, Connection, and Diagnostics in portrait and landscape. Preset controller overlap found during inspection was corrected and regression checked.
- Inspected saved profiles in both orientations. Duplicated and loaded a profile; it remained after force-stop/relaunch.
- In the layout editor, Center X changed the selected stick from X=8% to X=50%; Save created a new Room profile with a confirmation message. Automated tests also cover editing and removed-control persistence.

The existing editor canvas remains cramped in short landscape windows. Custom layouts can overlap if the user places controls that way; preset reflow does not rewrite custom coordinates.

## Physical checks still required

No physical Android controller phone or receiving host was available through ADB. Emulator registration does **not** establish Bluetooth radio or gameplay compatibility.

On suitable hardware, validate both modes separately: forget/re-pair, connect to the actual bonded host, hold simultaneous buttons/sticks/triggers, exercise all D-pad diagonals, disconnect/reconnect, and interrupt held input through navigation, focus loss, backgrounding, and service stop. Verify the receiver returns neutral and requires fresh touches after interruption. Windows mode additionally requires the unchanged external companion and its virtual-controller driver.

Physical multi-touch comfort, phone vibration, receiver delivery, reconnect reliability, host compatibility, and game latency remain unverified. RSSI, receiver loss, and round-trip latency remain unavailable rather than inferred from local sender counters.
