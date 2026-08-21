package com.prayertimes.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.prayertimes.R
import com.prayertimes.data.models.PrayerType
import java.util.Locale

/**
 * Resolves string resources for a specific language regardless of the device's actual system
 * locale or this process's Activity configuration. Needed by non-Compose code (the widget
 * provider) because the app's in-app language override can differ from the system locale, and
 * there is no guarantee a RemoteViews-building Context already reflects it.
 */
object LocalizedStrings {
    fun forLanguage(context: Context, isArabic: Boolean): Resources {
        val locale = Locale(if (isArabic) "ar" else "en")
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config).resources
    }

    private val prayerNameRes: Map<PrayerType, Int> = mapOf(
        PrayerType.FAJR to R.string.prayer_name_fajr,
        PrayerType.SUNRISE to R.string.prayer_name_sunrise,
        PrayerType.DHUHR to R.string.prayer_name_dhuhr,
        PrayerType.ASR to R.string.prayer_name_asr,
        PrayerType.MAGHRIB to R.string.prayer_name_maghrib,
        PrayerType.ISHA to R.string.prayer_name_isha
    )

    fun prayerName(res: Resources, type: PrayerType): String = res.getString(prayerNameRes.getValue(type))
}
