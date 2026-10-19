package com.salati.shared


data class HijriYearMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12)
    }

    fun plusMonths(amount: Int): HijriYearMonth {
        val zeroBased = year * 12L + (month - 1) + amount
        return HijriYearMonth(
            year = kotlin.math.floor(zeroBased / 12.0).toLong().toInt(),
            month = (((zeroBased % 12L + 12L) % 12L) + 1).toInt()
        )
    }

    val key: String get() = year.toString().padStart(4,'0') + "-" + month.toString().padStart(2,'0')
}

data class HijriCalendarDay(
    val gregorianDate: LocalDate,
    val hijriDate: HijriDate,
    val isInDisplayedMonth: Boolean
)

data class HijriCalendarMonth(
    val yearMonth: HijriYearMonth,
    val monthNameEn: String,
    val monthNameAr: String,
    val firstGregorianDate: LocalDate,
    val lastGregorianDate: LocalDate,
    val days: List<HijriCalendarDay>,
    val isSacredMonth: Boolean,
    val hasSixShawwalReminder: Boolean
)

object HijriCalendar {
    private const val MAX_CACHED_MONTHS = 24

    private data class MonthCacheKey(
        val yearMonth: HijriYearMonth,
        val adjustmentDays: Int
    )

    /**
     * Calendar months are immutable and relatively expensive to construct because each one
     * performs dozens of Umm al-Qura conversions. Keep a small process-local working set so the
     * header, pager, and a reopened calendar can share the same result.
     */
    private val monthCache = LruCache<MonthCacheKey, HijriCalendarMonth>(MAX_CACHED_MONTHS)

    fun monthContaining(date: LocalDate, adjustmentDays: Int): HijriYearMonth {
        val hijri = HijriDateCalculator.convertToHijri(date, adjustmentDays)
        return HijriYearMonth(hijri.year, hijri.month)
    }

    fun monthKeyFor(date: LocalDate): String = monthContaining(date, adjustmentDays = 0).key

    fun effectiveAdjustment(
        storedOffset: Int,
        storedAnchorMonth: String?,
        currentDate: LocalDate
    ): Int = if (storedAnchorMonth == monthKeyFor(currentDate)) storedOffset.coerceIn(-2, 2) else 0

    /** Precomputes the likely browsing window around [anchorDate]. */
    fun prewarm(
        anchorDate: LocalDate,
        adjustmentDays: Int,
        previousMonths: Int = 6,
        nextMonths: Int = 12
    ) {
        val anchorMonth = monthContaining(anchorDate, adjustmentDays)
        for (offset in -previousMonths.coerceAtLeast(0)..nextMonths.coerceAtLeast(0)) {
            generate(anchorMonth.plusMonths(offset), adjustmentDays)
        }
    }

    fun generate(yearMonth: HijriYearMonth, adjustmentDays: Int): HijriCalendarMonth {
        val cacheKey = MonthCacheKey(yearMonth, adjustmentDays)
        synchronized(monthCache) {
            monthCache[cacheKey]?.let { return it }
        }

        val generated = generateUncached(yearMonth, adjustmentDays)
        return synchronized(monthCache) {
            monthCache.getOrPut(cacheKey) { generated }
        }
    }

    private fun generateUncached(
        yearMonth: HijriYearMonth,
        adjustmentDays: Int
    ): HijriCalendarMonth {
        val firstGregorian = gregorianDateFor(yearMonth, day = 1, adjustmentDays)
        val nextFirst = gregorianDateFor(yearMonth.plusMonths(1), day = 1, adjustmentDays)
        val lastGregorian = nextFirst.minusDays(1)
        val leadingDays = (firstGregorian.dayOfWeek.value + 1) % 7 // Saturday = 0
        val gridStart = firstGregorian.minusDays(leadingDays.toLong())

        val days = List(42) { index ->
            val gregorian = gridStart.plusDays(index.toLong())
            val hijri = HijriDateCalculator.convertToHijri(gregorian, adjustmentDays)
            HijriCalendarDay(
                gregorianDate = gregorian,
                hijriDate = hijri,
                isInDisplayedMonth = hijri.year == yearMonth.year && hijri.month == yearMonth.month
            )
        }
        val firstHijri = HijriDateCalculator.convertToHijri(firstGregorian, adjustmentDays)
        return HijriCalendarMonth(
            yearMonth = yearMonth,
            monthNameEn = firstHijri.monthNameEn,
            monthNameAr = firstHijri.monthNameAr,
            firstGregorianDate = firstGregorian,
            lastGregorianDate = lastGregorian,
            days = days,
            isSacredMonth = firstHijri.isSacredMonth,
            hasSixShawwalReminder = yearMonth.month == 10
        )
    }

    /** Move the named month intact. Reconcile outward to the nearest baseline boundary,
     * using only 29/30-day months: a neighboring 30-day month can lose one day and a
     * 29-day month can gain one. Never repeat a label or invent Hijri days 31/32.
     * One stored correction has finite boundary effects, not a permanent global offset. */
    private fun boundaryShifts(storedOffset: Int, anchor: String?): Map<Int, Int> {
        val parts=anchor?.split('-') ?: return emptyMap()
        if(parts.size != 2) return emptyMap()
        val year=parts[0].toIntOrNull() ?: return emptyMap()
        val month=parts[1].toIntOrNull() ?: return emptyMap()
        val index=(year-1300)*12+month-1
        val shift=-storedOffset.coerceIn(-2,2)
        if(shift == 0 || month !in 1..12 || index !in 1 until ummAlQuraMonthStarts.lastIndex-1) return emptyMap()
        val shifts=mutableMapOf(index to shift,index+1 to shift)
        var delta=shift
        for(i in index+1 until ummAlQuraMonthStarts.lastIndex) {
            val length=ummAlQuraMonthStarts[i+1]-ummAlQuraMonthStarts[i]
            delta=0.coerceIn(29-length+delta,30-length+delta)
            shifts[i+1]=delta
            if(delta == 0) break
        }
        if(delta != 0) return emptyMap() // A correction cannot leave the supported range.
        delta=shift
        for(i in index-1 downTo 0) {
            val length=ummAlQuraMonthStarts[i+1]-ummAlQuraMonthStarts[i]
            delta=0.coerceIn(length+delta-30,length+delta-29)
            shifts[i]=delta
            if(delta == 0) break
        }
        return if(delta == 0) shifts else emptyMap()
    }
    fun scopedStart(month: HijriYearMonth, storedOffset: Int, anchor: String?): LocalDate {
        val index=(month.year-1300)*12+month.month-1
        return gregorianDateFor(month, 1, 0).plusDays((boundaryShifts(storedOffset,anchor)[index] ?: 0).toLong())
    }

    fun scopedDate(date: LocalDate, storedOffset: Int, anchor: String?): HijriDate {
        if (storedOffset == 0 || anchor == null) return HijriDateCalculator.convertToHijri(date)
        val raw = monthContaining(date, 0)
        // Offsets are bounded by two days, so only a neighboring month can own this date.
        return try {
            val month = (-1..1).map { raw.plusMonths(it) }.first {
                date.toEpochDay() >= scopedStart(it, storedOffset, anchor).toEpochDay() && date.toEpochDay() < scopedStart(it.plusMonths(1), storedOffset, anchor).toEpochDay()
            }
            val day = (date.toEpochDay() - scopedStart(month, storedOffset, anchor).toEpochDay() + 1).toInt()
            HijriDateCalculator.buildHijriDate(day, month.month, month.year)
        } catch (_: IllegalArgumentException) { HijriDateCalculator.convertToHijri(date) }
    }

    fun generateScoped(yearMonth:HijriYearMonth,storedOffset:Int,anchor:String?):HijriCalendarMonth {
        val unadjusted=generate(yearMonth,0)
        val first=scopedStart(yearMonth,storedOffset,anchor)
        val last=scopedStart(yearMonth.plusMonths(1),storedOffset,anchor).minusDays(1)
        val start=first.minusDays(((first.dayOfWeek.value+1)%7).toLong())
        val count=(((last.toEpochDay()-start.toEpochDay()+1)+6)/7*7).toInt()
        val days=(0 until count).map {
            val date=start.plusDays(it.toLong());val h=ReligiousDates.adjusted(date,storedOffset,anchor)
            HijriCalendarDay(date,h,h.year == yearMonth.year && h.month == yearMonth.month)
        }
        return unadjusted.copy(firstGregorianDate=first,lastGregorianDate=last,days=days)
    }

    fun gregorianDateFor(yearMonth: HijriYearMonth, day: Int, adjustmentDays: Int): LocalDate {
        val index = (yearMonth.year - 1300) * 12 + yearMonth.month - 1
        require(index in 0 until ummAlQuraMonthStarts.lastIndex) { "Outside supported Umm al-Qura range AH 1300–1600" }
        val length = ummAlQuraMonthStarts[index + 1] - ummAlQuraMonthStarts[index]
        require(day in 1..length)
        return LocalDate.ofEpochDay((ummAlQuraMonthStarts[index] + day - 1).toLong()).minusDays(adjustmentDays.toLong())
    }
}
