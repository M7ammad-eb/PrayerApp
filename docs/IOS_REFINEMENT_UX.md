# Native app UX refinement

The app UX spans PlacesView.swift (including onboarding), HomeView.swift, SettingsView.swift, AlertSettingsView.swift and Settings.swift. Shared policy, AppModel, resources and tests are integrated centrally.

## Onboarding

Five consecutive pages: language, location, calculation, alerts, completion. The compact native Form content scrolls independently of a persistent bottom progress / Continue control. Back preserves choices. Location Continue requires an actual saved location. GPS remains one-shot; the service exposes requesting, acquiring, resolution, success and failure. Offline city selection and coordinate entry remain available. Search shows loading, empty and actionable error states.

Calculation recommendations come exclusively from AppModel.applyOnboardingDefaults and the shared policy. Advanced controls are expanded only in onboarding; ordinary Settings retains method, Hanafi Asr and high-latitude controls. Each explicit method/Asr change records its own override flag. Legacy JSON without flags conservatively preserves any stored method/Asr choices, including interrupted older setup. Onboarding completion and all existing alert policies remain untouched on upgrade.

AppModel-owned onboarding/review steps survive the existing language host replacement. Review setup is an explicit Settings action and never clears data or onboarding completion. Changes save normally; Close does not pretend to roll them back.

Alerts remain off by default. The onboarding page links to per-prayer choices; permission controls appear after the corresponding choices. Permission itself never enables audible prayer policies. Continue skips optional alerts. The completion page confirms location, method/Asr, enabled-prayer count and automatic countdown.

## Home and Settings

Home keeps today's civil rows and separately shows the actual shared next event under Tomorrow when it is outside the civil schedule. The summary, clock, countdown and Tomorrow row all reference day.next rather than recoloring today's Fajr. Previous-day current prayer is labeled separately. Ended Fajr no longer receives current emphasis after sunrise. Numeric clocks/countdown are LTR with Western digits. Current and next labels provide a non-color distinction.

Location details are secondary disclosure in Settings; normal calculation lists and Display language remain directly available. Widget help describes the preserved original variant plus the new horizontal medium/large variant. Review setup is optional. Raw scheduling status moved from Home to diagnostics reached through alert settings.

## Notification journey

The existing per-prayer model is retained. A concise five-row list leads to a native detail page for delivery, sound and advance reminder. Off hides irrelevant sound controls without discarding their saved values. Full original recordings remain selectable only for system alarms; older selected recordings remain visibly retained when notification delivery uses system sound. Advance reminders remain separate, never adhan.

Countdown / advance-reminder options, contextual permission controls and ordinary actionable guidance stay on the main page. A Test an alert screen schedules the existing real one-minute test and includes explicit permission actions even when all regular prayer policies are Off. Listen to adhan is a separate immediate preview screen with explicit Stop; navigation does not terminate playback. Diagnostics remain a separate drill-down. No scheduler, sound asset, AlarmKit, ActivityKit or fallback implementation changed here.

## Official design guidance

- [Apple Onboarding](https://developer.apple.com/design/human-interface-guidelines/onboarding): short contextual introduction, purposeful steps, permission requests in relevant context.
- [Apple Privacy](https://developer.apple.com/design/human-interface-guidelines/privacy): request access when needed and retain an offline manual alternative.

## Validation

The complete native suite passed at `029b8dd`: 23 shared, 38 app, seven larger-device UI flows and three compact-device flows. Independent review inspected all 84 app captures, including five original-resolution checks. English/Arabic setup, notifications, calendar, repeated direction changes, Qibla geometry and large text passed. The compact setup test scrolls within the available Form area and confirms the location control is above the fixed footer before tapping it. GPS captures are simulated UI states, not sensor verification. Detailed workflow links and evidence limitations are in [the milestone handoff](IOS_REFINEMENT.md).

## Localization

Reviewed English/Arabic additions are maintained in `tools/ios/refinement_ux_translations.json` and the calendar/widget catalogs, then generated into both app and extension resources. Final package verification checks every added Arabic translation in both compiled bundles.

## Independent review and test implementation

Widget agent reviewed the app UX and requested accurate full-recording wording; the copy now says prepared for the alarm and retains an explicit physical-device test action. Lead-owned shared review found and fixed an unanchored scoped-offset regression: a missing anchor must not accidentally activate legacy global offset for Ramadan-dependent Isha while labels remain unadjusted.

An independent lightweight arithmetic check of all supported table anchor/offset combinations found 14,426 reconciled corrections with every adjacent month 29/30 days, and maximum finite reconciliation reach eight months. Fourteen edge-of-table cases cannot reconcile within supported bounds and correctly reject the correction. This complements, but does not replace, the lead's Kotlin test suite.

UI tests cover the five consecutive onboarding pages in both languages, manual-success and simulated GPS permission/acquiring/success/denial/timeout states; per-prayer detail, scheduled-test and immediate-preview journeys; deterministic five/six-week months, fixed height, ±2, swipe direction and numeric Gregorian correspondence in both languages; existing repeated language changes and Qibla regressions; isolated original/horizontal/night widget renderers and Activity surfaces. Actual system Live Activity captures use a real system activity on the Island simulator; renderer previews remain labeled separately. Lead and independent reviewers inspected the captures and resolved concrete visual findings before delivery.
