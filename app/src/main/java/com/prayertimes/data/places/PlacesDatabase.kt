package com.prayertimes.data.places

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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
                    .addCallback(searchIndexCallback)
                    .build()
                    .also { instance = it }
            }

        private val searchIndexCallback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                ensureSearchIndex(db)
            }
        }

        /** Adds the FTS index to databases created by older app versions as well as test DBs. */
        internal fun ensureSearchIndex(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS places_fts USING fts4(
                    nameEn,
                    asciiName,
                    nameAr,
                    content='places',
                    tokenize=unicode61
                )
                """.trimIndent()
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS places_search_meta(version INTEGER NOT NULL)")
            val indexVersion = db.query("SELECT max(version) FROM places_search_meta").use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else 0
            }
            if (indexVersion < 1) {
                db.execSQL("INSERT INTO places_fts(places_fts) VALUES('rebuild')")
                db.execSQL("DELETE FROM places_search_meta")
                db.execSQL("INSERT INTO places_search_meta(version) VALUES(1)")
            }
        }
    }
}
