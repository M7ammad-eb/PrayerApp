# Salati iOS implementation handoff

The current delivered milestone is native UX refinement, version 1.8/build 4, application source `725a63f382e06e04c6de750f2c011c83b04c8f0b`: `Salati-Refinement-1.8-build4.ipa`. Its corrected shared calendar/event/default policies, completed UX, passing runtime/Release workflows and independent delivery receipt are in [IOS_REFINEMENT.md](IOS_REFINEMENT.md) and [IOS_PROGRESS.md](IOS_PROGRESS.md). Phase 2 scheduling/audio remains documented in [IOS_PHASE2.md](IOS_PHASE2.md). Previous build 3 and earlier IPA files are preserved.

## Historical Phase 1 handoff

The implementation/build references and limitations below describe Phase 1. Where superseded, use the current refinement and Phase 2 handoffs above; in particular the calendar now keeps a fixed six-row grid area at accessibility sizes, onboarding uses consecutive pages, and sound roles follow Phase 2.

## Repository and review references

- Android baseline: `9d4382d60f23de3b8f2be30cf877b27b0211515d`, version 1.8 (8).
- Annotated tag: `android-1.8-baseline`. Both local and remote `main` and `Android_Legacy` remain at the baseline. The primary main checkout's pre-existing uncommitted changes are preserved.
- All migration work is on [ios](https://github.com/M7ammad-eb/PrayerApp/tree/ios). Latest application fix source: `bca1df0555e9ee517b11e8e9a435fc46c291364a`; see widget/language verification below. The initial migration test record is retained separately.
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

## Unsigned iPhone IPA

The separate `.github/workflows/unsigned-ipa.yml` workflow builds the actual `IosArm64` Kotlin framework and iPhoneOS app/widget binaries, then packages `Payload/Salati.app` into `Salati-unsigned.ipa`. It validates both Mach-O platform identifiers (iPhoneOS, not ARM64 simulator), bundle platform metadata, all 11 recordings, database presence and ZIP integrity. No certificate or provisioning profile is embedded. The current packaging lane uses Release for both Kotlin and Swift, explicitly disables debug dylibs, and retains app/widget dSYM symbols separately. It still requires proper signing/provisioning before installation.

Reproduce on the pinned Mac/Xcode environment:

```sh
KOTLIN_TARGET=IosArm64 KOTLIN_BUILD_TYPE=Release bash tools/ios/build.sh
bash tools/ios/package_unsigned_ipa.sh
```

Output: `build/unsigned-ipa/Salati-unsigned.ipa`, SHA-256 file and build-info.json. CI artifact `Salati-unsigned-iphone-ipa` also contains the app/widget App Group entitlements for subsequent signing. [Successful device packaging run](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39082393881), source `b10467999ca8329206062ef2a1d8e5f5953c956f`. The IPA was downloaded to `C:/Users/M1993/PrayerApp/build/ios/Salati-unsigned.ipa` (58,583,649 bytes), independently checked as unsigned ARM64 iPhoneOS app and widget binaries, with SHA-256 `ac9e700503e9be1e6716111929ccc65eb8b1ba40d03cabb86a0cebdc3428bbf9`. The user subsequently confirmed successful installation of this original IPA via iLoader/free Apple ID on iOS 27.0.1 after a restart/failed-install cleanup. Launch and individual feature behavior have not yet been confirmed.

## Signing and physical installation later

For the Xcode-managed development-signing path, use Mac/Xcode access (a trusted remote Mac can work), an Apple ID/team, development certificate/private key and provisioning profiles for the iPhone's UDID, plus Developer Mode. Windows iLoader with a free Apple ID is also user-confirmed to install the unsigned IPA after signing; that tool manages its own IDs/profiles. Widget App Group matching and all runtime features still need device validation. Generate the device framework with `KOTLIN_TARGET=IosArm64 bash tools/ios/build.sh`, open iosApp/Salati.xcodeproj and select the development team for app and widget targets.

Register app IDs `com.salati.prayertimes.ios` and `com.salati.prayertimes.ios.widgets` plus App Group `group.com.salati.prayertimes`, enabling that group in both profiles. If IDs are already claimed, change IDs/group consistently in project, entitlements and snapshot code. App Group capability may require paid Apple Developer membership; none was purchased or enrolled. Remote signed builds also require securely supplied development signing material and registered-device profiles. No App Store Connect key/distribution certificate is needed for simulator CI.

Short physical-iPhone checklist:

1. Deny/grant GPS; verify exact coordinates, nearest label/zone near borders, true heading and low-accuracy/no-compass handling against a reliable local reference.
2. Check Arabic RTL/Western digits/month names, both appearances, small iPhone, largest text, VoiceOver, Reduce Motion and Reduce Transparency.
3. Compare selected calculation methods with local mosque practice; cross midnight/DST/Ramadan and verify tomorrow Fajr, night milestones and Hijri-offset expiry.
4. Grant/revoke both alert permissions; verify Fajr alarm versus quieter prayers, reminders, selected sounds, Focus/silent mode and locked/terminated/rebooted behavior. Measure actual sound duration.
5. Change location/settings and confirm old alerts cancel. Test background denial and six-day horizon exhaustion/replenishment.
6. Add Home Screen widgets, change language/time settings and cross midnight/expiry; start/schedule/end Live Activities on Lock Screen/Dynamic Island with signed App Groups.

The user confirmed installation on a physical iPhone running iOS 27.0.1. Device feature validation, accessibility/size coverage and tuning based on observed results remain next. Resolve those before release/distribution.

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

## iLoader installation diagnosis

The reported iLoader/isideload 0.4.6 log reached `App signed!`, passed `VerifyingApplication`, and then failed during `SandboxingApplication` with `_GetContinuationFD` / ENOENT for an iPhone bundle-container directory. This points toward installation/container state on the device; it does not establish an IPA filename, signature or minimum-OS rejection. Restart the iPhone and remove only an incomplete/grey failed Salati installation before retrying. Do not treat a successful compile as proof of a successful device install.

The original IPA was a valid Debug build using Xcode's stub executable plus separate `.debug.dylib` and `__preview.dylib`. The published isideload 0.4.6 source recursively handles dylibs, so that layout is not a confirmed cause of this error. The replacement is a conventional Release build with no preview/debug dylibs, eliminating this packaging variable while preserving the widget extension. See [Apple's build-layout documentation](https://developer.apple.com/documentation/xcode/understanding-build-product-layout-changes). The user retried the original IPA after the restart/failed-install cleanup and confirmed installation succeeded. This supports a transient installation/container-state explanation; the exact iOS cause is not proven. A Release rebuild was not needed to obtain that successful installation. The user later confirmed initial main-app operation and reported the widget/language issues described below.

The optional Release IPA is available at `C:/Users/M1993/PrayerApp/build/ios/Salati-release-unsigned.ipa` (57,888,905 bytes), SHA-256 `402534c382fadef2c982dbc1bc550ef35a9e4c7bfa98dcfdafb826eaa430f519`. [Release build 39083123917](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39083123917) passed on packaging source `6ad3ca29368433302484baaf4375232c7a2f126c`; local checks confirmed unsigned ARM64 iPhoneOS binaries and no debug/preview dylibs. Widgets and Live Activities remain included. Original working installation can be kept; this optional file has not been installed on the device. Matching app/widget dSYM symbols are retained in that run's separate artifact.

## Widget storage and post-setup language update (build 2)

The user reported a working main application but the widget placeholder, together with the main-page shared-storage warning. The published isideload 0.4.6 signing code appends the team to the main bundle ID, cascades that ID into the widget, and provisions `group.<rewritten main bundle ID>` for both. Salati previously used only `group.com.salati.prayertimes`, so its signed app/widget could not access that original group.

`SharedScheduleStorage` now derives the parent app ID from either executable, prefers the corresponding iLoader group for rewritten IDs, and retains the declared native group as a fallback. FileManager still checks actual signed entitlement access; there is no private API or access-control bypass. Writes and reads try usable authorized containers consistently. The widget preserves language metadata on expired timelines. Background task registration reads the actual permitted identifier from the signed Info.plist, which iLoader also rewrites.

Language selection is a visible English/Arabic segmented control at the top of Settings. Existing preferences/location remain compatible. Runtime tests cover switching both ways after onboarding and retaining the selected language after relaunch.

The simulator signing helper adds a test-only iLoader-style group to the app/widget signatures. A runtime test writes into that actual OS App Group using a simulated rewritten app ID and reads it using the corresponding widget ID, while denying the original group in the test's container lookup. Device IPA entitlements remain the original provisioning configuration; iLoader supplies its own signed groups. Additional tests cover inaccessible-container fallback and expiry. This verifies the storage protocol, not actual Home Screen presentation on the user's iPhone.

The initial fix source `bca1df0555e9ee517b11e8e9a435fc46c291364a` passed [full tests 39085081651](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39085081651): 97 Android, 8 JVM shared, 6 native shared, 12 app tests and 2 UI flows. A subsequent screenshot review caught mirrored native Form content after Arabic-to-English switching. Source d3c1573 recreated only Form, but its stronger direction check still failed in [39086671539](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39086671539); all 12 app tests and the initial Arabic UI flow passed. Current source `a419ded` recreates the full screen hierarchy on language changes, with the app model and tab selection retained outside that identity. Tests check both RTL/LTR control positions, language persistence after relaunch, and settled screenshots. Final verification PASSED: [runtime 39087685774](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39087685774) and [IPA 39087685776](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39087685776), application source `a419ded14261322b33071d8f06d6c45c0df47834`. Totals: 97 Android tests/APK, 8 shared JVM tests, 6 native shared tests, 12 app XCTest tests and 2 UI flows. Both direction-transition PNGs were visually inspected; [current screenshot evidence](ios/screenshots/build2/README.md) includes original manifest and hashes. The preceding run 39086061128 failed before tests during cold simulator migration; the CI now boots in parallel with compilation and waits up to 10 minutes. Physical confirmation is required before claiming this update fixes the user's widget.

### AltStore Classic support

The user subsequently chose AltStore Classic. Its [profile provisioning implementation](https://github.com/altstoreio/AltStore/blob/classic/AltStore/Operations/FetchProvisioningProfilesOperation.swift) reads requested App Groups from the original app's Mach-O entitlements, appends the team ID to each group, and provisions those groups. Its [resigning implementation](https://github.com/altstoreio/AltStore/blob/classic/AltStore/Operations/ResignAppOperation.swift) records each signed profile's group names in `ALTAppGroups` for the main app and extensions.

A completely signature-free IPA does not embed those capability requests. `tools/ios/package_altstore_ipa.sh` therefore creates a separate `Salati-AltStore-ready.ipa`: local ad-hoc signatures embed the declared App Group request in app and widget, without a certificate or provisioning profile. AltStore must replace those signatures with the user's Apple-ID signing. CI verifies ad-hoc status, matching capability metadata, bundle integrity and absence of profiles. The fully unsigned IPA remains a separate artifact. This is capability preservation for normal re-signing, not device-installation signing.

Shared storage prioritizes Salati's groups from `ALTAppGroups`, ignores unrelated AltStore backup groups, then supports iLoader's derived group and the original declared group. Main-app settings now use private UserDefaults; accessible legacy settings migrate once without overriding newer private settings. Simulator-only signatures include a second signer-specific test group for AltStore. Tests cover different app/widget ID suffix placement, actual OS group read/write, private-settings migration and language persistence. When importing into AltStore Classic, retain the SalatiWidgets extension. Physical confirmation remains pending for the updated widget.


### Final AltStore Classic package (build 2)

The Release package from application source `a419ded14261322b33071d8f06d6c45c0df47834` passed [IPA CI run 39087685776](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39087685776), with matching runtime source in [successful run 39087685774](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39087685774).

- Delivery: `C:/Users/M1993/PrayerApp/build/ios/Salati-AltStore-1.8-build2.ipa` (57,915,412 bytes).
- SHA-256: `0b382882c9267aa293f2dda87bc16ab7b1c253faf45ccf477d34073af7a71d46`. Independent verification receipt, metadata and checksum are saved alongside it.
- Both binaries are actual arm64 iPhoneOS Release executables, build 2, carrying the same App Group request in ad-hoc signatures. No Apple certificate, provisioning profile, preview or debug dylib is included. Independent artifact/IPA hash, ZIP and Mach-O inspection also confirmed the database and all 11 MP3s. Matching dSYMs are in that CI run's symbols artifact.
- Import this package into AltStore Classic and retain SalatiWidgets. AltStore must sign it with the user's Apple ID. Open Salati once after installation to write the shared schedule and request widget reload. Re-add the Home Screen widget if a previous installation left a cached placeholder; no app deletion/reset is required by this fix.
- Language is the first section in Settings. Changes persist privately after relaunch; switching languages recreates the screen hierarchy while retaining the selected tab and app model. Position assertions pass in both reading directions, and exported transition screenshots show readable, unmirrored text.
- Actual AltStore signing and widget display on the user's iPhone still require physical confirmation. Simulator regression coverage verifies authorized group write/read and metadata resolution, rather than Home Screen presentation on a signed device.
