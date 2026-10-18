# Running Salati on iOS

Actual XCTest screenshots from [successful CI run 39080678204](https://github.com/M7ammad-eb/PrayerApp/actions/runs/39080678204), application source `39d9959635bb09e927aaa8d126494335f6ae6510`. iPhone 17 Pro simulator, iOS 26.4.1, Xcode 26.4.1. PNGs are unmodified 1206 × 2622 exports, renamed only for readability. Original xcresult manifest and SHA-256 provenance are saved alongside them.

| Screen | English / light (Damascus) | Arabic / dark (Madinah) |
| --- | --- | --- |
| Prayer | [Home](prayer-en-light.png) | [Home](prayer-ar-dark.png) |
| Hijri calendar | [Calendar](calendar-en-light.png) | [Calendar](calendar-ar-dark.png) |
| Qibla | [Qibla](qibla-en-light.png) | [Qibla](qibla-ar-dark.png) |
| Settings | [Settings](settings-en-light.png) | [Settings](settings-ar-dark.png) |

These show real offline database selections and calculated times. The simulator has no compass; the unavailable-sensor message is intentional. Its status-bar time uses the simulator's region/zone; prayer times use the selected city's zone. Screenshots were visually inspected for contrast, RTL, Western digits and visible native controls. Physical sensors, notification/alarm delivery, widgets, Live Activities and the full accessibility/size matrix remain unverified on an iPhone.
