package com.example.data.places

import androidx.room.Dao
import androidx.room.Query

@Dao
interface PlaceDao {

    @Query(
        """
        SELECT * FROM places
        WHERE latitude BETWEEN :latMin AND :latMax
        AND longitude BETWEEN :lonMin AND :lonMax
        """
    )
    suspend fun candidatesInBoundingBox(latMin: Double, latMax: Double, lonMin: Double, lonMax: Double): List<PlaceEntity>

    @Query("SELECT * FROM places")
    suspend fun all(): List<PlaceEntity>

    @Query(
        """
        SELECT * FROM places
        WHERE nameEn LIKE '%' || :query || '%'
        OR asciiName LIKE '%' || :query || '%'
        OR nameAr LIKE '%' || :query || '%'
        ORDER BY population DESC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 50): List<PlaceEntity>
}
