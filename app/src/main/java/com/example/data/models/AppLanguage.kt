package com.example.data.models

enum class AppLanguage(val code: String, val displayNameEn: String, val displayNameAr: String) {
    SYSTEM("system", "System Default (Match Phone)", "تلقائي (لغة الهاتف)"),
    ENGLISH("en", "English", "الإنجليزية (English)"),
    ARABIC("ar", "Arabic (العربية)", "العربية")
}
