package com.example.data.cities

import com.example.data.models.UserLocation

object CityDatabase {

    val DEFAULT_LOCATION = UserLocation(
        name = "Makkah",
        country = "Saudi Arabia",
        latitude = 21.422487,
        longitude = 39.826206,
        timeZoneId = "Asia/Riyadh",
        isGps = false
    )

    val PRESET_CITIES: List<UserLocation> = listOf(
        // Saudi Arabia
        UserLocation("Makkah", "Saudi Arabia", 21.422487, 39.826206, "Asia/Riyadh"),
        UserLocation("Madinah", "Saudi Arabia", 24.4672, 39.6111, "Asia/Riyadh"),
        UserLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh"),
        UserLocation("Jeddah", "Saudi Arabia", 21.5433, 39.1728, "Asia/Riyadh"),
        UserLocation("Dammam", "Saudi Arabia", 26.4207, 50.0888, "Asia/Riyadh"),

        // Middle East & North Africa
        UserLocation("Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo"),
        UserLocation("Alexandria", "Egypt", 31.2001, 29.9187, "Africa/Cairo"),
        UserLocation("Dubai", "United Arab Emirates", 25.2048, 55.2708, "Asia/Dubai"),
        UserLocation("Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773, "Asia/Dubai"),
        UserLocation("Sharjah", "United Arab Emirates", 25.3463, 55.4209, "Asia/Dubai"),
        UserLocation("Doha", "Qatar", 25.2854, 51.5310, "Asia/Qatar"),
        UserLocation("Kuwait City", "Kuwait", 29.3759, 47.9774, "Asia/Kuwait"),
        UserLocation("Manama", "Bahrain", 26.2285, 50.5860, "Asia/Bahrain"),
        UserLocation("Muscat", "Oman", 23.5880, 58.3829, "Asia/Muscat"),
        UserLocation("Amman", "Jordan", 31.9454, 35.9284, "Asia/Amman"),
        UserLocation("Jerusalem / Al-Quds", "Palestine", 31.7683, 35.2137, "Asia/Jerusalem"),
        UserLocation("Beirut", "Lebanon", 33.8938, 35.5018, "Asia/Beirut"),
        UserLocation("Damascus", "Syria", 33.5138, 36.2765, "Asia/Damascus"),
        UserLocation("Baghdad", "Iraq", 33.3152, 44.3661, "Asia/Baghdad"),
        UserLocation("Erbil", "Iraq", 36.1901, 43.9930, "Asia/Baghdad"),
        UserLocation("Rabat", "Morocco", 34.0209, -6.8416, "Africa/Casablanca"),
        UserLocation("Casablanca", "Morocco", 33.5731, -7.5898, "Africa/Casablanca"),
        UserLocation("Marrakech", "Morocco", 31.6295, -7.9811, "Africa/Casablanca"),
        UserLocation("Algiers", "Algeria", 36.7538, 3.0588, "Africa/Algiers"),
        UserLocation("Tunis", "Tunisia", 36.8065, 10.1815, "Africa/Tunis"),
        UserLocation("Tripoli", "Libya", 32.8872, 13.1913, "Africa/Tripoli"),
        UserLocation("Khartoum", "Sudan", 15.5007, 32.5599, "Africa/Khartoum"),
        UserLocation("Sana'a", "Yemen", 15.3694, 44.1910, "Asia/Aden"),

        // Turkey & Central Asia
        UserLocation("Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul"),
        UserLocation("Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul"),
        UserLocation("Izmir", "Turkey", 38.4237, 27.1428, "Europe/Istanbul"),
        UserLocation("Konya", "Turkey", 37.8746, 32.4932, "Europe/Istanbul"),
        UserLocation("Bursa", "Turkey", 40.1885, 29.0610, "Europe/Istanbul"),
        UserLocation("Tehran", "Iran", 35.6892, 51.3890, "Asia/Tehran"),
        UserLocation("Mashhad", "Iran", 36.2605, 59.6168, "Asia/Tehran"),
        UserLocation("Isfahan", "Iran", 32.6546, 51.6680, "Asia/Tehran"),
        UserLocation("Tashkent", "Uzbekistan", 41.2995, 69.2401, "Asia/Tashkent"),
        UserLocation("Samarkand", "Uzbekistan", 39.6270, 66.9750, "Asia/Samarkand"),
        UserLocation("Baku", "Azerbaijan", 40.4093, 49.8671, "Asia/Baku"),
        UserLocation("Almaty", "Kazakhstan", 43.2220, 76.8512, "Asia/Almaty"),
        UserLocation("Astana", "Kazakhstan", 51.1694, 71.4491, "Asia/Almaty"),
        UserLocation("Bishkek", "Kyrgyzstan", 42.8746, 74.5698, "Asia/Bishkek"),
        UserLocation("Dushanbe", "Tajikistan", 38.5598, 68.7870, "Asia/Dushanbe"),
        UserLocation("Ashgabat", "Turkmenistan", 37.9601, 58.3261, "Asia/Ashgabat"),

        // South Asia
        UserLocation("Karachi", "Pakistan", 24.8607, 67.0011, "Asia/Karachi"),
        UserLocation("Lahore", "Pakistan", 31.5204, 74.3587, "Asia/Karachi"),
        UserLocation("Islamabad", "Pakistan", 33.6844, 73.0479, "Asia/Karachi"),
        UserLocation("Rawalpindi", "Pakistan", 33.5651, 73.0169, "Asia/Karachi"),
        UserLocation("Peshawar", "Pakistan", 34.0151, 71.5249, "Asia/Karachi"),
        UserLocation("Faisalabad", "Pakistan", 31.4504, 73.1350, "Asia/Karachi"),
        UserLocation("Multan", "Pakistan", 30.1575, 71.5249, "Asia/Karachi"),
        UserLocation("Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka"),
        UserLocation("Chittagong", "Bangladesh", 22.3569, 91.7832, "Asia/Dhaka"),
        UserLocation("Sylhet", "Bangladesh", 24.8949, 91.8687, "Asia/Dhaka"),
        UserLocation("Delhi / New Delhi", "India", 28.6139, 77.2090, "Asia/Kolkata"),
        UserLocation("Mumbai", "India", 19.0760, 72.8777, "Asia/Kolkata"),
        UserLocation("Hyderabad", "India", 17.3850, 78.4867, "Asia/Kolkata"),
        UserLocation("Bengaluru", "India", 12.9716, 77.5946, "Asia/Kolkata"),
        UserLocation("Kolkata", "India", 22.5726, 88.3639, "Asia/Kolkata"),
        UserLocation("Chennai", "India", 13.0827, 80.2707, "Asia/Kolkata"),
        UserLocation("Kabul", "Afghanistan", 34.5553, 69.2075, "Asia/Kabul"),
        UserLocation("Colombo", "Sri Lanka", 6.9271, 79.8612, "Asia/Colombo"),
        UserLocation("Male", "Maldives", 4.1755, 73.5093, "Indian/Maldives"),

        // Southeast Asia
        UserLocation("Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta"),
        UserLocation("Surabaya", "Indonesia", -7.2575, 112.7521, "Asia/Jakarta"),
        UserLocation("Bandung", "Indonesia", -6.9175, 107.6191, "Asia/Jakarta"),
        UserLocation("Medan", "Indonesia", 3.5952, 98.6722, "Asia/Jakarta"),
        UserLocation("Semarang", "Indonesia", -6.9667, 110.4167, "Asia/Jakarta"),
        UserLocation("Makassar", "Indonesia", -5.1477, 119.4327, "Asia/Makassar"),
        UserLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        UserLocation("Penang / George Town", "Malaysia", 5.4141, 100.3288, "Asia/Kuala_Lumpur"),
        UserLocation("Johor Bahru", "Malaysia", 1.4927, 103.7414, "Asia/Kuala_Lumpur"),
        UserLocation("Singapore", "Singapore", 1.3521, 103.8198, "Asia/Singapore"),
        UserLocation("Bandar Seri Begawan", "Brunei", 4.9031, 114.9398, "Asia/Brunei"),
        UserLocation("Bangkok", "Thailand", 13.7563, 100.5018, "Asia/Bangkok"),
        UserLocation("Manila", "Philippines", 14.5995, 120.9842, "Asia/Manila"),

        // Europe
        UserLocation("London", "United Kingdom", 51.5074, -0.1278, "Europe/London"),
        UserLocation("Birmingham", "United Kingdom", 52.4862, -1.8904, "Europe/London"),
        UserLocation("Manchester", "United Kingdom", 53.4808, -2.2426, "Europe/London"),
        UserLocation("Paris", "France", 48.8566, 2.3522, "Europe/Paris"),
        UserLocation("Marseille", "France", 43.2965, 5.3698, "Europe/Paris"),
        UserLocation("Lyon", "France", 45.7640, 4.8357, "Europe/Paris"),
        UserLocation("Berlin", "Germany", 52.5200, 13.4050, "Europe/Berlin"),
        UserLocation("Frankfurt", "Germany", 50.1109, 8.6821, "Europe/Berlin"),
        UserLocation("Munich", "Germany", 48.1351, 11.5820, "Europe/Berlin"),
        UserLocation("Cologne", "Germany", 50.9375, 6.9603, "Europe/Berlin"),
        UserLocation("Amsterdam", "Netherlands", 52.3676, 4.9041, "Europe/Amsterdam"),
        UserLocation("Rotterdam", "Netherlands", 51.9244, 4.4777, "Europe/Amsterdam"),
        UserLocation("Brussels", "Belgium", 50.8503, 4.3517, "Europe/Brussels"),
        UserLocation("Antwerp", "Belgium", 51.2194, 4.4025, "Europe/Brussels"),
        UserLocation("Vienna", "Austria", 48.2082, 16.3738, "Europe/Vienna"),
        UserLocation("Zurich", "Switzerland", 47.3769, 8.5417, "Europe/Zurich"),
        UserLocation("Geneva", "Switzerland", 46.2044, 6.1432, "Europe/Zurich"),
        UserLocation("Rome", "Italy", 41.9028, 12.4964, "Europe/Rome"),
        UserLocation("Milan", "Italy", 45.4642, 9.1900, "Europe/Rome"),
        UserLocation("Madrid", "Spain", 40.4168, -3.7038, "Europe/Madrid"),
        UserLocation("Barcelona", "Spain", 41.3879, 2.1699, "Europe/Madrid"),
        UserLocation("Stockholm", "Sweden", 59.3293, 18.0686, "Europe/Stockholm"),
        UserLocation("Oslo", "Norway", 59.9139, 10.7522, "Europe/Oslo"),
        UserLocation("Copenhagen", "Denmark", 55.6761, 12.5683, "Europe/Copenhagen"),
        UserLocation("Helsinki", "Finland", 60.1699, 24.9384, "Europe/Helsinki"),
        UserLocation("Dublin", "Ireland", 53.3498, -6.2603, "Europe/Dublin"),
        UserLocation("Moscow", "Russia", 55.7558, 37.6173, "Europe/Moscow"),
        UserLocation("Kazan", "Russia", 55.7887, 49.1221, "Europe/Moscow"),
        UserLocation("Ufa", "Russia", 54.7388, 55.9721, "Asia/Yekaterinburg"),
        UserLocation("Grozny", "Russia", 43.3169, 45.6888, "Europe/Moscow"),
        UserLocation("Makhachkala", "Russia", 42.9831, 47.5047, "Europe/Moscow"),
        UserLocation("Sarajevo", "Bosnia and Herzegovina", 43.8563, 18.4131, "Europe/Sarajevo"),
        UserLocation("Pristina", "Kosovo", 42.6629, 21.1655, "Europe/Belgrade"),
        UserLocation("Tirana", "Albania", 41.3275, 19.8187, "Europe/Tirane"),
        UserLocation("Skopje", "North Macedonia", 41.9973, 21.4280, "Europe/Skopje"),
        UserLocation("Athens", "Greece", 37.9838, 23.7275, "Europe/Athens"),

        // North America
        UserLocation("New York", "United States", 40.7128, -74.0060, "America/New_York"),
        UserLocation("Los Angeles", "United States", 34.0522, -118.2437, "America/Los_Angeles"),
        UserLocation("Chicago", "United States", 41.8781, -87.6298, "America/Chicago"),
        UserLocation("Houston", "United States", 29.7604, -95.3698, "America/Chicago"),
        UserLocation("Dallas", "United States", 32.7767, -96.7970, "America/Chicago"),
        UserLocation("Philadelphia", "United States", 39.9526, -75.1652, "America/New_York"),
        UserLocation("Phoenix", "United States", 33.4484, -112.0740, "America/Phoenix"),
        UserLocation("San Francisco", "United States", 37.7749, -122.4194, "America/Los_Angeles"),
        UserLocation("Seattle", "United States", 47.6062, -122.3321, "America/Los_Angeles"),
        UserLocation("Detroit / Dearborn", "United States", 42.3314, -83.0458, "America/Detroit"),
        UserLocation("Washington, D.C.", "United States", 38.9072, -77.0369, "America/New_York"),
        UserLocation("Atlanta", "United States", 33.7490, -84.3880, "America/New_York"),
        UserLocation("Miami", "United States", 25.7617, -80.1918, "America/New_York"),
        UserLocation("Toronto", "Canada", 43.6532, -79.3832, "America/Toronto"),
        UserLocation("Montreal", "Canada", 45.5017, -73.5673, "America/Toronto"),
        UserLocation("Vancouver", "Canada", 49.2827, -123.1207, "America/Vancouver"),
        UserLocation("Calgary", "Canada", 51.0447, -114.0719, "America/Edmonton"),
        UserLocation("Ottawa", "Canada", 45.4215, -75.6972, "America/Toronto"),
        UserLocation("Edmonton", "Canada", 53.5461, -113.4938, "America/Edmonton"),

        // Africa (Sub-Saharan)
        UserLocation("Lagos", "Nigeria", 6.5244, 3.3792, "Africa/Lagos"),
        UserLocation("Abuja", "Nigeria", 9.0765, 7.3986, "Africa/Lagos"),
        UserLocation("Kano", "Nigeria", 12.0022, 8.5920, "Africa/Lagos"),
        UserLocation("Nairobi", "Kenya", -1.2921, 36.8219, "Africa/Nairobi"),
        UserLocation("Mombasa", "Kenya", -4.0435, 39.6682, "Africa/Nairobi"),
        UserLocation("Mogadishu", "Somalia", 2.0469, 45.3182, "Africa/Mogadishu"),
        UserLocation("Hargeisa", "Somalia", 9.5600, 44.0650, "Africa/Mogadishu"),
        UserLocation("Addis Ababa", "Ethiopia", 9.0300, 38.7400, "Africa/Addis_Ababa"),
        UserLocation("Dar es Salaam", "Tanzania", -6.7924, 39.2083, "Africa/Dar_es_Salaam"),
        UserLocation("Zanzibar", "Tanzania", -6.1659, 39.2026, "Africa/Dar_es_Salaam"),
        UserLocation("Kampala", "Uganda", 0.3476, 32.5825, "Africa/Kampala"),
        UserLocation("Dakar", "Senegal", 14.7167, -17.4677, "Africa/Dakar"),
        UserLocation("Bamako", "Mali", 12.6392, -8.0029, "Africa/Bamako"),
        UserLocation("Niamey", "Niger", 13.5116, 2.1254, "Africa/Niamey"),
        UserLocation("Johannesburg", "South Africa", -26.2041, 28.0473, "Africa/Johannesburg"),
        UserLocation("Cape Town", "South Africa", -33.9249, 18.4241, "Africa/Johannesburg"),
        UserLocation("Durban", "South Africa", -29.8587, 31.0218, "Africa/Johannesburg"),

        // Oceania, East Asia & Latin America
        UserLocation("Sydney", "Australia", -33.8688, 151.2093, "Australia/Sydney"),
        UserLocation("Melbourne", "Australia", -37.8136, 144.9631, "Australia/Melbourne"),
        UserLocation("Brisbane", "Australia", -27.4698, 153.0251, "Australia/Brisbane"),
        UserLocation("Perth", "Australia", -31.9505, 115.8605, "Australia/Perth"),
        UserLocation("Auckland", "New Zealand", -36.8485, 174.7633, "Pacific/Auckland"),
        UserLocation("Tokyo", "Japan", 35.6762, 139.6503, "Asia/Tokyo"),
        UserLocation("Seoul", "South Korea", 37.5665, 126.9780, "Asia/Seoul"),
        UserLocation("Beijing", "China", 39.9042, 116.4074, "Asia/Shanghai"),
        UserLocation("Urumqi", "China", 43.8256, 87.6168, "Asia/Urumqi"),
        UserLocation("Hong Kong", "Hong Kong", 22.3193, 114.1694, "Asia/Hong_Kong"),
        UserLocation("Taipei", "Taiwan", 25.0330, 121.5654, "Asia/Taipei"),
        UserLocation("Buenos Aires", "Argentina", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        UserLocation("São Paulo", "Brazil", -23.5505, -46.6333, "America/Sao_Paulo"),
        UserLocation("Mexico City", "Mexico", 19.4326, -99.1332, "America/Mexico_City")
    )

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

    fun findNearestCity(lat: Double, lon: Double): Pair<UserLocation, Double>? {
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
