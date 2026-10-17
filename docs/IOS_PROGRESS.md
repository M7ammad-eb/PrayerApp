# iOS migration progress

- Implementation checkout: `C:/Users/M1993/PrayerApp/.worktrees/ios`, branch `ios`. Do not work in the dirty primary `main` checkout.
- Baseline: `9d4382d60f23de3b8f2be30cf877b27b0211515d`, annotated tag `android-1.8-baseline`. `main` and `Android_Legacy` still point there; main's existing uncommitted work is preserved.
- Latest implementation commit: `a2029a7` (Xcode-managed local simulator signing). All source changes are pushed to origin/ios.
- Implemented: shared KMP calculator/calendar/resolvers/Qibla/search semantics; Android adapters; native SwiftUI onboarding, offline city/GPS/manual location, prayer home, settings, Hijri calendar, Qibla sensors, foreground adhan, bounded notifications/AlarmKit, widgets and Live Activities.
- Verified locally: Android APK + all 97 tests; 8 shared JVM tests including 18,144 baseline comparisons and every supported Umm al-Qura date; common metadata compilation. Android CI has repeatedly passed.
- Verified remotely: Kotlin simulator framework, unsigned SwiftUI app/extension/test compilation; earlier 5 common native tests passed. App-level framework, audio resources, bounded alert plan and Western-digit tests passed in the first runtime run.
- Fixed runtime issues: immutable bundled SQLite needed `immutable=1` to avoid WAL sidecar creation; unsigned App Groups lacked entitlements; a manual iPhone-style entitlement signature was rejected by macOS before app launch, so signing now uses Xcode's simulator path. None of the failed-launch tests is counted as passed.
- Active verification: https://github.com/M7ammad-eb/PrayerApp/actions/runs/39078667945 . Native job 117531419639. Check results, fix real failures, rerun; download exported screenshots and inspect English/Arabic screens before final handoff. The latest common test also preserves the actual previous-day Isha timestamp before Fajr.
- CI: `.github/workflows/multiplatform.yml`; Xcode 26.4.1 on macos-26. Compile unsigned first, then local certificate-free simulator signing with `bash tools/ios/sign_simulator.sh`. No Apple account, purchase, TestFlight or App Store action has occurred.
- Physical iPhone, actual AlarmKit sound behavior, GPS/heading accuracy, widget display and Live Activity presentation remain unverified. Automatic multi-minute adhan is not implemented; full recordings play in foreground, alerts use short excerpts.
- No Android assets/default audio changed. Audio update instructions, exact builds, signing prerequisites and physical checklist: [IOS_MIGRATION.md](IOS_MIGRATION.md).
