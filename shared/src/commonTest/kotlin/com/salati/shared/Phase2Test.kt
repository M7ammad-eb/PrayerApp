package com.salati.shared

import kotlin.test.*

class Phase2Test {
    private val engine=SalatiEngine()
    private fun day(date:String,now:Double=0.0,offset:Int=0,anchor:String?=null)=engine.dayScoped(date,
        24.4672,39.6111,"Asia/Riyadh","UMM_AL_QURA",false,"ANGLE_BASED",0,0,0,0,0,0,offset,anchor,now)

    @Test fun maghribChangesOnlyReligiousLabel() {
        val base=day("2026-10-18")
        val before=day(base.dateISO,base.maghribEpoch-1)
        val after=day(base.dateISO,base.maghribEpoch)
        assertEquals(base.events,before.events);assertEquals(base.events,after.events)
        assertEquals("2026-10-18",before.religiousDateISO)
        assertEquals("2026-10-19",after.religiousDateISO)
        assertEquals(day("2026-10-19").civilHijriEnglish,after.hijriEnglish)
        assertEquals(after.hijriEnglish,day("2026-10-19",base.civilEndEpoch).hijriEnglish)
    }
    @Test fun sunsetAtHijriYearBoundaryAndSelectedZone() {
        val newYear=HijriCalendar.gregorianDateFor(HijriYearMonth(1448,1),1,0)
        val previous=day(newYear.minusDays(1).toString())
        val after=day(previous.dateISO,previous.maghribEpoch)
        assertTrue(after.hijriEnglish.contains("1448"))
        assertEquals(newYear.toString(),after.religiousDateISO)
        // The same instant is independent of the phone's zone; the selected IANA zone owns civil dates.
        assertEquals(after.religiousDateISO,ReligiousDates.forSchedule(previous.dateISO,previous.maghribEpoch,previous.maghribEpoch,"Asia/Riyadh"))
        assertEquals(previous.dateISO,ReligiousDates.forSchedule(previous.dateISO,previous.civilEndEpoch+3600,previous.maghribEpoch,"Pacific/Auckland"))
    }
    @Test fun correctionIsAppliedOnceAndExpiresByAnchor() {
        val date=LocalDate.parse("2026-10-18");val anchor=HijriCalendar.monthKeyFor(date)
        for(offset in -2..2) {
            val result=day(date.toString(),0.0,offset,anchor)
            assertEquals(HijriDateCalculator.convertToHijri(date,offset).formattedEn,result.civilHijriEnglish)
            val page=engine.calendarScoped(date.toString(),0,offset,anchor)
            assertEquals(42,page.cells.size)
            for(cell in page.cells) assertEquals(ReligiousDates.adjusted(LocalDate.parse(cell.dateISO),offset,anchor).day,cell.day)
        }
        val other=HijriCalendar.gregorianDateFor(HijriYearMonth(1448,9),1,0)
        assertEquals(day(other.toString()).events,day(other.toString(),0.0,2,anchor).events)
        assertEquals(0,HijriCalendar.effectiveAdjustment(2,anchor,other))
    }
    @Test fun midnightAndSunriseTimelineMatchesSharedNextSequence() {
        val days=listOf(day("2026-10-17"),day("2026-10-18"),day("2026-10-19"))
        val today=days[1]
        val moments=SharedPresentation.timeline(days,today.civilStartEpoch,today.civilEndEpoch+1)
        val midnight=moments.first()
        assertEquals("2026-10-17.ISHA",midnight.currentEventID)
        assertEquals("2026-10-18.FAJR",midnight.nextEventID)
        val fajr=moments.first {it.epochSeconds==today.events[0].epochSeconds}
        assertEquals("2026-10-18.SUNRISE",fajr.nextEventID)
        assertEquals("SUNRISE",day(today.dateISO,today.events[0].epochSeconds+1).next.id)
        val sunrise=moments.first {it.epochSeconds==today.events[1].epochSeconds}
        assertNull(sunrise.currentEventID);assertEquals("2026-10-18.DHUHR",sunrise.nextEventID)
        val sunset=moments.first {it.epochSeconds==today.maghribEpoch}
        assertEquals(today.dateISO,sunset.civilDateISO)
        assertEquals(today.followingDateISO,sunset.religiousDateISO)
    }
    @Test fun planRetainsPrayerInstantAndIndependentAdvancePolicy() {
        val event=PlanEvent("2026-10-18.MAGHRIB","MAGHRIB","Maghrib",10000.0)
        val plan=SharedScheduling.plan(listOf(event,event),listOf(PlanPolicy("MAGHRIB","alarm",15)),1000.0,true)
        assertEquals(3,plan.size)
        assertEquals(10000.0,plan.first {it.kind=="alarm"}.triggerEpoch)
        assertEquals(9100.0,plan.first {it.kind=="reminder"}.triggerEpoch)
        assertEquals(9100.0,plan.first {it.kind=="live"}.triggerEpoch)
        assertEquals(plan,SharedScheduling.plan(listOf(event),listOf(PlanPolicy("MAGHRIB","alarm",15)),1000.0,true))
        assertEquals(1,SharedScheduling.plan(listOf(event),listOf(PlanPolicy("MAGHRIB","off",15)),1000.0,false).size)
    }
    @Test fun liveStartRemainsCanonicalInsideCountdownWindow() {
        val event=PlanEvent("d.FAJR","FAJR","Fajr",10000.0)
        val first=SharedScheduling.plan(listOf(event),emptyList(),9500.0,true).single()
        val second=SharedScheduling.plan(listOf(event),emptyList(),9600.0,true).single()
        assertEquals(first,second);assertEquals(9100.0,first.triggerEpoch)
        assertTrue(SharedScheduling.plan(listOf(event),emptyList(),10000.0,true).isEmpty())
    }
    @Test fun planDoesNotConfusePlatformCapacitiesOrSunrise() {
        val events=(1..100).map {PlanEvent("$it.FAJR","FAJR","Fajr",it*3600.0)}+
            PlanEvent("d.SUNRISE","SUNRISE","Sunrise",4000.0)
        val plan=SharedScheduling.plan(events,listOf(PlanPolicy("FAJR","alarm",15)),0.0,true)
        assertEquals(300,plan.size);assertFalse(plan.any {it.prayer=="SUNRISE"})
    }
    @Test fun locationAcceptanceRejectsStaleInaccurateAndManualOverride() {
        assertTrue(LocationAcceptance.usableFix(100.0,1000.0,1100.0,true))
        assertFalse(LocationAcceptance.usableFix(-1.0,1000.0,1100.0,true))
        assertFalse(LocationAcceptance.usableFix(300.0,1000.0,1100.0,true))
        assertFalse(LocationAcceptance.usableFix(10.0,1000.0,1901.0,true))
        assertFalse(LocationAcceptance.usableFix(10.0,1101.0,1100.0,true))
        assertFalse(LocationAcceptance.accept(24.0,39.0,false,25.0,39.0,true,false))
        assertTrue(LocationAcceptance.accept(24.0,39.0,false,25.0,39.0,true,true))
        assertFalse(LocationAcceptance.accept(24.0,39.0,true,24.001,39.0,false,false))
        assertTrue(LocationAcceptance.accept(24.0,39.0,true,24.1,39.0,false,false))
        assertFalse(LocationAcceptance.shouldAcquire(1000.0,1899.0,true,false))
        assertTrue(LocationAcceptance.shouldAcquire(1000.0,1900.0,true,false))
    }
    @Test fun readableNameDoesNotInventAParentRelationship() {
        assertEquals("Sultanah",LocationLabels.readable("Sulţānah","Sultanah",null,false))
        assertEquals("Sultanah",LocationLabels.readable("Sulţānah","Sultanah",null,true))
        assertEquals("المدينة المنورة",LocationLabels.readable("Madinah","Madinah","المدينة المنورة",true))
    }
    @Test fun compassUnwrapsAndAlignmentUsesReliableHysteresis() {
        val tracker=CompassAlignmentTracker()
        val first=tracker.update(0.0,359.0,2.0,100.0)
        val second=tracker.update(0.0,1.0,2.0,100.7)
        assertTrue(kotlin.math.abs(second.rotation-first.rotation)<3)
        assertTrue(second.aligned);assertTrue(second.enteredAlignment)
        assertFalse(tracker.update(0.0,6.0,2.0,101.0).enteredAlignment)
        assertTrue(tracker.update(0.0,6.0,2.0,101.1).aligned)
        assertFalse(tracker.update(0.0,6.0,25.0,101.2).aligned)
        assertFalse(tracker.update(0.0,0.0,25.0,102.0).reliable)
    }
    @Test fun compassGeometryHasNoLanguageDependentInputs() {
        val english=CompassAlignmentTracker();val arabic=CompassAlignmentTracker()
        for((index,heading) in listOf(359.0,1.0,90.0,180.0,270.0).withIndex())
            assertEquals(english.update(12.0,heading,3.0,index.toDouble()),arabic.update(12.0,heading,3.0,index.toDouble()))
    }
}
