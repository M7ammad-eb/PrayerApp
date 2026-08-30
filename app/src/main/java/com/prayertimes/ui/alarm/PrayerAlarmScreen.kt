package com.prayertimes.ui.alarm

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertimes.audio.AdhanPlaybackState
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.ui.locale.LocalAppStrings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerAlarmScreen(
    prayerType: PrayerType,
    prayerTimeFormatted: String,
    locationName: String,
    soundType: NotificationSoundType,
    playbackState: AdhanPlaybackState,
    onStopAthan: () -> Unit,
    onSnooze: () -> Unit,
    onOpenApp: () -> Unit,
    onTogglePlayPause: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val localizedPrayerName = strings.prayerName(prayerType)
    var showDuaSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-screen Celestial Time-of-Day Dynamic Canvas Artwork
        PrayerTimeOfDayArtworkCanvas(
            prayerType = prayerType,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark translucent overlay for text contrast and readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 2. Main Alarm Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Prayer Type Badge, Mosque Icon & Time of Day
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.alarmBadge(localizedPrayerName.uppercase()),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Text(
                    text = localizedPrayerName,
                    color = Color(0xFFFFE082),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )

                Text(
                    text = strings.alarmTimeForPrayer(localizedPrayerName),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = prayerTimeFormatted,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (locationName.isNotBlank()) {
                        Text(
                            text = " • $locationName",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Center Area: Adhan Audio Status & Progress
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = soundType.localizedDisplayName(strings.isArabic),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (soundType.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = soundType.subtitle,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live progress bar
                    LinearProgressIndicator(
                        progress = { playbackState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(0xFFFFD54F),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Action Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big Dismiss / Stop Athan Button
                Button(
                    onClick = onStopAthan,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("stop_athan_alarm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.alarmStopAthanBtn,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Action Row: Snooze & Dua
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSnooze,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("snooze_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.alarmSnooze5mBtn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = { showDuaSheet = true },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("dua_after_athan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.alarmDuaBtn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Open App Link Button
                OutlinedButton(
                    onClick = onOpenApp,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_app_from_alarm_button")
                ) {
                    Text(text = strings.alarmOpenAppBtn, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Dua After Athan
    if (showDuaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDuaSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.alarmDuaSheetTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 30.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Allahumma Rabba hadhihid-da'watit-tammah, was-salatil-qa'imah, ati Muhammadan al-wasilata wal-fadilah, wab'athhu maqaman mahmudan alladhi wa'adtah.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "\"O Allah, Lord of this perfect call and established prayer, grant Muhammad the intercession and superiority, and raise him to the praised station which You have promised him.\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showDuaSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(strings.close)
                }
            }
        }
    }
}

/**
 * Renders atmospheric procedural celestial time-of-day canvas artwork for each prayer.
 */
@Composable
fun PrayerTimeOfDayArtworkCanvas(
    prayerType: PrayerType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars_shimmer")
    val starShimmer by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starShimmer"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (prayerType) {
            PrayerType.FAJR -> {
                // FAJR: Dawn twilight (Deep Indigo to Ethereal Violet to Golden Dawn Horizon)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B),
                            Color(0xFF4C1D95),
                            Color(0xFFB45309),
                            Color(0xFFF59E0B)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                // Twinkling dawn stars fading in upper sky
                drawStars(this, starShimmer * 0.7f, count = 28, maxH = h * 0.45f)
                // Crescent moon at dawn
                drawCrescentMoon(this, center = Offset(w * 0.78f, h * 0.18f), radius = 32.dp.toPx(), color = Color(0xFFFEF3C7))
                // Dawn Islamic Mosque Silhouette
                drawMosqueSilhouette(this, baseColor = Color(0xFF0A0F1D))
            }

            PrayerType.SUNRISE -> {
                // SUNRISE: Radiant Golden Morning
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7C2D12),
                            Color(0xFFC2410C),
                            Color(0xFFEA580C),
                            Color(0xFFFBBF24),
                            Color(0xFFFEF08A)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                // Sun disk rising on the horizon
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.42f),
                        radius = 160.dp.toPx()
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = 160.dp.toPx()
                )
                drawSunRays(this, center = Offset(w * 0.5f, h * 0.42f), rayCount = 12, length = 220.dp.toPx())
                // Mosque Silhouette
                drawMosqueSilhouette(this, baseColor = Color(0xFF2A0800))
            }

            PrayerType.DHUHR -> {
                // DHUHR: Brilliant Midday Daylight (Zenith Sun & Turquoise Dome Ambiance)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0369A1),
                            Color(0xFF0284C7),
                            Color(0xFF38BDF8),
                            Color(0xFF7DD3FC),
                            Color(0xFFE0F2FE)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                // Radiant Zenith Sun
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFFEF08A), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.16f),
                        radius = 140.dp.toPx()
                    ),
                    center = Offset(w * 0.5f, h * 0.16f),
                    radius = 140.dp.toPx()
                )
                drawSunRays(this, center = Offset(w * 0.5f, h * 0.16f), rayCount = 16, length = 200.dp.toPx())
                // Mosque Silhouette
                drawMosqueSilhouette(this, baseColor = Color(0xFF075985))
            }

            PrayerType.ASR -> {
                // ASR: Warm Golden Hour Afternoon
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9A3412),
                            Color(0xFFC2410C),
                            Color(0xFFD97706),
                            Color(0xFFF59E0B),
                            Color(0xFFFDE68A)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                // Afternoon Sun positioned lower in the sky
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color.Transparent),
                        center = Offset(w * 0.28f, h * 0.32f),
                        radius = 120.dp.toPx()
                    ),
                    center = Offset(w * 0.28f, h * 0.32f),
                    radius = 120.dp.toPx()
                )
                // Mosque Silhouette
                drawMosqueSilhouette(this, baseColor = Color(0xFF431407))
            }

            PrayerType.MAGHRIB -> {
                // MAGHRIB: Breathtaking Sunset Twilight (Plum to Crimson to Amber Dusk)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E1065),
                            Color(0xFF581C87),
                            Color(0xFF9D174D),
                            Color(0xFFE11D48),
                            Color(0xFFF97316),
                            Color(0xFFFBBF24)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                // First emerging dusk stars
                drawStars(this, starShimmer * 0.8f, count = 18, maxH = h * 0.35f)
                // Glowing Ramadan Fanous Lantern outline in the upper sky
                drawFanousLantern(this, Offset(w * 0.8f, h * 0.18f), 24.dp.toPx())
                // Mosque Silhouette
                drawMosqueSilhouette(this, baseColor = Color(0xFF1E0A2E))
            }

            PrayerType.ISHA -> {
                // ISHA: Serene Celestial Midnight
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030712),
                            Color(0xFF0B0F19),
                            Color(0xFF111827),
                            Color(0xFF1E1B4B),
                            Color(0xFF312E81)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                // Dense glittering constellation stars
                drawStars(this, starShimmer, count = 45, maxH = h * 0.6f)
                // Radiant Crescent Moon with glowing aura
                drawCrescentMoon(this, center = Offset(w * 0.76f, h * 0.20f), radius = 36.dp.toPx(), color = Color(0xFFFDE68A))
                // Mosque Silhouette with warm illuminated dome windows
                drawMosqueSilhouette(this, baseColor = Color(0xFF050811), illuminateWindows = true)
            }
        }
    }
}

// Helpers for drawing atmospheric vectors on the Canvas

private fun drawStars(scope: DrawScope, shimmer: Float, count: Int, maxH: Float) {
    val w = scope.size.width
    val starCoords = listOf(
        0.12f to 0.08f, 0.28f to 0.14f, 0.45f to 0.06f, 0.62f to 0.12f, 0.85f to 0.09f,
        0.18f to 0.22f, 0.38f to 0.26f, 0.52f to 0.18f, 0.72f to 0.24f, 0.90f to 0.28f,
        0.08f to 0.34f, 0.31f to 0.38f, 0.59f to 0.32f, 0.82f to 0.36f, 0.22f to 0.44f,
        0.48f to 0.42f, 0.68f to 0.46f, 0.88f to 0.48f, 0.15f to 0.12f, 0.35f to 0.04f,
        0.75f to 0.05f, 0.55f to 0.08f, 0.95f to 0.16f, 0.05f to 0.20f, 0.42f to 0.30f
    )

    starCoords.take(count).forEachIndexed { idx, (rx, ry) ->
        val alpha = if (idx % 2 == 0) shimmer else (1.2f - shimmer).coerceIn(0.2f, 1.0f)
        val radius = if (idx % 3 == 0) 2.5f else 1.5f
        scope.drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(w * rx, maxH * (ry / 0.5f))
        )
    }
}

private fun drawCrescentMoon(scope: DrawScope, center: Offset, radius: Float, color: Color) {
    // Outer glow
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
            center = center,
            radius = radius * 2f
        ),
        center = center,
        radius = radius * 2f
    )

    val moonPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    val shadowPath = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                center.x - radius * 0.4f,
                center.y - radius * 1.1f,
                center.x + radius * 1.5f,
                center.y + radius * 0.9f
            )
        )
    }
    val resultCrescent = Path().apply {
        op(moonPath, shadowPath, androidx.compose.ui.graphics.PathOperation.Difference)
    }
    scope.drawPath(resultCrescent, color = color, style = Fill)
}

private fun drawSunRays(scope: DrawScope, center: Offset, rayCount: Int, length: Float) {
    val step = (2 * PI / rayCount).toFloat()
    for (i in 0 until rayCount) {
        val angle = i * step
        val startX = center.x + cos(angle) * (length * 0.4f)
        val startY = center.y + sin(angle) * (length * 0.4f)
        val endX = center.x + cos(angle) * length
        val endY = center.y + sin(angle) * length

        scope.drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

private fun drawFanousLantern(scope: DrawScope, center: Offset, size: Float) {
    // Fanous silhouette hanging
    scope.drawLine(
        color = Color(0xFFFFD54F).copy(alpha = 0.7f),
        start = Offset(center.x, 0f),
        end = Offset(center.x, center.y - size),
        strokeWidth = 2f
    )
    // Lantern body glow
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE082).copy(alpha = 0.6f), Color.Transparent),
            center = center,
            radius = size * 1.8f
        ),
        center = center,
        radius = size * 1.8f
    )
}

private fun drawMosqueSilhouette(scope: DrawScope, baseColor: Color, illuminateWindows: Boolean = false) {
    val w = scope.size.width
    val h = scope.size.height
    val baseY = h * 0.68f

    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, baseY + 60f)

        // Left Minaret
        lineTo(w * 0.08f, baseY + 60f)
        lineTo(w * 0.08f, baseY - 80f)
        lineTo(w * 0.11f, baseY - 120f) // minaret spire
        lineTo(w * 0.14f, baseY - 80f)
        lineTo(w * 0.14f, baseY + 60f)

        // Small left dome
        lineTo(w * 0.20f, baseY + 60f)
        cubicTo(w * 0.20f, baseY + 10f, w * 0.32f, baseY + 10f, w * 0.32f, baseY + 60f)

        // Main Grand Center Dome
        lineTo(w * 0.35f, baseY + 60f)
        lineTo(w * 0.35f, baseY + 30f)
        cubicTo(w * 0.36f, baseY - 70f, w * 0.64f, baseY - 70f, w * 0.65f, baseY + 30f)
        lineTo(w * 0.65f, baseY + 60f)

        // Small right dome
        lineTo(w * 0.68f, baseY + 60f)
        cubicTo(w * 0.68f, baseY + 10f, w * 0.80f, baseY + 10f, w * 0.80f, baseY + 60f)

        // Right Minaret
        lineTo(w * 0.86f, baseY + 60f)
        lineTo(w * 0.86f, baseY - 80f)
        lineTo(w * 0.89f, baseY - 120f) // minaret spire
        lineTo(w * 0.92f, baseY - 80f)
        lineTo(w * 0.92f, baseY + 60f)

        lineTo(w, baseY + 60f)
        lineTo(w, h)
        close()
    }

    scope.drawPath(path, color = baseColor, style = Fill)

    // Spires crescent finials
    scope.drawCircle(color = Color(0xFFFFD54F), radius = 4f, center = Offset(w * 0.11f, baseY - 122f))
    scope.drawCircle(color = Color(0xFFFFD54F), radius = 5f, center = Offset(w * 0.50f, baseY - 75f))
    scope.drawCircle(color = Color(0xFFFFD54F), radius = 4f, center = Offset(w * 0.89f, baseY - 122f))

    if (illuminateWindows) {
        // Glowing warm arched windows
        val windowColor = Color(0xFFFFD54F).copy(alpha = 0.85f)
        scope.drawCircle(color = windowColor, radius = 5f, center = Offset(w * 0.50f, baseY + 10f))
        scope.drawCircle(color = windowColor, radius = 4f, center = Offset(w * 0.44f, baseY + 20f))
        scope.drawCircle(color = windowColor, radius = 4f, center = Offset(w * 0.56f, baseY + 20f))
        scope.drawCircle(color = windowColor, radius = 3f, center = Offset(w * 0.11f, baseY - 20f))
        scope.drawCircle(color = windowColor, radius = 3f, center = Offset(w * 0.89f, baseY - 20f))
    }
}
