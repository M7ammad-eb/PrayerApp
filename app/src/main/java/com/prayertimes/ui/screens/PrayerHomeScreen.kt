package com.prayertimes.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prayertimes.data.calculator.HijriDateCalculator
import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.HijriDate
import com.prayertimes.data.models.NotificationPrayerConfig
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerTimeItem
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.locale.LocalAppStrings
import com.prayertimes.ui.theme.BentoBorder
import com.prayertimes.ui.viewmodel.NextPrayerInfo
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerHomeScreen(
    dailySchedule: DailyPrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo,
    settings: AppPrayerSettings,
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onRequestNotificationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var soundPickerPrayerType by remember { mutableStateOf<PrayerType?>(null) }
    var showExtraTimes by remember { mutableStateOf(false) }

    val bannerPrefs = remember { context.getSharedPreferences("notification_banner_prefs", Context.MODE_PRIVATE) }
    var notificationsEnabled by remember {
        mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var isWarningDismissed by remember {
        mutableStateOf(bannerPrefs.getBoolean("dismissed", false))
    }

    // areNotificationsEnabled() is a live OS query, not something Compose observes on its own - a
    // plain remember{} here would freeze the very first read forever, so the banner would never
    // clear after the user grants the permission from system settings and returns (short of fully
    // restarting the app). Re-checking on every resume catches that return trip. A dismissal is
    // also only "sticky" until permissions actually change, so silently re-disabling notifications
    // later brings the banner back instead of hiding it forever from one old dismiss tap.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                notificationsEnabled = nowEnabled
                if (nowEnabled && isWarningDismissed) {
                    isWarningDismissed = false
                    bannerPrefs.edit().putBoolean("dismissed", false).apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val strings = LocalAppStrings.current

    val isToday = selectedDate == LocalDate.now()

    val currentHijriDate = remember(selectedDate, settings.hijriAdjustmentDays, dailySchedule?.hijriDate) {
        dailySchedule?.hijriDate ?: HijriDateCalculator.convertToHijri(selectedDate, settings.hijriAdjustmentDays)
    }

    val timeFormatter = remember(settings.is24HourFormat) {
        if (settings.is24HourFormat) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern("h:mm a")
    }

    soundPickerPrayerType?.let { pickerType ->
        val currentConfig = settings.prayerConfigs[pickerType] ?: NotificationPrayerConfig()
        SoundPickerDialog(
            prayerType = pickerType,
            currentSound = currentConfig.soundType,
            onSelectSound = { sound ->
                onUpdateNotificationConfig(
                    pickerType,
                    sound != NotificationSoundType.SILENT,
                    sound,
                    currentConfig.preReminderMinutes
                )
                soundPickerPrayerType = null
            },
            onPreviewSound = { sound -> onPreviewSound(sound, pickerType) },
            onDismiss = { soundPickerPrayerType = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("prayer_home_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Warning Banner if OS Notifications are disabled
        if (!notificationsEnabled && !isWarningDismissed) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = strings.notificationsDisabledWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    isWarningDismissed = true
                                    bannerPrefs.edit().putBoolean("dismissed", true).apply()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                onRequestNotificationPermission?.invoke()
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.enableNotificationsBtn,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }

        // 1. Next Prayer Hero Card - always relative to the actual current time, so it stays
        // visible regardless of which day is being browsed below via the date navigator.
        item {
            if (nextPrayerInfo.prayerItem != null) {
                Spacer(modifier = Modifier.height(2.dp))
                BentoNextPrayerHeroCard(
                    nextPrayerInfo = nextPrayerInfo,
                    timeFormatter = timeFormatter,
                    onClick = { soundPickerPrayerType = nextPrayerInfo.prayerItem.type }
                )
            }
        }

        // 2. Date Navigation Selector (Gregorian & Umm al-Qura Hijri)
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

        // 3. Daily Prayer Times List Card (All 6 Times)
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

        // 4. Sunnah & Night Times Calculation Card (Duha, Midnight, Qiyam)
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BentoNextPrayerHeroCard(
    nextPrayerInfo: NextPrayerInfo,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    val prayerItem = nextPrayerInfo.prayerItem ?: return
    val prayerType = prayerItem.type
    val isDark = isSystemInDarkTheme()

    val cardBg = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val strings = LocalAppStrings.current
    val countdownText = remember(nextPrayerInfo.remainingSeconds, strings) {
        strings.formatCountdown(nextPrayerInfo.remainingSeconds)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("next_prayer_hero_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Background watermark icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(90.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = getPrayerIcon(prayerType),
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(86.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Tracking label
                Text(
                    text = if (nextPrayerInfo.isNextDayFajr) strings.tomorrowPrayerLabel else strings.nextPrayerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.75f),
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Large Display Title (Single clean prayer name)
                Text(
                    text = strings.prayerName(prayerType),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    fontSize = 32.sp
                )

                // Countdown remaining with guaranteed 123 numerals
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.90f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Athan time banner pill
                Row(
                    modifier = Modifier
                        .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.athanAt} ${prayerItem.time.format(timeFormatter)}",
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
private fun BentoDateSelector(
    selectedDate: LocalDate,
    hijriDate: HijriDate,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onPickDate: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.size(36.dp).testTag("prev_day_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.previousDay,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onPickDate() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = strings.pickDate,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isToday) "${strings.today} (${strings.formatDateShort(selectedDate)})"
                    else "${strings.formatDateShort(selectedDate)} ${selectedDate.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${if (strings.isArabic) hijriDate.formattedAr else hijriDate.formattedEn}",
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
                        contentDescription = strings.goToToday,
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
                    contentDescription = strings.nextDay,
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
    val cardBorderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else BentoBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
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
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isNext = item.isNext && isToday
    var showMenu by remember { mutableStateOf(false) }

    val rowBg = if (isNext) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val prayerTextColor = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val timeTextColor = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(14.dp))
            .clickable {
                val now = ZonedDateTime.now(item.zonedDateTime.zone)
                val diffSeconds = Duration.between(now, item.zonedDateTime).seconds
                val statusText = if (diffSeconds > 0) strings.formatCountdown(diffSeconds) else strings.formatSince(-diffSeconds)
                Toast.makeText(context, "${strings.prayerName(item.type)}: $statusText", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("prayer_card_${item.type.name.lowercase()}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Icon + Localized Name + Subtitle
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
                    contentDescription = strings.prayerName(item.type),
                    tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = strings.prayerName(item.type),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = prayerTextColor
                )
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
                        contentDescription = strings.notifSectionTitle,
                        tint = if (config.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    NotificationSoundType.values().forEach { sound ->
                        DropdownMenuItem(
                            text = { Text(strings.soundTypeName(sound)) },
                            onClick = {
                                val isEnabled = sound != NotificationSoundType.SILENT
                                onToggleNotification(item.type, config.copy(enabled = isEnabled, soundType = sound))
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sound == NotificationSoundType.SILENT) Icons.Default.NotificationsOff else Icons.Default.VolumeUp,
                                    contentDescription = null
                                )
                            }
                        )
                    }
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
    val strings = LocalAppStrings.current
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggleExpand() }
            .testTag("extra_sunnah_times_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = strings.additionalTimesTitle,
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
                    ExtraTimeRow(label = strings.duha, time = schedule.dhuha.format(timeFormatter))
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtraTimeRow(label = strings.midnight, time = schedule.islamicMidnight.format(timeFormatter))
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtraTimeRow(label = strings.lastThirdNight, time = schedule.lastThirdOfNight.format(timeFormatter))
                }
            }
        }
    }
}

@Composable
private fun ExtraTimeRow(label: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
