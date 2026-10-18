# Phase 2 native presentation work

Implementation source is integrated by the lead on `ios`. This document records UI architecture and evidence boundaries; passing builds, screenshots and physical results are recorded in the final progress/handoff after validation.

## Native layout and language

Home, calendar, Qibla, Settings, location sheets and coordinate entry use native inline navigation titles rather than large-title reserved space. No negative safe-area padding or device-specific vertical offsets were added. Language is grouped in Display again. The lead owns the per-host UIKit/SwiftUI reading-direction fix; UI files add no horizontal transforms or appearance-wide changes. Settings About shows bundle version/build for physical-device identification.

The UI test flow changes English → Arabic → English repeatedly, visits calendar/Qibla, reopens the location sheet, and verifies saved language after relaunch. Control-order assertions complement screenshot inspection; accessibility-tree text alone cannot prove glyphs are visually unmirrored.

## Calendar and compass

Calendar cells show the Hijri number with Gregorian day/month beneath; full Gregorian labels are available to accessibility and selected-day details. Today sits beside the month header. Today fill, selected outline/check, and observance dots are distinct. At accessibility text sizes, a readable list replaces the seven-column grid. Month correction (-2…2) and reset use AppModel/shared month-scoped preferences, including the Settings link to the same experience. The current religious-date highlight can advance at Maghrib; civil cell mapping and prayer schedules remain civil dates. Calendar boundary decisions are provided by shared code, not duplicated in Swift. Month correction remains temporary: it expires at the unadjusted month boundary, so labels can repeat when the correction resets. A separate active-correction row/reset stays visible if a negative adjustment moves Today into the preceding displayed month; users can still identify/reset the affected raw AH month.

Only compass geometry forces left-to-right layout; surrounding text follows the selected language. Shared CompassAlignmentTracker supplies continuous wrapped rotation, reliability, tolerance, hysteresis and debouncing. Swift feeds observations and a cancellable 650 ms later reevaluation for a stationary sensor. A single system alignment haptic is triggered on entry. Heading collection stops offscreen/inactive; Reduce Motion disables rotation interpolation. Real sensor accuracy/haptics still need physical verification.

## Widgets and Live Activity presentation

AppIntentConfiguration provides independent city/date/sunrise choices. Actual WidgetKit family controls each live layout:

- Small: next prayer, scheduled time, countdown, optional compact location/date.
- Medium: next prayer/countdown alongside the civil day's schedule.
- Large: location, religious/civil dates and schedule with current/next emphasis.
- A separate Lock Screen widget supports inline, circular and rectangular accessory families.

The provider uses shared-generated snapshot moments (prayer transitions, midnight and Maghrib), not per-second app execution. Domain current/next and religious date selection come from the central snapshot. System-rendered timers update countdowns. The existing signed-group resolver is untouched. Live missing/expired data has a localized refresh state and never substitutes sample prayer times. Illustrative data is confined to WidgetKit preview/placeholder contexts and the explicitly labeled UI-test showcase.

Compact Dynamic Island shows prayer NAME in compactLeading and remaining TIME in compactTrailing, with Western digits and no replacement icon. Minimal, expanded and balanced Lock Screen presentations are deliberate. The system's context.isStale switches expired cards/expanded text to the localized prayer-time message and removes the progress/“Until prayer” presentation; compact time stops at 0:00. This represents expiration, not a guarantee of automatic system dismissal. The app showcase reuses actual widget views and the exact compact/expanded region renderers. It composes those regions in ordinary SwiftUI and does not simulate system camera geometry, installation, ActivityKit lifecycle, or physical-device behavior.

## First run and location

Short native setup groups language, GPS/offline location, calculation defaults, optional alert preferences and confirmation. GPS progress and errors observe the same central LocationService as Settings/location search; manual selection stays available. Existing onboardingComplete preferences are preserved. Permission alone does not enable sound. Source and last update are visible where available. Naming uses the lead's offline display resolver; no parent-city relationship is inferred in Swift.

## Evidence to collect

The UI suite captures actual simulator Home/calendar/Qibla and repeated language transitions, widget/activity renderer previews in English/light and Arabic/dark, explicit simulated GPS success/denied fixtures, and an accessibility text case. The lead coordinates compact/larger simulator runs and visually inspects exported PNGs. Simulator fixtures and app-rendered previews must stay labeled as such; they do not prove physical location, heading, system widget placement, Dynamic Island geometry, alert audio or ActivityKit start/dismiss behavior.

Apple references consulted:

- https://developer.apple.com/design/human-interface-guidelines/toolbars
- https://developer.apple.com/design/human-interface-guidelines/widgets
- https://developer.apple.com/design/human-interface-guidelines/live-activities
- https://developer.apple.com/documentation/swiftui/layoutdirection
- https://developer.apple.com/documentation/widgetkit/appintentconfiguration
- https://developer.apple.com/documentation/widgetkit/dynamicisland

- https://developer.apple.com/documentation/widgetkit/activityviewcontext/isstale
- https://developer.apple.com/documentation/activitykit/activitycontent/staledate
