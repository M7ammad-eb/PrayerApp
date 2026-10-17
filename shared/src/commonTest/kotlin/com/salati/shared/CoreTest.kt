package com.salati.shared
import kotlin.test.*
class CoreTest {
    private val engine = SalatiEngine()
    private fun day(date: String, now: Double = 0.0) = engine.day(date,24.4672,39.6111,"Asia/Riyadh","UMM_AL_QURA",false,"ANGLE_BASED",0,0,0,0,0,0,0,now)
    @Test fun geographicDistanceRetainsAngularDifference() {
        assertEquals(0.0,com.prayertimes.util.GeoUtils.haversineDistanceKm(24.0,39.0,24.0,39.0),1e-9)
        assertEquals(111.19492664455873,com.prayertimes.util.GeoUtils.haversineDistanceKm(0.0,0.0,1.0,0.0),1e-7)
        val london=com.prayertimes.data.qibla.QiblaCalculator.calculateDistanceToKaabaKm(51.5074,-0.1278)
        assertTrue(london in 4700.0..4800.0)
    }
    @Test fun ramadanAndNormalIsha() {
        for((date,minutes) in listOf("2026-01-15" to 90,"2027-02-20" to 120)) {
            val d=day(date)
            assertEquals(minutes*60.0,d.events[5].epochSeconds-d.events[4].epochSeconds)
        }
    }
    @Test fun afterIshaUsesTomorrowFajr() {
        val today=day("2026-01-15")
        val after=day("2026-01-15",today.events.last().epochSeconds+60)
        assertEquals(day("2026-01-16").events.first(),after.next)
        assertEquals("ISHA",after.currentId)
    }
    @Test fun calendarHas42CellsAndObservances() {
        val page=engine.calendar("2027-02-20",0,0)
        assertEquals(9,page.month)
        assertEquals(42,page.cells.size)
        assertTrue(page.cells.any { "RAMADAN_START" in it.observances })
    }
    @Test fun offsetExpiresAfterAnchorMonth() {
        val date=LocalDate.parse("2026-01-15")
        assertEquals(2,HijriCalendar.effectiveAdjustment(2,HijriCalendar.monthKeyFor(date),date))
        assertEquals(0,HijriCalendar.effectiveAdjustment(2,"1440-01",date))
    }
}
