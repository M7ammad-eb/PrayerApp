# Salati recovery — build 5

Delivered **Salati 1.8, build 5**: `C:/Users/M1993/PrayerApp/build/ios/Salati-Recovery-1.8-build5.ipa`, 39,512,628 bytes. SHA-256: `d962ceb9b6c0b45d9a98fa04269f9e34bd113ad50c58d4bb79ae88d7646bdd43`. Application source: `dc22501456934274bc504c63a2c4e50fc125b2a1`.

[Release 39509731236](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39509731236) passed on that exact source. The [independent package receipt](ios/build5-verification.json) verifies ARM64 iPhoneOS app/widget bundles, identities and version, App Group request metadata, background Close intent metadata, native language-setting visibility, original recordings/database, compiled assets and Arabic resources. AltStore Classic must sign the package with the user's Apple ID. Later delivery-documentation commits do not change the application source.

## Product corrections

The user's installed build 4 exposed a gap in prior validation: app-hosted previews did not reproduce archived WidgetKit timer layout. The horizontal widget lost prayer identity/time because flexible timer text consumed the horizontal proposal. Timers now have explicit widths and text alignment; each horizontal prayer cell has a bounded column. Actual system gallery and installed-widget captures are required for this release.

Setup has three short screens: welcome, location, and ready with the calculated next prayer. Calculation overrides and alerts are optional sheets. Existing calculation choices, alerts, location and completed setup survive upgrades. GPS implementation details, digit-policy explanations, schedule-success logs and calendar reconciliation explanations are removed from normal surfaces. Denied notification and alarm access lead to iOS Settings and refresh on return.

The shared Kotlin presentation policy supplies six canonical prayer slots. After Isha it substitutes the actual upcoming Fajr occurrence into the Fajr slot. Across midnight it retains the actual current Isha occurrence in the Isha slot until Fajr. Native Home and widgets render those IDs without implementing their own overnight selection. Civil schedules and the Maghrib Hijri boundary remain distinct internally. Sunrise behavior is unchanged.

## Language architecture

iOS owns the application language. The Language row uses Apple's app-settings URL; the former in-app binary picker and root-host recreation are removed. `UIPrefersShowingLanguageSettings` keeps the native language choice available even when the phone has only one preferred language. Loading old preferences replaces only the language override with the bundle's resolved localization, preserving other choices. A user who previously chose a language only inside Salati may need to choose it once in iOS Settings.

`AppLanguage` resolves available bundle localizations, determines text direction through Foundation, formats Gregorian prayer times in the chosen language with Western digits, and reads translated prayer/alert labels from the catalogue. Calendar gestures, app root, widgets and Activities use the common direction rule. Adding supported languages does not require adding picker cases or another language preference. New language resources still need translations, including localized place/recording names and calendar content; this release ships English and Arabic only.

## Live Activity lifecycle

The Activity's prayer name/time, bounded countdown and 44-point Close control share the expanded main row. City is subordinate, and progress stays inside the lower curved edge. Compact and expanded forms constrain timer width. Prayer identity and scheduled time remain nonempty even if iOS retains an archived countdown at zero. A fresh stale-state render shows the end message; timely re-rendering with the application terminated is not guaranteed.

Lifecycle cleanup is independent of calculation, storage and alert replenishment. It runs before refresh on launch/foreground, observes Activity state changes and arms an expiry timer while the app can execute. End calls provide complete final content and immediate dismissal. Legacy records without usable event identity are retired on cleanup.

Close is a LiveActivityIntent intended to execute by the app process without opening the UI. It bypasses alert reconciliation waits, ends all copies of the chosen event and persists dismissal until that prayer instant. Submission rechecks suppression after asynchronous retention work. A system dismissal uses captured identity even if ActivityKit has already removed the record; app-initiated replacement endings do not suppress the replacement. Runtime logs identify unexpected extension routing rather than silently returning success.

An offline, suspended or terminated app cannot promise execution at an exact future deadline. `staleDate` marks stale content; it does not schedule `end()`. A future `timestamp` orders content updates; it is not a scheduled end. This implementation does not claim guaranteed automatic dismissal while the process cannot run. Nonempty stale content, Close, and immediate launch/foreground recovery address that execution boundary. Physical iOS 27 behavior remains a separate device check.

## Review and validation

Lead owns shared policy, language/storage integration, lifecycle, tests and delivery. Two bounded agents owned app UI and widget/Activity presentation, followed by independent cross-review. Review resolved two dismissal races, alarm-denial recovery, missing legacy event identity and binary direction/formatting assumptions. Only one resource-intensive build/test job runs at a time; iOS CI waits for shared/Android validation.

The first recovery run (39488536765) passed 25 shared JVM tests, 99 Android tests and 45 app tests. Two UI selectors failed (city toolbar button and widget gallery cell). Visual inspection also invalidated the initial Close test's success claim: its one-second capture still showed the Activity, and foreground cleanup could mask failed or delayed interaction. The replacement test requires the actual system Activity to disappear before reopening Salati.

Run 39495919171, source `37dc28ec95b5d4c4f5f4e369332af4b822e8a7c8`, passed 23 native shared tests, 46 app tests and eight of nine main UI flows. Actual system Close succeeded for both active and expired Activities before foregrounding Salati; app-process intent logs corroborate execution. English/Arabic expanded captures keep the progress inside the curved edge. Four alternating OS language launches preserve Damascus and verify mirrored tab order. The native Settings screenshot shows the simulator's root Settings screen, so it is not proof of the per-app language picker interaction.

That run's remaining UI failure was a gallery interaction: the exact Add Widget selector missed the visible control because its accessibility label has a leading space. The selector now tolerates that whitespace and has an observed button-position fallback. Installation also requires actual content: the Home Screen's Done control exists beneath an open gallery, so that selector alone is insufficient proof.

Run 39502308401, source `6c9c64790542d6898778c8f513c4c98971d9f30c`, passed the gallery flow and all three iPhone 17e flows. Review accepted five populated English gallery families and the Arabic installed widget; it rejected the English installed loading skeleton and cached English previews mislabeled by the test as Arabic. Final focused run 39507214129 on the delivered application source passes a stronger check requiring localized city and prayer text within the installed widget Icon. Its five Arabic gallery layouts and both English/Arabic installed horizontal-large widgets are populated and visually reviewed. The total accepted coverage is **193 unit tests and 12 distinct UI flows**; [machine-readable validation record](ios/build5-validation.json) separates each run's source and accepted checks.

[150 original PNGs](ios/screenshots/build5/README.md) retain individual source/workflow/hash provenance. Lead and independent reviewers inspected the actual system surfaces as well as app and compact-phone captures. The only production change after the 46-app-test run is the native language-visibility plist setting, which is included in final simulator compilation and independently checked in the Release IPA. Swift, Kotlin and runtime translations are unchanged.

## Installation and device checks

Import the build 5 IPA into AltStore Classic and retain the SalatiWidgets extension. Open Salati once after updating so its schedule and obsolete Activities are refreshed. Existing bundle identities and user preferences are preserved. The package requests the App Group through ad-hoc metadata; AltStore must replace it with the user's Apple-ID signing.

Physical iOS 27 was not available to the agent. Verify the native Settings language picker, normal widget refresh across prayer boundaries, and real alarm/audio delivery on the phone. Simulator captures prove system layout and the tested Close interaction, not a guarantee of all physical-device background behavior. No exact offline terminated-app automatic Activity end is claimed.

## Primary references

- [Apple: timer text layout](https://developer.apple.com/documentation/swiftui/text/init(timerinterval:pausetime:countsdown:showshours:)) — flexible archived timer layout needs a width and text alignment.
- [Apple: onboarding](https://developer.apple.com/design/human-interface-guidelines/onboarding), [writing](https://developer.apple.com/design/human-interface-guidelines/writing), [widgets](https://developer.apple.com/design/human-interface-guidelines/widgets).
- [Apple: localization](https://developer.apple.com/localization/), [bundle language selection](https://developer.apple.com/library/archive/qa/qa1828/_index.html).
- [Apple: multilingual-ready apps](https://developer.apple.com/videos/play/wwdc2024/10185/) — the app-settings URL and always-visible native language preference.
- [Apple: ending an Activity](https://developer.apple.com/documentation/activitykit/activity/end(_:dismissalpolicy:)), [stale state](https://developer.apple.com/documentation/activitykit/activitystate/stale), [interactive widgets and Live Activities](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities).
