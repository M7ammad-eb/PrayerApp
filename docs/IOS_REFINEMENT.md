# Native UX refinement — build 4

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

Initial local regression: 24 shared JVM tests and 97 Android tests plus APK passed. Subsequent review fixes add a missing-anchor case and two Android retention cases; final CI is the authoritative complete-source run. Native compile, simulator evidence review and Release package verification are pending at this implementation commit. Do not interpret this document as a delivery claim until the final receipt is added.

Physical-device checklist: AltStore retain widget extension; open app and verify both widget variants; inspect actual compact/expanded Island and Lock Screen in English/Arabic; check current Isha/next actual Fajr around midnight; verify calendar correction/swipes; confirm prior preferences retained; test alarm/full audio and scheduled Activity start/end on the iPhone. Simulator fixtures do not verify GPS, compass hardware, audible delivery or system scheduling reliability.
