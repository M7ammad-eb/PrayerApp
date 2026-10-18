# Salati iOS implementation handoff

## Repository and review references

- Android baseline: `9d4382d60f23de3b8f2be30cf877b27b0211515d`, version 1.8 (8).
- Annotated tag: `android-1.8-baseline`. Both local and remote `main` and `Android_Legacy` remain at the baseline. The primary main checkout's pre-existing uncommitted changes are preserved.
- All migration work is on [ios](https://github.com/M7ammad-eb/PrayerApp/tree/ios). Latest application source: `39d9959635bb09e927aaa8d126494335f6ae6510`; subsequent documentation commits do not change the tested source.
- Implementation checkout: `C:/Users/M1993/PrayerApp/.worktrees/ios`. Detached baseline verification checkout: `.worktrees/baseline`.
- [Final successful Android and iOS verification](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39080678204): both jobs completed successfully on the final application source. APK, test reports, xcresult and screenshots are available in its artifacts.
- No main merge, history rewrite, default-audio replacement, paid enrollment, TestFlight upload or App Store publication occurred.

## Architecture and behavior

`app` remains the existing Android application, with the same identity, UI, assets, preference keys and Java time interface. Thin adapters call `shared`. Production prayer/calendar/resolver/Qibla logic has one implementation. Baseline source is retained only under JVM test sources for differential checks. Android notification/audio integration, Collator and audio-stream constants remain Android-specific. Shared enum names preserve stored preferences.

`shared` builds JVM 11 bytecode and native iOS device/simulator frameworks. It uses Kotlin 2.4.10, kotlinx-datetime 0.7.1, real UTC instants, IANA time zones and platform locks. Swift sees a small SalatiEngine facade using ISO local dates, identifiers and epoch seconds. DST, UTC day rollover, yesterday's current Isha before Fajr, tomorrow's Fajr, Islamic midnight, last third, manual adjustments and Ramadan Umm al-Qura Isha remain real date-aware calculations. Offline FTS token rules and geographic distance are shared; database access is platform-specific.

Existing Gradle 9.7.0/AGP 9.1.1 were retained. They exceed Kotlin's published tested maxima (Gradle 9.5/AGP 9.1.0); the recorded successful builds establish this repository's tested combination, not blanket vendor compatibility. No unrelated dependency upgrade was made.

Umm al-Qura AH 1300–1600 month starts are generated from the baseline's OpenJDK Hijrah-umalqura calendar by `tools/migration/GenerateUmmAlQura.java`. Every supported day is compared with Java. The versioned table prevents silent Foundation-calendar differences. Outside this range the original converter fallback remains explicitly labelled tabular; calendar navigation requires the supported range. Baseline parity is regression evidence, not independent astronomical certification.

## Native iOS experience

The iOS 26+ app uses SwiftUI tabs, navigation, forms, sheets, SF Symbols and standard system controls. Native bars adopt Liquid Glass; restrained adaptive sage accents establish hierarchy without decorative glass everywhere. It supports English/Arabic and RTL, Western digits, localized month names, light/dark/system appearance, Dynamic Type and combined VoiceOver prayer labels. Calendar switches to a list at accessibility text sizes. Compass animation respects Reduce Motion; native materials follow system accessibility behavior. Largest text sizes, VoiceOver and Reduce Transparency still need a dedicated device audit.

Connected screens and integrations:

- Onboarding with optional permissions, offline Arabic/English city search, manual coordinates and explicit IANA zone selection.
- GPS coordinates stay exact, with offline nearest-city naming and distance. The nearest city's zone is used; border locations must verify or manually select the correct zone. No online geocoding is used.
- Current/next event, native countdown, daily schedule and night milestones. Before Fajr the current Isha is labelled previous day rather than falsely highlighting tonight's row.
- All 14 calculation methods, Hanafi Asr, high-latitude rules, manual adjustments, month-scoped Hijri offset, language/appearance/time settings.
- Hijri calendar and original observances; true-north Qibla bearing/distance with native heading and clear unavailable/inaccurate sensor states.
- AVFoundation foreground full-adhan playback and an explicit foreground playback notification action.
- Per-prayer off/notification/system-alarm policies, sound selection, reminders, bounded replenishment, WidgetKit timelines, and immediate/scheduled ActivityKit countdown requests.

## Original assets and offline operation

Baseline assets contain 69,632 places, 736 entries in each English/Arabic Android catalogue and 11 MP3 recordings. The primary checkout's newer uncommitted catalogue changes remain untouched. No backend, prayer API, remote geocoder, account, advertisement or subscription is required.

The original SQLite database, translations, recordings and suitable app icon are reused. Bundled SQLite is opened read-only with documented URI `immutable=1`; its checkpointed WAL header otherwise caused read-only sidecar errors in the simulator. The database is not rewritten. GeoNames attribution is displayed.

Generators convert method/observance translations and retain full Android catalogues as reference JSON; iOS UI and permission explanations use separate native strings. Regeneration does not patch Swift sources. Full MP3 files remain byte-identical. macOS generates 25-second PCM CAF excerpts for alerts. Baseline `athan_default.mp3` SHA-256: `88FF93BC31DC49B4DB6C177DCB50F9AE03C5F64A07073913EDBFD5EF0A454E9C`.

## Alerts, widgets and platform limits

All prayer delivery policies default to off. Granting permission alone enables no alerts. iOS policies do not map Android ALARM/MEDIA/RINGTONE streams. AlarmKit is the prominent audible system-alarm policy; silent delivery uses notifications. Ordinary notifications use silence, system sound or a 25-second recording excerpt. Full recordings play through AVFoundation while the app is open.

**Automatic multi-minute adhan is not implemented or verified.** AlarmKit receives short excerpts; its actual custom-sound duration, audibility under Focus/silent mode, and locked/terminated/reboot behavior require a physical iPhone. Simulator compilation does not prove sound delivery. No silent loops, private APIs or continuous background-execution tricks are used.

A maximum of 60 combined alerts/reminders over six changing date-specific schedules is prepared. Location/settings changes, foregrounding and significant time changes cancel/rebuild schedules. Background refresh requests replenish opportunistically; iOS decides whether they execute. No fixed daily repeating prayer times or indefinite unattended scheduling are claimed. Open the app regularly before its displayed horizon expires. Denials/errors are surfaced.

A lightweight extension reads schedule snapshots through `group.com.salati.prayertimes` and uses WidgetKit timelines/system countdown rendering. App Group read/write is simulator-tested; actual Home Screen widget rendering remains device-unverified. A Live Activity covers the next event only, with an optional foreground request to start five minutes before it. Lock Screen/Dynamic Island presentation and system lifetime limits need device validation.

## Verification and resolved failures

| Area | Evidence | Remaining limit |
| --- | --- | --- |
| Baseline Android | 97 tests and debug APK passed on Windows with existing ignored debug key | Baseline without that key fails validateSigningDebug |
| Integrated Android | 97 tests and debug APK pass locally and on Linux CI | No new Android device/manual UI session |
| Shared JVM | 8 tests pass: 6 common plus 2 parity tests | Matching baseline does not independently establish astronomy accuracy |
| Shared native | 6 common tests pass on iOS 26.4.1 simulator | Physical ARM64 device execution untested |
| Swift framework/app/extension | Native framework consumed by Swift; unsigned app/extension/tests compile with Xcode 26.4.1 | Device signing/install still required |
| iOS runtime | 8 app XCTest tests and 2 end-to-end UI tests pass on the final source, including accent contrast and selected-zone Arabic date formatting | No physical iPhone execution |
| Resources/storage | Arabic/English/nearest-place SQLite queries, 11 full/short audio resources and actual App Group snapshot round-trip pass | Playback audibility and widget presentation untested |
| UI | Actual English/light and Arabic/dark onboarding, city selection, home/calendar/Qibla/settings screenshots reviewed | Other iPhone sizes and full accessibility device audit pending |

The parity matrix covers 18,144 combinations: Madinah, Makkah, Damascus, London, New York, Tokyo, Auckland, Honolulu and Tromso; 6 dates including Ramadan/non-Ramadan and DST changes; 14 methods; both juristic choices; 4 high-latitude rules; 3 Hijri offsets and manual adjustments. Existing Android boundary/calendar tests plus common tests cover after-Isha/tomorrow-Fajr and before-Fajr/yesterday-Isha timestamps.

The [unchanged Linux baseline audit](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39076463651) reproduced a scheduler assertion race. PrayerApplication's background DataStore collector could overwrite explicit unit-test settings. Only the direct scheduler test class now uses a plain Application; every assertion is preserved and receiver/process integration tests still use PrayerApplication. No production scheduler change was made to satisfy it. Original KSP AWT shutdown/deprecation warnings also occurred in passing baseline builds.

Migration failures were fixed rather than hidden: geographic radians extraction, native binary search, Xcode resource configuration/architecture, bundled SQLite immutable access and simulator App Group signing. Unsigned compilation is checked separately; runtime tests use Xcode-managed local ad-hoc simulator signing. A prior manual iPhone-style entitlement signature was rejected by macOS before app launch and is not counted as test success. Screenshot inspection also caught a nearly transparent accent asset; it now uses opaque floating-point components, a configured global accent and light/dark contrast assertions.

## Screenshots from the running application

All eight final screenshots were downloaded from successful CI, inspected, and saved unmodified in [the screenshot gallery](ios/screenshots/README.md). English/light and Arabic/dark cover prayer home, Hijri calendar, Qibla and settings. The corrected accent, selected tabs, calendar controls, method picker and compass arrow are visible in both appearances. The original export manifest and SHA-256 mapping identify their exact source/run. No generated mockups are included.

## Exact build and CI instructions

Android/JVM requires JDK 21 (recommended), Android SDK platform 36.1/build-tools 36.0.0 and an SDK path in local.properties or ANDROID_HOME:

```powershell
.\gradlew.bat :shared:jvmTest :shared:compileCommonMainKotlinMetadata :app:testDebugUnitTest :app:assembleDebug --console=plain --max-workers=2
```

This Windows machine has Java 25 in the existing Android Studio JBR; verified local command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
$env:GRADLE_USER_HOME='C:\Users\M1993\.gradle'
Set-Location 'C:\Users\M1993\PrayerApp\.worktrees\ios'
.\gradlew.bat :shared:jvmTest :shared:compileCommonMainKotlinMetadata :app:testDebugUnitTest :app:assembleDebug --console=plain --max-workers=2
```

Debug builds use the existing ignored repository key when present, otherwise normal generated Android debug signing. No key is committed; release signing remains environment-only.

macOS ARM64: install/select Xcode 26.4.1, JDK 21 and XcodeGen, then from the ios checkout:

```sh
export DEVELOPER_DIR=/Applications/Xcode_26.4.1.app/Contents/Developer
brew install xcodegen
bash tools/ios/build.sh
xcodebuild -project iosApp/Salati.xcodeproj -scheme Salati -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/.derivedData CODE_SIGNING_ALLOWED=NO ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build-for-testing
bash tools/ios/sign_simulator.sh
xcrun simctl list devices available
# Set DEVICE to an available iPhone UDID on iOS 26.4, then:
xcrun simctl boot "$DEVICE" || true
xcrun simctl bootstatus "$DEVICE" -b
bash ./gradlew :shared:iosSimulatorArm64Test --device "$DEVICE" --console=plain
xcodebuild -project iosApp/Salati.xcodeproj -scheme Salati -destination "id=$DEVICE" -derivedDataPath iosApp/.derivedData -resultBundlePath iosApp/TestResults.xcresult CODE_SIGNING_ALLOWED=NO ARCHS=arm64 ONLY_ACTIVE_ARCH=YES -parallel-testing-enabled NO test-without-building
xcrun xcresulttool export attachments --path iosApp/TestResults.xcresult --output-path iosApp/screenshots
```

The initial compile is unsigned. Runtime tests use locally ad-hoc signed simulator products from sign_simulator.sh; Xcode supplies correct simulator entitlements, without an Apple account, certificate or provisioning profile. Intel Macs use the generator's IosX64 framework, x86_64 builds and iosX64Test instead. Regenerate after shared logic/resources change. Generated Xcode project, framework, resource copies and build output are ignored.

`.github/workflows/multiplatform.yml` runs on ios pushes and relevant pull requests, selecting macos-26/Xcode 26.4.1 explicitly and JDK 21. It compiles unsigned first, signs locally for runtime App Groups, runs common native/XCTest/UI tests, exports xcresult screenshots and uploads results. Android runs JVM parity, all Android tests and APK. Review/re-run an existing Actions run or push a source change to ios; docs-only pushes intentionally skip builds. Existing results are linked above. No distribution workflow is configured.

## Signing and physical installation later

Device installation needs Mac/Xcode access (a trusted remote Mac can work), an Apple ID/team, development certificate/private key and provisioning profiles for the iPhone's UDID, plus Developer Mode. Generate the device framework with `KOTLIN_TARGET=IosArm64 bash tools/ios/build.sh`, open iosApp/Salati.xcodeproj and select the development team for app and widget targets.

Register app IDs `com.salati.prayertimes.ios` and `com.salati.prayertimes.ios.widgets` plus App Group `group.com.salati.prayertimes`, enabling that group in both profiles. If IDs are already claimed, change IDs/group consistently in project, entitlements and snapshot code. App Group capability may require paid Apple Developer membership; none was purchased or enrolled. Remote signed builds also require securely supplied development signing material and registered-device profiles. No App Store Connect key/distribution certificate is needed for simulator CI.

Short physical-iPhone checklist:

1. Deny/grant GPS; verify exact coordinates, nearest label/zone near borders, true heading and low-accuracy/no-compass handling against a reliable local reference.
2. Check Arabic RTL/Western digits/month names, both appearances, small iPhone, largest text, VoiceOver, Reduce Motion and Reduce Transparency.
3. Compare selected calculation methods with local mosque practice; cross midnight/DST/Ramadan and verify tomorrow Fajr, night milestones and Hijri-offset expiry.
4. Grant/revoke both alert permissions; verify Fajr alarm versus quieter prayers, reminders, selected sounds, Focus/silent mode and locked/terminated/rebooted behavior. Measure actual sound duration.
5. Change location/settings and confirm old alerts cancel. Test background denial and six-day horizon exhaustion/replenishment.
6. Add Home Screen widgets, change language/time settings and cross midnight/expiry; start/schedule/end Live Activities on Lock Screen/Dynamic Island with signed App Groups.

No physical iPhone has been tested. Next work is this device/signing validation, then accessibility/size coverage and tuning based on observed results. Resolve those before release/distribution.

## Carry the independent Android_Legacy audio update into ios

Keep the baseline tag fixed. On Android_Legacy, commit only the intended recording and genuinely required metadata. On ios inspect the proposed commit first:

```sh
git show --stat AUDIO_COMMIT
# If the commit is exclusively the intended audio change:
git cherry-pick AUDIO_COMMIT
# If it also contains unrelated Android changes, instead:
git restore --source=AUDIO_COMMIT -- app/src/main/res/raw/athan_default.mp3
git add app/src/main/res/raw/athan_default.mp3
git commit -m "Update default adhan recording"
```

Choose one path, not both. Do not merge Android_Legacy wholesale. Rebuild Android and run tools/ios/build.sh/CI to regenerate the full recording and short CAF; verify duration and physical playback. Keep audio attribution/rights metadata with the change where applicable.

## Official references consulted

- [Kotlin Multiplatform compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)
- [Apple: adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Apple: scheduling AlarmKit alarms](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit)
- [Apple: notification sounds](https://developer.apple.com/documentation/usernotifications/unnotificationsound)
- [Apple: scheduled Live Activities](https://developer.apple.com/documentation/activitykit/activity/request(attributes:content:pushtype:style:alertconfiguration:start:))
- [SQLite immutable database URI](https://www.sqlite.org/uri.html#uriimmutable)
- [XcodeGen project/resource schema](https://github.com/yonaskolb/XcodeGen/blob/master/Docs/ProjectSpec.md)
