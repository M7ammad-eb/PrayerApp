# Build 4 visual evidence

Delivered application source: `725a63f382e06e04c6de750f2c011c83b04c8f0b`, version 1.8, build 4.

The final collection combines the complete app/compact-device validation at `029b8ddd5ee2a51e00c1af9e0aef19d2f1078868` ([39310536112](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39310536112)) with the focused widget/Activity validation at the delivered source ([39318100255](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39318100255)). The only production difference is expanded Dynamic Island content margins. App, shared, Android and test sources are identical. Each PNG has its own source commit, workflow, label, device and SHA-256 in `index.json`; original PNG bytes are preserved.

The earlier full run's 38 main-device widget/Activity captures are replaced by the focused captures. Its 84 app and 10 compact-device widget captures are retained. Original exported manifests are retained separately for audit; the full manifest consequently also names superseded captures that are not in this final collection. `SHA256SUMS` covers the retained PNGs and original manifests.

## Inspected coverage

- English/light and Arabic/dark five-page onboarding, manual location selection, simulated GPS permission/acquisition/success/denial/timeout, calculation summary and completion.
- Home, location and normal calculation settings, language changes in both directions, relaunch, sheets and Qibla at an injected physical heading.
- Per-prayer alert choices, accurate default-system-alarm sound explanation, scheduled-test versus immediate audio-preview navigation, permissions and diagnostics.
- Unadjusted and ±2 calendars, actual five/six-week grids, unchanged reserved height, numeric Western day/month in both languages, arrows/swipes and large text on both sizes.
- Preserved original widget and new horizontal medium/large variant; actual tomorrow Fajr after Isha and previous-day Isha after midnight; compact-width Arabic.
- Separately labeled app-rendered compact, expanded, minimal and Lock Screen Activity previews, plus four actual simulator SpringBoard compact/expanded captures in English and Arabic.

Lead and independent reviewers inspected the full capture set and original-resolution images for findings. Visual review corrected a truncated large-text adjustment heading, Arabic active-adjustment label, misleading default-alarm sound footer, horizontal-large spacing and actual expanded-Island corner clipping. The final independent review inspected all 38 focused captures and the four system originals: no clipping or visual regression found, and compact prayer name/time is preserved in both languages.

## Limits

All evidence is from iOS 26.4.1 simulators: iPhone 17 Pro Max and iPhone 17e. App-rendered widget/Lock Screen previews are not actual WidgetKit/Lock Screen-host screenshots. Only the four labeled SpringBoard captures establish actual compact/expanded Island geometry. Simulated location and injected heading establish UI behavior, not sensor accuracy. One Arabic +2 calendar capture catches its digit transition immediately after adjustment; settled adjusted captures and numeric assertions pass.

Physical AltStore signing, App Group access, installed widget refresh, automatic Activity scheduling/ending, Lock Screen behavior, alarm/audio delivery, GPS/heading and accessibility interaction still require the user's iPhone. See [milestone handoff](../../../IOS_REFINEMENT.md) for the concise checklist and final package receipt.
