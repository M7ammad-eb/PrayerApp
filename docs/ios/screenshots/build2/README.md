# Build 2 language switching

Actual unmodified PNG exports from [successful runtime CI 39087685774](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39087685774), source `a419ded14261322b33071d8f06d6c45c0df47834`. iPhone 17 Pro simulator, iOS 26.4.1, Xcode 26.4.1; 1206 x 2622. Original export manifest and per-image SHA-256 provenance are included.

- [After switching English to Arabic](settings-after-arabic.png).
- [After reopening in Arabic and switching back to English](settings-after-english.png).

Both images were visually inspected: native Form text is readable and unmirrored, reading direction and language controls are correct, the selected Settings tab and Damascus location remain. Tests assert both control orders, language persistence after relaunch, and visible localized tabs. All 12 app tests and both UI flows passed; the full artifact contains 10 named screenshots. Actual AltStore signing and widget display still need physical-iPhone confirmation.
