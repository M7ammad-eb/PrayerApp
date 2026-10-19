# Widget and Live Activity refinement

## Architecture and preserved behavior

The existing `SalatiPrayer` widget kind and its standard small/medium/large layouts remain. A separate clearly named `SalatiPrayerHorizontal` / “Prayer times · Horizontal” gallery variant supports medium and large. Both use the same AppIntent city/date/sunrise options, timeline provider and signed App Group snapshot. The original small and Lock Screen accessory families retain purpose-built designs. There is no squeezed seven-column medium layout.

Horizontal layout follows the supplied Android screenshot's information order, not its colors or containers: optional city/date header, prominent next prayer/time/countdown, then side-by-side prayer names above times. Current events use semantic primary foreground/dot; upcoming events use blue foreground/dot and restrained emphasis. Other events are secondary. All live event selection is supplied by shared presentation moments.

## Overnight context

Shared-generated `displayEventIDs`, `tomorrowEventID` and `previousEventID` are authoritative. Swift does not infer tomorrow by replacing a passed row or recalculating day boundaries.

After Isha the six civil-day entries remain, with tomorrow's actual Fajr as a seventh explicitly labeled entry. The horizontal variant keeps its six readable civil-day columns and presents tomorrow Fajr separately below. The next summary, timer and highlighted entry resolve the same stable event ID. After midnight the displayed six entries switch to the new civil date; the previous day's current Isha is shown separately as “Previous day” until Fajr. Sunrise visibility only changes the schedule's presentation, never domain next-event identity.

## Live Activity

The scheduler and sound behavior are untouched. Compact Island content remains prayer NAME and remaining TIME, without a decorative replacement icon or a fake island shape. A stable width range is applied only to the countdown text; the system owns overall/camera geometry. Expanded content uses public per-mode 14-point content margins and `belowIfTooWide` for the heading. Clock time is explicitly introduced with “At”, remaining duration has a separate label, and location is secondary. Lock Screen presentation uses the same hierarchy in a compact balanced card. Progress uses the real `start...date` interval. Expired content remains controlled by `context.isStale`, which does not promise system dismissal.

## Evidence support and limits

The existing UI-test showcase reuses the exact renderer structs. Each preview has an “Open preview” destination to capture the complete renderer without adjacent cards being obscured by tab/navigation bars. New IDs cover horizontal medium/large, both layouts after Isha and both layouts after midnight. Overnight previews use the lead's shared-engine snapshot for a fixed calculation date and display a static remaining-duration string relative to that fixture instant; this is presentation formatting, not an alternative event policy. Live widgets still use native system timer text.

A UI-testing-only action starts a real local ActivityKit activity with a separate `salati.showcase.*` identity, plus cleanup and visible request errors. Tests can background the app and capture the actual simulator system surface when supported. This does not modify production prayer schedules, sound preferences or permission settings. App-rendered previews, actual simulator system surfaces and physical-device evidence must remain separately labeled. The supplied screenshot was inspected as layout evidence.

## Validation status

Implementation and lightweight diff checks completed by the widget agent. Lead owns native compile/runtime tests, shared overnight regression tests, actual screenshot inspection, packaging and device-limit documentation. No physical-device claim is made here.

References:

- https://developer.apple.com/documentation/widgetkit/making-a-configurable-widget
- https://developer.apple.com/design/human-interface-guidelines/live-activities
- https://developer.apple.com/documentation/widgetkit/dynamicisland
- https://developer.apple.com/documentation/swiftui/view/dynamicisland(verticalplacement:)
