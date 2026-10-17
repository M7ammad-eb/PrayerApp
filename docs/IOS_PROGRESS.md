# iOS migration progress

- Work only in `C:/Users/M1993/PrayerApp/.worktrees/ios`, branch `ios`; the primary main checkout has preserved user changes.
- Baseline `9d4382d60f23de3b8f2be30cf877b27b0211515d`, annotated tag `android-1.8-baseline`; local/remote main and Android_Legacy remain there.
- Latest source pushed: `39d9959635bb09e927aaa8d126494335f6ae6510`. Shared KMP core is consumed by Android and native SwiftUI iOS 26 app/extension.
- Implemented: real offline location/prayer/calendar/Qibla/settings flows; foreground full adhan; optional bounded notifications/AlarmKit; widgets and Live Activity requests.
- Local verification: 97 Android tests + APK; 8 shared JVM tests including 18,144 baseline comparisons and every supported Umm al-Qura day; common metadata compile.
- Previous complete green CI: https://github.com/M7ammad-eb/PrayerApp/actions/runs/39078667945 (6 common native tests, 6 app tests, 2 UI flows, unsigned compile, Xcode-managed local simulator signing, actual App Group round-trip).
- Final source CI: https://github.com/M7ammad-eb/PrayerApp/actions/runs/39080678204. Await results, inspect failures if any, download and visually inspect final screenshots. Final app tests add opaque/contrasting adaptive accent and Arabic selected-zone Western-digit date formatting; Arabic UI flow now captures Qibla/settings too.
- Actual previous screenshots exposed transparent accent controls; fixed asset alpha `1.000`, global accent configuration and added runtime contrast tests. Also localized horizon feedback and corrected true-north marker rotation. Do not deliver old screenshots as final visual verification.
- Resolved earlier issues: native binary search, resource inclusion, SQLite WAL sidecar access via immutable URI, simulator architecture/signing, actual previous-day Isha timestamp and date-aware next-row highlight, baseline Linux scheduler test race (no assertions weakened).
- Physical iPhone, actual AlarmKit sound delivery/duration, GPS/heading accuracy, widget presentation and Live Activity presentation are unverified. Automatic multi-minute adhan is not implemented; full recordings play in foreground, system alerts use short excerpts.
- No original Android assets/default audio changed, no signing secrets committed, no purchases or distribution actions. Exact builds, signing/device checklist, limitations and independent Android_Legacy audio transfer: [IOS_MIGRATION.md](IOS_MIGRATION.md).
