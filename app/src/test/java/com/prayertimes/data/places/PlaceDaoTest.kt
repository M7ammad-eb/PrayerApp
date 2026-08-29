package com.prayertimes.data.places

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaceDaoTest {

    private lateinit var db: PlacesDatabase
    private lateinit var dao: PlaceDao

    private val madinahLat = 24.4672
    private val madinahLon = 39.6111

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlacesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.placeDao()
        db.openHelper.writableDatabase.execSQL(insertSql(1, "Madinah", "المدينة المنورة", madinahLat, madinahLon, "Asia/Riyadh"))
        db.openHelper.writableDatabase.execSQL(insertSql(2, "Far Away", null, madinahLat, madinahLon + 3.0, "UTC"))
        db.openHelper.writableDatabase.execSQL(insertSql(3, "Cairo", "القاهرة", 30.0444, 31.2357, "Africa/Cairo"))
        PlacesDatabase.ensureSearchIndex(db.openHelper.writableDatabase)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun insertSql(id: Long, nameEn: String, nameAr: String?, lat: Double, lon: Double, tz: String): String {
        val nameArSql = if (nameAr == null) "NULL" else "'$nameAr'"
        return "INSERT INTO places (geonameId, nameEn, nameAr, asciiName, countryCode, latitude, longitude, timeZoneId, population) " +
            "VALUES ($id, '$nameEn', $nameArSql, '$nameEn', 'XX', $lat, $lon, '$tz', 100)"
    }

    @Test
    fun `bounding box within 2 degrees excludes a place 3 degrees away`() = runTest {
        val within2 = dao.candidatesInBoundingBox(madinahLat - 2, madinahLat + 2, madinahLon - 2, madinahLon + 2)
        assertEquals(setOf("Madinah"), within2.map { it.nameEn }.toSet())
    }

    @Test
    fun `bounding box within 5 degrees includes the farther place`() = runTest {
        val within5 = dao.candidatesInBoundingBox(madinahLat - 5, madinahLat + 5, madinahLon - 5, madinahLon + 5)
        assertEquals(setOf("Madinah", "Far Away"), within5.map { it.nameEn }.toSet())
    }

    @Test
    fun `search matches English name`() = runTest {
        val results = dao.search(PlaceRepository.buildSearchQuery("madinah")!!)
        assertTrue(results.any { it.nameEn == "Madinah" })
    }

    @Test
    fun `search matches Arabic name`() = runTest {
        val results = dao.search(PlaceRepository.buildSearchQuery("القاهرة")!!)
        assertTrue(results.any { it.nameEn == "Cairo" })
    }

    @Test
    fun `search finds nothing for an unmatched query`() = runTest {
        assertTrue(dao.search(PlaceRepository.buildSearchQuery("Nonexistent")!!).isEmpty())
    }
}
