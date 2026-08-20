package com.example.data.places

import android.content.Context
import com.example.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PlaceRelation { SAME_CITY, NEAR_CITY, NEAREST_CITY }

data class NearestPlaceResult(
    val place: PlaceEntity,
    val distanceKm: Double,
    val relation: PlaceRelation
)

/**
 * Offline nearest-place lookup backed by the ~69k-row GeoNames dataset in places.db, replacing
 * both Android's Geocoder (network/OEM-dependent) and the old 162-city hardcoded approximation.
 * Bounding-box pre-filter (expanding degrees, then a full-table scan as a last resort) keeps a
 * GPS lookup from scanning all ~69k rows on every call.
 */
object PlaceRepository {

    private val boxExpansionsDegrees = listOf(2.0, 5.0)

    // Distance beyond which the offline dataset simply doesn't have a close-enough match, per the
    // "Same city / Near city / Nearest city" display rule.
    private const val SAME_CITY_MAX_KM = 15.0
    private const val NEAR_CITY_MAX_KM = 50.0

    suspend fun nearestPlace(context: Context, lat: Double, lon: Double): NearestPlaceResult? =
        withContext(Dispatchers.IO) {
            val dao = PlacesDatabase.getInstance(context).placeDao()
            for (delta in boxExpansionsDegrees) {
                val candidates = dao.candidatesInBoundingBox(lat - delta, lat + delta, lon - delta, lon + delta)
                if (candidates.isNotEmpty()) return@withContext nearestOf(candidates, lat, lon)
            }
            val all = dao.all()
            if (all.isEmpty()) null else nearestOf(all, lat, lon)
        }

    suspend fun search(context: Context, query: String, limit: Int = 50): List<PlaceEntity> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) emptyList() else PlacesDatabase.getInstance(context).placeDao().search(trimmed, limit)
        }

    // internal (not private) so relation-bucketing math can be unit-tested without spinning up Room.
    internal fun nearestOf(candidates: List<PlaceEntity>, lat: Double, lon: Double): NearestPlaceResult {
        val (place, distanceKm) = candidates
            .map { it to GeoUtils.haversineDistanceKm(lat, lon, it.latitude, it.longitude) }
            .minBy { it.second }
        val relation = when {
            distanceKm <= SAME_CITY_MAX_KM -> PlaceRelation.SAME_CITY
            distanceKm <= NEAR_CITY_MAX_KM -> PlaceRelation.NEAR_CITY
            else -> PlaceRelation.NEAREST_CITY
        }
        return NearestPlaceResult(place, distanceKm, relation)
    }
}
