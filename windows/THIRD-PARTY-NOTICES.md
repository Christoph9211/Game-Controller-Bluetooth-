# Third-party components and provenance

No x360ce application source or binaries are copied into this project. The independently implemented bridge uses the same type of Windows virtual-controller backend.

- **Nefarius.ViGEm.Client 1.21.256 / ViGEm.NET:** MIT; Copyright (c) 2018 Benjamin Höglinger-Stelzer. https://github.com/nefarius/ViGEm.NET and https://www.nuget.org/packages/Nefarius.ViGEm.Client/1.21.256
- **Native ViGEmClient bundled by that SDK:** MIT; Copyright (c) 2018 Benjamin Höglinger-Stelzer. https://github.com/nefarius/ViGEmClient
- **ViGEmBus, upstream setup 1.22.0:** BSD-3-Clause; Copyright (c) 2016-2020 Nefarius Software Solutions e.U. https://github.com/nefarius/ViGEmBus/releases/tag/v1.22.0
- **Self-contained Microsoft .NET 10 runtime:** MIT and applicable third-party notices. https://github.com/dotnet/runtime

Build.ps1 includes full upstream license texts under `licenses/` in the distributed folder. Preserve those files when redistributing the application. The original driver installer is embedded unmodified; its original publisher signature is distinct from this project's currently unsigned application. No endorsement by Microsoft, Nefarius or x360ce is claimed.

Upstream backend retirement: https://docs.nefarius.at/projects/ViGEm/End-of-Life/

## Protocol/API references

- https://learn.microsoft.com/en-us/windows/win32/api/xinput/ns-xinput-xinput_gamepad
- https://learn.microsoft.com/en-us/windows/win32/inputdev/raw-input
- https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-rawhid
- https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice

Windows installer download provenance, verified publisher and actual SHA-256 are retained in `driver-provenance.json` in each bundle. Windows administrator approval is intentionally never bypassed.
