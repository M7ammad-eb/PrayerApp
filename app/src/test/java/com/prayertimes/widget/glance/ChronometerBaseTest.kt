package com.prayertimes.widget.glance

import org.junit.Assert.assertEquals
import org.junit.Test

class ChronometerBaseTest {

    @Test
    fun moreThanOneHourUsesElapsedRealtimePlusFullDuration() {
        assertEquals(7_300_000L, chronometerBaseElapsedRealtime(100_000L, 7_200_000L))
    }

    @Test
    fun lessThanOneHourUsesElapsedRealtimePlusRemainingDuration() {
        assertEquals(3_599_000L, chronometerBaseElapsedRealtime(99_000L, 3_500_000L))
    }

    @Test
    fun lessThanOneMinutePreservesMillisecondPrecision() {
        assertEquals(124_999L, chronometerBaseElapsedRealtime(100_000L, 24_999L))
    }

    @Test
    fun prayerBoundaryUsesCurrentElapsedRealtime() {
        assertEquals(100_000L, chronometerBaseElapsedRealtime(100_000L, 0L))
    }

    @Test
    fun staleNegativeDurationCannotCreatePastBase() {
        assertEquals(100_000L, chronometerBaseElapsedRealtime(100_000L, -1L))
    }

    @Test
    fun overflowSaturatesSafely() {
        assertEquals(Long.MAX_VALUE, chronometerBaseElapsedRealtime(Long.MAX_VALUE - 5L, 10L))
    }
}
