package com.example.data.cities

import android.content.res.Resources
import androidx.annotation.StringRes
import com.example.R
import com.example.data.models.UserLocation

/**
 * A location preset whose display name/country are Android string resources, so the searchable
 * city list shows correctly regardless of the app's language setting without needing a bilingual
 * data class - future languages only require new values-* resource folders, not code changes.
 */
data class CityPreset(
    @StringRes val nameRes: Int,
    @StringRes val countryRes: Int,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
) {
    fun toUserLocation(res: Resources): UserLocation = UserLocation(
        name = res.getString(nameRes),
        country = res.getString(countryRes),
        latitude = latitude,
        longitude = longitude,
        timeZoneId = timeZoneId,
        isGps = false
    )
}

object CityDatabase {

    val PRESET_CITIES: List<CityPreset> = listOf(
        // Saudi Arabia
        CityPreset(R.string.city_makkah, R.string.country_saudi_arabia, 21.422487, 39.826206, "Asia/Riyadh"),
        CityPreset(R.string.city_madinah, R.string.country_saudi_arabia, 24.4672, 39.6111, "Asia/Riyadh"),
        CityPreset(R.string.city_riyadh, R.string.country_saudi_arabia, 24.7136, 46.6753, "Asia/Riyadh"),
        CityPreset(R.string.city_jeddah, R.string.country_saudi_arabia, 21.5433, 39.1728, "Asia/Riyadh"),
        CityPreset(R.string.city_dammam, R.string.country_saudi_arabia, 26.4207, 50.0888, "Asia/Riyadh"),
        // Middle East & North Africa
        CityPreset(R.string.city_cairo, R.string.country_egypt, 30.0444, 31.2357, "Africa/Cairo"),
        CityPreset(R.string.city_alexandria, R.string.country_egypt, 31.2001, 29.9187, "Africa/Cairo"),
        CityPreset(R.string.city_dubai, R.string.country_united_arab_emirates, 25.2048, 55.2708, "Asia/Dubai"),
        CityPreset(R.string.city_abu_dhabi, R.string.country_united_arab_emirates, 24.4539, 54.3773, "Asia/Dubai"),
        CityPreset(R.string.city_sharjah, R.string.country_united_arab_emirates, 25.3463, 55.4209, "Asia/Dubai"),
        CityPreset(R.string.city_doha, R.string.country_qatar, 25.2854, 51.5310, "Asia/Qatar"),
        CityPreset(R.string.city_kuwait_city, R.string.country_kuwait, 29.3759, 47.9774, "Asia/Kuwait"),
        CityPreset(R.string.city_manama, R.string.country_bahrain, 26.2285, 50.5860, "Asia/Bahrain"),
        CityPreset(R.string.city_muscat, R.string.country_oman, 23.5880, 58.3829, "Asia/Muscat"),
        CityPreset(R.string.city_amman, R.string.country_jordan, 31.9454, 35.9284, "Asia/Amman"),
        CityPreset(R.string.city_jerusalem_al_quds, R.string.country_palestine, 31.7683, 35.2137, "Asia/Jerusalem"),
        CityPreset(R.string.city_beirut, R.string.country_lebanon, 33.8938, 35.5018, "Asia/Beirut"),
        CityPreset(R.string.city_damascus, R.string.country_syria, 33.5138, 36.2765, "Asia/Damascus"),
        CityPreset(R.string.city_baghdad, R.string.country_iraq, 33.3152, 44.3661, "Asia/Baghdad"),
        CityPreset(R.string.city_erbil, R.string.country_iraq, 36.1901, 43.9930, "Asia/Baghdad"),
        CityPreset(R.string.city_rabat, R.string.country_morocco, 34.0209, -6.8416, "Africa/Casablanca"),
        CityPreset(R.string.city_casablanca, R.string.country_morocco, 33.5731, -7.5898, "Africa/Casablanca"),
        CityPreset(R.string.city_marrakech, R.string.country_morocco, 31.6295, -7.9811, "Africa/Casablanca"),
        CityPreset(R.string.city_algiers, R.string.country_algeria, 36.7538, 3.0588, "Africa/Algiers"),
        CityPreset(R.string.city_tunis, R.string.country_tunisia, 36.8065, 10.1815, "Africa/Tunis"),
        CityPreset(R.string.city_tripoli, R.string.country_libya, 32.8872, 13.1913, "Africa/Tripoli"),
        CityPreset(R.string.city_khartoum, R.string.country_sudan, 15.5007, 32.5599, "Africa/Khartoum"),
        CityPreset(R.string.city_sana_a, R.string.country_yemen, 15.3694, 44.1910, "Asia/Aden"),
        // Turkey & Central Asia
        CityPreset(R.string.city_istanbul, R.string.country_turkey, 41.0082, 28.9784, "Europe/Istanbul"),
        CityPreset(R.string.city_ankara, R.string.country_turkey, 39.9334, 32.8597, "Europe/Istanbul"),
        CityPreset(R.string.city_izmir, R.string.country_turkey, 38.4237, 27.1428, "Europe/Istanbul"),
        CityPreset(R.string.city_konya, R.string.country_turkey, 37.8746, 32.4932, "Europe/Istanbul"),
        CityPreset(R.string.city_bursa, R.string.country_turkey, 40.1885, 29.0610, "Europe/Istanbul"),
        CityPreset(R.string.city_tehran, R.string.country_iran, 35.6892, 51.3890, "Asia/Tehran"),
        CityPreset(R.string.city_mashhad, R.string.country_iran, 36.2605, 59.6168, "Asia/Tehran"),
        CityPreset(R.string.city_isfahan, R.string.country_iran, 32.6546, 51.6680, "Asia/Tehran"),
        CityPreset(R.string.city_tashkent, R.string.country_uzbekistan, 41.2995, 69.2401, "Asia/Tashkent"),
        CityPreset(R.string.city_samarkand, R.string.country_uzbekistan, 39.6270, 66.9750, "Asia/Samarkand"),
        CityPreset(R.string.city_baku, R.string.country_azerbaijan, 40.4093, 49.8671, "Asia/Baku"),
        CityPreset(R.string.city_almaty, R.string.country_kazakhstan, 43.2220, 76.8512, "Asia/Almaty"),
        CityPreset(R.string.city_astana, R.string.country_kazakhstan, 51.1694, 71.4491, "Asia/Almaty"),
        CityPreset(R.string.city_bishkek, R.string.country_kyrgyzstan, 42.8746, 74.5698, "Asia/Bishkek"),
        CityPreset(R.string.city_dushanbe, R.string.country_tajikistan, 38.5598, 68.7870, "Asia/Dushanbe"),
        CityPreset(R.string.city_ashgabat, R.string.country_turkmenistan, 37.9601, 58.3261, "Asia/Ashgabat"),
        // South Asia
        CityPreset(R.string.city_karachi, R.string.country_pakistan, 24.8607, 67.0011, "Asia/Karachi"),
        CityPreset(R.string.city_lahore, R.string.country_pakistan, 31.5204, 74.3587, "Asia/Karachi"),
        CityPreset(R.string.city_islamabad, R.string.country_pakistan, 33.6844, 73.0479, "Asia/Karachi"),
        CityPreset(R.string.city_rawalpindi, R.string.country_pakistan, 33.5651, 73.0169, "Asia/Karachi"),
        CityPreset(R.string.city_peshawar, R.string.country_pakistan, 34.0151, 71.5249, "Asia/Karachi"),
        CityPreset(R.string.city_faisalabad, R.string.country_pakistan, 31.4504, 73.1350, "Asia/Karachi"),
        CityPreset(R.string.city_multan, R.string.country_pakistan, 30.1575, 71.5249, "Asia/Karachi"),
        CityPreset(R.string.city_dhaka, R.string.country_bangladesh, 23.8103, 90.4125, "Asia/Dhaka"),
        CityPreset(R.string.city_chittagong, R.string.country_bangladesh, 22.3569, 91.7832, "Asia/Dhaka"),
        CityPreset(R.string.city_sylhet, R.string.country_bangladesh, 24.8949, 91.8687, "Asia/Dhaka"),
        CityPreset(R.string.city_delhi_new_delhi, R.string.country_india, 28.6139, 77.2090, "Asia/Kolkata"),
        CityPreset(R.string.city_mumbai, R.string.country_india, 19.0760, 72.8777, "Asia/Kolkata"),
        CityPreset(R.string.city_hyderabad, R.string.country_india, 17.3850, 78.4867, "Asia/Kolkata"),
        CityPreset(R.string.city_bengaluru, R.string.country_india, 12.9716, 77.5946, "Asia/Kolkata"),
        CityPreset(R.string.city_kolkata, R.string.country_india, 22.5726, 88.3639, "Asia/Kolkata"),
        CityPreset(R.string.city_chennai, R.string.country_india, 13.0827, 80.2707, "Asia/Kolkata"),
        CityPreset(R.string.city_kabul, R.string.country_afghanistan, 34.5553, 69.2075, "Asia/Kabul"),
        CityPreset(R.string.city_colombo, R.string.country_sri_lanka, 6.9271, 79.8612, "Asia/Colombo"),
        CityPreset(R.string.city_male, R.string.country_maldives, 4.1755, 73.5093, "Indian/Maldives"),
        // Southeast Asia
        CityPreset(R.string.city_jakarta, R.string.country_indonesia, -6.2088, 106.8456, "Asia/Jakarta"),
        CityPreset(R.string.city_surabaya, R.string.country_indonesia, -7.2575, 112.7521, "Asia/Jakarta"),
        CityPreset(R.string.city_bandung, R.string.country_indonesia, -6.9175, 107.6191, "Asia/Jakarta"),
        CityPreset(R.string.city_medan, R.string.country_indonesia, 3.5952, 98.6722, "Asia/Jakarta"),
        CityPreset(R.string.city_semarang, R.string.country_indonesia, -6.9667, 110.4167, "Asia/Jakarta"),
        CityPreset(R.string.city_makassar, R.string.country_indonesia, -5.1477, 119.4327, "Asia/Makassar"),
        CityPreset(R.string.city_kuala_lumpur, R.string.country_malaysia, 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CityPreset(R.string.city_penang_george_town, R.string.country_malaysia, 5.4141, 100.3288, "Asia/Kuala_Lumpur"),
        CityPreset(R.string.city_johor_bahru, R.string.country_malaysia, 1.4927, 103.7414, "Asia/Kuala_Lumpur"),
        CityPreset(R.string.city_singapore, R.string.country_singapore, 1.3521, 103.8198, "Asia/Singapore"),
        CityPreset(R.string.city_bandar_seri_begawan, R.string.country_brunei, 4.9031, 114.9398, "Asia/Brunei"),
        CityPreset(R.string.city_bangkok, R.string.country_thailand, 13.7563, 100.5018, "Asia/Bangkok"),
        CityPreset(R.string.city_manila, R.string.country_philippines, 14.5995, 120.9842, "Asia/Manila"),
        // Europe
        CityPreset(R.string.city_london, R.string.country_united_kingdom, 51.5074, -0.1278, "Europe/London"),
        CityPreset(R.string.city_birmingham, R.string.country_united_kingdom, 52.4862, -1.8904, "Europe/London"),
        CityPreset(R.string.city_manchester, R.string.country_united_kingdom, 53.4808, -2.2426, "Europe/London"),
        CityPreset(R.string.city_paris, R.string.country_france, 48.8566, 2.3522, "Europe/Paris"),
        CityPreset(R.string.city_marseille, R.string.country_france, 43.2965, 5.3698, "Europe/Paris"),
        CityPreset(R.string.city_lyon, R.string.country_france, 45.7640, 4.8357, "Europe/Paris"),
        CityPreset(R.string.city_berlin, R.string.country_germany, 52.5200, 13.4050, "Europe/Berlin"),
        CityPreset(R.string.city_frankfurt, R.string.country_germany, 50.1109, 8.6821, "Europe/Berlin"),
        CityPreset(R.string.city_munich, R.string.country_germany, 48.1351, 11.5820, "Europe/Berlin"),
        CityPreset(R.string.city_cologne, R.string.country_germany, 50.9375, 6.9603, "Europe/Berlin"),
        CityPreset(R.string.city_amsterdam, R.string.country_netherlands, 52.3676, 4.9041, "Europe/Amsterdam"),
        CityPreset(R.string.city_rotterdam, R.string.country_netherlands, 51.9244, 4.4777, "Europe/Amsterdam"),
        CityPreset(R.string.city_brussels, R.string.country_belgium, 50.8503, 4.3517, "Europe/Brussels"),
        CityPreset(R.string.city_antwerp, R.string.country_belgium, 51.2194, 4.4025, "Europe/Brussels"),
        CityPreset(R.string.city_vienna, R.string.country_austria, 48.2082, 16.3738, "Europe/Vienna"),
        CityPreset(R.string.city_zurich, R.string.country_switzerland, 47.3769, 8.5417, "Europe/Zurich"),
        CityPreset(R.string.city_geneva, R.string.country_switzerland, 46.2044, 6.1432, "Europe/Zurich"),
        CityPreset(R.string.city_rome, R.string.country_italy, 41.9028, 12.4964, "Europe/Rome"),
        CityPreset(R.string.city_milan, R.string.country_italy, 45.4642, 9.1900, "Europe/Rome"),
        CityPreset(R.string.city_madrid, R.string.country_spain, 40.4168, -3.7038, "Europe/Madrid"),
        CityPreset(R.string.city_barcelona, R.string.country_spain, 41.3879, 2.1699, "Europe/Madrid"),
        CityPreset(R.string.city_stockholm, R.string.country_sweden, 59.3293, 18.0686, "Europe/Stockholm"),
        CityPreset(R.string.city_oslo, R.string.country_norway, 59.9139, 10.7522, "Europe/Oslo"),
        CityPreset(R.string.city_copenhagen, R.string.country_denmark, 55.6761, 12.5683, "Europe/Copenhagen"),
        CityPreset(R.string.city_helsinki, R.string.country_finland, 60.1699, 24.9384, "Europe/Helsinki"),
        CityPreset(R.string.city_dublin, R.string.country_ireland, 53.3498, -6.2603, "Europe/Dublin"),
        CityPreset(R.string.city_moscow, R.string.country_russia, 55.7558, 37.6173, "Europe/Moscow"),
        CityPreset(R.string.city_kazan, R.string.country_russia, 55.7887, 49.1221, "Europe/Moscow"),
        CityPreset(R.string.city_ufa, R.string.country_russia, 54.7388, 55.9721, "Asia/Yekaterinburg"),
        CityPreset(R.string.city_grozny, R.string.country_russia, 43.3169, 45.6888, "Europe/Moscow"),
        CityPreset(R.string.city_makhachkala, R.string.country_russia, 42.9831, 47.5047, "Europe/Moscow"),
        CityPreset(R.string.city_sarajevo, R.string.country_bosnia_and_herzegovina, 43.8563, 18.4131, "Europe/Sarajevo"),
        CityPreset(R.string.city_pristina, R.string.country_kosovo, 42.6629, 21.1655, "Europe/Belgrade"),
        CityPreset(R.string.city_tirana, R.string.country_albania, 41.3275, 19.8187, "Europe/Tirane"),
        CityPreset(R.string.city_skopje, R.string.country_north_macedonia, 41.9973, 21.4280, "Europe/Skopje"),
        CityPreset(R.string.city_athens, R.string.country_greece, 37.9838, 23.7275, "Europe/Athens"),
        // North America
        CityPreset(R.string.city_new_york, R.string.country_united_states, 40.7128, -74.0060, "America/New_York"),
        CityPreset(R.string.city_los_angeles, R.string.country_united_states, 34.0522, -118.2437, "America/Los_Angeles"),
        CityPreset(R.string.city_chicago, R.string.country_united_states, 41.8781, -87.6298, "America/Chicago"),
        CityPreset(R.string.city_houston, R.string.country_united_states, 29.7604, -95.3698, "America/Chicago"),
        CityPreset(R.string.city_dallas, R.string.country_united_states, 32.7767, -96.7970, "America/Chicago"),
        CityPreset(R.string.city_philadelphia, R.string.country_united_states, 39.9526, -75.1652, "America/New_York"),
        CityPreset(R.string.city_phoenix, R.string.country_united_states, 33.4484, -112.0740, "America/Phoenix"),
        CityPreset(R.string.city_san_francisco, R.string.country_united_states, 37.7749, -122.4194, "America/Los_Angeles"),
        CityPreset(R.string.city_seattle, R.string.country_united_states, 47.6062, -122.3321, "America/Los_Angeles"),
        CityPreset(R.string.city_detroit_dearborn, R.string.country_united_states, 42.3314, -83.0458, "America/Detroit"),
        CityPreset(R.string.city_washington_d_c, R.string.country_united_states, 38.9072, -77.0369, "America/New_York"),
        CityPreset(R.string.city_atlanta, R.string.country_united_states, 33.7490, -84.3880, "America/New_York"),
        CityPreset(R.string.city_miami, R.string.country_united_states, 25.7617, -80.1918, "America/New_York"),
        CityPreset(R.string.city_toronto, R.string.country_canada, 43.6532, -79.3832, "America/Toronto"),
        CityPreset(R.string.city_montreal, R.string.country_canada, 45.5017, -73.5673, "America/Toronto"),
        CityPreset(R.string.city_vancouver, R.string.country_canada, 49.2827, -123.1207, "America/Vancouver"),
        CityPreset(R.string.city_calgary, R.string.country_canada, 51.0447, -114.0719, "America/Edmonton"),
        CityPreset(R.string.city_ottawa, R.string.country_canada, 45.4215, -75.6972, "America/Toronto"),
        CityPreset(R.string.city_edmonton, R.string.country_canada, 53.5461, -113.4938, "America/Edmonton"),
        // Africa (Sub-Saharan)
        CityPreset(R.string.city_lagos, R.string.country_nigeria, 6.5244, 3.3792, "Africa/Lagos"),
        CityPreset(R.string.city_abuja, R.string.country_nigeria, 9.0765, 7.3986, "Africa/Lagos"),
        CityPreset(R.string.city_kano, R.string.country_nigeria, 12.0022, 8.5920, "Africa/Lagos"),
        CityPreset(R.string.city_nairobi, R.string.country_kenya, -1.2921, 36.8219, "Africa/Nairobi"),
        CityPreset(R.string.city_mombasa, R.string.country_kenya, -4.0435, 39.6682, "Africa/Nairobi"),
        CityPreset(R.string.city_mogadishu, R.string.country_somalia, 2.0469, 45.3182, "Africa/Mogadishu"),
        CityPreset(R.string.city_hargeisa, R.string.country_somalia, 9.5600, 44.0650, "Africa/Mogadishu"),
        CityPreset(R.string.city_addis_ababa, R.string.country_ethiopia, 9.0300, 38.7400, "Africa/Addis_Ababa"),
        CityPreset(R.string.city_dar_es_salaam, R.string.country_tanzania, -6.7924, 39.2083, "Africa/Dar_es_Salaam"),
        CityPreset(R.string.city_zanzibar, R.string.country_tanzania, -6.1659, 39.2026, "Africa/Dar_es_Salaam"),
        CityPreset(R.string.city_kampala, R.string.country_uganda, 0.3476, 32.5825, "Africa/Kampala"),
        CityPreset(R.string.city_dakar, R.string.country_senegal, 14.7167, -17.4677, "Africa/Dakar"),
        CityPreset(R.string.city_bamako, R.string.country_mali, 12.6392, -8.0029, "Africa/Bamako"),
        CityPreset(R.string.city_niamey, R.string.country_niger, 13.5116, 2.1254, "Africa/Niamey"),
        CityPreset(R.string.city_johannesburg, R.string.country_south_africa, -26.2041, 28.0473, "Africa/Johannesburg"),
        CityPreset(R.string.city_cape_town, R.string.country_south_africa, -33.9249, 18.4241, "Africa/Johannesburg"),
        CityPreset(R.string.city_durban, R.string.country_south_africa, -29.8587, 31.0218, "Africa/Johannesburg"),
        // Oceania, East Asia & Latin America
        CityPreset(R.string.city_sydney, R.string.country_australia, -33.8688, 151.2093, "Australia/Sydney"),
        CityPreset(R.string.city_melbourne, R.string.country_australia, -37.8136, 144.9631, "Australia/Melbourne"),
        CityPreset(R.string.city_brisbane, R.string.country_australia, -27.4698, 153.0251, "Australia/Brisbane"),
        CityPreset(R.string.city_perth, R.string.country_australia, -31.9505, 115.8605, "Australia/Perth"),
        CityPreset(R.string.city_auckland, R.string.country_new_zealand, -36.8485, 174.7633, "Pacific/Auckland"),
        CityPreset(R.string.city_tokyo, R.string.country_japan, 35.6762, 139.6503, "Asia/Tokyo"),
        CityPreset(R.string.city_seoul, R.string.country_south_korea, 37.5665, 126.9780, "Asia/Seoul"),
        CityPreset(R.string.city_beijing, R.string.country_china, 39.9042, 116.4074, "Asia/Shanghai"),
        CityPreset(R.string.city_urumqi, R.string.country_china, 43.8256, 87.6168, "Asia/Urumqi"),
        CityPreset(R.string.city_hong_kong, R.string.country_hong_kong, 22.3193, 114.1694, "Asia/Hong_Kong"),
        CityPreset(R.string.city_taipei, R.string.country_taiwan, 25.0330, 121.5654, "Asia/Taipei"),
        CityPreset(R.string.city_buenos_aires, R.string.country_argentina, -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        CityPreset(R.string.city_sao_paulo, R.string.country_brazil, -23.5505, -46.6333, "America/Sao_Paulo"),
        CityPreset(R.string.city_mexico_city, R.string.country_mexico, 19.4326, -99.1332, "America/Mexico_City")
    )

    // Makkah is always the first preset (see PRESET_CITIES above); reused as the default location
    // so a fresh install localizes correctly instead of hardcoding "Makkah"/"Saudi Arabia" literals.
    val DEFAULT_PRESET: CityPreset get() = PRESET_CITIES.first()

    fun defaultLocation(res: Resources): UserLocation = DEFAULT_PRESET.toUserLocation(res)

    /**
     * Preset cities are stored with fixed, exact lat/lon literals, so an exact match here safely
     * identifies "this location came from picking a preset" without accidentally matching a
     * GPS/geocoded location that happens to be nearby but carries far more decimal precision.
     */
    private fun findPresetByCoordinates(lat: Double, lon: Double): CityPreset? =
        PRESET_CITIES.firstOrNull { it.latitude == lat && it.longitude == lon }

    /**
     * Whether [location] is this exact preset, currently selected. Compares by coordinates (and
     * excludes GPS fixes) rather than by display name, so it stays correct across a language
     * switch and can't mis-highlight a preset city that merely shares a name with a GPS fix.
     */
    fun isSelectedPreset(location: UserLocation, preset: CityPreset): Boolean =
        !location.isGps && location.latitude == preset.latitude && location.longitude == preset.longitude

    /**
     * A location selected from the preset list should always display in the *current* app
     * language, not whichever language was active when it was picked - otherwise switching the
     * app's language leaves the saved city name stuck until the user manually reselects it. GPS
     * or manually-entered locations have no second-language variant to fall back to, so they
     * keep showing whatever the geocoder/user originally provided.
     */
    fun localizedName(res: Resources, location: UserLocation): String {
        val preset = findPresetByCoordinates(location.latitude, location.longitude)
        return preset?.let { res.getString(it.nameRes) } ?: location.name
    }

    fun localizedCountry(res: Resources, location: UserLocation): String {
        val preset = findPresetByCoordinates(location.latitude, location.longitude)
        return preset?.let { res.getString(it.countryRes) } ?: location.country
    }

}
