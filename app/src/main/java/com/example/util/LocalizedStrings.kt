package com.example.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
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
}
