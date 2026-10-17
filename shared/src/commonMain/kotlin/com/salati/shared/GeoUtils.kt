package com.prayertimes.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared great-circle distance math, used by both the Qibla calculator (distance to the Kaaba)
 * and the city database (nearest-preset lookup) - previously each reimplemented haversine with
 * its own independently hardcoded Earth radius constant.
 */
object GeoUtils {
    const val EARTH_RADIUS_KM = 6371.0

    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = (lat1 * kotlin.math.PI / 180.0)
        val lat2Rad = (lat2 * kotlin.math.PI / 180.0)
        val dLat = ((lat2 - lat1) * (kotlin.math.PI / 180.0))
        val dLon = ((lon2 - lon1) * (kotlin.math.PI / 180.0))

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }
}
