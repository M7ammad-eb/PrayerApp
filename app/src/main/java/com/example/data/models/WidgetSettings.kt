package com.example.data.models

enum class WidgetThemeMode(
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String,
    val previewBgColor: Long,
    val previewAccentColor: Long,
    val previewTextColor: Long
) {
    APP_THEME(
        titleEn = "Match App Theme",
        titleAr = "مماثل لمظهر التطبيق",
        descEn = "Automatically adopts your app's current theme & palette",
        descAr = "يتطابق تلقائياً مع السمة والألوان المختارة في التطبيق",
        previewBgColor = 0xFF1E293B,
        previewAccentColor = 0xFF10B981,
        previewTextColor = 0xFFFFFFFF
    ),
    MATERIAL_YOU(
        titleEn = "Material You Dynamic",
        titleAr = "ألوان النظام التلقائية",
        descEn = "Uses Android 12+ dynamic wallpaper colors",
        descAr = "يستخرج الألوان ديناميكياً من خلفية شاشة جهازك",
        previewBgColor = 0xFF2A3439,
        previewAccentColor = 0xFF7DD3FC,
        previewTextColor = 0xFFFFFFFF
    ),
    DARK_ELEGANT(
        titleEn = "Dark Charcoal & Emerald",
        titleAr = "داكن أنيق مع زمرد",
        descEn = "Deep charcoal glass with luminous emerald highlights",
        descAr = "خلفية فحمية داكنة مع لمسات زمردية مضيئة",
        previewBgColor = 0xFF121820,
        previewAccentColor = 0xFF10B981,
        previewTextColor = 0xFFF1F5F9
    ),
    LIGHT_CLEAN(
        titleEn = "Pristine Light",
        titleAr = "فاتح ناصع",
        descEn = "Clean bright card with crisp typography",
        descAr = "بطاقة بيضاء نقية مع خطوط عالية التباين",
        previewBgColor = 0xFFF8FAFC,
        previewAccentColor = 0xFF059669,
        previewTextColor = 0xFF0F172A
    ),
    OLED_BLACK(
        titleEn = "AMOLED Pitch Black",
        titleAr = "أسود داكن عميق (OLED)",
        descEn = "True #000000 background for AMOLED battery savings",
        descAr = "أسود نقي تماماً لتوفير طاقة شاشات أموليد",
        previewBgColor = 0xFF000000,
        previewAccentColor = 0xFF34D399,
        previewTextColor = 0xFFFFFFFF
    ),
    EMERALD_ISLAMIC(
        titleEn = "Islamic Emerald",
        titleAr = "الزمرد الإسلامي",
        descEn = "Rich traditional green gradient with gold accents",
        descAr = "أخضر زمردي إسلامي عريق مع لمسات ذهبية",
        previewBgColor = 0xFF064E3B,
        previewAccentColor = 0xFFFBBF24,
        previewTextColor = 0xFFECFDF5
    ),
    GOLDEN_HOUR(
        titleEn = "Desert Amber & Gold",
        titleAr = "الذهب والكهرمان الصحراوي",
        descEn = "Warm sunset ochre and golden tones",
        descAr = "ألوان ذهبية وعنبرية دافئة ومميزة",
        previewBgColor = 0xFF451A03,
        previewAccentColor = 0xFFF59E0B,
        previewTextColor = 0xFFFEF3C7
    ),
    ROYAL_BLUE(
        titleEn = "Midnight Sapphire",
        titleAr = "الياقوت الأزرق الليلي",
        descEn = "Deep ocean navy with sky blue highlights",
        descAr = "أزرق كحلي ملكي هادئ مع تفاصيل سماوية",
        previewBgColor = 0xFF0F172A,
        previewAccentColor = 0xFF38BDF8,
        previewTextColor = 0xFFF0F9FF
    ),
    MONOCHROME(
        titleEn = "Minimal Monochrome",
        titleAr = "أحادي اللون مبسط",
        descEn = "Clean grayscale minimalist aesthetic",
        descAr = "تصميم رمادي وأبيض مبسط ومريح للعين",
        previewBgColor = 0xFF18181B,
        previewAccentColor = 0xFFE4E4E7,
        previewTextColor = 0xFFFAFAFA
    )
}

enum class WidgetBackgroundStyle(
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String
) {
    TRANSLUCENT(
        titleEn = "Translucent Glass",
        titleAr = "زجاجي شبه شفاف",
        descEn = "Modern frosted translucency blending with wallpaper",
        descAr = "بطاقة شبه شفافة ناعمة تمتزج بانسيابية مع خلفية الشاشة"
    ),
    SOLID_SURFACE(
        titleEn = "Solid Surface",
        titleAr = "معتم مصمت",
        descEn = "Opaque solid background with highest readability",
        descAr = "خلفية معتمة غير شفافة لأعلى درجات الوضوح والتباين"
    ),
    FROSTED_GLASS(
        titleEn = "Deep Blur Glass",
        titleAr = "زجاج ضبابي عميق",
        descEn = "Soft blurred tinted glass container",
        descAr = "تأثير زجاجي مضلل مع إطار خفيف جذاب"
    ),
    MINIMAL_BORDER(
        titleEn = "Border Outline Only",
        titleAr = "إطار خارجي فقط",
        descEn = "Transparent background with a subtle crisp outline",
        descAr = "خلفية شفافة تماماً مع إطار خارجي رفيع وأنيق"
    ),
    TRANSPARENT_CLEAN(
        titleEn = "Fully Transparent (Floating)",
        titleAr = "شفاف تماماً (عائم)",
        descEn = "Zero background, prayer times float directly on wallpaper",
        descAr = "بدون خلفية إطلاقاً، تظهر الأوقات عائمة فوق خلفية الشاشة"
    )
}

enum class WidgetFontSize(
    val titleEn: String,
    val titleAr: String,
    val scaleFactor: Float
) {
    COMPACT(
        titleEn = "Compact",
        titleAr = "مضغوط وصغير",
        scaleFactor = 0.88f
    ),
    STANDARD(
        titleEn = "Standard",
        titleAr = "متوسط قياسي",
        scaleFactor = 1.0f
    ),
    LARGE(
        titleEn = "Large",
        titleAr = "كبير وواضح",
        scaleFactor = 1.15f
    ),
    EXTRA_LARGE(
        titleEn = "Extra Large",
        titleAr = "كبير جداً",
        scaleFactor = 1.30f
    )
}

enum class WidgetTextStyle(
    val titleEn: String,
    val titleAr: String
) {
    AUTO(
        titleEn = "Auto (Theme Default)",
        titleAr = "تلقائي (حسب السمة)"
    ),
    LIGHT(
        titleEn = "Always Light Text",
        titleAr = "نص فاتح دائماً"
    ),
    DARK(
        titleEn = "Always Dark Text",
        titleAr = "نص داكن دائماً"
    )
}

data class WidgetCustomizationSettings(
    val themeMode: WidgetThemeMode = WidgetThemeMode.APP_THEME,
    val bgStyle: WidgetBackgroundStyle = WidgetBackgroundStyle.TRANSLUCENT,
    val opacityPercent: Int = 85,
    val fontSize: WidgetFontSize = WidgetFontSize.STANDARD,
    val textStyle: WidgetTextStyle = WidgetTextStyle.AUTO,
    val showLocation: Boolean = true,
    val showHijriDate: Boolean = true,
    val showCountdown: Boolean = true,
    val showProgressBar: Boolean = true,
    val showSunrise: Boolean = true,
    val showAllPrayersList: Boolean = true,
    val showHeroCard: Boolean = true
)
