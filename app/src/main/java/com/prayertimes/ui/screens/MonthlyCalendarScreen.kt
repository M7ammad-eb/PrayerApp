package com.prayertimes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertimes.R
import com.prayertimes.data.calendar.HijriCalendar
import com.prayertimes.data.calendar.HijriCalendarDay
import com.prayertimes.data.calendar.HijriCalendarMonth
import com.prayertimes.data.calendar.HijriYearMonth
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.models.IslamicObservance
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.locale.LocalAppStrings
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MonthlyCalendarScreen(
    selectedDate: LocalDate,
    settings: AppPrayerSettings,
    hijriAdjustmentDays: Int,
    onViewPrayerTimes: (LocalDate) -> Unit
) {
    val strings = LocalAppStrings.current
    val prayerZone = remember(settings.location.timeZoneId) {
        runCatching { ZoneId.of(settings.location.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    }
    var islamicToday by remember(settings) {
        mutableStateOf(resolveCurrentHijriCivilDate(settings, prayerZone, ZonedDateTime.now(prayerZone)).first)
    }
    var previousIslamicToday by remember { mutableStateOf(islamicToday) }
    val pagerOrigin = remember { HijriYearMonth(1300, 1) }
    val locationCivilToday = ZonedDateTime.now(prayerZone).toLocalDate()
    val initialSelectedDate = if (selectedDate == locationCivilToday) islamicToday else selectedDate
    val initialMonth = remember { HijriCalendar.monthContaining(initialSelectedDate, hijriAdjustmentDays) }
    val initialPage = (initialMonth.year - pagerOrigin.year) * 12 + initialMonth.month - pagerOrigin.month
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3600 })
    val coroutineScope = rememberCoroutineScope()
    val visibleMonth = pagerOrigin.plusMonths(pagerState.settledPage)
    var selectedGregorianDate by remember { mutableStateOf(initialSelectedDate) }
    val month = remember(visibleMonth, hijriAdjustmentDays) {
        HijriCalendar.generate(visibleMonth, hijriAdjustmentDays)
    }
    val visibleWeekCount = remember(month) {
        month.days.chunked(7).count { week -> week.any { it.isInDisplayedMonth } }
    }
    val calendarHeight = if (visibleWeekCount >= 6) 374.dp else 324.dp

    // A Hijri date changes at Maghrib. Sleep until that exact boundary rather than running a
    // calendar timer continuously; when it arrives, advance the live day and its selected cell.
    LaunchedEffect(settings, prayerZone) {
        while (isActive) {
            val now = ZonedDateTime.now(prayerZone)
            val (currentDate, nextMaghrib) = resolveCurrentHijriCivilDate(settings, prayerZone, now)
            islamicToday = currentDate
            delay(Duration.between(now, nextMaghrib).toMillis().coerceAtLeast(1_000L) + 500L)
        }
    }

    LaunchedEffect(islamicToday) {
        if (islamicToday != previousIslamicToday && selectedGregorianDate == previousIslamicToday) {
            selectedGregorianDate = islamicToday
            val todayMonth = HijriCalendar.monthContaining(islamicToday, hijriAdjustmentDays)
            val todayPage = (todayMonth.year - pagerOrigin.year) * 12 + todayMonth.month - pagerOrigin.month
            pagerState.scrollToPage(todayPage)
        }
        previousIslamicToday = islamicToday
    }

    LaunchedEffect(visibleMonth, hijriAdjustmentDays) {
        if (selectedGregorianDate !in month.firstGregorianDate..month.lastGregorianDate) {
            selectedGregorianDate = month.firstGregorianDate
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("hijri_calendar_screen")
    ) {
        HijriMonthHeader(
            month = month,
            isArabic = strings.isArabic,
            onPreviousMonth = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNextMonth = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            onToday = {
                val todayMonth = HijriCalendar.monthContaining(islamicToday, hijriAdjustmentDays)
                val todayPage = (todayMonth.year - pagerOrigin.year) * 12 + todayMonth.month - pagerOrigin.month
                coroutineScope.launch { pagerState.animateScrollToPage(todayPage) }
                selectedGregorianDate = islamicToday
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calendarHeight),
                // Avoid composing two 42-cell off-screen calendars while this tab is opening.
                // Generated months are cached, so an adjacent page is still ready after first use.
                beyondViewportPageCount = 0,
                key = { it }
            ) { page ->
                val pageMonth = remember(page, hijriAdjustmentDays) {
                    HijriCalendar.generate(pagerOrigin.plusMonths(page), hijriAdjustmentDays)
                }
                HijriMonthGrid(
                    month = pageMonth,
                    selectedDate = selectedGregorianDate,
                    today = islamicToday,
                    onSelectDate = { selectedGregorianDate = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 10.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val selectedDay = month.days.firstOrNull {
                    it.isInDisplayedMonth && it.gregorianDate == selectedGregorianDate
                }
                    ?: month.days.first { it.isInDisplayedMonth }
                SelectedHijriDayCard(
                    day = selectedDay,
                    isArabic = strings.isArabic,
                    onViewPrayerTimes = { onViewPrayerTimes(selectedDay.gregorianDate) }
                )
            }
            item {
                CalendarSourceNote()
            }
        }
    }
}

@Composable
private fun HijriMonthHeader(
    month: HijriCalendarMonth,
    isArabic: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
    val rangeFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val yearFormatter = remember(locale) { DateTimeFormatter.ofPattern("yyyy", locale) }
    val gregorianRange = buildString {
        append(month.firstGregorianDate.format(rangeFormatter))
        append(" – ")
        append(month.lastGregorianDate.format(rangeFormatter))
        append(" ")
        append(month.lastGregorianDate.format(yearFormatter))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(40.dp)) {
            Icon(
                if (isArabic) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.prev_month)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .height(76.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (month.isSacredMonth) {
                    stringResource(R.string.hijri_calendar_sacred_month_badge)
                } else {
                    " "
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (month.isSacredMonth) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isArabic) {
                    "${month.monthNameAr} ${month.yearMonth.year} هـ"
                } else {
                    "${month.monthNameEn} ${month.yearMonth.year} AH"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = gregorianRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = onToday,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                ) {
                    Text(
                        stringResource(R.string.hijri_calendar_today),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        IconButton(onClick = onNextMonth, modifier = Modifier.size(40.dp)) {
            Icon(
                if (isArabic) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}

private fun resolveCurrentHijriCivilDate(
    settings: AppPrayerSettings,
    zoneId: ZoneId,
    now: ZonedDateTime
): Pair<LocalDate, ZonedDateTime> {
    fun maghribFor(date: LocalDate): ZonedDateTime = PrayerTimesCalculator.calculateDailySchedule(
        date = date,
        latitude = settings.location.latitude,
        longitude = settings.location.longitude,
        zoneId = zoneId,
        method = settings.calculationMethod,
        juristicMethod = settings.juristicMethod,
        highLatitudeRule = settings.highLatitudeRule,
        adjustments = settings.adjustments,
        hijriAdjustmentDays = settings.hijriAdjustmentDays,
        now = now
    ).prayerItems.first { it.type == PrayerType.MAGHRIB }.zonedDateTime

    val civilToday = now.toLocalDate()
    val todayMaghrib = maghribFor(civilToday)
    return if (now.isBefore(todayMaghrib)) {
        civilToday to todayMaghrib
    } else {
        civilToday.plusDays(1) to maghribFor(civilToday.plusDays(1))
    }
}

@Composable
private fun HijriMonthGrid(
    month: HijriCalendarMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf(
        R.string.calendar_weekday_sat_short,
        R.string.calendar_weekday_sun_short,
        R.string.calendar_weekday_mon_short,
        R.string.calendar_weekday_tue_short,
        R.string.calendar_weekday_wed_short,
        R.string.calendar_weekday_thu_short,
        R.string.calendar_weekday_fri_short
    )
    Column(modifier = modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEachIndexed { index, label ->
                    Text(
                        text = stringResource(label),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == 6) FontWeight.Bold else FontWeight.Medium,
                        color = if (index == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            month.days.chunked(7).filter { week -> week.any { it.isInDisplayedMonth } }.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    week.forEach { day ->
                        HijriDayCell(
                            day = day,
                            selected = day.gregorianDate == selectedDate,
                            today = day.gregorianDate == today,
                            onClick = { onSelectDate(day.gregorianDate) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            CalendarGridLegend()
    }
}

@Composable
private fun HijriDayCell(
    day: HijriCalendarDay,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eid = day.hijriDate.observances.any { it == IslamicObservance.EID_AL_FITR || it == IslamicObservance.EID_AL_ADHA }
    val fasting = day.hijriDate.observances.any {
        it == IslamicObservance.TASUA || it == IslamicObservance.ASHURA ||
            it == IslamicObservance.WHITE_DAY || it == IslamicObservance.ARAFAH
    }
    val special = day.hijriDate.observances.any {
        it != IslamicObservance.RAMADAN && it != IslamicObservance.WHITE_DAY &&
            it != IslamicObservance.FASTING_PROHIBITED
    }
    val eventColor = if (
        eid || fasting || special || day.hijriDate.observances.contains(IslamicObservance.FASTING_PROHIBITED)
    ) MaterialTheme.colorScheme.error else null
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        today -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        day.hijriDate.month == 9 && day.isInDisplayedMonth -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val outline = when {
        selected -> MaterialTheme.colorScheme.primary
        today -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }
    val primaryTextColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (day.isInDisplayedMonth) {
        Box(
            modifier = modifier
                .padding(horizontal = 2.dp, vertical = 1.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background, MaterialTheme.shapes.small)
                    .border(if (today && !selected) 1.dp else 0.dp, outline, MaterialTheme.shapes.small)
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.hijriDate.day.toString(),
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (selected || today || eid) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) primaryTextColor else if (eid) MaterialTheme.colorScheme.primary else primaryTextColor
                )
                Text(
                    text = "${day.gregorianDate.dayOfMonth}/${day.gregorianDate.monthValue}",
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = if (!selected) MaterialTheme.colorScheme.primary else secondaryTextColor,
                    maxLines = 1
                )
            }
            if (eventColor != null) {
                EventDot(
                    color = eventColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                )
            }
        }
    } else {
        Spacer(modifier)
    }
}

@Composable
private fun EventDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(7.dp).background(color, CircleShape))
}

@Composable
private fun CalendarGridLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                stringResource(R.string.hijri_calendar_legend_selected),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                stringResource(R.string.hijri_calendar_legend_special),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarSourceNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            CalendarNoteRow(
                title = stringResource(R.string.hijri_calendar_day_boundary_title),
                description = stringResource(R.string.hijri_calendar_day_boundary_note),
                icon = Icons.Default.Star
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
            CalendarNoteRow(
                title = stringResource(R.string.hijri_calendar_source_title),
                description = stringResource(R.string.hijri_calendar_source_note),
                icon = Icons.Default.CalendarMonth
            )
        }
    }
}

@Composable
private fun CalendarNoteRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedHijriDayCard(
    day: HijriCalendarDay,
    isArabic: Boolean,
    onViewPrayerTimes: () -> Unit
) {
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
    val gregorianFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val visibleObservances = day.hijriDate.observances.filterNot {
        // Ramadan is already clear from the month title; keep this section specific to events
        // that belong to the selected date.
        it == IslamicObservance.RAMADAN
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) day.hijriDate.formattedAr else day.hijriDate.formattedEn,
                        style = MaterialTheme.typography.titleMedium,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = day.gregorianDate.format(gregorianFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onViewPrayerTimes,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.nav_prayer_times), fontWeight = FontWeight.Bold)
                }
            }
            if (visibleObservances.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
                visibleObservances.forEach { observance ->
                    ObservanceRow(observance)
                }
            }
        }
    }
}

@Composable
private fun ObservanceRow(observance: IslamicObservance) {
    val (title, description) = observanceText(observance)
    InfoRow(
        title = title,
        description = description,
        fasting = observance in setOf(
            IslamicObservance.TASUA,
            IslamicObservance.ASHURA,
            IslamicObservance.WHITE_DAY,
            IslamicObservance.ARAFAH
        )
    )
}

@Composable
private fun InfoRow(title: String, description: String, fasting: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = if (fasting) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (fasting) Icons.Default.Restaurant else Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (fasting) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (description.isNotBlank()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun observanceText(observance: IslamicObservance): Pair<String, String> = when (observance) {
    IslamicObservance.HIJRI_NEW_YEAR -> stringResource(R.string.hijri_observance_new_year) to ""
    IslamicObservance.TASUA -> stringResource(R.string.hijri_observance_tasua) to stringResource(R.string.hijri_observance_recommended_fasting)
    IslamicObservance.ASHURA -> stringResource(R.string.hijri_observance_ashura) to stringResource(R.string.hijri_observance_recommended_fasting)
    IslamicObservance.WHITE_DAY -> stringResource(R.string.hijri_observance_white_day) to stringResource(R.string.hijri_observance_white_day_desc)
    IslamicObservance.RAMADAN -> stringResource(R.string.hijri_observance_ramadan) to ""
    IslamicObservance.RAMADAN_START -> stringResource(R.string.hijri_observance_ramadan_start) to ""
    IslamicObservance.RAMADAN_LAST_TEN_NIGHTS -> stringResource(R.string.hijri_observance_last_ten_nights) to stringResource(R.string.hijri_observance_last_ten_nights_desc)
    IslamicObservance.RAMADAN_ODD_NIGHT -> stringResource(R.string.hijri_observance_odd_night) to stringResource(R.string.hijri_observance_odd_night_desc)
    IslamicObservance.EID_AL_FITR -> stringResource(R.string.hijri_observance_eid_fitr) to ""
    IslamicObservance.FIRST_TEN_DHU_AL_HIJJAH -> stringResource(R.string.hijri_observance_first_ten_dhul_hijjah) to ""
    IslamicObservance.TARWIYAH -> stringResource(R.string.hijri_observance_tarwiyah) to ""
    IslamicObservance.ARAFAH -> stringResource(R.string.hijri_observance_arafah) to stringResource(R.string.hijri_observance_arafah_desc)
    IslamicObservance.EID_AL_ADHA -> stringResource(R.string.hijri_observance_eid_adha) to ""
    IslamicObservance.TASHREEQ -> stringResource(R.string.hijri_observance_tashreeq) to stringResource(R.string.hijri_observance_tashreeq_desc)
    IslamicObservance.FASTING_PROHIBITED -> stringResource(R.string.hijri_observance_fasting_prohibited) to stringResource(R.string.hijri_observance_fasting_prohibited_desc)
}
