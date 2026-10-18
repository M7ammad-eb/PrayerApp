# Phase 2 implementation and validation

Build 3 continues the existing iOS application on `ios`. Build 2 remains archived; its earlier language claims do not establish that the new physical report was resolved.

## Shared architecture

Kotlin now supplies civil/religious date correspondence, month-scoped calendar corrections, widget current/next transition moments, stable scheduling plans, GPS acceptance/naming and compass alignment. Swift adapts those values to public native APIs. The Android consumer on `ios` uses the same date/calendar/location fixes; main and Android_Legacy are untouched. Sunrise remains the next displayed event in the existing six-event contract; alarms and automatic activities concern obligatory prayers only. After Maghrib only the religious label/Today correspondence advances: civil prayer rows stay on their original date.

The previous Android fix requirements (5 km movement; explicit cached fix <=15 minutes/250 m; routine <=2 hours/500 m) were reused. A shared 15-minute routine acquisition throttle is new. Both platforms reject late GPS callbacks after manual selection. Exact GPS coordinates are retained. The actual bundled table lacks parent/admin links and Sultanah has no Arabic alternate: ASCII Sultanah is honest; claiming Madinah as its administrative parent is unsupported.

## Native implementation

See [scheduling/audio](IOS_PHASE2_SCHEDULING.md) and [presentation](IOS_PHASE2_UI.md) for adapter behavior, reviewed failure cases, official Apple references and physical checklist. The per-host UIKit direction and SwiftUI direction agree; the model/tab selection survive host recreation. Language returns to Display. Inline titles remove the large-title void. Calendar has dual dates, Today and temporary month correction. Compass geometry is language independent. Settings migrate old choices, audio is never enabled by permission alone, and diagnostics distinguish submission from observation.

Widget layouts are family-specific, including large/accessory families and concise intents. Snapshot moments are generated in shared Kotlin; old snapshots decode but require an app refresh before use. The original AltStore/iLoader signed-group resolver remains intact. Automatic ActivityKit starts use prayer minus 15 minutes; ordinary reminders cover unaccepted requests. Exact future offline dismissal is not exposed by the public API: stale presentation and cleanup when execution is granted are implemented honestly.

## Validation status

Implementation and cross-review are complete. Local shared JVM tests passed (19 tests); Android regression tests/APK passed before the final manual-location callback guards, whose final rebuild is running. Native compilation, simulator runtime tests, visual inspection and final Release packaging are pending. This status will be replaced with actual run links and screenshot evidence after those checks; source changes alone are not proof of device behavior.

No physical-device audio, locked automatic start/dismissal, real compass sensor or newly signed App Group behavior was verified from Windows. Full original AlarmKit sound requests are implemented but uninterrupted full playback/repetition on the user's iOS version still needs the controlled device test. Ordinary notifications temporarily use system sound/silence until approved short recordings or exact cut points arrive. Eleven original MP3 assets are preserved.
