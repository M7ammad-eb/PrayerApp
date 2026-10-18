# Phase 2 implementation and validation

Build 3 continues the existing iOS application on `ios`. Build 2 remains archived; its earlier language claims do not establish that the new physical report was resolved.

## Shared architecture

Kotlin now supplies civil/religious date correspondence, month-scoped calendar corrections, widget current/next transition moments, stable scheduling plans, GPS acceptance/naming and compass alignment. Swift adapts those values to public native APIs. The Android consumer on `ios` uses the same date/calendar/location fixes; main and Android_Legacy are untouched. Sunrise remains the next displayed event in the existing six-event contract; alarms and automatic activities concern obligatory prayers only. After Maghrib only the religious label/Today correspondence advances: civil prayer rows stay on their original date.

The previous Android fix requirements (5 km movement; explicit cached fix <=15 minutes/250 m; routine <=2 hours/500 m) were reused. A shared 15-minute routine acquisition throttle is new. Both platforms reject late GPS callbacks after manual selection. Exact GPS coordinates are retained. The actual bundled table lacks parent/admin links and Sultanah has no Arabic alternate: ASCII Sultanah is honest; claiming Madinah as its administrative parent is unsupported.

## Native implementation

See [scheduling/audio](IOS_PHASE2_SCHEDULING.md) and [presentation](IOS_PHASE2_UI.md) for adapter behavior, reviewed failure cases, official Apple references and physical checklist. The per-host UIKit direction and SwiftUI direction agree; the model/tab selection survive host recreation. Language returns to Display. Inline titles remove the large-title void. Calendar has dual dates, Today and temporary month correction. Compass geometry is language independent. Settings migrate old choices, audio is never enabled by permission alone, and diagnostics distinguish submission from observation.

Widget layouts are family-specific, including large/accessory families and concise intents. Snapshot moments are generated in shared Kotlin; old snapshots decode but require an app refresh before use. The original AltStore/iLoader signed-group resolver remains intact. Automatic ActivityKit starts use prayer minus 15 minutes; ordinary reminders cover unaccepted requests. Exact future offline dismissal is not exposed by the public API: stale presentation and cleanup when execution is granted are implemented honestly.

## Validation and evidence

[Runtime CI 39155706316](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39155706316) passed on application source d3f45d5: 97 Android tests/APK, 19 JVM shared tests, native shared test task (17 common cases), 33 app tests, 5 UI flows on iPhone 17 Pro Max and 2 on iPhone 17e; iOS 26.4.1. Native compiler validation caught and corrected unsupported Section overloads and a base trigger API call. Earlier failed compiler attempts were 39153947373 and 39154905726. Final local Android tests/APK also passed using one worker/2 GB heap; shared JVM tests used 1.5 GB.

Visual inspection of [52 original PNGs](ios/screenshots/build3/README.md) confirms readable unmirrored glyphs after both direction changes/relaunch, stable Qibla geometry at the same simulated heading, readable dual dates and large-text lists, compact native titles, meaningful GPS success/denied UI fixtures, and all three widget sizes/accessories and Activity renderer states. Neighboring showcase cards sometimes cross the scroll viewport edges; overlapping captures show their content. These are real app/simulator renders; the widget/Activity showcase is explicitly an app-hosted preview, not installed WidgetKit or system Island geometry. No tinted-system-surface or physical sensor proof is claimed.

Visual review found the manual/GPS location-source labels missing from the Arabic dictionary. Final source 3e8187d adds only those three localization entries (including the nearest-city format) after the fully tested source; native/shared/Android logic is identical. Release packaging compiles and independently checks the Arabic values in both bundles; these corrected labels were not rendered again in the saved captures. The skipped duplicate push CI does not skip the explicitly dispatched [Release workflow 39158515340](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39158515340), PASSED.

No physical-device audio, locked automatic start/dismissal, real compass sensor or newly signed App Group behavior was verified from Windows. Full original AlarmKit sound requests are implemented but uninterrupted full playback/repetition on the user's iOS version still needs the controlled device test. Ordinary notifications temporarily use system sound/silence until approved short recordings or exact cut points arrive. Eleven original MP3 assets are preserved.

## Physical-device check after AltStore installation

1. Keep SalatiWidgets when signing/importing. Confirm Settings/About version 1.8 (3), open Salati to publish the shared snapshot and check Home/Lock Screen widgets after a language/location change.
2. Use Notifications and adhan → scheduled Test in one minute with a full original recording. Lock the phone and compare audible start/end with local requested UTC times. Repeat backgrounded/terminated, Focus/silent and with/without FaceTime; cancel test alerts afterward. A scheduling receipt is not an audible-delivery timestamp.
3. Enable automatic countdowns and open Salati to prepare them. Check the accepted 15-minute start, compact NAME/TIME and prayer-time stale label. Reopen to clean expired activities; exact offline dismissal at the deadline is not guaranteed by the public API.
4. Switch Arabic → English repeatedly, reopen sheets/relaunch, then check real compass direction, reliable alignment and a single entry haptic. Simulator geometry is verified; actual calibration/haptics remain device checks.
5. Check Hijri Today before/after Maghrib, month correction/reset and manual-location precedence. Exact acquired coordinates must remain selected; the bundled data cannot establish a Madinah–Sultanah parent relationship.

## Final delivery

- IPA: `C:/Users/M1993/PrayerApp/build/ios/Salati-Phase2-1.8-build3.ipa` (39,226,581 bytes), version 1.8/build 3, Release arm64 iPhoneOS.
- Packaged source: `3e8187df00af123e60efeeaae9257ffa69f44928`. Later documentation-only commits do not change the application.
- SHA-256: `edfad79c505cb132a8a489b08b71556885683b1aca872ee84cab0deeaeb44d99`.
- [Successful Release workflow](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39158515340), [successful runtime workflow](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39155706316), [independent receipt](ios/build3-verification.json).
- Both app/extension actual Mach-O platform/ARM64/build numbers, matching embedded ad-hoc App Group requests, ZIP/hash/size, absence of Apple profiles/debug dylibs, unchanged eleven original MP3s and the three corrected Arabic entries were independently checked after download. The database and dedicated silent one-shot CAF are present, and unapproved `_short.caf` files are absent.
- Sign/install through AltStore Classic with the widget extension retained, then open Salati once. Previous working IPA files remain intact. No Apple account credentials or paid signing were used by CI.
