# iOS migration progress

- Implementation is complete for the available Windows/GitHub macOS environment. Worktree: `C:/Users/M1993/PrayerApp/.worktrees/ios`, branch `ios`; main's existing uncommitted work is preserved.
- Baseline `9d4382d60f23de3b8f2be30cf877b27b0211515d`, annotated tag `android-1.8-baseline`; main and Android_Legacy remain at that baseline locally/remotely.
- Final application source pushed: `39d9959635bb09e927aaa8d126494335f6ae6510`. Later commits contain handoff/screenshots only.
- Implemented: genuinely shared KMP prayer/calendar/resolver/Qibla/search logic consumed by Android and Swift; native SwiftUI iOS 26 offline location/prayer/calendar/Qibla/settings flows; foreground adhan, bounded notifications/AlarmKit, widgets and Live Activity requests.
- Final CI PASSED: https://github.com/M7ammad-eb/PrayerApp/actions/runs/39080678204 . Android job 117537201368 and iOS job 117537201166 both succeeded.
- Verification: 97 Android tests + APK; 8 shared JVM tests (18,144 baseline combinations and every supported Umm al-Qura date); 6 native shared tests; 8 app XCTest tests; 2 English/Arabic UI flows. Unsigned app/extension/test compilation and Xcode-managed local simulator signing passed without Apple credentials.
- All 8 final native screenshots were visually inspected and committed under docs/ios/screenshots with original manifest/checksums: readable adaptive accents, English/light, Arabic/dark, RTL and Western digits. They are actual simulator exports, not mockups.
- Resolved: native binary search, resource inclusion, immutable bundled SQLite, simulator architecture/signing, previous-day current Isha and next-day row highlighting, accent alpha/contrast, baseline Linux scheduler unit-test race (assertions preserved).
- Android assets/default audio are unchanged. No signing secrets, purchases, main merges or distribution actions.
- Next: signed physical-iPhone testing of actual alarms/audio, GPS/heading, widget/Live Activity presentation, accessibility/sizes and schedule replenishment. No physical iPhone was tested. Automatic multi-minute adhan is not implemented; full recordings play in foreground, alerts use short excerpts.
- Exact builds, known limits, signing prerequisites, physical checklist and independent Android_Legacy audio transfer: [IOS_MIGRATION.md](IOS_MIGRATION.md).
