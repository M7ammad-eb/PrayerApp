package com.prayertimes.data.models

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
) {
    SYSTEM("system", "تلقائي (لغة الجهاز)", "System Default (Device)"),
    ARABIC("ar", "العربية", "Arabic (العربية)"),
    ENGLISH("en", "English", "English");

    fun getDisplayName(isArabic: Boolean): String {
        return if (isArabic) {
            when (this) {
                SYSTEM -> "تلقائي (لغة الجهاز)"
                ARABIC -> "العربية"
                ENGLISH -> "English (الإنجليزية)"
            }
        } else {
            englishName
        }
    }

    /**
     * Resolves this language setting to a concrete isArabic boolean, falling back to the device's
     * system locale when set to SYSTEM. Shared by every non-Compose code path (widget provider,
     * broadcast receivers, foreground services) that needs to pick a display language without an
     * ambient Compose locale to read from.
     */
    fun resolveIsArabic(context: Context): Boolean = when (this) {
        ARABIC -> true
        ENGLISH -> false
        SYSTEM -> {
            // Some OEMs incorrectly return the active per-app override from systemLocales. The
            // system_locales setting remains the actual device choice; use the public API and
            // global Resources as fallbacks because that setting is not present on every build.
            val storedSystemLanguage = runCatching {
                Settings.System.getString(context.contentResolver, "system_locales")
                    ?.substringBefore(',')
                    ?.let(Locale::forLanguageTag)
                    ?.language
            }.getOrNull()
            val managerLanguage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val locales = context.getSystemService(android.app.LocaleManager::class.java)?.systemLocales
                if (locales != null && !locales.isEmpty) locales[0]?.language else null
            } else null
            val resourceLocales = Resources.getSystem().configuration.locales
            val resourceLanguage = if (!resourceLocales.isEmpty) resourceLocales[0]?.language else null
            val systemLanguage = storedSystemLanguage ?: managerLanguage ?: resourceLanguage
            (systemLanguage ?: Locale.getDefault().language).equals("ar", ignoreCase = true)
        }
    }
}
