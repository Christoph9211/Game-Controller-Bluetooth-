# Scheduling, diagnostics and latency boundaries

Android input changes wake `InputSender`; analog updates use the selected 4 ms (experimental), 8 ms (default), or 16 ms (compatibility) interval. Digital transitions are queued, analog updates coalesce, and queue expiry/overflow requests neutral. Idle heartbeat is 250 ms and the UI watchdog is 750 ms. These are local scheduling and safety settings, not measured radio rates or end-to-end latency.

The Windows `InputWorker` receives Raw Input and submits Xbox state on a dedicated message-loop thread. The UI reads snapshots every 100 ms; device scans run off the input thread with notifications and a 30-second fallback. Protocol decoding, output submission, gaps, and safety resets feed local diagnostics. A 1,000 ms safety timeout resets output; neutral input is required to re-arm. Tests with fake output cannot validate the driver or Bluetooth stack.

Android counters describe local API acceptance, rejection, queue age, scheduling lateness and call duration. The Compose adapter does not supply original touch timestamps. Windows counters describe local processing. Neither establishes receiver delivery, receiver packet loss, RSSI, game response or round-trip latency.

For available checks and unverified Windows/hardware gates, see [validation](VALIDATION.md) and [Windows setup](../windows/README.md). Measure end-to-end latency on physical controller/receiver hardware with a documented method before making latency claims. No such measurement is recorded for this repository.
