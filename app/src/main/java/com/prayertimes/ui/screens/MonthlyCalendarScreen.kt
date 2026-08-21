package com.prayertimes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.UserLocation
import androidx.compose.ui.platform.LocalContext
import com.prayertimes.ui.locale.LocalAppStrings
import com.prayertimes.ui.theme.GoldAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyCalendarScreen(
    monthlySchedule: List<DailyPrayerSchedule>,
    selectedDate: LocalDate,
    location: UserLocation,
    is24HourFormat: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val strings = LocalAppStrings.current
    val timeFormatter = remember(is24HourFormat) {
        if (is24HourFormat) DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH) else DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    }

    val headerTitle = "${strings.monthName(selectedDate.monthValue)} ${selectedDate.year}"
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("monthly_calendar_screen")
    ) {
        // Month Selector Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.prevMonth)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = com.prayertimes.data.cities.CityDatabase.localizedName(LocalContext.current.resources, location),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = strings.nextMonth)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Table Header & Rows
        val horizontalScrollState = rememberScrollState()

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = strings.dateCol, width = 60.dp, isHeader = true, textColor = Color.White)
                    TableCell(text = strings.prayerName(PrayerType.FAJR), width = 76.dp, isHeader = true, textColor = Color.White)
                    TableCell(text = strings.prayerName(PrayerType.SUNRISE), width = 76.dp, isHeader = true, textColor = Color.White)
                    TableCell(text = strings.prayerName(PrayerType.DHUHR), width = 76.dp, isHeader = true, textColor = Color.White)
                    TableCell(text = strings.prayerName(PrayerType.ASR), width = 76.dp, isHeader = true, textColor = Color.White)
                    TableCell(text = strings.prayerName(PrayerType.MAGHRIB), width = 76.dp, isHeader = true, textColor = Color.White)
                    TableCell(text = strings.prayerName(PrayerType.ISHA), width = 76.dp, isHeader = true, textColor = Color.White)
                }

                // Days Rows
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(monthlySchedule) { index, item ->
                        val isTodayRow = item.date == today
                        val rowBg = when {
                            isTodayRow -> GoldAccent.copy(alpha = 0.2f)
                            index % 2 == 1 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dayText = "${item.date.dayOfMonth}"
                            TableCell(
                                text = dayText,
                                width = 60.dp,
                                isHighlight = isTodayRow
                            )
                            TableCell(text = item.fajr.format(timeFormatter), width = 76.dp, isHighlight = isTodayRow)
                            TableCell(text = item.sunrise.format(timeFormatter), width = 76.dp, isHighlight = isTodayRow)
                            TableCell(text = item.dhuhr.format(timeFormatter), width = 76.dp, isHighlight = isTodayRow)
                            TableCell(text = item.asr.format(timeFormatter), width = 76.dp, isHighlight = isTodayRow)
                            TableCell(text = item.maghrib.format(timeFormatter), width = 76.dp, isHighlight = isTodayRow)
                            TableCell(text = item.isha.format(timeFormatter), width = 76.dp, isHighlight = isTodayRow)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isHighlight: Boolean = false,
    textColor: Color? = null
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader || isHighlight) FontWeight.Bold else FontWeight.Normal,
        color = textColor ?: if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        fontSize = if (isHeader) 13.sp else 12.sp
    )
}
