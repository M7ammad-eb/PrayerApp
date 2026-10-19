# Native UX refinement — build 4

Delivered **Salati 1.8, build 4**: `C:/Users/M1993/PrayerApp/build/ios/Salati-Refinement-1.8-build4.ipa` (39,508,222 bytes), source `725a63f382e06e04c6de750f2c011c83b04c8f0b`. SHA-256: `c6071e4611b59d122609324023a6b311b772cb72ba8d4451ed9fd8619142ede5`.

[Release workflow 39320315492](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39320315492) passed on that exact source. [Independent verification receipt](ios/build4-verification.json) confirms both ARM64 iPhoneOS bundles, version/build/identities, WidgetKit extension identity, ad-hoc App Group request metadata, all 11 original MP3 hashes and the exact offline database hash, compiled assets/icon presence, Arabic refinement translations and absence of Apple profiles/debug dylibs. Workflow checkout identity and Release compiler commands establish source/configuration provenance; that provenance is not inferred from the binary alone.

Import this IPA into **AltStore Classic**, retain the widget extension, then open Salati once. AltStore must sign it with the user's Apple ID; the package contains capability request metadata, not an Apple distribution signature. Existing preferences and bundle identities are preserved. The prior build 3 IPA is unchanged (SHA-256 `edfad79c505cb132a8a489b08b71556885683b1aca872ee84cab0deeaeb44d99`).

## Implementation and architecture

Continues `ios` from delivered build 3 / handoff `2127b6b`. Original widget kind, bundle identities, App Group resolver, unchanged full audio recordings, scheduled-alert/ActivityKit lifecycle and existing user preferences are preserved. No changes to main, Android_Legacy or baseline tags.

The shared module now owns adjusted month boundaries, finite correction reconciliation, calendar week count, overnight displayed event IDs and onboarding defaults. Swift only renders these decisions. Native setup and notification data models remain compatible.

### Corrected scoped calendar semantics

The old model selected an offset by the **unadjusted civil month** then relabeled cells. Its reset duplicated day 1 and could omit the adjusted first day. Build 4 shifts the named month's start and end together by the selected −2…+2 correction, preserving all of its 29/30 days exactly once. To reconcile with the unchanged distant calendar, neighboring boundaries move only as far as necessary: a baseline 30-day month can lose one day; a 29-day month can gain one. Reconciliation proceeds outward until the offset is absorbed. All months remain 29 or 30 days; no repeated/missing labels, overlapping civil dates or synthetic day 31/32. The stored preference remains one named-month correction, not a global offset. Replacing/resetting that correction recalculates its finite neighborhood. This is a manual civil correspondence, not a claim that local moon sighting has been observed.

Corrections are retained for historical browsing. Android no longer deletes them on entering a raw new month. Its calculation adapters and cache invalidation include the anchor; the Ramadan Isha rule uses the same shared date mapping. A missing anchor applies no scoped correction. The legacy unscoped calendar/calculation APIs remain available for callers explicitly requesting a global offset.

Calendar pages derive actual first/last dates before their Saturday-based complete-week range. Shared returns 35 or 42 real cells. iOS reserves exactly six stable scaled rows and uses noninteractive, accessibility-hidden empty space for unused slots. Numeric civil labels are Western `day/month`, isolated LTR. The viewed month is anchored while adjusting, so editing a boundary cannot jump the page. Horizontal swipes follow native previous/next semantics in each language; arrows and Today remain available.

### Overnight event identity

Shared presentation retains the civil schedule and adds tomorrow's calculated Fajr ID after Isha. It never recolors the passed Fajr. At midnight the day switches, with previous Isha represented separately until Fajr. Snapshots carry displayEventIDs, tomorrowEventID and previousEventID; legacy files decode safely. Both widget layouts use these decisions. Home uses shared calculated current/next instants and explicitly separates Tomorrow from civil rows. Sunrise retains its established next-event behavior and ends current Fajr.

### Onboarding defaults

The existing project IANA-zone calculation-method recommendation is reused. Unknown regions use Muslim World League. Standard Asr remains the reviewable existing default; coordinates do not infer a person's madhhab. Shared resolve policy preserves each explicit override and every completed setup. Legacy persisted method/Asr choices migrate conservatively as explicit. Advanced disclosure applies only to onboarding; normal Settings retains ordinary method and Asr controls.

## UI and review

- [App/onboarding/notification UX](IOS_REFINEMENT_UX.md).
- [Preserved and horizontal widgets, Activity layouts](IOS_REFINEMENT_WIDGETS.md).
- Two bounded agents plus lead; shared/model/localization integration centralized. Independent reviews identified and resolved neighbor day-length conflicts, Android expiration, and missing-anchor Ramadan behavior. No overlapping file edits or agent-run builds.
- Public Apple HIG and DynamicIsland contentMargins/verticalPlacement documentation checked. Actual Activity preview controls exist only under explicit UI-test showcase launch arguments and clean up their own event IDs.

## Validation and delivery

Shared/Android validation at `372d45f`: 25 shared JVM tests, 99 Android tests and APK passed in [Android job](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39300912470/job/118225558932). That run's native compile failure (explicit Swift `self.locale` capture) was fixed; subsequent commits do not change shared/Android sources.

First native runtime [39303087513](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39303087513), source `930ad38`: 23 native shared tests, 38 app tests and all 7 larger-device UI flows passed. On iPhone 17e, normal navigation and widget flows passed; large-text setup failed because the test tapped an offscreen location row under the fixed footer. Its captured accessibility hierarchy shows the location button at y799.7 with height126, below the usable content region. The repaired test scrolls within the Form's unobscured region and asserts the entire control is above the footer before tapping.

Lead and independent reviewers inspected all 122 first-pass PNGs (74 app, 48 widget/Activity), including original-resolution checks where needed. Review found and fixed the large-text adjustment label, untranslated active-adjustment note, alarm-default footer wording and unused space in the new horizontal large widget. Calendar fonts and cell padding are bounded consistently, retaining stable month geometry. An explicit Stepper accessibility label preserves its purpose when the visible heading moves above it. Medium widgets, including Arabic after-Isha/midnight on 17e, showed no clipping or identity mismatch.

Actual-system evidence in this first pass is limited: the Arabic expanded Island rendered inside its curved bounds; compact captures and the English expansion were not populated. They are not claimed as verified. The retry waits for asynchronous system presentation and expands Arabic alert-flow coverage. App-hosted widget/Lock Screen previews remain labeled as previews.

Full native validation [39310536112](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39310536112), source `029b8ddd5ee2a51e00c1af9e0aef19d2f1078868`, passed: 23 native shared tests, 38 app tests, seven iPhone 17 Pro Max UI flows and three iPhone 17e UI flows, all without failures on iOS 26.4.1. The 38 app tests include 19 scheduling regression tests. Lead and independent reviewers inspected all 132 PNGs (84 app and 48 widget/Activity), with original-resolution checks. The previous app issues are corrected. One Arabic +2 calendar capture catches digits during its transition; settled adjusted calendars and numeric/accessibility assertions pass. This is a capture-timing limitation, not evidence of a persistent overlap.

Actual system compact Islands show the complete prayer name and countdown in both languages. The full run's English expanded presentation still clipped at its upper corners, which the app-hosted preview could not expose. Source `725a63f382e06e04c6de750f2c011c83b04c8f0b` increases only the expanded system content margins to 24 points horizontally and 20 vertically. Its focused widget/actual-system validation [39318100255](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39318100255) passed, executing the selected UI test in 352 seconds and exporting 38 captures. Lead inspection of all four actual system originals confirms English/Arabic expanded text is inside the curves and compact name/time remains readable. All app, shared and Android source remains identical to the passing full validation; the focused run avoids repeating unrelated flows.

The final [132-image collection](ios/screenshots/build4/README.md) retains 84 app and 10 compact-widget captures from the full run and replaces its 38 larger-device widget/Activity captures with the focused run. Each image records its exact source, workflow and hash. Raw artifact SHA-256: full runtime `b090795afb13df3e5b8b30436de253418ba63714f82a31405be76b4a3677279b`; focused runtime `ab998d0aaa42c0b636b82ec09ce9579513509185cfc17c20221c8453c4607380`. Superseded clipped/unpopulated captures are not represented as final evidence.

Physical-device checklist: AltStore retain widget extension; open app and verify both widget variants; inspect actual compact/expanded Island and Lock Screen in English/Arabic; check current Isha/next actual Fajr around midnight; verify calendar correction/swipes; confirm prior preferences retained; test alarm/full audio and scheduled Activity start/end on the iPhone. Simulator fixtures do not verify GPS, compass hardware, audible delivery or system scheduling reliability.
