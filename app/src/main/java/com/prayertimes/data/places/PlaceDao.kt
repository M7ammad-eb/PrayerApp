package com.prayertimes.data.places

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

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

    @RawQuery
    suspend fun search(query: SupportSQLiteQuery): List<PlaceEntity>
}
