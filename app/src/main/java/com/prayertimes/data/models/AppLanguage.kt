package com.prayertimes.data.models

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
    fun resolveIsArabic(): Boolean = when (this) {
        ARABIC -> true
        ENGLISH -> false
        SYSTEM -> Locale.getDefault().language.equals("ar", ignoreCase = true)
    }
}
