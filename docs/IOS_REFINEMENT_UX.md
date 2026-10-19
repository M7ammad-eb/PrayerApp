# Native app UX refinement

Ownership: PlacesView.swift (including onboarding), HomeView.swift, SettingsView.swift, AlertSettingsView.swift, Settings.swift. Shared policy, AppModel, resources and tests are integrated by the lead.

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

## Validation handoff

No resource-intensive builds started by this agent. Lead owns combined compiler/runtime checks and screenshot capture. UI tests must press continueOnboarding once before chooseLocation, then continue through calculation/alerts/completion. GPS fixture screenshots advance past welcome once. Accessibility identifiers: onboardingStep, onboardingBack, onboardingAdvanced, onboardingAlertSettings, onboardingAllowNotifications, continueOnboarding, chooseLocation, onboardingSelectedLocation; prayerAlert.FAJR (and other prayer IDs), alertDelivery; scheduleTestAlert now lives inside Test an alert.

## New English resource keys for lead localization

The list below is a static string scan; verify dynamic arrays (onboarding titles/subtitles) and compiled resource coverage during integration.

- A regional method is not available for this time zone. Muslim World League and Standard Asr provide a reviewable starting point.
- A starting point for your location, with the freedom to adjust.
- Add a Salati widget to keep prayer times on your Home Screen.
- Add a widget
- Advance reminder
- Advance reminders use the system sound or silence, never adhan.
- Advanced settings
- Alert status and diagnostics
- Alerts stay off until you choose them. Continue without enabling alerts if you prefer.
- Alerts, your way
- Allow Live Activities in iOS Settings to show the countdown.
- Allow the selected delivery type, then start the test and lock your iPhone.
- Automatic prayer countdown
- Back
- Based on your time zone. Standard Asr is the default; you can change either choice.
- Cancel test alerts
- Choose Prayer times for the original layout, or Prayer times · Horizontal for side-by-side prayer times in medium and large widgets.
- Choose a city offline, or use your precise location. Check the time zone if you are near a border.
- Choose an audible alarm sound
- Choose delivery and sound for each prayer. Off means no prayer alert; advance reminders are a separate choice.
- Choose prayer alerts
- City search is unavailable. Try again or enter coordinates.
- Clear local log
- Close
- Countdown and reminders
- Enabled prayers
- English
- Hanafi
- Help
- Keep prayer times close. Choose only the alerts you want.
- Listen to adhan
- Local diagnostics
- Location details
- Notifications are turned off in iOS Settings.
- Notifications follow your iPhone sound settings. System alarms use a separate permission.
- Notifications use the system sound or silence. Full adhan recordings are available with system alarms and in Listen to adhan.
- Open Salati after installing or changing location to update your widgets.
- Open Salati regularly to keep future alerts ready.
- Open iOS Settings
- Permission applies to the choices above. It does not enable other prayer alerts.
- Permission lets Salati deliver the reminders you selected. It does not turn on other prayer alerts.
- Permissions
- Plays immediately. Listening does not change your prayer alert settings.
- Prayer times for your location
- Ready for your day
- Review setup
- Review your alert settings
- Salati · صلاتي
- Search for Salati and choose a size.
- Searching offline cities…
- Selected adhan — system notification sound for now
- Selected location
- Setup progress
- Share local log
- Sound for advance reminders
- Standard
- Start using Salati
- Stored only on this phone. Scheduling acceptance does not prove audible delivery. A notification response records user interaction, not the actual delivery instant.
- The selected full recording is prepared for the alarm. Use Test an alert to check playback on your iPhone.
- Test an alert
- Test an alert for delivery in one minute, or listen to a recording immediately.
- Test in one minute — lock the phone
- The countdown starts 15 minutes before prayer. If it cannot start, Salati uses a notification reminder when permitted.
- The recording could not play. Try another recording.
- This test uses the chosen prayer’s sound. Your prayer schedule stays unchanged.
- Tomorrow
- Touch and hold the widget, then choose Edit Widget to choose location, date, and sunrise details.
- Touch and hold your Home Screen, then choose Edit and Add Widget.
- Try your settings
- Use prayer sound preference
- Widgets
- Your changes are kept when you choose another location.
- Your choices are saved. You can change them anytime in Settings.
- Your chosen calculation settings are preserved.
- Your prayer calculation
- Your setup

## Independent review and test implementation

Widget agent reviewed the app UX and requested accurate full-recording wording; the copy now says prepared for the alarm and retains an explicit physical-device test action. Lead-owned shared review found and fixed an unanchored scoped-offset regression: a missing anchor must not accidentally activate legacy global offset for Ramadan-dependent Isha while labels remain unadjusted.

An independent lightweight arithmetic check of all supported table anchor/offset combinations found 14,426 reconciled corrections with every adjacent month 29/30 days, and maximum finite reconciliation reach eight months. Fourteen edge-of-table cases cannot reconcile within supported bounds and correctly reject the correction. This complements, but does not replace, the lead's Kotlin test suite.

UI tests now cover the five consecutive onboarding pages in both languages, manual-success and simulated GPS permission/acquiring/success/denial/timeout states; per-prayer detail, scheduled-test and immediate-preview journeys; deterministic five/six-week months, fixed height, ±2, swipe direction and numeric Gregorian correspondence in both languages; existing repeated language changes and Qibla regressions; isolated original/horizontal/night widget renderers and Activity surfaces. Optional system Live Activity capture uses the real system activity on the Island simulator and records unavailability rather than substituting rendered previews. Screenshots still require visual inspection by the lead.
