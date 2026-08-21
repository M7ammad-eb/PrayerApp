# Salati — Prayer Times & Qibla

Salati (صلاتي) is a fully offline Android app for accurate prayer times, Qibla direction, and Islamic calendar features. All astronomical calculations, place lookup, and Qibla bearing run entirely on-device — the app requests no `INTERNET` permission and makes no network calls.

## Features

- **Offline prayer time engine** — Jean Meeus astronomical algorithms compute Fajr, Dhuhr, Asr, Maghrib, and Isha with no network dependency, supporting 14 calculation methods (Umm Al-Qura, MWL, ISNA, Egyptian, Karachi, Gulf, Qatar, Kuwait, Turkey/Diyanet, Tehran, Shia Ithna Ashari, Singapore, France/UOIF, Russia) plus Hanafi/Standard Asr juristic rules and a configurable high-latitude rule.
- **Live Qibla compass** — sensor-fused heading (rotation vector, with an accelerometer + magnetometer fallback), real-time distance to the Kaaba, and calibration guidance.
- **Offline place database** — ~69,000 places sourced from [GeoNames](https://www.geonames.org/) power GPS reverse-lookup and city search, replacing Android's `Geocoder` entirely so location naming works with no connectivity. GPS fixes are labeled by proximity (same city / near / nearest match) alongside the exact coordinates used for calculation.
- **Notifications & alarms** — exact-alarm-scheduled athan playback with a full-screen alarm UI, a Live Athan Countdown notification, per-prayer sound selection across multiple reciters, and configurable audio output channel (alarm/media/ringtone stream).
- **Home screen widgets** — fully customizable size, theme, background style, opacity, font size, and content (countdown, progress bar, full schedule, Hijri date, location).
- **Hijri calendar** — Umm Al-Qura standard calendar with White Days and Sacred Months indicators, plus a manual day-offset adjustment.
- **Full Arabic & English localization** — complete RTL layout support, with every user-facing string resource-driven (no hardcoded per-language strings) so new languages only require a new `values-*` folder.
- **Material You theming** — dynamic system-color extraction plus curated color presets, light/dark/system theme modes.
- **First-run setup wizard** — guided onboarding for language, location, calculation method (pre-suggested from location), notifications, athan sounds, and appearance.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Persistence:** Room (offline place database), Jetpack DataStore (preferences)
- **Concurrency:** Kotlin Coroutines & Flow
- **Testing:** JUnit, Robolectric, Roborazzi (screenshot testing)
- **Build:** Gradle (Kotlin DSL), KSP

## Project Structure

```
app/src/main/java/com/example/
├── data/            # Models, preferences, calculation engine, offline place DB, Qibla sensor logic
├── ui/              # Compose screens, components, theming, and localization (AppStrings)
├── notifications/   # Athan alarms, full-screen alarm UI, Live Athan Countdown
├── widget/          # Home screen widget provider and customization
├── audio/           # Athan playback
└── util/            # Shared utilities

app/src/main/res/
├── values/          # English strings
└── values-ar/       # Arabic strings

tools/geonames/      # Offline place-database build script (regenerates app/src/main/assets/places.db)
```

## Building

1. Clone the repository and open it in Android Studio (or build via CLI with the Gradle wrapper).
2. **Debug signing:** `debug.keystore` is intentionally excluded from version control. Generate one before building a debug variant:
   ```bash
   keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```
   Place it at the repository root.
3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

Release builds sign via `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables — no release keystore is committed either.

## Data & Attribution

City and place names are provided by [GeoNames.org](https://www.geonames.org/), licensed under [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/). This attribution is also surfaced in-app under Settings → About.

## Credits

This app was built with the assistance of AI development tools:

- **[Google AI Studio](https://aistudio.google.com/)** — initial project scaffold and prototyping.
- **[Claude](https://claude.com/) (Anthropic)** — iterative feature development, bug fixing, code review, and architecture work, including the offline GeoNames place database, the Qibla compass sensor pipeline, and the full Arabic/English localization migration.
- **[ChatGPT](https://chatgpt.com/) (OpenAI)** — additional development and debugging assistance.
