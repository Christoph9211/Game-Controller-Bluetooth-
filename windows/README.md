# Phone Gamepad Bridge 0.2.0 (Windows x64)

**Experimental, opt-in companion implementation.** Added to this repository in
`4d8920b` on September 28, 2026. Android/generic HID is the direct controller
mode; neither mode is certified for production here. Windows build scripts and
tests are present, but this checkout has no recorded Windows build or hardware
acceptance result. ViGEmBus remains retired. Driver setup requires explicit
consent and Windows UAC. Preserve bundled notices and provenance when packaging.

Code for an integrated, self-contained Windows companion for this repository's Android controller. It supplies the narrow feature needed from x360ce v4: translate phone state into a virtual Xbox 360 controller. It is a new implementation using the ViGEm backend, NOT a copy or embedded instance of x360ce. No Steam, button-mapping wizard, separately installed .NET, game-folder DLLs, HidHide, or Internet connection during play.

## Install / first connection

1. Build the package below on Windows; no successful downloadable artifact is established by this checkout. Keep the entire output folder and license notices together. Run `PhoneGamepadBridge.exe`. The bridge itself is an **unsigned test build**, not an upstream-signed x360ce release. Do not disable security protections to run it.
2. Existing compatible ViGEm installations can use **Start bridge** directly. Otherwise select **Install controller support (one time)** in the bridge. Read the retired-driver warning, approve Windows UAC, and complete the bundled official driver installer. No external download or separate controller configuration is required. This is a guided first-run setup, NOT an entirely silent or zero-install system. Only this requested installer runs elevated; normal gameplay does not.
3. Install this repository's Android debug APK (`com.aistudio.btgamepad.zqxrvk`; Android version name `1.0`, separate from bridge version `0.2.0`). A different debug signing key may require removing the older debug app first, which removes saved data. Open **Connect**, stop Bluetooth, and switch **Host mode** to **Windows bridge (experimental)**.
4. Changing mode changes the Bluetooth HID descriptor. Forget the pairing between these TWO devices on Windows and Android, then tap **Start Bluetooth**, wait for registration, use **Make phone discoverable**, pair from Windows, and **Refresh paired hosts > Connect**. Do not globally reset Bluetooth or delete unrelated devices. Keep the phone controller screen visible.
5. The bridge detects the dedicated phone channel. A single phone is selected automatically; choose one if several are present. Press **Start bridge**, release all phone controls to arm it, then launch the game normally. Keep the bridge open or minimized.

This bridge deliberately does not accept the old generic-mode APK or another gamepad. If no phone appears, confirm **Connect > Host mode** is **Windows bridge (experimental)**, refresh the pairing after mode changes, and verify the phone reports a connected host. This Compose app does not include the source project's Receiver Host test screen.

## What is mapped

A/B/X/Y to Xbox A/B/X/Y; L1/R1 to shoulders; Select/Start to Back/Start; L3/R3 to stick clicks; the eight-way hat to Xbox D-pad; L2/R2 to the two trigger axes; both sticks to their matching Xbox axes. Android Y increases down, XInput Y increases up; the bridge inverts Y exactly once. No guessed DirectInput button order, calibration wizard or extra dead zone is applied. The existing phone dead zone still applies. This Compose app supplies analog trigger pressure from 0–255; the bridge preserves those bytes.

PC mode exposes a vendor-defined HID collection rather than a physical gamepad collection. Games should see only the virtual Xbox controller, avoiding the physical-plus-virtual double-input path without a system-wide HID-hiding filter. This design and particular Bluetooth stacks still need the hardware gate below.

## Driver boundary and security

- Xbox emulation still requires a Windows kernel driver. Bundling makes setup integrated; it does not make that requirement disappear or allow an Android APK to install Windows drivers.
- The bundled official **ViGEmBus setup 1.22.0** is retired. It removes the old auto-updater; it does NOT represent a newly maintained driver. The release notes warn that an in-place upgrade can need a second installer run. We do not loop/retry elevation automatically, force uninstall a shared driver, or restart Windows without the user.
- Packaging fetches only that pinned upstream release, checks its known length, verifies Windows Authenticode and the expected Nefarius publisher, then embeds its SHA-256 and exact bytes. First-run setup rechecks that embedded digest while holding a non-write-sharing lock before launching the installer. See `driver-provenance.json`.
- No root, custom unsigned kernel driver, security disabling, HID Guardian/HidHide changes, keyboard/mouse interception, startup persistence, firewall changes, accounts, analytics or network listener.
- The bridge remains a foreground-user desktop process with background input reception while minimized, not a privileged Windows service.
- Normal close/disconnect/timeout/suspend releases controls. After a recovery it waits for neutral before accepting held buttons. A killed process or failed driver cannot guarantee graceful cleanup; restart the bridge/Windows if a virtual controller remains.

## Validation boundaries

Available checks include protocol/axis/button and randomized-report tests (`Bridge.Tests`), worker isolation with fake output (`Worker.Tests`), compilation/publish and package probes (`Build.ps1`), and a window-open/clean-close script (`Smoke-Test.ps1`). They are scripts and tests, not recorded passes. There is no GitHub Actions workflow in this checkout. See [validation evidence](../docs/VALIDATION.md). **These checks do not install the kernel driver or establish physical Bluetooth/XInput/gameplay/latency validation.**

Hardware gate on a Windows 11 PC + S22 Ultra:
- Install fresh via the in-app button; test UAC cancellation and requested reboot handling without security workarounds.
- Switch Android to PC bridge mode, re-pair these devices, detect exactly one phone, and start one Xbox output.
- In `joy.cpl`, see the virtual Xbox controller, not a second physical gamepad. Check both sticks in four cardinal directions and every button. Xbox up/right is positive.
- In Deep Rock Galactic: Survivor without Steam Input/x360ce, A must confirm and movement must match. Record the game/version and result; do not claim gameplay acceptance until this passes.
- Hold a button/stick and disconnect Bluetooth, close the app, lock the phone, and suspend/resume the PC. Controls must release and held input must not resume before a neutral packet.
- Run minimized for five minutes, reconnect, and test another real controller present. XInput player slot 1 is not forcibly reassigned; games limited to the first slot may need other controllers disconnected before starting the bridge.
- Measure end-to-end latency on hardware. Neither the 8 ms analog interval nor report count is a latency result.

## Build

Windows, .NET 10 SDK, network access for official build dependencies:

```powershell
./windows/Build.ps1
```

A successful build is intended to produce a `windows/dist` folder with a self-contained x64 executable, runtime/backend and bundled driver, notices and provenance. It does not require users to install .NET. The package is x64 only for now. ARM64 Windows, controller rumble, multiple simultaneous phone outputs, startup/tray integration, and production app code signing are not implemented.

Core-only tests (no driver/Windows needed): `dotnet run --project windows/Bridge.Tests -c Release`.

## Removal

Close the bridge, delete its extracted folder, and optionally remove `%LOCALAPPDATA%/PhoneGamepadBridge` (installer cache). Uninstall ViGEmBus through Windows Installed apps only after checking no other controller software uses it. Return the phone to Android/generic mode and re-pair to use direct Android gamepad input again.

## Experimental input isolation and diagnostics

Raw Input and Xbox submissions run on a dedicated message-loop thread. The UI reads snapshots every 100 ms; notification-driven device scans and a 30-second fallback run off the input thread. Use Copy diagnostics and Reset diagnostics for local timing summaries. See [low-latency validation and limitations](../docs/LOW_LATENCY.md). `dotnet run --project windows/Worker.Tests -c Release` tests isolation using fake output and real message loops without installing a driver.
