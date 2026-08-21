package com.prayertimes.data.qibla

import com.prayertimes.util.GeoUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaCalculator {

    // Kaaba Coordinates in Makkah al-Mukarramah
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    /**
     * Calculates the Qibla bearing in degrees from True North (0° - 360° clockwise)
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val lat1 = latitude * DEG_TO_RAD
        val lon1 = longitude * DEG_TO_RAD
        val lat2 = KAABA_LATITUDE * DEG_TO_RAD
        val lon2 = KAABA_LONGITUDE * DEG_TO_RAD

        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = atan2(y, x) * RAD_TO_DEG
        bearing = (bearing + 360.0) % 360.0
        return bearing
    }

    /**
     * Calculates distance to Kaaba in kilometers
     */
    fun calculateDistanceToKaabaKm(latitude: Double, longitude: Double): Double =
        GeoUtils.haversineDistanceKm(latitude, longitude, KAABA_LATITUDE, KAABA_LONGITUDE)

    fun getDirectionCardinal(degrees: Double): String {
        val normalized = (degrees + 360.0) % 360.0
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = ((normalized + 11.25) / 22.5).toInt() % 16
        return directions[index]
    }
}
