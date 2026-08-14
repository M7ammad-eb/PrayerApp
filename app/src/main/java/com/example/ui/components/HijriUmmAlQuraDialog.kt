package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.HijriDate
import com.example.ui.theme.BentoSkyBlue
import com.example.ui.theme.BentoSkyBlueDark
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HijriUmmAlQuraDialog(
    hijriDate: HijriDate,
    gregorianDate: LocalDate,
    onPickDateToConvert: (LocalDate) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val gregorianFormatted = gregorianDate.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    )

    val isMondayOrThursday = gregorianDate.dayOfWeek == DayOfWeek.MONDAY || gregorianDate.dayOfWeek == DayOfWeek.THURSDAY

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("hijri_umm_al_qura_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BentoSkyBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = BentoSkyBlueDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Hijri Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Umm al-Qura Standard",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_hijri_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Hijri Date Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoSkyBlue)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = hijriDate.formattedAr,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoSkyBlueDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = hijriDate.formattedEn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BentoSkyBlueDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gregorian equivalent pill
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = BentoSkyBlueDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = gregorianFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoSkyBlueDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Islamic Significance / Events
                if (hijriDate.islamicEvent != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF4E3))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = hijriDate.islamicEvent,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                if (hijriDate.islamicEventAr != null) {
                                    Text(
                                        text = hijriDate.islamicEventAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Cultural Context Badges
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hijriDate.isSacredMonth) {
                        CulturalBadge(
                            title = "Sacred Month (الأشهر الحرم)",
                            description = "${hijriDate.monthNameEn} (${hijriDate.monthNameAr}) is one of the four sacred months in Islam (Dhu al-Qi'dah, Dhu al-Hijjah, Muharram, and Rajab) in which good deeds are amplified.",
                            icon = Icons.Default.Mosque,
                            tint = Color(0xFF0F766E),
                            bgColor = Color(0xFFCCFBF1)
                        )
                    }

                    if (hijriDate.isWhiteDay) {
                        CulturalBadge(
                            title = "White Days (الأيام البيض)",
                            description = "Day ${hijriDate.day} of the lunar month. Fasting the 13th, 14th, and 15th of each Hijri month is an established Sunnah of the Prophet ﷺ.",
                            icon = Icons.Default.Star,
                            tint = Color(0xFF1D4ED8),
                            bgColor = Color(0xFFDBEAFE)
                        )
                    }

                    if (isMondayOrThursday) {
                        CulturalBadge(
                            title = "Sunnah Fasting Day",
                            description = "Today is ${gregorianDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}. Fasting on Mondays and Thursdays is an authentic Sunnah practice.",
                            icon = Icons.Default.AutoAwesome,
                            tint = Color(0xFF7C3AED),
                            bgColor = Color(0xFFEDE9FE)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // About Umm Al Qura Calendar
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "About Umm al-Qura Calendar (تقويم أم القرى)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "The Umm al-Qura calendar is the official standardized Islamic lunar calendar calculated by the King Abdulaziz City for Science and Technology (KACST) for the holy city of Makkah al-Mukarramah. It aligns astronomical conjunctions with Islamic calendar principles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val dialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onPickDateToConvert(LocalDate.of(year, month + 1, dayOfMonth))
                                },
                                gregorianDate.year,
                                gregorianDate.monthValue - 1,
                                gregorianDate.dayOfMonth
                            )
                            dialog.show()
                        },
                        modifier = Modifier.weight(1f).testTag("convert_date_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Convert Date", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToSettings()
                        },
                        modifier = Modifier.weight(1f).testTag("hijri_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Adjust Days", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CulturalBadge(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bgColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tint.copy(alpha = 0.9f),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
