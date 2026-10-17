# Salati iOS migration

## Branch safety and baseline

- Baseline: `9d4382d60f23de3b8f2be30cf877b27b0211515d`, Android 1.8 (8).
- Annotated tag: `android-1.8-baseline`.
- `main` and all its pre-existing uncommitted changes are preserved. Those changes are intentionally not part of this migration.
- `Android_Legacy` and `ios` started at that exact commit; both and the tag were pushed to the original repository. Do not move Android_Legacy as part of migration.
- Implementation checkout on this Windows host: `C:/Users/M1993/PrayerApp/.worktrees/ios`.
- Detached baseline test checkout: `.worktrees/baseline`.

## Implemented architecture

`app` remains the original Android app and retains its identity, UI, preference keys and Java time API. Its thin adapters call `shared`; production calculator/calendar/resolver/Qibla math has one implementation. A frozen baseline calculator exists only in JVM tests for differential verification. Notification/audio policy stays in Android. Shared value enums retain their names for preference compatibility.

`shared` uses Kotlin 2.4.10, JVM 11 bytecode (consumed by Android), native iOS device and simulator frameworks, kotlinx-datetime 0.7.1 and explicit platform locks. UTC instants precede IANA-zone conversion, preserving DST and signed UTC day rollover. Swift uses SalatiEngine, ISO dates, identifiers and epoch seconds. The existing Gradle 9.7.0/AGP 9.1.1 versions are retained rather than upgraded; these are beyond Kotlin's published tested maxima (Gradle 9.5/AGP 9.1.0). Actual local/CI results determine this repository's verification, not a claim of blanket vendor compatibility.

Umm al-Qura AH 1300–1600 uses month boundaries generated from OpenJDK's existing Hijrah-umalqura calendar. All days are compared with Java. Outside that range the original converter fallback remains, now honestly labelled tabular. Calendar navigation requires the supported range, as Java did. Data generation source is tools/migration/GenerateUmmAlQura.java; table is versioned, so Foundation Islamic calendars cannot silently diverge. Prayer baseline parity is regression evidence, not independent astronomical certification.

`iosApp` is a SwiftUI app (iOS 26+) with native tabs, navigation, forms, sheets, system typography and controls. System bars adopt Liquid Glass. Prayer content uses restrained sage accents. Native material respects system Reduce Transparency and custom compass animation respects Reduce Motion. The app supports Arabic/English, RTL, Western digits, localized Gregorian/Hijri month names, Dynamic Type and combined VoiceOver prayer labels.

Connected features: onboarding, offline Arabic/English FTS city search, exact GPS with nearest offline label, manual coordinates/IANA zone, current/next prayers and system-rendered countdown, daily schedule/night milestones, calculation methods/Hanafi/high latitudes/manual adjustments/month-scoped Hijri offset, appearance/language/time format, calendar/observances, true-north Qibla bearing and distance with compass accuracy handling, AVFoundation foreground full-adhan playback, optional per-prayer local notifications/system alarms, reminders, WidgetKit timelines, immediate and scheduled ActivityKit countdown requests.

## Assets

Verified baseline: 69,632 database rows, 736 string entries in each Android catalogue and 11 MP3 recordings. Main has newer uncommitted translations which remain untouched. Resources are generated from the repository originals; no default recording is replaced. iOS-specific UI/permission strings are separate; method and observance translations are converted from Android. Full catalogues are retained as generated reference JSON, never used to display Android permission instructions. GeoNames attribution remains visible. Full MP3 and short 25-second PCM CAF excerpts are bundled on macOS; original recordings remain byte-identical.

## Alerts and platform limits

All prayer policies default to off. Permissions and per-prayer delivery are independent. Notification, audible AlarmKit system alarm and off are iOS policies, not Android ALARM/MEDIA/RINGTONE stream mappings. Silent is a notification policy; a system alarm must be audible. Ordinary notifications use system sound, silence or a 25-second recording excerpt. Full recordings play through AVFoundation while the app is open. This implementation does not claim unattended multi-minute adhan via UserNotifications or AlarmKit. Custom AlarmKit excerpt playback, silent-switch/Focus behavior and actual alarm duration need physical-device testing.

A maximum of 60 combined alerts/reminders over six date-specific prayer schedules is prepared. Changing settings/location, foregrounding or significant time changes cancels and rebuilds the plan. App refresh requests replenish opportunistically; iOS decides whether background work runs. There are no repeating fixed prayer times, silent audio loops, private APIs, account, server or subscription. Reopen before the horizon expires. Errors/denied permissions are surfaced. App Group snapshots let a lightweight widget extension render timelines without loading the engine/database. A Live Activity requests the next prayer only; user action can schedule it five minutes before that prayer. Activity lifetime and system limits still apply.

## Verification record (updated during implementation)

- Windows: original baseline Android unit tests + debug APK passed using existing ignored debug keystore. Without that key the baseline failed validateSigningDebug; migration falls back to generated Android debug signing safely.
- Shared JVM suite passed: every supported Umm al-Qura day and 18,144 prayer combinations (Madinah/Makkah/Damascus/London/New York/Tokyo/Auckland/Honolulu/Tromso; 14 methods; both juristic choices; all high latitude rules; Ramadan/non-Ramadan; DST dates; adjustments and Hijri offsets), plus common end-to-end boundary/calendar tests.
- Integrated Android: all 97 unchanged tests and debug APK passed. Six geographic failures from a radians-parentheses extraction error were fixed; no test expectations were changed. Shared suite: 7 tests passed, including the 18,144-case differential matrix and all supported calendar days. Existing KSP AWT shutdown exception and deprecation warnings also appeared in the passing baseline.
- iOS source implemented. First macOS run reached native compilation and exposed a JVM-only IntArray binarySearch extension; replaced with a common binary search. Android CI SDK bootstrap fixed to omit the retired tools package. Swift integration and simulator/UI tests pending rerun; no physical iPhone validation or simulator screenshots claimed yet.

## Build locally and in CI

Android/JVM (JDK 21 recommended, Android SDK 36.1):

```powershell
.\gradlew.bat :shared:jvmTest :app:testDebugUnitTest :app:assembleDebug --console=plain
```

On this Windows machine set JAVA_HOME to the installed Android Studio JBR and GRADLE_USER_HOME to the normal user Gradle directory if the execution environment overrides it. No local signing secrets are needed for generated debug signing. Release credentials remain environment-only.

macOS: select Xcode 26.4.1, install JDK 21 and XcodeGen, then:

```sh
export DEVELOPER_DIR=/Applications/Xcode_26.4.1.app/Contents/Developer
bash tools/ios/build.sh
xcodebuild -project iosApp/Salati.xcodeproj -scheme Salati -destination 'platform=iOS Simulator,name=YOUR_AVAILABLE_IPHONE' CODE_SIGNING_ALLOWED=NO test
```

The generator builds the correct simulator framework, copies unchanged database/audio, creates notification excerpts, and generates the Xcode project. Run it after changing Kotlin/resources. For an ARM64 device use `KOTLIN_TARGET=IosArm64 bash tools/ios/build.sh`, open the project and configure signing. `.github/workflows/multiplatform.yml` selects macos-26 and Xcode 26.4.1 explicitly, builds the unsigned app/extension, runs framework/SQLite/scheduler and UI tests, retains xcresult attachments and captures an actual simulator screenshot. The Android job builds APK and tests with JDK 21. No TestFlight/App Store operations are configured.

## Signing and physical installation later

Unsigned simulator builds need no Apple account. Device installation requires access to a Mac/Xcode (a trusted remote Mac is sufficient), an Apple ID/appropriate development team, a registered iPhone with Developer Mode enabled and development signing/provisioning. For the shared App Group capabilities, register the app IDs `com.salati.prayertimes.ios` and `.widgets`, register `group.com.salati.prayertimes`, and enable it for both IDs/profiles. Change IDs/group consistently if already claimed. Select your team for all targets in Xcode. A paid Apple Developer membership may be required for these capabilities; none has been purchased or enrolled here. No distribution certificate, App Store Connect API key or release credentials are necessary for unsigned CI. Signed distribution/remote installation remains a separate user action.

Physical checklist:

1. Deny then grant GPS; compare exact coordinates, nearest-city label and border time zone. Verify unavailable/low-accuracy true heading and Qibla in a known location.
2. Check Arabic RTL, Western digits, month names, light/dark, largest accessibility text, VoiceOver, Reduce Motion and Reduce Transparency.
3. Compare local mosque times with each selected method/adjustment; cross midnight, DST and Ramadan; verify tomorrow Fajr/night milestones and month offset expiry.
4. Grant/revoke both alert permissions; test Fajr alarm versus quieter other prayers, reminders, all selected sounds, Focus/silent mode, locked/terminated/rebooted device. Measure actual AlarmKit playback; never infer full-length behavior from simulator success.
5. Change location/settings and confirm old alerts disappear. Leave beyond the six-day horizon to document refresh limits. Verify background denial and exhaustion messaging.
6. Add widget, switch language/time format, check midnight/timeline expiry; start/schedule/end Live Activity on Lock Screen and Dynamic Island. Test App Group entitlements on the signed build.

## Independent upcoming default-audio update

On Android_Legacy commit only the intended recording (and genuinely required audio metadata), e.g. app/src/main/res/raw/athan_default.mp3. On ios inspect `git show --stat AUDIO_COMMIT`, then `git cherry-pick AUDIO_COMMIT` if the commit contains only that update. If mixed with unrelated Android work, use `git restore --source=AUDIO_COMMIT -- app/src/main/res/raw/athan_default.mp3` and commit that file on ios; do not merge the branch wholesale. Run tools/ios/build.sh to regenerate full audio and 25-second excerpts; recheck audio rights, duration and device playback. Never move the baseline tag.

## Next work

Run/repair remote iOS and Android checks, inspect actual simulator screens, review accessibility/Arabic layouts, improve any observed failures, then physical device validation and signing when hardware/account access exists. Keep implemented, compiled, simulator-tested and device-tested status separate.
