package com.example.data.models

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
}
