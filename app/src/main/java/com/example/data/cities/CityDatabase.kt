package com.example.data.cities

import com.example.data.models.UserLocation

/**
 * A location preset carrying both English and Arabic display names, so the searchable city list
 * shows correctly regardless of the app's language setting (previously always English, since
 * UserLocation itself has only a single name/country pair - that's still the right shape for a
 * GPS-resolved or already-selected location, just not for this bilingual picker source list).
 */
data class CityPreset(
    val nameEn: String,
    val nameAr: String,
    val countryEn: String,
    val countryAr: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
) {
    fun toUserLocation(isArabic: Boolean): UserLocation = UserLocation(
        name = if (isArabic) nameAr else nameEn,
        country = if (isArabic) countryAr else countryEn,
        latitude = latitude,
        longitude = longitude,
        timeZoneId = timeZoneId,
        isGps = false
    )
}

object CityDatabase {

    val DEFAULT_LOCATION = UserLocation(
        name = "Makkah",
        country = "Saudi Arabia",
        latitude = 21.422487,
        longitude = 39.826206,
        timeZoneId = "Asia/Riyadh",
        isGps = false
    )

    val PRESET_CITIES: List<CityPreset> = listOf(
        // Saudi Arabia
        CityPreset("Makkah", "مكة المكرمة", "Saudi Arabia", "السعودية", 21.422487, 39.826206, "Asia/Riyadh"),
        CityPreset("Madinah", "المدينة المنورة", "Saudi Arabia", "السعودية", 24.4672, 39.6111, "Asia/Riyadh"),
        CityPreset("Riyadh", "الرياض", "Saudi Arabia", "السعودية", 24.7136, 46.6753, "Asia/Riyadh"),
        CityPreset("Jeddah", "جدة", "Saudi Arabia", "السعودية", 21.5433, 39.1728, "Asia/Riyadh"),
        CityPreset("Dammam", "الدمام", "Saudi Arabia", "السعودية", 26.4207, 50.0888, "Asia/Riyadh"),

        // Middle East & North Africa
        CityPreset("Cairo", "القاهرة", "Egypt", "مصر", 30.0444, 31.2357, "Africa/Cairo"),
        CityPreset("Alexandria", "الإسكندرية", "Egypt", "مصر", 31.2001, 29.9187, "Africa/Cairo"),
        CityPreset("Dubai", "دبي", "United Arab Emirates", "الإمارات العربية المتحدة", 25.2048, 55.2708, "Asia/Dubai"),
        CityPreset("Abu Dhabi", "أبوظبي", "United Arab Emirates", "الإمارات العربية المتحدة", 24.4539, 54.3773, "Asia/Dubai"),
        CityPreset("Sharjah", "الشارقة", "United Arab Emirates", "الإمارات العربية المتحدة", 25.3463, 55.4209, "Asia/Dubai"),
        CityPreset("Doha", "الدوحة", "Qatar", "قطر", 25.2854, 51.5310, "Asia/Qatar"),
        CityPreset("Kuwait City", "مدينة الكويت", "Kuwait", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        CityPreset("Manama", "المنامة", "Bahrain", "البحرين", 26.2285, 50.5860, "Asia/Bahrain"),
        CityPreset("Muscat", "مسقط", "Oman", "عُمان", 23.5880, 58.3829, "Asia/Muscat"),
        CityPreset("Amman", "عمّان", "Jordan", "الأردن", 31.9454, 35.9284, "Asia/Amman"),
        CityPreset("Jerusalem / Al-Quds", "القدس", "Palestine", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityPreset("Beirut", "بيروت", "Lebanon", "لبنان", 33.8938, 35.5018, "Asia/Beirut"),
        CityPreset("Damascus", "دمشق", "Syria", "سوريا", 33.5138, 36.2765, "Asia/Damascus"),
        CityPreset("Baghdad", "بغداد", "Iraq", "العراق", 33.3152, 44.3661, "Asia/Baghdad"),
        CityPreset("Erbil", "أربيل", "Iraq", "العراق", 36.1901, 43.9930, "Asia/Baghdad"),
        CityPreset("Rabat", "الرباط", "Morocco", "المغرب", 34.0209, -6.8416, "Africa/Casablanca"),
        CityPreset("Casablanca", "الدار البيضاء", "Morocco", "المغرب", 33.5731, -7.5898, "Africa/Casablanca"),
        CityPreset("Marrakech", "مراكش", "Morocco", "المغرب", 31.6295, -7.9811, "Africa/Casablanca"),
        CityPreset("Algiers", "الجزائر العاصمة", "Algeria", "الجزائر", 36.7538, 3.0588, "Africa/Algiers"),
        CityPreset("Tunis", "تونس العاصمة", "Tunisia", "تونس", 36.8065, 10.1815, "Africa/Tunis"),
        CityPreset("Tripoli", "طرابلس", "Libya", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli"),
        CityPreset("Khartoum", "الخرطوم", "Sudan", "السودان", 15.5007, 32.5599, "Africa/Khartoum"),
        CityPreset("Sana'a", "صنعاء", "Yemen", "اليمن", 15.3694, 44.1910, "Asia/Aden"),

        // Turkey & Central Asia
        CityPreset("Istanbul", "إسطنبول", "Turkey", "تركيا", 41.0082, 28.9784, "Europe/Istanbul"),
        CityPreset("Ankara", "أنقرة", "Turkey", "تركيا", 39.9334, 32.8597, "Europe/Istanbul"),
        CityPreset("Izmir", "إزمير", "Turkey", "تركيا", 38.4237, 27.1428, "Europe/Istanbul"),
        CityPreset("Konya", "قونية", "Turkey", "تركيا", 37.8746, 32.4932, "Europe/Istanbul"),
        CityPreset("Bursa", "بورصة", "Turkey", "تركيا", 40.1885, 29.0610, "Europe/Istanbul"),
        CityPreset("Tehran", "طهران", "Iran", "إيران", 35.6892, 51.3890, "Asia/Tehran"),
        CityPreset("Mashhad", "مشهد", "Iran", "إيران", 36.2605, 59.6168, "Asia/Tehran"),
        CityPreset("Isfahan", "أصفهان", "Iran", "إيران", 32.6546, 51.6680, "Asia/Tehran"),
        CityPreset("Tashkent", "طشقند", "Uzbekistan", "أوزبكستان", 41.2995, 69.2401, "Asia/Tashkent"),
        CityPreset("Samarkand", "سمرقند", "Uzbekistan", "أوزبكستان", 39.6270, 66.9750, "Asia/Samarkand"),
        CityPreset("Baku", "باكو", "Azerbaijan", "أذربيجان", 40.4093, 49.8671, "Asia/Baku"),
        CityPreset("Almaty", "ألماتي", "Kazakhstan", "كازاخستان", 43.2220, 76.8512, "Asia/Almaty"),
        CityPreset("Astana", "أستانا", "Kazakhstan", "كازاخستان", 51.1694, 71.4491, "Asia/Almaty"),
        CityPreset("Bishkek", "بيشكيك", "Kyrgyzstan", "قيرغيزستان", 42.8746, 74.5698, "Asia/Bishkek"),
        CityPreset("Dushanbe", "دوشنبه", "Tajikistan", "طاجيكستان", 38.5598, 68.7870, "Asia/Dushanbe"),
        CityPreset("Ashgabat", "عشق آباد", "Turkmenistan", "تركمانستان", 37.9601, 58.3261, "Asia/Ashgabat"),

        // South Asia
        CityPreset("Karachi", "كراتشي", "Pakistan", "باكستان", 24.8607, 67.0011, "Asia/Karachi"),
        CityPreset("Lahore", "لاهور", "Pakistan", "باكستان", 31.5204, 74.3587, "Asia/Karachi"),
        CityPreset("Islamabad", "إسلام آباد", "Pakistan", "باكستان", 33.6844, 73.0479, "Asia/Karachi"),
        CityPreset("Rawalpindi", "راولبندي", "Pakistan", "باكستان", 33.5651, 73.0169, "Asia/Karachi"),
        CityPreset("Peshawar", "بيشاور", "Pakistan", "باكستان", 34.0151, 71.5249, "Asia/Karachi"),
        CityPreset("Faisalabad", "فيصل آباد", "Pakistan", "باكستان", 31.4504, 73.1350, "Asia/Karachi"),
        CityPreset("Multan", "ملتان", "Pakistan", "باكستان", 30.1575, 71.5249, "Asia/Karachi"),
        CityPreset("Dhaka", "دكا", "Bangladesh", "بنغلاديش", 23.8103, 90.4125, "Asia/Dhaka"),
        CityPreset("Chittagong", "شيتاغونغ", "Bangladesh", "بنغلاديش", 22.3569, 91.7832, "Asia/Dhaka"),
        CityPreset("Sylhet", "سيلهت", "Bangladesh", "بنغلاديش", 24.8949, 91.8687, "Asia/Dhaka"),
        CityPreset("Delhi / New Delhi", "دلهي", "India", "الهند", 28.6139, 77.2090, "Asia/Kolkata"),
        CityPreset("Mumbai", "مومباي", "India", "الهند", 19.0760, 72.8777, "Asia/Kolkata"),
        CityPreset("Hyderabad", "حيدر آباد", "India", "الهند", 17.3850, 78.4867, "Asia/Kolkata"),
        CityPreset("Bengaluru", "بنغالورو", "India", "الهند", 12.9716, 77.5946, "Asia/Kolkata"),
        CityPreset("Kolkata", "كولكاتا", "India", "الهند", 22.5726, 88.3639, "Asia/Kolkata"),
        CityPreset("Chennai", "تشيناي", "India", "الهند", 13.0827, 80.2707, "Asia/Kolkata"),
        CityPreset("Kabul", "كابل", "Afghanistan", "أفغانستان", 34.5553, 69.2075, "Asia/Kabul"),
        CityPreset("Colombo", "كولومبو", "Sri Lanka", "سريلانكا", 6.9271, 79.8612, "Asia/Colombo"),
        CityPreset("Male", "ماليه", "Maldives", "جزر المالديف", 4.1755, 73.5093, "Indian/Maldives"),

        // Southeast Asia
        CityPreset("Jakarta", "جاكرتا", "Indonesia", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta"),
        CityPreset("Surabaya", "سورابايا", "Indonesia", "إندونيسيا", -7.2575, 112.7521, "Asia/Jakarta"),
        CityPreset("Bandung", "باندونغ", "Indonesia", "إندونيسيا", -6.9175, 107.6191, "Asia/Jakarta"),
        CityPreset("Medan", "ميدان", "Indonesia", "إندونيسيا", 3.5952, 98.6722, "Asia/Jakarta"),
        CityPreset("Semarang", "سيمارانغ", "Indonesia", "إندونيسيا", -6.9667, 110.4167, "Asia/Jakarta"),
        CityPreset("Makassar", "ماكاسار", "Indonesia", "إندونيسيا", -5.1477, 119.4327, "Asia/Makassar"),
        CityPreset("Kuala Lumpur", "كوالالمبور", "Malaysia", "ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CityPreset("Penang / George Town", "بينانغ", "Malaysia", "ماليزيا", 5.4141, 100.3288, "Asia/Kuala_Lumpur"),
        CityPreset("Johor Bahru", "جوهور بهرو", "Malaysia", "ماليزيا", 1.4927, 103.7414, "Asia/Kuala_Lumpur"),
        CityPreset("Singapore", "سنغافورة", "Singapore", "سنغافورة", 1.3521, 103.8198, "Asia/Singapore"),
        CityPreset("Bandar Seri Begawan", "بندر سري بكاوان", "Brunei", "بروناي", 4.9031, 114.9398, "Asia/Brunei"),
        CityPreset("Bangkok", "بانكوك", "Thailand", "تايلاند", 13.7563, 100.5018, "Asia/Bangkok"),
        CityPreset("Manila", "مانيلا", "Philippines", "الفلبين", 14.5995, 120.9842, "Asia/Manila"),

        // Europe
        CityPreset("London", "لندن", "United Kingdom", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London"),
        CityPreset("Birmingham", "برمنغهام", "United Kingdom", "المملكة المتحدة", 52.4862, -1.8904, "Europe/London"),
        CityPreset("Manchester", "مانشستر", "United Kingdom", "المملكة المتحدة", 53.4808, -2.2426, "Europe/London"),
        CityPreset("Paris", "باريس", "France", "فرنسا", 48.8566, 2.3522, "Europe/Paris"),
        CityPreset("Marseille", "مرسيليا", "France", "فرنسا", 43.2965, 5.3698, "Europe/Paris"),
        CityPreset("Lyon", "ليون", "France", "فرنسا", 45.7640, 4.8357, "Europe/Paris"),
        CityPreset("Berlin", "برلين", "Germany", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin"),
        CityPreset("Frankfurt", "فرانكفورت", "Germany", "ألمانيا", 50.1109, 8.6821, "Europe/Berlin"),
        CityPreset("Munich", "ميونخ", "Germany", "ألمانيا", 48.1351, 11.5820, "Europe/Berlin"),
        CityPreset("Cologne", "كولونيا", "Germany", "ألمانيا", 50.9375, 6.9603, "Europe/Berlin"),
        CityPreset("Amsterdam", "أمستردام", "Netherlands", "هولندا", 52.3676, 4.9041, "Europe/Amsterdam"),
        CityPreset("Rotterdam", "روتردام", "Netherlands", "هولندا", 51.9244, 4.4777, "Europe/Amsterdam"),
        CityPreset("Brussels", "بروكسل", "Belgium", "بلجيكا", 50.8503, 4.3517, "Europe/Brussels"),
        CityPreset("Antwerp", "أنتويرب", "Belgium", "بلجيكا", 51.2194, 4.4025, "Europe/Brussels"),
        CityPreset("Vienna", "فيينا", "Austria", "النمسا", 48.2082, 16.3738, "Europe/Vienna"),
        CityPreset("Zurich", "زيورخ", "Switzerland", "سويسرا", 47.3769, 8.5417, "Europe/Zurich"),
        CityPreset("Geneva", "جنيف", "Switzerland", "سويسرا", 46.2044, 6.1432, "Europe/Zurich"),
        CityPreset("Rome", "روما", "Italy", "إيطاليا", 41.9028, 12.4964, "Europe/Rome"),
        CityPreset("Milan", "ميلانو", "Italy", "إيطاليا", 45.4642, 9.1900, "Europe/Rome"),
        CityPreset("Madrid", "مدريد", "Spain", "إسبانيا", 40.4168, -3.7038, "Europe/Madrid"),
        CityPreset("Barcelona", "برشلونة", "Spain", "إسبانيا", 41.3879, 2.1699, "Europe/Madrid"),
        CityPreset("Stockholm", "ستوكهولم", "Sweden", "السويد", 59.3293, 18.0686, "Europe/Stockholm"),
        CityPreset("Oslo", "أوسلو", "Norway", "النرويج", 59.9139, 10.7522, "Europe/Oslo"),
        CityPreset("Copenhagen", "كوبنهاغن", "Denmark", "الدانمارك", 55.6761, 12.5683, "Europe/Copenhagen"),
        CityPreset("Helsinki", "هلسنكي", "Finland", "فنلندا", 60.1699, 24.9384, "Europe/Helsinki"),
        CityPreset("Dublin", "دبلن", "Ireland", "أيرلندا", 53.3498, -6.2603, "Europe/Dublin"),
        CityPreset("Moscow", "موسكو", "Russia", "روسيا", 55.7558, 37.6173, "Europe/Moscow"),
        CityPreset("Kazan", "قازان", "Russia", "روسيا", 55.7887, 49.1221, "Europe/Moscow"),
        CityPreset("Ufa", "أوفا", "Russia", "روسيا", 54.7388, 55.9721, "Asia/Yekaterinburg"),
        CityPreset("Grozny", "غروزني", "Russia", "روسيا", 43.3169, 45.6888, "Europe/Moscow"),
        CityPreset("Makhachkala", "محج قلعة", "Russia", "روسيا", 42.9831, 47.5047, "Europe/Moscow"),
        CityPreset("Sarajevo", "سراييفو", "Bosnia and Herzegovina", "البوسنة والهرسك", 43.8563, 18.4131, "Europe/Sarajevo"),
        CityPreset("Pristina", "بريشتينا", "Kosovo", "كوسوفو", 42.6629, 21.1655, "Europe/Belgrade"),
        CityPreset("Tirana", "تيرانا", "Albania", "ألبانيا", 41.3275, 19.8187, "Europe/Tirane"),
        CityPreset("Skopje", "سكوبيه", "North Macedonia", "مقدونيا الشمالية", 41.9973, 21.4280, "Europe/Skopje"),
        CityPreset("Athens", "أثينا", "Greece", "اليونان", 37.9838, 23.7275, "Europe/Athens"),

        // North America
        CityPreset("New York", "نيويورك", "United States", "الولايات المتحدة", 40.7128, -74.0060, "America/New_York"),
        CityPreset("Los Angeles", "لوس أنجلوس", "United States", "الولايات المتحدة", 34.0522, -118.2437, "America/Los_Angeles"),
        CityPreset("Chicago", "شيكاغو", "United States", "الولايات المتحدة", 41.8781, -87.6298, "America/Chicago"),
        CityPreset("Houston", "هيوستن", "United States", "الولايات المتحدة", 29.7604, -95.3698, "America/Chicago"),
        CityPreset("Dallas", "دالاس", "United States", "الولايات المتحدة", 32.7767, -96.7970, "America/Chicago"),
        CityPreset("Philadelphia", "فيلادلفيا", "United States", "الولايات المتحدة", 39.9526, -75.1652, "America/New_York"),
        CityPreset("Phoenix", "فينيكس", "United States", "الولايات المتحدة", 33.4484, -112.0740, "America/Phoenix"),
        CityPreset("San Francisco", "سان فرانسيسكو", "United States", "الولايات المتحدة", 37.7749, -122.4194, "America/Los_Angeles"),
        CityPreset("Seattle", "سياتل", "United States", "الولايات المتحدة", 47.6062, -122.3321, "America/Los_Angeles"),
        CityPreset("Detroit / Dearborn", "ديترويت", "United States", "الولايات المتحدة", 42.3314, -83.0458, "America/Detroit"),
        CityPreset("Washington, D.C.", "واشنطن العاصمة", "United States", "الولايات المتحدة", 38.9072, -77.0369, "America/New_York"),
        CityPreset("Atlanta", "أتلانتا", "United States", "الولايات المتحدة", 33.7490, -84.3880, "America/New_York"),
        CityPreset("Miami", "ميامي", "United States", "الولايات المتحدة", 25.7617, -80.1918, "America/New_York"),
        CityPreset("Toronto", "تورونتو", "Canada", "كندا", 43.6532, -79.3832, "America/Toronto"),
        CityPreset("Montreal", "مونتريال", "Canada", "كندا", 45.5017, -73.5673, "America/Toronto"),
        CityPreset("Vancouver", "فانكوفر", "Canada", "كندا", 49.2827, -123.1207, "America/Vancouver"),
        CityPreset("Calgary", "كالغاري", "Canada", "كندا", 51.0447, -114.0719, "America/Edmonton"),
        CityPreset("Ottawa", "أوتاوا", "Canada", "كندا", 45.4215, -75.6972, "America/Toronto"),
        CityPreset("Edmonton", "إدمنتون", "Canada", "كندا", 53.5461, -113.4938, "America/Edmonton"),

        // Africa (Sub-Saharan)
        CityPreset("Lagos", "لاغوس", "Nigeria", "نيجيريا", 6.5244, 3.3792, "Africa/Lagos"),
        CityPreset("Abuja", "أبوجا", "Nigeria", "نيجيريا", 9.0765, 7.3986, "Africa/Lagos"),
        CityPreset("Kano", "كانو", "Nigeria", "نيجيريا", 12.0022, 8.5920, "Africa/Lagos"),
        CityPreset("Nairobi", "نيروبي", "Kenya", "كينيا", -1.2921, 36.8219, "Africa/Nairobi"),
        CityPreset("Mombasa", "مومباسا", "Kenya", "كينيا", -4.0435, 39.6682, "Africa/Nairobi"),
        CityPreset("Mogadishu", "مقديشو", "Somalia", "الصومال", 2.0469, 45.3182, "Africa/Mogadishu"),
        CityPreset("Hargeisa", "هرجيسا", "Somalia", "الصومال", 9.5600, 44.0650, "Africa/Mogadishu"),
        CityPreset("Addis Ababa", "أديس أبابا", "Ethiopia", "إثيوبيا", 9.0300, 38.7400, "Africa/Addis_Ababa"),
        CityPreset("Dar es Salaam", "دار السلام", "Tanzania", "تنزانيا", -6.7924, 39.2083, "Africa/Dar_es_Salaam"),
        CityPreset("Zanzibar", "زنجبار", "Tanzania", "تنزانيا", -6.1659, 39.2026, "Africa/Dar_es_Salaam"),
        CityPreset("Kampala", "كمبالا", "Uganda", "أوغندا", 0.3476, 32.5825, "Africa/Kampala"),
        CityPreset("Dakar", "داكار", "Senegal", "السنغال", 14.7167, -17.4677, "Africa/Dakar"),
        CityPreset("Bamako", "باماكو", "Mali", "مالي", 12.6392, -8.0029, "Africa/Bamako"),
        CityPreset("Niamey", "نيامي", "Niger", "النيجر", 13.5116, 2.1254, "Africa/Niamey"),
        CityPreset("Johannesburg", "جوهانسبرغ", "South Africa", "جنوب أفريقيا", -26.2041, 28.0473, "Africa/Johannesburg"),
        CityPreset("Cape Town", "كيب تاون", "South Africa", "جنوب أفريقيا", -33.9249, 18.4241, "Africa/Johannesburg"),
        CityPreset("Durban", "ديربان", "South Africa", "جنوب أفريقيا", -29.8587, 31.0218, "Africa/Johannesburg"),

        // Oceania, East Asia & Latin America
        CityPreset("Sydney", "سيدني", "Australia", "أستراليا", -33.8688, 151.2093, "Australia/Sydney"),
        CityPreset("Melbourne", "ملبورن", "Australia", "أستراليا", -37.8136, 144.9631, "Australia/Melbourne"),
        CityPreset("Brisbane", "بريسبن", "Australia", "أستراليا", -27.4698, 153.0251, "Australia/Brisbane"),
        CityPreset("Perth", "بيرث", "Australia", "أستراليا", -31.9505, 115.8605, "Australia/Perth"),
        CityPreset("Auckland", "أوكلاند", "New Zealand", "نيوزيلندا", -36.8485, 174.7633, "Pacific/Auckland"),
        CityPreset("Tokyo", "طوكيو", "Japan", "اليابان", 35.6762, 139.6503, "Asia/Tokyo"),
        CityPreset("Seoul", "سيول", "South Korea", "كوريا الجنوبية", 37.5665, 126.9780, "Asia/Seoul"),
        CityPreset("Beijing", "بكين", "China", "الصين", 39.9042, 116.4074, "Asia/Shanghai"),
        CityPreset("Urumqi", "أورومتشي", "China", "الصين", 43.8256, 87.6168, "Asia/Urumqi"),
        CityPreset("Hong Kong", "هونغ كونغ", "Hong Kong", "هونغ كونغ", 22.3193, 114.1694, "Asia/Hong_Kong"),
        CityPreset("Taipei", "تايبيه", "Taiwan", "تايوان", 25.0330, 121.5654, "Asia/Taipei"),
        CityPreset("Buenos Aires", "بوينس آيرس", "Argentina", "الأرجنتين", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        CityPreset("São Paulo", "ساو باولو", "Brazil", "البرازيل", -23.5505, -46.6333, "America/Sao_Paulo"),
        CityPreset("Mexico City", "مكسيكو سيتي", "Mexico", "المكسيك", 19.4326, -99.1332, "America/Mexico_City")
    )

    val popularCities: List<CityPreset> get() = PRESET_CITIES

    /**
     * Preset cities are stored with fixed, exact lat/lon literals, so an exact match here safely
     * identifies "this location came from picking a preset" without accidentally matching a
     * GPS/geocoded location that happens to be nearby but carries far more decimal precision.
     */
    private fun findPresetByCoordinates(lat: Double, lon: Double): CityPreset? =
        PRESET_CITIES.firstOrNull { it.latitude == lat && it.longitude == lon }

    /**
     * A location selected from the preset list should always display in the *current* app
     * language, not whichever language was active when it was picked - otherwise switching the
     * app's language leaves the saved city name stuck until the user manually reselects it. GPS
     * or manually-entered locations have no second-language variant to fall back to, so they
     * keep showing whatever the geocoder/user originally provided.
     */
    fun localizedName(location: UserLocation, isArabic: Boolean): String {
        val preset = findPresetByCoordinates(location.latitude, location.longitude)
        return if (preset != null) (if (isArabic) preset.nameAr else preset.nameEn) else location.name
    }

    fun localizedCountry(location: UserLocation, isArabic: Boolean): String {
        val preset = findPresetByCoordinates(location.latitude, location.longitude)
        return if (preset != null) (if (isArabic) preset.countryAr else preset.countryEn) else location.country
    }

    fun searchCities(query: String): List<CityPreset> {
        val q = query.trim().lowercase()
        return PRESET_CITIES.filter {
            it.nameEn.lowercase().contains(q) || it.countryEn.lowercase().contains(q) ||
                it.nameAr.contains(query.trim()) || it.countryAr.contains(query.trim())
        }
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun findNearestCity(lat: Double, lon: Double): Pair<CityPreset, Double>? {
        if (PRESET_CITIES.isEmpty()) return null
        return PRESET_CITIES.map { city ->
            city to calculateDistanceKm(lat, lon, city.latitude, city.longitude)
        }.minByOrNull { it.second }
    }

    fun estimateTimeZone(lat: Double, lon: Double): String {
        val nearest = findNearestCity(lat, lon)
        return nearest?.first?.timeZoneId ?: java.time.ZoneId.systemDefault().id
    }
}
