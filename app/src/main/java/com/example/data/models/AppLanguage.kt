package com.example.data.models

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
) {
    SYSTEM("system", "تلقائي (لغة الجهاز)", "System Default (Device)"),
    ENGLISH("en", "English", "English"),
    ARABIC("ar", "العربية", "Arabic (العربية)"),
    FRENCH("fr", "Français", "French (Français)"),
    TURKISH("tr", "Türkçe", "Turkish (Türkçe)"),
    URDU("ur", "اردو", "Urdu (اردو)"),
    INDONESIAN("in", "Bahasa Indonesia", "Indonesian (Bahasa)");

    fun getDisplayName(isArabic: Boolean): String {
        return if (isArabic) {
            when (this) {
                SYSTEM -> "تلقائي (لغة الجهاز)"
                ENGLISH -> "English (الإنجليزية)"
                ARABIC -> "العربية"
                FRENCH -> "Français (الفرنسية)"
                TURKISH -> "Türkçe (التركية)"
                URDU -> "اردو (الأردية)"
                INDONESIAN -> "Bahasa Indonesia (الإندونيسية)"
            }
        } else {
            englishName
        }
    }
}
