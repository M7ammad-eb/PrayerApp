package com.salati.shared

import kotlin.test.*

class RefinementTest {
    private val engine=SalatiEngine()
    private fun day(date:String)=engine.dayScoped(date,24.4672,39.6111,"Asia/Riyadh","UMM_AL_QURA",false,"ANGLE_BASED",0,0,0,0,0,0,0,null,0.0)

    @Test fun everyOffsetHasCompleteMonthsAndBijectiveCivilDates() {
        var five=false;var six=false;var startCrossed=false;var endCrossed=false
        for(year in 1447..1450) for(month in 1..12) for(offset in -2..2) {
            val anchor=HijriYearMonth(year,month)
            val original=HijriCalendar.generate(anchor,0)
            val adjusted=HijriCalendar.generateScoped(anchor,offset,anchor.key)
            assertEquals(original.firstGregorianDate.minusDays(offset.toLong()),adjusted.firstGregorianDate)
            assertEquals(original.lastGregorianDate.minusDays(offset.toLong()),adjusted.lastGregorianDate)
            startCrossed=startCrossed || adjusted.days.first().gregorianDate.toEpochDay()<original.days.first().gregorianDate.toEpochDay()
            endCrossed=endCrossed || adjusted.days.size>35
            val mapped=mutableMapOf<LocalDate,String>()
            for(delta in -8..8) {
                val target=anchor.plusMonths(delta)
                val page=HijriCalendar.generateScoped(target,offset,anchor.key)
                val owned=page.days.filter {it.isInDisplayedMonth}
                assertTrue(owned.size in 29..30,"$target offset $offset has ${owned.size} days")
                assertEquals((1..owned.size).toList(),owned.map {it.hijriDate.day})
                assertEquals(page.firstGregorianDate,owned.first().gregorianDate)
                assertEquals(page.lastGregorianDate,owned.last().gregorianDate)
                assertEquals(6,page.days.first().gregorianDate.dayOfWeek.value) // Saturday
                assertEquals(page.days.size,page.days.map {it.gregorianDate}.distinct().size)
                assertTrue(page.days.size in listOf(35,42))
                five=five || page.days.size == 35;six=six || page.days.size == 42
                assertTrue(page.days.takeLast(7).any {it.isInDisplayedMonth})
                for((index,cell) in page.days.withIndex()) {
                    assertEquals(page.days.first().gregorianDate.plusDays(index.toLong()),cell.gregorianDate)
                    assertEquals(ReligiousDates.adjusted(cell.gregorianDate,offset,anchor.key),cell.hijriDate)
                    if(cell.isInDisplayedMonth) assertNull(mapped.put(cell.gregorianDate,cell.hijriDate.formattedEn))
                }
                if(offset == 0) assertEquals(HijriCalendar.generate(target,0).days.take(page.days.size),page.days)
            }
            val dates=mapped.keys.sortedBy {it.toEpochDay()}
            assertEquals(dates.last().toEpochDay()-dates.first().toEpochDay()+1,dates.size.toLong())
            // Reconciliation ends; it never becomes a permanent global offset.
            for(delta in listOf(-8,8)) {
                val far=anchor.plusMonths(delta)
                assertEquals(HijriCalendar.generate(far,0).firstGregorianDate, HijriCalendar.generateScoped(far,offset,anchor.key).firstGregorianDate)
            }
        }
        assertTrue(five && six && startCrossed && endCrossed)
    }
    @Test fun calendarPageReservesSixRowsButOnlyReturnsRealWeeks() {
        for(month in 1..12) for(offset in -2..2) {
            val page=engine.calendarMonth(1448,month,offset,"1448-${month.toString().padStart(2,'0')}")
            assertEquals(6,page.reservedWeekCount)
            assertEquals(page.weekCount*7,page.cells.size)
            assertTrue(page.weekCount in 5..6)
        }
    }
    @Test fun overnightRowsUseTomorrowActualEventAndPreserveCivilContext() {
        val days=listOf(day("2026-10-18"),day("2026-10-19"),day("2026-10-20"))
        val today=days[0];val tomorrow=days[1]
        for(now in listOf(today.events.last().epochSeconds+1,today.civilEndEpoch-1,today.civilEndEpoch,tomorrow.events[0].epochSeconds-1,tomorrow.events[0].epochSeconds)) {
            val state=SharedPresentation.timeline(days,now,days.last().civilEndEpoch).first()
            if(now<today.civilEndEpoch) {
                assertEquals(today.dateISO,state.civilDateISO)
                assertEquals("2026-10-19.FAJR",state.tomorrowEventID)
                assertEquals("2026-10-19.FAJR",state.displayEventIDs.last())
                assertEquals("2026-10-18.ISHA",state.currentEventID)
            } else if(now<tomorrow.events[0].epochSeconds) {
                assertEquals(tomorrow.dateISO,state.civilDateISO)
                assertNull(state.tomorrowEventID)
                assertEquals("2026-10-18.ISHA",state.previousEventID)
            } else {
                assertEquals("2026-10-19.FAJR",state.currentEventID)
                assertEquals("2026-10-19.SUNRISE",state.nextEventID)
                assertNull(state.previousEventID)
            }
            assertTrue(state.nextEventID in state.displayEventIDs)
            if(now<tomorrow.events[0].epochSeconds) assertEquals("2026-10-19.FAJR",state.nextEventID)
        }
        assertNotEquals(today.events[0].epochSeconds,tomorrow.events[0].epochSeconds)
    }
    @Test fun onboardingDefaultsNeverInferMadhhabOrOverwriteExplicitChoices() {
        assertEquals("UMM_AL_QURA",OnboardingDefaults.recommendation("Asia/Riyadh").method)
        assertEquals("KARACHI",OnboardingDefaults.recommendation("Asia/Karachi").method)
        assertFalse(OnboardingDefaults.recommendation("Asia/Karachi").hanafi)
        assertTrue(OnboardingDefaults.recommendation("UTC").fallback)
        for(done in listOf(false,true)) for(methodOverride in listOf(false,true)) for(asrOverride in listOf(false,true)) {
            val value=OnboardingDefaults.resolve("Asia/Riyadh","ISNA",true,methodOverride,asrOverride,done)
            assertEquals(if(done || methodOverride) "ISNA" else "UMM_AL_QURA",value.method)
            assertEquals(done || asrOverride,value.hanafi)
        }
    }
    @Test fun correctedRamadanBoundaryDrivesIshaPolicyToo() {
        val ramadan=HijriYearMonth(1448,9)
        for(offset in -2..2) {
            val start=HijriCalendar.scopedStart(ramadan,offset,ramadan.key)
            for(date in listOf(start.minusDays(1),start)) {
                val schedule=PrayerTimesCalculator.calculateDailySchedule(date,24.4672,39.6111,ZoneId.of("Asia/Riyadh"),CalculationMethod.UMM_AL_QURA,
                    hijriAdjustmentDays=offset,hijriAnchor=ramadan.key)
                assertEquals(ReligiousDates.adjusted(date,offset,ramadan.key),schedule.hijriDate)
                val expected=if(date == start) 120 else 90
                val events=schedule.prayerItems
                assertEquals(expected*60L,events[5].zonedDateTime.epochSeconds-events[4].zonedDateTime.epochSeconds)
            }
        }
    }
    @Test fun scopedOffsetWithoutAnchorNeverBecomesGlobalCalculationOffset() {
        val date=HijriCalendar.gregorianDateFor(HijriYearMonth(1448,9),1,0).minusDays(1).toString()
        val base=day(date)
        val missing=engine.dayScoped(date,24.4672,39.6111,"Asia/Riyadh","UMM_AL_QURA",false,"ANGLE_BASED",0,0,0,0,0,0,2,null,0.0)
        assertEquals(base.events,missing.events);assertEquals(base.civilHijriEnglish,missing.civilHijriEnglish)
    }
}
