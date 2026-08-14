package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AdhanPlaybackState
import com.example.data.calculator.HijriDateCalculator
import com.example.data.models.DailyPrayerSchedule
import com.example.data.models.HijriDate
import com.example.data.models.NotificationPrayerConfig
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerTimeItem
import com.example.data.models.PrayerType
import com.example.data.models.UserLocation
import com.example.data.preferences.AppPrayerSettings
import com.example.data.qibla.QiblaCalculator
import com.example.ui.components.AthanPlayerDialog
import com.example.ui.components.HijriUmmAlQuraDialog
import com.example.ui.locale.LocalAppStrings
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoLavender
import com.example.ui.theme.BentoLavenderDark
import com.example.ui.theme.BentoPillPurple
import com.example.ui.theme.BentoRose
import com.example.ui.theme.BentoRoseDark
import com.example.ui.theme.BentoSkyBlue
import com.example.ui.theme.BentoSkyBlueDark
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NextPrayerInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PrayerHomeScreen(
    dailySchedule: DailyPrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo,
    settings: AppPrayerSettings,
    selectedDate: LocalDate,
    audioPlaybackState: AdhanPlaybackState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onPlayPrayerAthan: (PrayerType) -> Unit,
    onStopAudio: () -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var showAthanDialog by remember { mutableStateOf(false) }
    var showHijriDialog by remember { mutableStateOf(false) }
    var showExtraTimes by remember { mutableStateOf(false) }

    val isToday = selectedDate == LocalDate.now()

    val currentHijriDate = remember(selectedDate, settings.hijriAdjustmentDays, dailySchedule?.hijriDate) {
        dailySchedule?.hijriDate ?: HijriDateCalculator.convertToHijri(selectedDate, settings.hijriAdjustmentDays)
    }

    val timeFormatter = remember(settings.is24HourFormat) {
        if (settings.is24HourFormat) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern("h:mm a")
    }

    if (showAthanDialog) {
        AthanPlayerDialog(
            playbackState = audioPlaybackState,
            onPlayPrayer = onPlayPrayerAthan,
            onStop = onStopAudio,
            onDismiss = { showAthanDialog = false }
        )
    }

    if (showHijriDialog) {
        HijriUmmAlQuraDialog(
            hijriDate = currentHijriDate,
            gregorianDate = selectedDate,
            onPickDateToConvert = onDatePicked,
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = { showHijriDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("prayer_home_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Bento Header Bar
        item {
            Spacer(modifier = Modifier.height(4.dp))
            BentoHeaderSection(
                location = settings.location,
                onLocationClick = onNavigateToSettings,
                onQiblaClick = onNavigateToQibla,
                onOpenAthanDialog = { showAthanDialog = true },
                onSettingsClick = onNavigateToSettings
            )
        }

        // 2. Next Prayer Bento Hero Card
        item {
            if (isToday && nextPrayerInfo.prayerItem != null) {
                BentoNextPrayerHeroCard(
                    nextPrayerInfo = nextPrayerInfo,
                    timeFormatter = timeFormatter,
                    onListenAthan = {
                        showAthanDialog = true
                        onPlayPrayerAthan(nextPrayerInfo.prayerItem.type)
                    }
                )
            }
        }

        // 3. Bento 2-Column Grid (Qibla & Hijri Cards)
        item {
            BentoTwoColumnGrid(
                location = settings.location,
                hijriDate = currentHijriDate,
                calculationMethod = settings.calculationMethod.displayName,
                onQiblaClick = onNavigateToQibla,
                onHijriClick = { showHijriDialog = true }
            )
        }

        // 4. Date Navigation Selector
        item {
            BentoDateSelector(
                selectedDate = selectedDate,
                hijriDate = currentHijriDate,
                isToday = isToday,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onSelectToday = onSelectToday,
                onPickDate = {
                    val dialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onDatePicked(LocalDate.of(year, month + 1, dayOfMonth))
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    )
                    dialog.show()
                }
            )
        }

        // 5. Daily Prayer Times Bento List Card
        item {
            if (dailySchedule != null) {
                BentoPrayerTimesListCard(
                    prayerItems = dailySchedule.prayerItems,
                    prayerConfigs = settings.prayerConfigs,
                    timeFormatter = timeFormatter,
                    isToday = isToday,
                    onToggleNotification = { prayerType, newConfig ->
                        onUpdateNotificationConfig(
                            prayerType,
                            newConfig.enabled,
                            newConfig.soundType,
                            newConfig.preReminderMinutes
                        )
                    }
                )
            }
        }

        // 6. Extra Sunnah / Night Times Bento Card
        item {
            if (dailySchedule != null) {
                BentoExtraSunnahCard(
                    schedule = dailySchedule,
                    timeFormatter = timeFormatter,
                    expanded = showExtraTimes,
                    onToggleExpand = { showExtraTimes = !showExtraTimes }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BentoHeaderSection(
    location: UserLocation,
    onLocationClick: () -> Unit,
    onQiblaClick: () -> Unit,
    onOpenAthanDialog: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val pillBg = if (isDark) BentoLavenderDark else BentoPillPurple
    val pillIconTint = if (isDark) BentoLavender else BentoLavenderDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .clickable { onLocationClick() }
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Noor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${location.name}${if (location.country.isNotEmpty() && !location.country.contains("°")) ", ${location.country}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Athan audio button pill
            IconButton(
                onClick = onOpenAthanDialog,
                modifier = Modifier
                    .size(40.dp)
                    .background(pillBg, CircleShape)
                    .testTag("open_athan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Listen to Athan",
                    tint = pillIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings button pill
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(pillBg, CircleShape)
                    .testTag("quick_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = pillIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BentoNextPrayerHeroCard(
    nextPrayerInfo: NextPrayerInfo,
    timeFormatter: DateTimeFormatter,
    onListenAthan: () -> Unit
) {
    val prayerItem = nextPrayerInfo.prayerItem ?: return
    val prayerType = prayerItem.type
    val isDark = isSystemInDarkTheme()

    val cardBg = if (isDark) Color(0xFF42222E) else BentoRose
    val contentColor = if (isDark) Color(0xFFFFD8E4) else BentoRoseDark
    val strings = LocalAppStrings.current
    val countdownText = remember(nextPrayerInfo.remainingSeconds, strings) {
        strings.formatCountdown(nextPrayerInfo.remainingSeconds)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onListenAthan() }
            .testTag("next_prayer_hero_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Background subtle large watermark icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(100.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = getPrayerIcon(prayerType),
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(96.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Label uppercase tracking
                Text(
                    text = if (nextPrayerInfo.isNextDayFajr) (if (strings.isArabic) "صلاة الغد" else "TOMORROW'S PRAYER") else (if (strings.isArabic) "الصلاة القادمة" else "NEXT PRAYER"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.75f),
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Large Display Title + Arabic
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (strings.isArabic) strings.prayerName(prayerType) else prayerType.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        fontSize = 34.sp
                    )

                    Text(
                        text = if (strings.isArabic) strings.prayerSubtitle(prayerType) else prayerType.arabicName,
                        fontSize = if (strings.isArabic) 18.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }

                // Countdown remaining with guaranteed 123 numerals
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.90f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Athan time banner pill
                Row(
                    modifier = Modifier
                        .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (strings.isArabic) "الأذان في" else "Athan at"} ${prayerItem.time.format(timeFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoTwoColumnGrid(
    location: UserLocation,
    hijriDate: HijriDate,
    calculationMethod: String,
    onQiblaClick: () -> Unit,
    onHijriClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bearing = remember(location.latitude, location.longitude) {
        QiblaCalculator.calculateQiblaBearing(location.latitude, location.longitude).toInt()
    }

    val qiblaCardBg = if (isDark) Color(0xFF2E263D) else BentoLavender
    val qiblaTextColor = if (isDark) Color(0xFFE8DEF8) else BentoLavenderDark

    val hijriCardBg = if (isDark) Color(0xFF1E2E42) else BentoSkyBlue
    val hijriTextColor = if (isDark) Color(0xFFD3E4FF) else BentoSkyBlueDark

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Qibla Bento Card (Left Column)
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onQiblaClick() }
                .testTag("quick_qibla_button"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = qiblaCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Qibla Direction",
                        tint = qiblaTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "QIBLA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = qiblaTextColor,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$bearing° SE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = qiblaTextColor
                )
                Text(
                    text = "Accurate Direction",
                    style = MaterialTheme.typography.bodySmall,
                    color = qiblaTextColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 2. Hijri Bento Card (Right Column) - Umm al-Qura Standard
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onHijriClick() }
                .testTag("bento_hijri_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hijriCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Hijri Calendar",
                        tint = hijriTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "UMM AL-QURA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = hijriTextColor,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominent Arabic Calligraphic Hijri Date
                Text(
                    text = hijriDate.formattedAr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = hijriTextColor,
                    maxLines = 1,
                    fontSize = 17.sp
                )

                // English Subtitle
                Text(
                    text = hijriDate.formattedEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = hijriTextColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    fontSize = 12.sp
                )

                // Cultural indicator pill if applicable
                val tagText = when {
                    hijriDate.islamicEvent != null -> hijriDate.islamicEvent
                    hijriDate.isWhiteDay -> "White Day (Sunnah)"
                    hijriDate.isSacredMonth -> "Sacred Month (شهر حرام)"
                    else -> "Makkah Calendar"
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelSmall,
                    color = hijriTextColor.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BentoDateSelector(
    selectedDate: LocalDate,
    hijriDate: HijriDate,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onPickDate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.size(36.dp).testTag("prev_day_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Day",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onPickDate() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Pick Date",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isToday) "Today (${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }})"
                    else "${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${hijriDate.formattedEn} • ${hijriDate.formattedAr}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.5.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isToday) {
                IconButton(
                    onClick = onSelectToday,
                    modifier = Modifier.size(36.dp).testTag("today_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Go to Today",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = onNextDay,
                modifier = Modifier.size(36.dp).testTag("next_day_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BentoPrayerTimesListCard(
    prayerItems: List<PrayerTimeItem>,
    prayerConfigs: Map<PrayerType, NotificationPrayerConfig>,
    timeFormatter: DateTimeFormatter,
    isToday: Boolean,
    onToggleNotification: (PrayerType, NotificationPrayerConfig) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBorderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else BentoBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            prayerItems.forEachIndexed { index, item ->
                BentoPrayerRow(
                    item = item,
                    config = prayerConfigs[item.type] ?: NotificationPrayerConfig(),
                    timeFormatter = timeFormatter,
                    isToday = isToday,
                    onToggleNotification = onToggleNotification
                )
                if (index < prayerItems.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoPrayerRow(
    item: PrayerTimeItem,
    config: NotificationPrayerConfig,
    timeFormatter: DateTimeFormatter,
    isToday: Boolean,
    onToggleNotification: (PrayerType, NotificationPrayerConfig) -> Unit
) {
    val isNext = item.isNext && isToday
    var showMenu by remember { mutableStateOf(false) }

    val rowBg = if (isNext) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val prayerTextColor = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val timeTextColor = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("prayer_card_${item.type.name.lowercase()}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Icon + English Name + Arabic Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPrayerIcon(item.type),
                    contentDescription = item.type.title,
                    tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.type.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = prayerTextColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.type.arabicName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (item.type == PrayerType.SUNRISE) {
                    Text(
                        text = "Sunrise (Shuruk)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Right: Time + Notification Mode Icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.time.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = timeTextColor
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = when {
                            !config.enabled -> Icons.Default.NotificationsOff
                            config.soundType == NotificationSoundType.FULL_ATHAN -> Icons.Default.NotificationsActive
                            config.soundType == NotificationSoundType.SILENT -> Icons.Default.NotificationsOff
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = "Notification Mode",
                        tint = if (config.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Makkah Adhan") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.FULL_ATHAN))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.VolumeUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Madinah Adhan") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.ATHAN_MADINAH))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.VolumeUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Al-Aqsa Adhan") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.ATHAN_AL_AQSA))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.VolumeUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Cairo Adhan") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.ATHAN_CAIRO))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.VolumeUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Takbeer Only") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.SHORT_TAKBEER))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.NotificationsActive, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Gentle Chime") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.MELODIC_TONE))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Notifications, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Vibrate Only") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = true, soundType = NotificationSoundType.VIBRATE_ONLY))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Notifications, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Silent / Off") },
                        onClick = {
                            onToggleNotification(item.type, config.copy(enabled = false))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoExtraSunnahCard(
    schedule: DailyPrayerSchedule,
    timeFormatter: DateTimeFormatter,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else BentoBorder

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onToggleExpand() }
            .testTag("extra_sunnah_times_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sunnah & Night Calculations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    ExtraTimeRow(label = "Dhuha Time (approx.)", time = schedule.dhuha.format(timeFormatter), arabic = "الضحى")
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtraTimeRow(label = "Islamic Midnight", time = schedule.islamicMidnight.format(timeFormatter), arabic = "منتصف الليل")
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtraTimeRow(label = "Qiyam / Last 1/3 of Night (Tahajjud)", time = schedule.lastThirdOfNight.format(timeFormatter), arabic = "قيام الليل")
                }
            }
        }
    }
}

@Composable
private fun ExtraTimeRow(label: String, time: String, arabic: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = arabic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

fun getPrayerIcon(type: PrayerType): ImageVector {
    return when (type) {
        PrayerType.FAJR -> Icons.Default.WbTwilight
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.Nightlight
        PrayerType.ISHA -> Icons.Default.Bedtime
    }
}
