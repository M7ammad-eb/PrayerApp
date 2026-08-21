package com.prayertimes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.prayertimes.MainActivity
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.PrayerTimeItem
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.WidgetBackgroundStyle
import com.prayertimes.data.models.WidgetHeroTimeMode
import com.prayertimes.data.models.WidgetTextStyle
import com.prayertimes.data.models.WidgetThemeMode
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.util.LocalizedStrings
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetColorScheme(
    val rootBgColor: Int,
    val rootBorderColor: Int,
    val heroBgColor: Int,
    val heroStrokeColor: Int,
    val accentColor: Int,
    val textPrimaryColor: Int,
    val textSecondaryColor: Int,
    val textOnAccentColor: Int,
    val activePrayerBgColor: Int,
    val inactivePrayerBgColor: Int,
    val countdownBgColor: Int,
    val fontScale: Float
)

class PrayerAppWidgetProvider : AppWidgetProvider() {

    companion object {
        // Namespaced under the real applicationId rather than the generic "com.salati.prayertimes" package
        // this project started from - a widely-reused placeholder namespace risks colliding with
        // another app built from the same template that also broadcasts/listens on it, since this
        // receiver is necessarily exported (required for the OS to deliver widget updates).
        const val ACTION_REFRESH_WIDGET = "com.salati.prayertimes.widget.ACTION_REFRESH_WIDGET"
        private const val REQUEST_CODE_REFRESH = 4001
        private const val REQUEST_CODE_ALARM_UPDATE = 4002

        // Previously both broadcast ACTION_APPWIDGET_UPDATE (which the system dispatches back to
        // onUpdate()) AND called onUpdate() directly on a manually-built instance - one logical
        // refresh request did the DataStore read, calculation, and RemoteViews work twice. Calling
        // the actual update work directly, once, on the app's structured scope is the single path
        // now; private members of the enclosing class are visible here since this is its companion.
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PrayerAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                PrayerApplication.instance.applicationScope.launch {
                    try {
                        PrayerAppWidgetProvider().updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun resolveWidgetColors(context: Context, settings: AppPrayerSettings): WidgetColorScheme {
            val wSet = settings.widgetSettings
            val opacityFrac = (wSet.opacityPercent / 100f).coerceIn(0f, 1f)

            data class ColorPalette(val primaryAccent: Int, val bgCardColor: Int, val textPrimary: Int, val textSecondary: Int)

            // Reads the device's real Android 12+ Material You dynamic color scheme instead of a
            // hardcoded guess, so this theme actually tracks the system wallpaper colors like it
            // claims to. Uses the same dynamicDarkColorScheme() derivation as MyApplicationTheme
            // (Theme.kt) and this file's own "Match App Theme" resolver, rather than hand-picked
            // raw tonal-palette tokens - other apps that properly implement Material You dynamic
            // theming go through this same official derivation, so this is what actually lines
            // up with them. Returns null (falls back to the hardcoded palette below) on API < 31
            // or if dynamic color isn't available for some reason.
            fun resolveSystemDynamicPalette(): ColorPalette? {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
                return try {
                    // Was hardcoded to dynamicDarkColorScheme() regardless of the device's actual
                    // light/dark setting, so this stayed dark even with the system in light mode.
                    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                    val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    // Measured against the OS's own themed widgets (Calendar, Weather) on a real
                    // device: none of the flat surfaceContainer* tonal roles matched their
                    // background - those roles are near-neutral (R/G/B within ~8 of each other),
                    // but the real widgets show a visibly more saturated, primary-tinted tone.
                    // surfaceColorAtElevation(8.dp) (surface + primary blended in at an
                    // elevation-scaled alpha - Material3's actual elevation-overlay formula)
                    // reproduced their measured color within 1-3 units per channel.
                    ColorPalette(
                        primaryAccent = scheme.primary.toArgb(),
                        bgCardColor = scheme.surfaceColorAtElevation(8.dp).toArgb(),
                        textPrimary = scheme.onSurface.toArgb(),
                        textSecondary = scheme.onSurfaceVariant.toArgb()
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // "Match App Theme" mirrors MyApplicationTheme's own resolution logic
            // (Theme.kt: getPresetColorScheme + the dynamic-color branch) instead of a fixed
            // dark-green palette, so it actually tracks the app's current theme/color preset -
            // including switching with light/dark mode and the user's chosen accent color.
            fun resolveAppThemePalette(): ColorPalette {
                val isDark = when (settings.themeMode) {
                    AppThemeMode.SYSTEM ->
                        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                }

                val useDynamic = settings.followSystemColors &&
                    settings.colorPreset == AppColorPreset.SYSTEM_DYNAMIC &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                if (useDynamic) {
                    try {
                        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                        return ColorPalette(
                            primaryAccent = scheme.primary.toArgb(),
                            bgCardColor = scheme.surfaceColorAtElevation(8.dp).toArgb(),
                            textPrimary = scheme.onSurface.toArgb(),
                            textSecondary = scheme.onSurfaceVariant.toArgb()
                        )
                    } catch (e: Exception) {
                        // fall through to the preset palette below
                    }
                }

                val preset = if (settings.colorPreset == AppColorPreset.SYSTEM_DYNAMIC) {
                    AppColorPreset.EMERALD_GOLD
                } else {
                    settings.colorPreset
                }
                // Reuse the app's own getPresetColorScheme() (Theme.kt) rather than a
                // hand-picked bg guess, so surfaceContainerHigh comes from Compose Material3's
                // real tonal computation - the same one MyApplicationTheme itself renders with.
                val scheme = com.prayertimes.ui.theme.getPresetColorScheme(preset, isDark)
                return if (isDark) {
                    ColorPalette(
                        primaryAccent = preset.primaryDark.toInt(),
                        bgCardColor = scheme.surfaceColorAtElevation(8.dp).toArgb(),
                        textPrimary = scheme.onSurface.toArgb(),
                        textSecondary = scheme.onSurfaceVariant.toArgb()
                    )
                } else {
                    ColorPalette(
                        primaryAccent = preset.primaryLight.toInt(),
                        bgCardColor = scheme.surfaceColorAtElevation(8.dp).toArgb(),
                        textPrimary = scheme.onSurface.toArgb(),
                        textSecondary = scheme.onSurfaceVariant.toArgb()
                    )
                }
            }

            val palette = when (wSet.themeMode) {
                WidgetThemeMode.APP_THEME -> resolveAppThemePalette()
                WidgetThemeMode.MATERIAL_YOU -> resolveSystemDynamicPalette() ?: ColorPalette(
                    primaryAccent = 0xFF3F51B5.toInt(),
                    bgCardColor = 0xFF1C1B1F.toInt(),
                    textPrimary = 0xFFE6E1E5.toInt(),
                    textSecondary = 0xFFCAC4D0.toInt()
                )
                WidgetThemeMode.DARK_ELEGANT -> ColorPalette(
                    primaryAccent = 0xFF10B981.toInt(),
                    bgCardColor = 0xFF121820.toInt(),
                    textPrimary = 0xFFF1F5F9.toInt(),
                    textSecondary = 0xFF94A3B8.toInt()
                )
                WidgetThemeMode.LIGHT_CLEAN -> ColorPalette(
                    primaryAccent = 0xFF059669.toInt(),
                    bgCardColor = 0xFFFFFFFF.toInt(),
                    textPrimary = 0xFF0F172A.toInt(),
                    textSecondary = 0xFF64748B.toInt()
                )
                WidgetThemeMode.OLED_BLACK -> ColorPalette(
                    primaryAccent = 0xFF34D399.toInt(),
                    bgCardColor = 0xFF000000.toInt(),
                    textPrimary = 0xFFFFFFFF.toInt(),
                    textSecondary = 0xFFA1A1AA.toInt()
                )
                WidgetThemeMode.EMERALD_ISLAMIC -> ColorPalette(
                    primaryAccent = 0xFFF59E0B.toInt(),
                    bgCardColor = 0xFF064E3B.toInt(),
                    textPrimary = 0xFFECFDF5.toInt(),
                    textSecondary = 0xFFA7F3D0.toInt()
                )
                WidgetThemeMode.GOLDEN_HOUR -> ColorPalette(
                    primaryAccent = 0xFFF59E0B.toInt(),
                    bgCardColor = 0xFF451A03.toInt(),
                    textPrimary = 0xFFFEF3C7.toInt(),
                    textSecondary = 0xFFFDE68A.toInt()
                )
                WidgetThemeMode.ROYAL_BLUE -> ColorPalette(
                    primaryAccent = 0xFF38BDF8.toInt(),
                    bgCardColor = 0xFF0F172A.toInt(),
                    textPrimary = 0xFFF0F9FF.toInt(),
                    textSecondary = 0xFFBAE6FD.toInt()
                )
                WidgetThemeMode.MONOCHROME -> ColorPalette(
                    primaryAccent = 0xFFFAFAFA.toInt(),
                    bgCardColor = 0xFF18181B.toInt(),
                    textPrimary = 0xFFFAFAFA.toInt(),
                    textSecondary = 0xFFA1A1AA.toInt()
                )
            }.let { p ->
                // Manual light/dark text override - a theme's own text colors can't be relied
                // on for contrast when the background is fully or mostly transparent, since
                // there's no way to know what's actually behind the widget (see textStyle docs).
                when (wSet.textStyle) {
                    WidgetTextStyle.AUTO -> p
                    WidgetTextStyle.LIGHT -> p.copy(textPrimary = 0xFFFFFFFF.toInt(), textSecondary = 0xFFE2E8F0.toInt())
                    WidgetTextStyle.DARK -> p.copy(textPrimary = 0xFF0F172A.toInt(), textSecondary = 0xFF334155.toInt())
                }
            }

            // Each style now renders distinctly instead of TRANSLUCENT/SOLID_SURFACE/FROSTED_GLASS
            // all falling through to the same look:
            //  - SOLID_SURFACE ignores the opacity slider and stays fully opaque ("highest
            //    readability" per its own description).
            //  - FROSTED_GLASS blends the card color toward white and floors the opacity so it
            //    reads as a soft frosted panel even at a low slider value (RemoteViews can't do
            //    a real Gaussian blur of the wallpaper, so this is the closest approximation).
            //  - MINIMAL_BORDER pairs a near-invisible interior with a strong stroke, rendered
            //    through shape_widget_root_border.xml's real outline (transparent fill + stroke)
            //    rather than a solid wash.
            val finalRootBg = when (wSet.bgStyle) {
                WidgetBackgroundStyle.TRANSPARENT_CLEAN -> Color.TRANSPARENT
                WidgetBackgroundStyle.MINIMAL_BORDER -> ColorUtils.setAlphaComponent(0xFF000000.toInt(), (0.15f * opacityFrac * 255).toInt())
                WidgetBackgroundStyle.SOLID_SURFACE -> ColorUtils.setAlphaComponent(palette.bgCardColor, 255)
                WidgetBackgroundStyle.FROSTED_GLASS -> {
                    val frosted = ColorUtils.blendARGB(palette.bgCardColor, Color.WHITE, 0.25f)
                    ColorUtils.setAlphaComponent(frosted, (opacityFrac.coerceAtLeast(0.55f) * 255).toInt())
                }
                WidgetBackgroundStyle.TRANSLUCENT -> ColorUtils.setAlphaComponent(palette.bgCardColor, (opacityFrac * 255).toInt())
            }

            val rootBorder = when (wSet.bgStyle) {
                WidgetBackgroundStyle.TRANSPARENT_CLEAN -> Color.TRANSPARENT
                WidgetBackgroundStyle.MINIMAL_BORDER -> ColorUtils.setAlphaComponent(palette.primaryAccent, (0.80f * 255).toInt())
                WidgetBackgroundStyle.SOLID_SURFACE -> ColorUtils.setAlphaComponent(palette.primaryAccent, (0.35f * 255).toInt())
                WidgetBackgroundStyle.FROSTED_GLASS -> ColorUtils.setAlphaComponent(Color.WHITE, (0.20f * 255).toInt())
                WidgetBackgroundStyle.TRANSLUCENT -> ColorUtils.setAlphaComponent(palette.primaryAccent, (0.25f * 255).toInt())
            }

            // Alpha bumped above the "looks fine on a dark mockup" range: a real home screen
            // wallpaper is often bright/busy, and a faint white wash barely registers against it
            // (the same wash reads much stronger against the Settings preview's dark backdrop).
            val heroBg = when (wSet.bgStyle) {
                WidgetBackgroundStyle.TRANSPARENT_CLEAN -> ColorUtils.setAlphaComponent(palette.textPrimary, (0.12f * 255).toInt())
                else -> ColorUtils.setAlphaComponent(palette.textPrimary, (0.16f * 255).toInt())
            }

            val heroStroke = ColorUtils.setAlphaComponent(palette.primaryAccent, (0.15f * 255).toInt())

            val activePrayerBg = palette.primaryAccent
            val inactivePrayerBg = ColorUtils.setAlphaComponent(palette.textPrimary, (0.14f * 255).toInt())
            val countdownBg = palette.primaryAccent

            // Threshold lowered from 0.60: Google's own widgets (e.g. Calendar event chips)
            // default to dark text on their colored badges far more readily than a strict
            // "only if very light" rule would - most of our accent colors are bright/saturated
            // enough to read fine with dark text, and it reads as more consistent with that
            // convention than defaulting to white so often.
            val textOnAccent = if (ColorUtils.calculateLuminance(palette.primaryAccent) > 0.42) {
                0xFF0F172A.toInt()
            } else {
                Color.WHITE
            }

            return WidgetColorScheme(
                rootBgColor = finalRootBg,
                rootBorderColor = rootBorder,
                heroBgColor = heroBg,
                heroStrokeColor = heroStroke,
                accentColor = palette.primaryAccent,
                textPrimaryColor = palette.textPrimary,
                textSecondaryColor = palette.textSecondary,
                textOnAccentColor = textOnAccent,
                activePrayerBgColor = activePrayerBg,
                inactivePrayerBgColor = inactivePrayerBg,
                countdownBgColor = countdownBg,
                fontScale = wSet.fontSize.scaleFactor
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return

        // goAsync() extends this receiver's lifetime past onUpdate() returning - a detached scope
        // here could get cut off by a process kill mid-update, especially right after boot when
        // the system tends to reclaim processes aggressively. Null-safe finish(): updateAllWidgets()
        // below also calls onUpdate() directly on a manually-constructed instance (not dispatched
        // by the system), where goAsync() has no real pending result to return.
        val pendingResult = goAsync()
        PrayerApplication.instance.applicationScope.launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        PrayerApplication.instance.applicationScope.launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, intArrayOf(appWidgetId))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            // Fired system-wide on light/dark mode toggles (and other config changes) - without
            // this, Material You/App Theme colors only refreshed on the next scheduled update or
            // a manual tap, while every other themed widget on the launcher switched instantly.
            Intent.ACTION_CONFIGURATION_CHANGED,
            "com.salati.prayertimes.ACTION_PRAYER_ALARM" -> {
                updateAllWidgets(context)
            }
        }
    }

    private suspend fun updateWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val settings = PrayerPreferences.getInitialSettings(context)
        val isArabic = settings.language.resolveIsArabic()
        val layoutDirection = if (isArabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        val zoneId = try {
            ZoneId.of(settings.location.timeZoneId)
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()

        val timeFormatter = if (settings.is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }

        val todaySchedule = PrayerTimesCalculator.calculateDailySchedule(
            date = today,
            latitude = settings.location.latitude,
            longitude = settings.location.longitude,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustments = settings.adjustments,
            hijriAdjustmentDays = settings.hijriAdjustmentDays,
            now = now
        )

        // Determine next prayer
        val nextItem = todaySchedule.prayerItems.firstOrNull {
            it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(now)
        }

        val (nextPrayerType, nextPrayerZoned, isTomorrowFajr) = if (nextItem != null) {
            Triple(nextItem.type, nextItem.zonedDateTime, false)
        } else {
            val tomorrow = today.plusDays(1)
            val tomorrowSchedule = PrayerTimesCalculator.calculateDailySchedule(
                date = tomorrow,
                latitude = settings.location.latitude,
                longitude = settings.location.longitude,
                zoneId = zoneId,
                method = settings.calculationMethod,
                juristicMethod = settings.juristicMethod,
                highLatitudeRule = settings.highLatitudeRule,
                adjustments = settings.adjustments,
                hijriAdjustmentDays = settings.hijriAdjustmentDays,
                now = now
            )
            val fajrItem = tomorrowSchedule.prayerItems.first { it.type == PrayerType.FAJR }
            Triple(PrayerType.FAJR, fajrItem.zonedDateTime, true)
        }

        val diffSeconds = Duration.between(now, nextPrayerZoned).seconds

        val previousItem = todaySchedule.prayerItems
            .filter { it.type != PrayerType.SUNRISE && it.zonedDateTime.isBefore(nextPrayerZoned) }
            .lastOrNull()
        val totalSpanSeconds = if (previousItem != null) {
            Duration.between(previousItem.zonedDateTime, nextPrayerZoned).seconds.coerceAtLeast(1)
        } else {
            Duration.between(today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId), nextPrayerZoned).seconds.coerceAtLeast(1)
        }
        val elapsed = totalSpanSeconds - diffSeconds
        val progressPercent = ((elapsed.toFloat() / totalSpanSeconds) * 100).toInt().coerceIn(0, 100)

        // Previous prayer ("current prayer, since...") - falls back to yesterday's Isha before
        // today's first prayer has happened yet, same as the totalSpanSeconds fallback above.
        val previousPrayerType = previousItem?.type ?: PrayerType.ISHA
        val previousPrayerZoned = previousItem?.zonedDateTime
            ?: today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId)
        val sinceSeconds = Duration.between(previousPrayerZoned, now).seconds.coerceAtLeast(0)
        val sinceFormatted = formatSince(context, sinceSeconds, isArabic)

        // Location & Hijri display strings. A preset-picked city always shows in the *current*
        // app language (not whichever language was active when it was selected) - only a GPS or
        // manually-entered location, which has no second-language variant, keeps its stored name.
        val localizedRes = LocalizedStrings.forLanguage(context, isArabic)
        val locationFormatted = formatLocationString(
            com.prayertimes.data.cities.CityDatabase.localizedName(localizedRes, settings.location),
            com.prayertimes.data.cities.CityDatabase.localizedCountry(localizedRes, settings.location)
        )
        val hijriFormatted = if (isArabic) {
            todaySchedule.hijriDate?.formattedAr ?: todaySchedule.hijriDateString
        } else {
            todaySchedule.hijriDate?.formattedEn ?: todaySchedule.hijriDateString
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refreshIntent = Intent(context, PrayerAppWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REFRESH,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The hero card shows either the next prayer ("In 2h 41m") or the current/previous one
        // ("Since 2h 10m"), per widget settings - everything below just reads these three,
        // so no other builder code needs to know which mode is active.
        val showingPrevious = settings.widgetSettings.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
        val prayerDisplayName = getPrayerName(if (showingPrevious) previousPrayerType else nextPrayerType, settings.language)
        val formattedNextTime = (if (showingPrevious) previousPrayerZoned else nextPrayerZoned).format(timeFormatter)
        val countdownFormatted = if (showingPrevious) sinceFormatted else formatCountdown(context, diffSeconds, isArabic)

        // For heroTimeMode=BOTH: the true next/previous values regardless of the single-hero
        // fallback above, used by the dual side-by-side display on wide widgets.
        val previousPrayerDisplayName = getPrayerName(previousPrayerType, settings.language)
        val formattedPreviousTime = previousPrayerZoned.format(timeFormatter)
        val nextPrayerDisplayName = getPrayerName(nextPrayerType, settings.language)
        val trueNextFormattedTime = nextPrayerZoned.format(timeFormatter)

        val colors = resolveWidgetColors(context, settings)

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

            val hijriInline = minWidth >= 220
            val showBothHero = settings.widgetSettings.heroTimeMode == WidgetHeroTimeMode.BOTH && minWidth >= 220

            val microViews = buildMicroWidget(
                context = context,
                colors = colors,
                prayerTime = formattedNextTime,
                mainIntent = mainPendingIntent,
                layoutDirection = layoutDirection
            )

            val verticalViews = buildVerticalWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                isTomorrowFajr = isTomorrowFajr,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                layoutDirection = layoutDirection
            )

            val smallViews = buildSmallWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent,
                layoutDirection = layoutDirection
            )

            val slimViews = buildSlimWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent,
                layoutDirection = layoutDirection
            )

            val mediumViews = buildMediumWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                progressPercent = progressPercent,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent,
                layoutDirection = layoutDirection
            )

            val largeViews = buildLargeWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                hijriText = hijriFormatted,
                hijriInline = hijriInline,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                isTomorrowFajr = isTomorrowFajr,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                diffSeconds = diffSeconds,
                progressPercent = progressPercent,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent,
                layoutDirection = layoutDirection,
                isArabic = isArabic,
                showBothHero = showBothHero,
                previousPrayerName = previousPrayerDisplayName,
                previousPrayerTime = formattedPreviousTime,
                sinceText = sinceFormatted,
                nextPrayerName = nextPrayerDisplayName,
                nextPrayerTimeText = trueNextFormattedTime
            )

            val expandedViews = buildExpandedWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                hijriText = hijriFormatted,
                hijriInline = hijriInline,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                isTomorrowFajr = isTomorrowFajr,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                diffSeconds = diffSeconds,
                progressPercent = progressPercent,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent,
                layoutDirection = layoutDirection,
                isArabic = isArabic,
                showBothHero = showBothHero,
                previousPrayerName = previousPrayerDisplayName,
                previousPrayerTime = formattedPreviousTime,
                sinceText = sinceFormatted,
                nextPrayerName = nextPrayerDisplayName,
                nextPrayerTimeText = trueNextFormattedTime
            )

            // Size buckets are calibrated to each layout's real minimum content size
            // (not aspirational targets), using Android's cell-size formula 70*n-30dp,
            // so the OS never force-fits a layout into a box too small for its content:
            //  - 1 row (h<90dp), 1 col (w<90dp)  -> micro (icon + time only)
            //  - 1 row (h<90dp), 2+ cols          -> slim (single horizontal bar)
            //  - 2+ rows, 1 col (w<110dp)         -> vertical (stacked 6-row column)
            //  - 2+ rows, 2-3 cols                -> small (compact square card)
            //  - 2 rows, 4+ cols                  -> medium (hero + 3-slot split)
            //  - 3-4 rows, 4+ cols                 -> large (header + hero + ribbon)
            //  - 5+ rows, 4+ cols                  -> expanded (full 6-row schedule)
            val widgetViews: RemoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(
                    mapOf(
                        SizeF(40f, 40f) to microViews,
                        SizeF(90f, 40f) to slimViews,
                        SizeF(40f, 110f) to verticalViews,
                        SizeF(110f, 100f) to smallViews,
                        SizeF(230f, 90f) to mediumViews,
                        SizeF(250f, 180f) to largeViews,
                        SizeF(250f, 320f) to expandedViews
                    )
                )
            } else {
                when {
                    minHeight < 90 && minWidth < 90 -> microViews
                    minHeight < 90 -> slimViews
                    minWidth < 110 -> verticalViews
                    minWidth >= 250 && minHeight >= 320 -> expandedViews
                    minWidth >= 250 && minHeight >= 180 -> largeViews
                    minWidth >= 230 -> mediumViews
                    else -> smallViews
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, widgetViews)
        }

        scheduleNextWidgetUpdate(context, nextPrayerZoned)
    }

    private fun buildMicroWidget(
        context: Context,
        colors: WidgetColorScheme,
        prayerTime: String,
        mainIntent: PendingIntent,
        layoutDirection: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_micro)

        views.setInt(R.id.widget_root_micro, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_micro_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_micro_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_micro_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_micro_root_border_img, View.GONE)
        }

        tintShape(views, R.id.widget_micro_icon, colors.accentColor)
        views.setTextViewText(R.id.widget_micro_time, prayerTime)
        views.setTextColor(R.id.widget_micro_time, colors.textPrimaryColor)

        views.setOnClickPendingIntent(R.id.widget_root_micro, mainIntent)
        return views
    }

    private fun buildVerticalWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        isTomorrowFajr: Boolean,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        layoutDirection: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_vertical)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root_vert, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_vert_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_vert_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_vert_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_vert_root_border_img, View.GONE)
        }

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_vert_header, View.VISIBLE)
            views.setTextViewText(R.id.widget_vert_location, locationText)
            views.setTextColor(R.id.widget_vert_location, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_vert_location, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_vert_header, View.GONE)
        }

        if (wSet.showHeroCard) {
            views.setViewVisibility(R.id.widget_vert_hero, View.VISIBLE)
            tintShape(views, R.id.widget_vert_hero_bg_img, colors.heroBgColor)

            views.setTextViewText(R.id.widget_vert_next_name, prayerName)
            views.setTextColor(R.id.widget_vert_next_name, colors.accentColor)
            views.setTextViewTextSize(R.id.widget_vert_next_name, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

            views.setTextViewText(R.id.widget_vert_next_time, prayerTime)
            views.setTextColor(R.id.widget_vert_next_time, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_vert_next_time, TypedValue.COMPLEX_UNIT_SP, 14f * scale)

            if (wSet.showCountdown) {
                views.setViewVisibility(R.id.widget_vert_countdown_container, View.VISIBLE)
                tintShape(views, R.id.widget_vert_countdown_bg_img, colors.countdownBgColor)
                tintContainerBackground(views, R.id.widget_vert_countdown_container, colors.countdownBgColor)
                views.setTextViewText(R.id.widget_vert_countdown, countdown)
                views.setTextColor(R.id.widget_vert_countdown, colors.textOnAccentColor)
                views.setTextViewTextSize(R.id.widget_vert_countdown, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            } else {
                views.setViewVisibility(R.id.widget_vert_countdown_container, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_vert_hero, View.GONE)
        }

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        fun bindRow(rowId: Int, bgId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            if (type == PrayerType.SUNRISE && !wSet.showSunrise) {
                views.setViewVisibility(rowId, View.GONE)
                return
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")

            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 10f * scale)

            val isHighlighted = !isTomorrowFajr && nextPrayerType == type
            if (isHighlighted) {
                tintShape(views, bgId, colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                tintShape(views, bgId, colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        bindRow(R.id.widget_vert_fajr_row, R.id.widget_vert_fajr_bg_img, R.id.widget_vert_fajr_name, R.id.widget_vert_fajr_time, PrayerType.FAJR)
        bindRow(R.id.widget_vert_sunrise_row, R.id.widget_vert_sunrise_bg_img, R.id.widget_vert_sunrise_name, R.id.widget_vert_sunrise_time, PrayerType.SUNRISE)
        bindRow(R.id.widget_vert_dhuhr_row, R.id.widget_vert_dhuhr_bg_img, R.id.widget_vert_dhuhr_name, R.id.widget_vert_dhuhr_time, PrayerType.DHUHR)
        bindRow(R.id.widget_vert_asr_row, R.id.widget_vert_asr_bg_img, R.id.widget_vert_asr_name, R.id.widget_vert_asr_time, PrayerType.ASR)
        bindRow(R.id.widget_vert_maghrib_row, R.id.widget_vert_maghrib_bg_img, R.id.widget_vert_maghrib_name, R.id.widget_vert_maghrib_time, PrayerType.MAGHRIB)
        bindRow(R.id.widget_vert_isha_row, R.id.widget_vert_isha_bg_img, R.id.widget_vert_isha_name, R.id.widget_vert_isha_time, PrayerType.ISHA)

        views.setOnClickPendingIntent(R.id.widget_root_vert, mainIntent)
        return views
    }

    private fun buildSlimWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent,
        layoutDirection: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_slim)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root_slim, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_slim_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_slim_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_slim_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_slim_root_border_img, View.GONE)
        }

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_slim_location, View.VISIBLE)
            views.setTextViewText(R.id.widget_slim_location, locationText)
            views.setTextColor(R.id.widget_slim_location, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_slim_location, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_slim_location, View.GONE)
        }

        views.setTextViewText(R.id.widget_slim_prayer_name, prayerName)
        views.setTextColor(R.id.widget_slim_prayer_name, colors.accentColor)
        views.setTextViewTextSize(R.id.widget_slim_prayer_name, TypedValue.COMPLEX_UNIT_SP, 13f * scale)

        views.setTextViewText(R.id.widget_slim_prayer_time, prayerTime)
        views.setTextColor(R.id.widget_slim_prayer_time, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_slim_prayer_time, TypedValue.COMPLEX_UNIT_SP, 14f * scale)

        if (wSet.showCountdown) {
            views.setViewVisibility(R.id.widget_slim_countdown_container, View.VISIBLE)
            tintShape(views, R.id.widget_slim_countdown_bg_img, colors.countdownBgColor)
            tintContainerBackground(views, R.id.widget_slim_countdown_container, colors.countdownBgColor)
            views.setTextViewText(R.id.widget_slim_countdown, countdown)
            views.setTextColor(R.id.widget_slim_countdown, colors.textOnAccentColor)
            views.setTextViewTextSize(R.id.widget_slim_countdown, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
        } else {
            views.setViewVisibility(R.id.widget_slim_countdown_container, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root_slim, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_slim_refresh_btn, refreshIntent)
        return views
    }

    private fun buildSmallWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent,
        layoutDirection: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_small)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root_small, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_small_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_small_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_small_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_small_root_border_img, View.GONE)
        }

        tintShape(views, R.id.widget_small_hero_bg_img, colors.heroBgColor)

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_small_location, View.VISIBLE)
            views.setTextViewText(R.id.widget_small_location, locationText)
            views.setTextColor(R.id.widget_small_location, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_small_location, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
        } else {
            views.setViewVisibility(R.id.widget_small_location, View.GONE)
        }

        views.setTextViewText(R.id.widget_small_prayer_name, prayerName)
        views.setTextColor(R.id.widget_small_prayer_name, colors.accentColor)
        views.setTextViewTextSize(R.id.widget_small_prayer_name, TypedValue.COMPLEX_UNIT_SP, 13f * scale)

        views.setTextViewText(R.id.widget_small_prayer_time, prayerTime)
        views.setTextColor(R.id.widget_small_prayer_time, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_small_prayer_time, TypedValue.COMPLEX_UNIT_SP, 18f * scale)

        if (wSet.showCountdown) {
            views.setViewVisibility(R.id.widget_small_countdown_container, View.VISIBLE)
            tintShape(views, R.id.widget_small_countdown_bg_img, colors.countdownBgColor)
            tintContainerBackground(views, R.id.widget_small_countdown_container, colors.countdownBgColor)
            views.setTextViewText(R.id.widget_small_countdown, countdown)
            views.setTextColor(R.id.widget_small_countdown, colors.textOnAccentColor)
            views.setTextViewTextSize(R.id.widget_small_countdown, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_small_countdown_container, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root_small, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_small_refresh_btn, refreshIntent)
        return views
    }

    private fun buildMediumWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        progressPercent: Int,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent,
        layoutDirection: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_medium)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root_medium, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_med_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_med_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_med_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_med_root_border_img, View.GONE)
        }

        tintShape(views, R.id.widget_med_hero_bg_img, colors.heroBgColor)
        tintShape(views, R.id.widget_med_hero_border_img, colors.heroStrokeColor)

        views.setTextViewText(R.id.widget_med_location, locationText)
        views.setTextColor(R.id.widget_med_location, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_med_location, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

        views.setTextViewText(R.id.widget_med_next_prayer_name, prayerName)
        views.setTextColor(R.id.widget_med_next_prayer_name, colors.accentColor)
        views.setTextViewTextSize(R.id.widget_med_next_prayer_name, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

        views.setTextViewText(R.id.widget_med_next_prayer_time, prayerTime)
        views.setTextColor(R.id.widget_med_next_prayer_time, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_med_next_prayer_time, TypedValue.COMPLEX_UNIT_SP, 19f * scale)

        if (wSet.showCountdown) {
            views.setViewVisibility(R.id.widget_med_countdown_container, View.VISIBLE)
            tintShape(views, R.id.widget_med_countdown_bg_img, colors.countdownBgColor)
            tintContainerBackground(views, R.id.widget_med_countdown_container, colors.countdownBgColor)
            views.setTextViewText(R.id.widget_med_countdown, countdown)
            views.setTextColor(R.id.widget_med_countdown, colors.textOnAccentColor)
            views.setTextViewTextSize(R.id.widget_med_countdown, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_med_countdown_container, View.GONE)
        }

        if (wSet.showProgressBar) {
            views.setViewVisibility(R.id.widget_med_prayer_progress, View.VISIBLE)
            views.setProgressBar(R.id.widget_med_prayer_progress, 100, progressPercent, false)
        } else {
            views.setViewVisibility(R.id.widget_med_prayer_progress, View.GONE)
        }

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        val prayerList = listOf(PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB)

        fun bindSlot(bgId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")
            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)

            if (nextPrayerType == type) {
                tintShape(views, bgId, colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                tintShape(views, bgId, colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        bindSlot(R.id.widget_med_slot1_bg_img, R.id.widget_med_slot1_name, R.id.widget_med_slot1_time, prayerList[0])
        bindSlot(R.id.widget_med_slot2_bg_img, R.id.widget_med_slot2_name, R.id.widget_med_slot2_time, prayerList[1])
        bindSlot(R.id.widget_med_slot3_bg_img, R.id.widget_med_slot3_name, R.id.widget_med_slot3_time, prayerList[2])

        views.setOnClickPendingIntent(R.id.widget_root_medium, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_med_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_med_refresh_btn, refreshIntent)
        return views
    }

    private fun buildLargeWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        hijriText: String,
        hijriInline: Boolean,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        isTomorrowFajr: Boolean,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        diffSeconds: Long,
        progressPercent: Int,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent,
        layoutDirection: Int,
        isArabic: Boolean,
        showBothHero: Boolean,
        previousPrayerName: String,
        previousPrayerTime: String,
        sinceText: String,
        nextPrayerName: String,
        nextPrayerTimeText: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_layout)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_root_border_img, View.GONE)
        }

        tintShape(views, R.id.widget_mosque_icon, colors.accentColor)
        tintShape(views, R.id.widget_refresh_button, colors.textSecondaryColor)

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_location_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_location_text, locationText)
            views.setTextColor(R.id.widget_location_text, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_location_text, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
        } else {
            views.setViewVisibility(R.id.widget_location_text, View.GONE)
        }

        if (wSet.showHijriDate) {
            if (hijriInline) {
                views.setViewVisibility(R.id.widget_hijri_text_inline, View.VISIBLE)
                views.setTextViewText(R.id.widget_hijri_text_inline, "• $hijriText")
                views.setTextColor(R.id.widget_hijri_text_inline, colors.textSecondaryColor)
                views.setTextViewTextSize(R.id.widget_hijri_text_inline, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
                views.setViewVisibility(R.id.widget_hijri_text, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_hijri_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_hijri_text, hijriText)
                views.setTextColor(R.id.widget_hijri_text, colors.textSecondaryColor)
                views.setTextViewTextSize(R.id.widget_hijri_text, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
                views.setViewVisibility(R.id.widget_hijri_text_inline, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_hijri_text, View.GONE)
            views.setViewVisibility(R.id.widget_hijri_text_inline, View.GONE)
        }

        if (wSet.showHeroCard) {
            views.setViewVisibility(R.id.widget_hero_card, View.VISIBLE)
            tintShape(views, R.id.widget_hero_bg_img, colors.heroBgColor)
            tintShape(views, R.id.widget_hero_border_img, colors.heroStrokeColor)

            if (showBothHero) {
                views.setViewVisibility(R.id.widget_hero_single_content, View.GONE)
                views.setViewVisibility(R.id.widget_hero_dual_content, View.VISIBLE)

                val heroLabelRes = LocalizedStrings.forLanguage(context, isArabic)
                views.setTextViewText(R.id.widget_hero_prev_label, heroLabelRes.getString(R.string.widget_hero_prev_label))
                views.setTextColor(R.id.widget_hero_prev_label, colors.textSecondaryColor)
                views.setTextViewText(R.id.widget_hero_prev_name, previousPrayerName)
                views.setTextColor(R.id.widget_hero_prev_name, colors.accentColor)
                views.setTextViewText(R.id.widget_hero_prev_time, previousPrayerTime)
                views.setTextColor(R.id.widget_hero_prev_time, colors.textPrimaryColor)
                views.setTextViewText(R.id.widget_hero_prev_since, sinceText)
                views.setTextColor(R.id.widget_hero_prev_since, colors.textSecondaryColor)

                views.setTextViewText(R.id.widget_hero_next_label, heroLabelRes.getString(R.string.widget_hero_next_label))
                views.setTextColor(R.id.widget_hero_next_label, colors.textSecondaryColor)
                views.setTextViewText(R.id.widget_hero_next_name, nextPrayerName)
                views.setTextColor(R.id.widget_hero_next_name, colors.accentColor)
                views.setTextViewText(R.id.widget_hero_next_time, nextPrayerTimeText)
                views.setTextColor(R.id.widget_hero_next_time, colors.textPrimaryColor)
                views.setTextViewText(R.id.widget_hero_next_in, countdown)
                views.setTextColor(R.id.widget_hero_next_in, colors.textSecondaryColor)

                views.setViewVisibility(R.id.widget_prayer_progress, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_hero_single_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_hero_dual_content, View.GONE)

                views.setTextViewText(R.id.widget_next_prayer_name, prayerName)
                views.setTextColor(R.id.widget_next_prayer_name, colors.accentColor)
                views.setTextViewTextSize(R.id.widget_next_prayer_name, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

                views.setTextViewText(R.id.widget_next_prayer_time, prayerTime)
                views.setTextColor(R.id.widget_next_prayer_time, colors.textPrimaryColor)
                views.setTextViewTextSize(R.id.widget_next_prayer_time, TypedValue.COMPLEX_UNIT_SP, 20f * scale)

                if (wSet.showCountdown) {
                    views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                    tintShape(views, R.id.widget_countdown_bg_img, colors.countdownBgColor)
                    tintContainerBackground(views, R.id.widget_countdown_container, colors.countdownBgColor)
                    views.setTextViewText(R.id.widget_countdown_text, countdown)
                    views.setTextColor(R.id.widget_countdown_text, colors.textOnAccentColor)
                    views.setTextViewTextSize(R.id.widget_countdown_text, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

                    // "In 2h 41m" already says everything; a "Remaining" label under it just
                    // reads redundantly. Only show a status line once the prayer time has
                    // actually arrived, where it adds real information.
                    if (wSet.heroTimeMode == WidgetHeroTimeMode.NEXT && diffSeconds <= 60) {
                        views.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                        views.setTextViewText(R.id.widget_status_text, LocalizedStrings.forLanguage(context, isArabic).getString(R.string.widget_status_prayer_time))
                        views.setTextColor(R.id.widget_status_text, colors.textOnAccentColor)
                        views.setTextViewTextSize(R.id.widget_status_text, TypedValue.COMPLEX_UNIT_SP, 8f * scale)
                    } else {
                        views.setViewVisibility(R.id.widget_status_text, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_countdown_container, View.GONE)
                }

                if (wSet.showProgressBar) {
                    views.setViewVisibility(R.id.widget_prayer_progress, View.VISIBLE)
                    views.setProgressBar(R.id.widget_prayer_progress, 100, progressPercent, false)
                } else {
                    views.setViewVisibility(R.id.widget_prayer_progress, View.GONE)
                }
            }
        } else {
            views.setViewVisibility(R.id.widget_hero_card, View.GONE)
        }

        if (wSet.showAllPrayersList) {
            views.setViewVisibility(R.id.widget_prayer_ribbon, View.VISIBLE)
            val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
            populatePrayerRibbon(context, views, colors, prayerMap, nextPrayerType, isTomorrowFajr, timeFormatter, settings.language, wSet.showSunrise)
        } else {
            views.setViewVisibility(R.id.widget_prayer_ribbon, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshIntent)
        return views
    }

    private fun buildExpandedWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        hijriText: String,
        hijriInline: Boolean,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        isTomorrowFajr: Boolean,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        diffSeconds: Long,
        progressPercent: Int,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent,
        layoutDirection: Int,
        isArabic: Boolean,
        showBothHero: Boolean,
        previousPrayerName: String,
        previousPrayerTime: String,
        sinceText: String,
        nextPrayerName: String,
        nextPrayerTimeText: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_expanded)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root_expanded, "setLayoutDirection", layoutDirection)
        tintShape(views, R.id.widget_exp_root_bg_img, colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_exp_root_border_img, View.VISIBLE)
            tintShape(views, R.id.widget_exp_root_border_img, colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_exp_root_border_img, View.GONE)
        }

        tintShape(views, R.id.widget_exp_mosque_icon, colors.accentColor)
        tintShape(views, R.id.widget_exp_refresh_btn, colors.textSecondaryColor)

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_exp_location, View.VISIBLE)
            views.setTextViewText(R.id.widget_exp_location, locationText)
            views.setTextColor(R.id.widget_exp_location, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_exp_location, TypedValue.COMPLEX_UNIT_SP, 13f * scale)
        } else {
            views.setViewVisibility(R.id.widget_exp_location, View.GONE)
        }

        if (wSet.showHijriDate) {
            if (hijriInline) {
                views.setViewVisibility(R.id.widget_exp_hijri_inline, View.VISIBLE)
                views.setTextViewText(R.id.widget_exp_hijri_inline, "• $hijriText")
                views.setTextColor(R.id.widget_exp_hijri_inline, colors.textSecondaryColor)
                views.setTextViewTextSize(R.id.widget_exp_hijri_inline, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                views.setViewVisibility(R.id.widget_exp_hijri, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_exp_hijri, View.VISIBLE)
                views.setTextViewText(R.id.widget_exp_hijri, hijriText)
                views.setTextColor(R.id.widget_exp_hijri, colors.textSecondaryColor)
                views.setTextViewTextSize(R.id.widget_exp_hijri, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                views.setViewVisibility(R.id.widget_exp_hijri_inline, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_exp_hijri, View.GONE)
            views.setViewVisibility(R.id.widget_exp_hijri_inline, View.GONE)
        }

        if (wSet.showHeroCard) {
            views.setViewVisibility(R.id.widget_exp_hero_card, View.VISIBLE)
            tintShape(views, R.id.widget_exp_hero_bg_img, colors.heroBgColor)
            tintShape(views, R.id.widget_exp_hero_border_img, colors.heroStrokeColor)

            if (showBothHero) {
                views.setViewVisibility(R.id.widget_exp_hero_single_content, View.GONE)
                views.setViewVisibility(R.id.widget_exp_hero_dual_content, View.VISIBLE)

                val expHeroLabelRes = LocalizedStrings.forLanguage(context, isArabic)
                views.setTextViewText(R.id.widget_exp_hero_prev_label, expHeroLabelRes.getString(R.string.widget_hero_prev_label))
                views.setTextColor(R.id.widget_exp_hero_prev_label, colors.textSecondaryColor)
                views.setTextViewText(R.id.widget_exp_hero_prev_name, previousPrayerName)
                views.setTextColor(R.id.widget_exp_hero_prev_name, colors.accentColor)
                views.setTextViewText(R.id.widget_exp_hero_prev_time, previousPrayerTime)
                views.setTextColor(R.id.widget_exp_hero_prev_time, colors.textPrimaryColor)
                views.setTextViewText(R.id.widget_exp_hero_prev_since, sinceText)
                views.setTextColor(R.id.widget_exp_hero_prev_since, colors.textSecondaryColor)

                views.setTextViewText(R.id.widget_exp_hero_next_label, expHeroLabelRes.getString(R.string.widget_hero_next_label))
                views.setTextColor(R.id.widget_exp_hero_next_label, colors.textSecondaryColor)
                views.setTextViewText(R.id.widget_exp_hero_next_name, nextPrayerName)
                views.setTextColor(R.id.widget_exp_hero_next_name, colors.accentColor)
                views.setTextViewText(R.id.widget_exp_hero_next_time, nextPrayerTimeText)
                views.setTextColor(R.id.widget_exp_hero_next_time, colors.textPrimaryColor)
                views.setTextViewText(R.id.widget_exp_hero_next_in, countdown)
                views.setTextColor(R.id.widget_exp_hero_next_in, colors.textSecondaryColor)

                views.setViewVisibility(R.id.widget_exp_prayer_progress, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_exp_hero_single_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_exp_hero_dual_content, View.GONE)

                views.setTextViewText(R.id.widget_exp_next_prayer_name, prayerName)
                views.setTextColor(R.id.widget_exp_next_prayer_name, colors.accentColor)
                views.setTextViewTextSize(R.id.widget_exp_next_prayer_name, TypedValue.COMPLEX_UNIT_SP, 13f * scale)

                views.setTextViewText(R.id.widget_exp_next_prayer_time, prayerTime)
                views.setTextColor(R.id.widget_exp_next_prayer_time, colors.textPrimaryColor)
                views.setTextViewTextSize(R.id.widget_exp_next_prayer_time, TypedValue.COMPLEX_UNIT_SP, 24f * scale)

                if (wSet.showCountdown) {
                    views.setViewVisibility(R.id.widget_exp_countdown_container, View.VISIBLE)
                    tintShape(views, R.id.widget_exp_countdown_bg_img, colors.countdownBgColor)
                    tintContainerBackground(views, R.id.widget_exp_countdown_container, colors.countdownBgColor)
                    views.setTextViewText(R.id.widget_exp_countdown_text, countdown)
                    views.setTextColor(R.id.widget_exp_countdown_text, colors.textOnAccentColor)
                    views.setTextViewTextSize(R.id.widget_exp_countdown_text, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

                    if (wSet.heroTimeMode == WidgetHeroTimeMode.NEXT && diffSeconds <= 60) {
                        views.setViewVisibility(R.id.widget_exp_status_text, View.VISIBLE)
                        views.setTextViewText(R.id.widget_exp_status_text, LocalizedStrings.forLanguage(context, isArabic).getString(R.string.widget_status_prayer_time))
                        views.setTextColor(R.id.widget_exp_status_text, colors.textOnAccentColor)
                        views.setTextViewTextSize(R.id.widget_exp_status_text, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
                    } else {
                        views.setViewVisibility(R.id.widget_exp_status_text, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_exp_countdown_container, View.GONE)
                }

                if (wSet.showProgressBar) {
                    views.setViewVisibility(R.id.widget_exp_prayer_progress, View.VISIBLE)
                    views.setProgressBar(R.id.widget_exp_prayer_progress, 100, progressPercent, false)
                } else {
                    views.setViewVisibility(R.id.widget_exp_prayer_progress, View.GONE)
                }
            }
        } else {
            views.setViewVisibility(R.id.widget_exp_hero_card, View.GONE)
        }

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        fun bindExpRow(rowId: Int, bgId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            if (type == PrayerType.SUNRISE && !wSet.showSunrise) {
                views.setViewVisibility(rowId, View.GONE)
                return
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")

            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

            val isHighlighted = !isTomorrowFajr && nextPrayerType == type
            if (isHighlighted) {
                tintShape(views, bgId, colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                tintShape(views, bgId, colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        if (wSet.showAllPrayersList) {
            views.setViewVisibility(R.id.widget_exp_schedule_list, View.VISIBLE)
            bindExpRow(R.id.widget_exp_fajr_row, R.id.widget_exp_fajr_bg_img, R.id.widget_exp_fajr_name, R.id.widget_exp_fajr_time, PrayerType.FAJR)
            bindExpRow(R.id.widget_exp_sunrise_row, R.id.widget_exp_sunrise_bg_img, R.id.widget_exp_sunrise_name, R.id.widget_exp_sunrise_time, PrayerType.SUNRISE)
            bindExpRow(R.id.widget_exp_dhuhr_row, R.id.widget_exp_dhuhr_bg_img, R.id.widget_exp_dhuhr_name, R.id.widget_exp_dhuhr_time, PrayerType.DHUHR)
            bindExpRow(R.id.widget_exp_asr_row, R.id.widget_exp_asr_bg_img, R.id.widget_exp_asr_name, R.id.widget_exp_asr_time, PrayerType.ASR)
            bindExpRow(R.id.widget_exp_maghrib_row, R.id.widget_exp_maghrib_bg_img, R.id.widget_exp_maghrib_name, R.id.widget_exp_maghrib_time, PrayerType.MAGHRIB)
            bindExpRow(R.id.widget_exp_isha_row, R.id.widget_exp_isha_bg_img, R.id.widget_exp_isha_name, R.id.widget_exp_isha_time, PrayerType.ISHA)
        } else {
            views.setViewVisibility(R.id.widget_exp_schedule_list, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root_expanded, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_exp_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_exp_refresh_btn, refreshIntent)
        return views
    }

    private fun populatePrayerRibbon(
        context: Context,
        views: RemoteViews,
        colors: WidgetColorScheme,
        prayerMap: Map<PrayerType, PrayerTimeItem>,
        nextType: PrayerType,
        isTomorrowFajr: Boolean,
        formatter: DateTimeFormatter,
        language: AppLanguage,
        showSunrise: Boolean
    ) {
        val scale = colors.fontScale
        fun updateSlot(
            containerId: Int,
            bgId: Int,
            nameId: Int,
            timeId: Int,
            type: PrayerType
        ) {
            if (type == PrayerType.SUNRISE && !showSunrise) {
                views.setViewVisibility(containerId, View.GONE)
                return
            }
            views.setViewVisibility(containerId, View.VISIBLE)
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, language))
            views.setTextViewText(timeId, item?.time?.format(formatter) ?: "--:--")

            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)

            val isHighlighted = !isTomorrowFajr && nextType == type
            if (isHighlighted) {
                tintShape(views, bgId, colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                tintShape(views, bgId, colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        updateSlot(R.id.widget_fajr_container, R.id.widget_fajr_bg_img, R.id.widget_fajr_name, R.id.widget_fajr_time, PrayerType.FAJR)
        updateSlot(R.id.widget_sunrise_container, R.id.widget_sunrise_bg_img, R.id.widget_sunrise_name, R.id.widget_sunrise_time, PrayerType.SUNRISE)
        updateSlot(R.id.widget_dhuhr_container, R.id.widget_dhuhr_bg_img, R.id.widget_dhuhr_name, R.id.widget_dhuhr_time, PrayerType.DHUHR)
        updateSlot(R.id.widget_asr_container, R.id.widget_asr_bg_img, R.id.widget_asr_name, R.id.widget_asr_time, PrayerType.ASR)
        updateSlot(R.id.widget_maghrib_container, R.id.widget_maghrib_bg_img, R.id.widget_maghrib_name, R.id.widget_maghrib_time, PrayerType.MAGHRIB)
        updateSlot(R.id.widget_isha_container, R.id.widget_isha_bg_img, R.id.widget_isha_name, R.id.widget_isha_time, PrayerType.ISHA)
    }

    /**
     * Tints a shape-backed ImageView (the drawable's base fill must be opaque).
     * ImageView.setColorFilter(int) uses PorterDuff.Mode.SRC_ATOP, whose result
     * alpha always equals the *destination* alpha, not the source color's alpha.
     * So variable transparency (opacity slider, transparent styles) can't be baked
     * into the filter color itself - it has to be applied separately via
     * setImageAlpha, after tinting with a fully opaque version of the color.
     */
    private fun tintShape(views: RemoteViews, viewId: Int, color: Int) {
        views.setInt(viewId, "setColorFilter", ColorUtils.setAlphaComponent(color, 255))
        views.setInt(viewId, "setImageAlpha", Color.alpha(color))
    }

    /**
     * A second, independent way to color a pill badge, used alongside tintShape() on its
     * ImageView. The countdown pill kept rendering with no visible background across multiple
     * rebuilds - including a structural layout rewrite - which pointed away from a pure XML/
     * layout bug and toward the widget host reapplying updates onto a cached View tree rather
     * than a fresh inflate (a real RemoteViews optimization when the layout id is unchanged).
     * setColorStateList/setBackgroundTintList tints a background drawable declared directly in
     * XML on the container itself - a completely different code path from the ImageView overlay,
     * so if one is affected by stale-view reuse the other still has a chance to render correctly.
     * API 31+ only (older devices keep relying on the ImageView alone).
     */
    private fun tintContainerBackground(views: RemoteViews, containerId: Int, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(containerId, "setBackgroundTintList", ColorStateList.valueOf(color))
        }
    }

    private fun formatLocationString(city: String, country: String): String {
        return city.ifBlank { country }
    }

    private fun getPrayerName(type: PrayerType, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ARABIC -> when (type) {
                PrayerType.FAJR -> "الفجر"
                PrayerType.SUNRISE -> "الشروق"
                PrayerType.DHUHR -> "الظهر"
                PrayerType.ASR -> "العصر"
                PrayerType.MAGHRIB -> "المغرب"
                PrayerType.ISHA -> "العشاء"
            }
            AppLanguage.ENGLISH -> type.title
            AppLanguage.SYSTEM -> {
                val isAr = Locale.getDefault().language.equals("ar", ignoreCase = true)
                if (isAr) {
                    when (type) {
                        PrayerType.FAJR -> "الفجر"
                        PrayerType.SUNRISE -> "الشروق"
                        PrayerType.DHUHR -> "الظهر"
                        PrayerType.ASR -> "العصر"
                        PrayerType.MAGHRIB -> "المغرب"
                        PrayerType.ISHA -> "العشاء"
                    }
                } else {
                    type.title
                }
            }
        }
    }

    private fun formatCountdown(context: Context, seconds: Long, isArabic: Boolean): String {
        val res = LocalizedStrings.forLanguage(context, isArabic)
        if (seconds <= 0) return res.getString(R.string.widget_countdown_now)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> res.getString(R.string.widget_countdown_hours_minutes, hours, minutes)
            minutes > 0 -> res.getString(R.string.widget_countdown_minutes_only, minutes)
            else -> res.getString(R.string.widget_countdown_less_than_min)
        }
    }

    private fun formatSince(context: Context, seconds: Long, isArabic: Boolean): String {
        val res = LocalizedStrings.forLanguage(context, isArabic)
        if (seconds <= 60) return res.getString(R.string.widget_since_just_now)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> res.getString(R.string.widget_since_hours_minutes, hours, minutes)
            else -> res.getString(R.string.widget_since_minutes_only, minutes)
        }
    }

    private fun scheduleNextWidgetUpdate(context: Context, nextPrayerTime: ZonedDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAppWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // A real prayer-boundary crossing already refreshes the widget via ACTION_PRAYER_ALARM
        // (when the user has that prayer's notification enabled) or the settings/date/timezone
        // change broadcasts - this alarm exists only as a coarse safety net to keep the countdown
        // text from drifting stale otherwise, so it doesn't need to be nearly as frequent as an
        // actual prayer boundary. Widened from 15 to 60 minutes; the exact-boundary branch below
        // still wakes it sooner if a prayer falls within that window regardless.
        val nowMillis = System.currentTimeMillis()
        val prayerMillis = nextPrayerTime.toInstant().toEpochMilli()
        val coarseFallbackMillis = nowMillis + 60 * 60 * 1000L
        val triggerMillis = if (prayerMillis in (nowMillis + 1000)..coarseFallbackMillis) {
            prayerMillis
        } else {
            coarseFallbackMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            // Fallback
        }
    }
}
