# Build 5 visual evidence

Application source: `dc22501456934274bc504c63a2c4e50fc125b2a1`, version 1.8, build 5. Runtime, visual review and [independent Release package verification](../../build5-verification.json) are complete.

The collection preserves original PNG bytes, with individual source commit, workflow, device, attachment label and SHA-256 in `index.json`. Exported manifests are retained separately and can also name rejected or superseded captures; only the index identifies the accepted collection. `SHA256SUMS` covers retained PNGs and manifests.

## Provenance

- [39495919171](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39495919171), source `37dc28ec95b5d4c4f5f4e369332af4b822e8a7c8`: app flows, app-rendered previews, and actual English/Arabic compact/expanded Activities. The failed gallery flow is excluded.
- [39502308401](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39502308401), source `6c9c64790542d6898778c8f513c4c98971d9f30c`: compact iPhone 17e flows, all five populated English gallery families, and populated Arabic installed horizontal-large widget. The English installed loading skeleton and Arabic-labeled gallery images that retained English cached content are excluded.
- [39507214129](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39507214129), source `dc22501456934274bc504c63a2c4e50fc125b2a1`: all five populated Arabic gallery layouts and both English/Arabic installed horizontal-large widgets. The English-labeled gallery images retain Arabic cached content and are excluded. Lead inspected original installed captures and all Arabic gallery families; no heading collapse, time truncation or missing countdown remains on these surfaces.

Swift/Kotlin presentation and scheduling code and runtime translations are identical across these sources. The final application additionally sets `UIPrefersShowingLanguageSettings` in its Info.plist; remaining differences are UI tests, workflow selection and documentation. The independent IPA verifier checks that final plist setting.

## Reviewed behavior

- Three-screen English/Arabic setup; city selection, next prayer, optional calculation/alert sheets, and simulated location permission/acquisition/denial/timeout states.
- Home with visible city, six canonical prayer slots, readable accessibility text, and next Fajr substituted after Isha without day-relative labels or a duplicate row.
- Native Settings layout and four alternating OS-language relaunches preserving the city and other preferences. The former custom root recreation is removed.
- English/Arabic calendar, month adjustment, RTL gestures, normal and accessibility sizes; Qibla at a controlled injected heading.
- Actual system compact and expanded Island views. Both active and expired Close tests require disappearance before Salati is foregrounded. App-process intent logs corroborate that the action executes, rather than returning a successful extension no-op.
- Actual WidgetKit archived timer layout, separated from app-rendered previews. Installed-widget readiness requires localized city and prayer text inside the system widget Icon, not merely the existence of its container or a Home Screen button.

## Limits

All images come from iOS 26.4.1 simulators. App-hosted previews, simulated GPS states and injected heading are labeled as such; they do not prove physical sensor accuracy or actual Lock Screen placement. Large accessibility content is scrollable, so a single screenshot does not show every calendar row or prayer at once.

The native Settings screenshots show the simulator's root Settings page. Their attachment names must not be interpreted as proof of exercising the per-app language picker. Bundle-language resolution and preference preservation are tested independently; physical native-picker navigation remains a device check.

WidgetKit can retain gallery previews across launch-argument language fixtures. Gallery content is reviewed by its visible language, not its attachment label. A populated installed timeline is separate evidence. Small widget secondary Hijri text can truncate to retain prayer identity, time and countdown.

With Salati terminated, the system can retain a readable countdown at `0:00`; exact offline automatic dismissal is not guaranteed. Close works without opening the app, and launch/foreground cleanup removes obsolete Activities. Physical iOS 27 signing, lifecycle and notification/audio behavior remain to be checked on the user's iPhone.

See [recovery handoff](../../../IOS_RECOVERY.md) and [validation record](../../build5-validation.json).
