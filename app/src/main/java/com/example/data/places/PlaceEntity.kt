package com.example.data.places

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row of the offline GeoNames `cities5000` dataset (CC BY 4.0), pre-processed into
 * app/src/main/assets/places.db by tools/geonames/build_places_db.py. nameAr comes from
 * GeoNames' alternateNamesV2 dump filtered to Arabic entries for the same geonameId.
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val geonameId: Long,
    val nameEn: String,
    val nameAr: String?,
    val asciiName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val population: Long
)
