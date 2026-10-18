# Phase 2: native scheduling and audio

Implementation notes for the lead integration. Runtime CI and physical-device results must be recorded in `IOS_PROGRESS.md`; this document does not claim that a source change alone proves audible delivery.

## Shared boundary and rolling reconciliation

The lead's shared Kotlin plan supplies stable event IDs, actual prayer instants, independently chosen reminder instants and canonical 15-minute Live Activity starts. Swift receives `ScheduledAlert` and `ScheduledLivePrayer` values; it does not calculate prayer/date boundaries or manufacture a second prayer-time algorithm.

`PrayerScheduler` serializes production refreshes, test scheduling and test cancellation through `SerialSchedulingQueue`. A waiting obsolete production revision is skipped. Work already submitted to an iOS API is reconciled by the next revision rather than interrupted by blanket deletion. The private `schedule.ledger.v2` stores accepted requests. Routine refreshes retain matching future requests and submit only missing/changed generations.

Notifications reuse the stable shared ID: UserNotifications can replace that pending request without a cancellation gap. AlarmKit UUIDs are deterministic hashes of the complete native request. A changed alarm generation is submitted first; only success permits cancelling the previous generation. Failed replacements retain previous authorized coverage and report the risk that an old setting/time remains. Cancellation failures remain visible and are retried by subsequent reconciliation. A failure reading pending state leaves the ledger and system schedule untouched.

Replenishment must not cancel an alarm that is already alerting. The adapter inspects the public `Alarm.state` and preserves it until the person's Stop action/system lifecycle finishes it. This is covered by a focused regression test because replenishment at a prayer boundary otherwise could cut off the audio it just scheduled.

Phase 1 used random AlarmKit IDs and notification IDs with a trailing UUID, with no persisted alarm ledger. The public AlarmManager query returns alarms belonging to this app. Migration removes a legacy fixed-date alarm after a replacement for the same instant succeeds, and removes other obsolete fixed-date Salati alarms only after the complete replacement plan succeeds. A partial failure preserves unreplaced legacy coverage. It does not delete arbitrary system Clock alarms, timers with no fixed schedule, or separate test requests. Tests explicitly cover partial migration and recovery.

The notification adapter reserves a conservative budget of 60 future production requests minus non-Salati/test requests, leaving space for a test and a silent horizon warning. It does not apply that budget to AlarmKit or ActivityKit. The legacy Apple local-notification documentation documents a 64-entry limit; the current adapter remains conservative and surfaces actual API errors. AlarmKit documents `maximumLimitReached` without a numeric promise, and ActivityKit's scheduled plus active capacity depends on the device/system. No invented universal cap is used for those APIs.

The status horizon is the earliest retained/submitted coverage boundary across delivery APIs, capped by an encountered failed request, rather than the longest alarm date. A silent reminder asks the user to open Salati before that horizon. Foreground/relevant accepted setting or location changes, significant time changes, granted BG refresh, authorization changes, and actual alarm Stop intent execution provide independent replenishment opportunities. An alarm merely sounding does not promise app execution; the process-local alarm observer only helps when iOS is already executing the app.

## Sound roles and what is known

* AlarmKit requests the unchanged original `<recording>.mp3` from the main bundle through `AlertConfiguration.AlertSound.named`. There is no generated 25-second excerpt in this route, no post-alert countdown/snooze, and no arbitrary cut point. Apple documents the named main-bundle/Library-Sounds path. The consulted AlarmKit/ActivityKit sound documentation does not state the UserNotifications 30-second cap. This establishes the supported asset request, **not proof that iOS 27.0.1 will play the entire recording once, uninterrupted, while locked**. That still requires the user's device.
* Ordinary prayer notifications use the system sound or silence. Previously selected full-recording IDs survive upgrade and remain available for the explicit full-playback action, but are not misleadingly submitted as short notification sounds. Approved short recordings/cut points are pending from the user.
* Advance reminder notifications use the system sound or silence according to the separate reminder preference. They never use the adhan recording. ActivityKit requires a nonoptional alert sound for a scheduled start; its silent reminder choice uses a dedicated 0.1-second zero-PCM CAF system-alert asset. This is a single alert, never an AVFoundation loop or background-execution workaround.

`short_sounds.py` only removes obsolete generated `_short.caf`/`_decoded.caf` files and prepares the dedicated silent ActivityKit sound. It never edits the eleven original MP3 files.

The original app explicitly stopped AVFoundation when the alert settings page disappeared, and declared no background audio capability. That could explain interruption on navigation/suspension, but does not establish the cause of a particular disappearing-banner report. Phase 2 removes the navigation stop, keeps the legitimate user-initiated recording playing across navigation, uses the public playback audio session, records interruptions/route changes/media resets, and resumes after a call only when iOS supplies `shouldResume`. Headphone removal stops playback to avoid an unexpected speaker transition. The lead adds the legitimate audio background mode for this audible user-initiated playback. The app never starts a silent loop, pretends that notification delivery launches AVFoundation, or restarts audio merely because a banner vanished.

## Automatic Live Activities

The scheduler uses the public local scheduled-start ActivityKit API with the shared canonical prayer-minus-15-minutes start. Several future activities are attempted in chronological order while the app is in the foreground; the system starts accepted requests even when the app is subsequently backgrounded. Existing accepted matching requests are retained, including language, city and clock-format fields. An update submits the replacement before ending the previous future generation. Permission revocation ends app-visible activities and requests a fresh reconciliation.

The first reported ActivityKit global/app capacity error or authorization/entitlement/visibility restriction stops further new submissions in that reconciliation. The remaining scan still retains matching accepted activities and produces the conventional reminder fallback for unscheduled events. Payload-specific errors do not automatically block unrelated requests. This avoids dozens of futile submissions on each refresh without inventing a numeric ActivityKit cap. If replacement of an outdated activity fails, its previous presentation is ended after the corrected fallback reminder is accepted. If both paths fail (or the reminder start has already passed), prior coverage is retained with an explicit outdated time/location warning and local diagnostic entry, rather than silently presenting it as correct.

Conventional advance notifications are suppressed only for a successfully accepted activity at the same chosen reminder instant. If Live Activities are disabled/unavailable or a future request fails, an ordinary reminder at that same 15-minute start is prepared with its own stable fallback identity. Other explicitly chosen reminder offsets remain independent. Actual prayer-time alarm/notification choices are unchanged.

**Exact offline dismissal limitation:** `staleDate` marks content outdated; it is not an end/dismiss command. The consulted public APIs offer immediate `end` while the app executes and a dismissal policy for an already ended activity, not a local future-end schedule. The implementation immediately ends expired activities on every execution/reconciliation and uses a cancellable in-process expiry task as additional assistance only while executing. The widget receives the prayer-time stale date and shows the localized ended label when the system marks it stale. No server/push dependency or fictional guaranteed background timer is introduced. An expired Lock Screen activity can remain until the app next executes or the person/system removes it. Apple may keep a standard activity active for up to eight hours, then on the Lock Screen for up to four more; those are system ceilings, not a promised prayer-time dismissal.

The automatic preference defaults off for an existing install, preserving prior audible choices. The user can opt in on the existing per-prayer alert settings page. Lack of Dynamic Island hardware does not disable Lock Screen Live Activities.

## Local diagnostics and real tests

The private local journal keeps at most 160 entries: stable event ID, calculated prayer UTC instant, submitted UTC instant, API result/failure, observed foreground notification callback/user response, AlarmKit alerting state while executing, Stop intent execution, and AVFoundation interruptions. It stores no GPS coordinates or Apple ID credentials and has no upload. Sharing requires the person to choose Share in the local diagnostics page.

An observed foreground delegate callback is labeled as such. A response/Stop timestamp records interaction, never a fabricated delivery/audible-start timestamp. A missing one-shot alarm in a later system query cannot distinguish every unobserved disappearance cause, so no inferred sound-delivery time is generated.

The settings page schedules an actual notification or AlarmKit alarm about one minute ahead using the same native backend, with separate `salati.test.*` identities and the chosen prayer's sound preference. This is distinct from “Play full adhan now.” Test cancellation affects only persisted test entries. Production refresh does not erase the test, and tests do not erase production schedules.

`SchedulingTests.swift` adds 18 focused cases for idempotence/relaunch, failed replacement retention, submission-before-cancellation order, production/test isolation, legacy alarm migration, legacy notification migration, pending-query failure, atomic notification replacement, distinct sound roles, API-specific horizon, notification permission revocation, cancellation failure/recovery, serialization across suspension, superseded waiting revisions, preservation of already-alerting audio, independent migration when a different delivery API fails, and bounded ActivityKit submission attempts while retaining later existing activities. Their actual run result belongs in the lead's CI evidence; no device audio was played from this Windows environment.

## Physical iPhone checklist

1. Verify the new build number and location/timezone/manual adjustments. Compare Maghrib's displayed local time with local diagnostics' prayer UTC instant and submitted UTC instant. Preserve the journal if there is another late alert.
2. On the existing alerts page choose a real AlarmKit test and an original full recording, schedule one minute ahead, lock the phone, and note audible start/end/repetition. Repeat with the app backgrounded and terminated, and with Focus/silent mode on. Cancel test identities afterward. Do not infer full playback from the mere existence of a scheduled alarm.
3. Repeat the same controlled test once without a FaceTime call and once with one. Record call state and observed times. A difference is evidence to investigate, not proof of a particular OS cause.
4. Test an ordinary notification and an advance reminder. They should use the system sound/silence, never the full adhan. An explicit full-playback action should continue across navigation and screen lock; check Stop, headphones removed, and a real call interruption.
5. Enable automatic countdowns, open Salati to prepare them, then lock the phone before the 15-minute start. Check automatic appearance, name/time compact regions and Lock Screen display. At prayer time distinguish the stale/ended label from actual dismissal. Reopen Salati to confirm immediate expired-activity cleanup. Disable/re-enable Live Activities in iOS Settings and confirm conventional reminder fallback.
6. Change a calculation adjustment and inspect that future requests reconcile without duplicates; deliberately denying a permission must show a useful failure rather than “prepared.” Open Salati regularly before the displayed horizon expires.

## Official API references checked

* [Apple: Scheduling an alarm with AlarmKit](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit)
* [Apple: named alert sounds](https://developer.apple.com/documentation/activitykit/alertconfiguration/alertsound/named(_:))
* [Apple: UserNotifications sound formats and under-30-second rule](https://developer.apple.com/documentation/usernotifications/unnotificationsound)
* [Apple: AlarmManager alarms belong to the current client](https://developer.apple.com/documentation/alarmkit/alarmmanager/alarms)
* [Apple: fixed alarm schedule](https://developer.apple.com/documentation/alarmkit/alarm/schedule-swift.enum/fixed(_:))
* [Apple: AlarmKit capacity error](https://developer.apple.com/documentation/alarmkit/alarmmanager/alarmerror/maximumlimitreached)
* [Apple: legacy documented local-notification capacity](https://developer.apple.com/documentation/uikit/uilocalnotification)
* [Apple: scheduled Activity request and foreground requirement](https://developer.apple.com/documentation/activitykit/activity/request(attributes:content:pushtype:style:alertconfiguration:start:))
* [Apple: Live Activity lifecycle/capacity constraints](https://developer.apple.com/documentation/activitykit/displaying-live-data-with-live-activities)
* [Apple: ending an active Live Activity](https://developer.apple.com/documentation/activitykit/activity/end(_:dismissalpolicy:))
* [Apple: mandatory scheduled alert sound parameter](https://developer.apple.com/documentation/activitykit/alertconfiguration/init(title:body:sound:))
* [Apple: ActivityKit capacity, authorization and payload error cases](https://developer.apple.com/documentation/activitykit/activityauthorizationerror)
* [Apple: AlarmKit stopIntent factory](https://developer.apple.com/documentation/alarmkit/alarmmanager/alarmconfiguration/alarm(schedule:attributes:stopintent:secondaryintent:sound:))
