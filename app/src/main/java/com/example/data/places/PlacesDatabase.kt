package com.example.data.places

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlaceEntity::class], version = 1, exportSchema = false)
abstract class PlacesDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao

    companion object {
        @Volatile
        private var instance: PlacesDatabase? = null

        fun getInstance(context: Context): PlacesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlacesDatabase::class.java,
                    "places.db"
                ).createFromAsset("places.db")
                    .build()
                    .also { instance = it }
            }
    }
}
