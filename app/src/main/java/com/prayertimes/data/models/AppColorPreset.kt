package com.prayertimes.data.models

enum class AppColorPreset(
    val title: String,
    val arabicTitle: String,
    val primaryLight: Long,
    val primaryDark: Long,
    val secondaryLight: Long,
    val secondaryDark: Long,
    val previewColor: Long
) {
    SYSTEM_DYNAMIC(
        title = "System Dynamic (Material You)",
        arabicTitle = "ألوان النظام التلقائية (ماتيريال يو)",
        primaryLight = 0xFF1B5E20,
        primaryDark = 0xFF81C784,
        secondaryLight = 0xFFB8860B,
        secondaryDark = 0xFFE6C687,
        previewColor = 0xFF4CAF50
    ),
    EMERALD_GOLD(
        title = "Emerald & Gold",
        arabicTitle = "الزمرد والذهب (التقليدي)",
        primaryLight = 0xFF165B33,
        primaryDark = 0xFF4ADE80,
        secondaryLight = 0xFFC59B27,
        secondaryDark = 0xFFF3D27C,
        previewColor = 0xFF165B33
    ),
    ROYAL_AMBER(
        title = "Desert Amber",
        arabicTitle = "العنبر الصحراوي",
        primaryLight = 0xFFB45309,
        primaryDark = 0xFFFBBF24,
        secondaryLight = 0xFFD97706,
        secondaryDark = 0xFFFCD34D,
        previewColor = 0xFFD97706
    ),
    SAPPHIRE_NIGHT(
        title = "Midnight Sapphire",
        arabicTitle = "الياقوت الأزرق الليلي",
        primaryLight = 0xFF1E40AF,
        primaryDark = 0xFF60A5FA,
        secondaryLight = 0xFF3B82F6,
        secondaryDark = 0xFF93C5FD,
        previewColor = 0xFF1E40AF
    ),
    MEDINA_TEAL(
        title = "Medina Teal",
        arabicTitle = "الأخضر الفيروزي النبوي",
        primaryLight = 0xFF0F766E,
        primaryDark = 0xFF2DD4BF,
        secondaryLight = 0xFF14B8A6,
        secondaryDark = 0xFF5EEAD4,
        previewColor = 0xFF0F766E
    ),
    ROSE_CLOVE(
        title = "Damascus Rose",
        arabicTitle = "الورد الدمشقي",
        primaryLight = 0xFF831843,
        primaryDark = 0xFFF472B6,
        secondaryLight = 0xFFBE185D,
        secondaryDark = 0xFFFBCFE8,
        previewColor = 0xFF9D174D
    ),
    SLATE_CHARCOAL(
        title = "Minimal Slate",
        arabicTitle = "الرمادي الأنيق",
        primaryLight = 0xFF334155,
        primaryDark = 0xFF94A3B8,
        secondaryLight = 0xFF475569,
        secondaryDark = 0xFFCBD5E1,
        previewColor = 0xFF334155
    )
}
