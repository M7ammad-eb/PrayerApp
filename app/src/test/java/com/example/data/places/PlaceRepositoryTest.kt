package com.example.data.places

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure relation-bucketing math - no Room/Android needed, so this runs as a plain JVM test. */
class PlaceRepositoryTest {

    private fun place(lat: Double, lon: Double, name: String = "Test") = PlaceEntity(
        geonameId = 1,
        nameEn = name,
        nameAr = null,
        asciiName = name,
        countryCode = "XX",
        latitude = lat,
        longitude = lon,
        timeZoneId = "UTC",
        population = 0
    )

    // 1 degree of latitude is ~111.19km near the equator; offsetting purely in latitude keeps the
    // haversine distance close to this simple linear estimate, which is precise enough to land
    // comfortably on either side of the 15km/50km thresholds without relying on GeoUtils itself.
    private fun latOffsetForKm(km: Double) = km / 111.19

    @Test
    fun `distance just under 15km is SAME_CITY`() {
        val result = PlaceRepository.nearestOf(listOf(place(latOffsetForKm(14.0), 0.0)), 0.0, 0.0)
        assertEquals(PlaceRelation.SAME_CITY, result.relation)
    }

    @Test
    fun `distance just over 15km is NEAR_CITY`() {
        val result = PlaceRepository.nearestOf(listOf(place(latOffsetForKm(16.0), 0.0)), 0.0, 0.0)
        assertEquals(PlaceRelation.NEAR_CITY, result.relation)
    }

    @Test
    fun `distance just under 50km is NEAR_CITY`() {
        val result = PlaceRepository.nearestOf(listOf(place(latOffsetForKm(49.0), 0.0)), 0.0, 0.0)
        assertEquals(PlaceRelation.NEAR_CITY, result.relation)
    }

    @Test
    fun `distance just over 50km is NEAREST_CITY`() {
        val result = PlaceRepository.nearestOf(listOf(place(latOffsetForKm(51.0), 0.0)), 0.0, 0.0)
        assertEquals(PlaceRelation.NEAREST_CITY, result.relation)
    }

    @Test
    fun `picks the closest of several candidates`() {
        val near = place(latOffsetForKm(5.0), 0.0, name = "Near")
        val far = place(latOffsetForKm(40.0), 0.0, name = "Far")
        val result = PlaceRepository.nearestOf(listOf(far, near), 0.0, 0.0)
        assertEquals("Near", result.place.nameEn)
        assertEquals(PlaceRelation.SAME_CITY, result.relation)
    }
}
