package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.UserLocation
import com.example.data.qibla.CompassAccuracy
import com.example.data.qibla.CompassSensorManager
import com.example.data.qibla.CompassState
import com.example.data.qibla.QiblaCalculator
import androidx.compose.ui.platform.LocalContext
import com.example.ui.locale.LocalAppStrings
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight
import com.example.ui.theme.KaabaBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    compassSensorManager: CompassSensorManager,
    compassState: CompassState,
    location: UserLocation
) {
    val strings = LocalAppStrings.current

    // Start sensor when screen is visible
    DisposableEffect(Unit) {
        compassSensorManager.start()
        onDispose {
            compassSensorManager.stop()
        }
    }

    var showCalibDialog by remember { mutableStateOf(false) }
    var manualHeading by remember { mutableFloatStateOf(0f) }
    var isManualMode by remember { mutableStateOf(false) }

    val effectiveAzimuth = if (isManualMode || !compassState.isSensorAvailable) manualHeading else compassState.azimuth
    val qiblaBearing = compassState.qiblaBearing
    val relAngle = ((qiblaBearing - effectiveAzimuth + 540f) % 360f) - 180f
    val isAligned = kotlin.math.abs(relAngle) <= 3.5f

    if (showCalibDialog) {
        AlertDialog(
            onDismissRequest = { showCalibDialog = false },
            icon = { Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(strings.compassCalibTitle, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    strings.compassCalibText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showCalibDialog = false }) {
                    Text(strings.gotIt)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("qibla_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location & Qibla Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = com.example.data.cities.CityDatabase.localizedName(LocalContext.current.resources, location),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${strings.qiblaBearingLabel} ${qiblaBearing.roundToInt()}° ${strings.cardinalDirection(qiblaBearing.toDouble())}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${strings.formatNumber(compassState.distanceKm.roundToInt())} ${strings.kmUnit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.distanceToKaabaLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Qibla Alignment Status Badge
        AlignmentStatusBadge(isAligned = isAligned, relAngle = relAngle)

        // Main Animated Compass Canvas
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val animatedDialRotation by animateFloatAsState(
                targetValue = -effectiveAzimuth,
                animationSpec = tween(durationMillis = 250),
                label = "dial_rotation"
            )

            // Compass Dial Background & Markings
            QiblaCompassCanvas(
                dialRotation = animatedDialRotation,
                qiblaBearing = qiblaBearing,
                isAligned = isAligned
            )

            // Center Mecca / Kaaba Emblem
            KaabaCenterIndicator(isAligned = isAligned)
        }

        // Telemetry Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TelemetryItem(label = strings.currentHeadingLabel, value = "${effectiveAzimuth.roundToInt()}°")
                TelemetryItem(label = strings.qiblaBearingLabel, value = "${qiblaBearing.roundToInt()}°")
                TelemetryItem(
                    label = strings.qiblaTurnNeededLabel,
                    value = if (isAligned) strings.qiblaAlignedValue else "${relAngle.roundToInt()}°"
                )
            }
        }

        // Accuracy & Sensor Calibration Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val accColor = when (compassState.accuracy) {
                    CompassAccuracy.HIGH -> MaterialTheme.colorScheme.primary
                    CompassAccuracy.MEDIUM -> GoldAccent
                    else -> MaterialTheme.colorScheme.error
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${strings.qiblaSensorLabel} ${when (compassState.accuracy) {
                        CompassAccuracy.HIGH -> strings.qiblaAccuracyHigh
                        CompassAccuracy.MEDIUM -> strings.qiblaAccuracyMedium
                        CompassAccuracy.LOW -> strings.qiblaAccuracyLow
                        CompassAccuracy.UNRELIABLE -> strings.qiblaAccuracyUnreliable
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = { showCalibDialog = true }) {
                Icon(imageVector = Icons.Default.CompassCalibration, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.qiblaCalibrateButton, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Manual orientation test slider (for devices/testing without physical compass)
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.manualCompassMode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { isManualMode = !isManualMode },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = strings.qiblaToggleManualDesc,
                            tint = if (isManualMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Slider(
                    value = manualHeading,
                    onValueChange = {
                        manualHeading = it
                        isManualMode = true
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.testTag("compass_manual_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AlignmentStatusBadge(isAligned: Boolean, relAngle: Float) {
    val strings = LocalAppStrings.current
    val backgroundColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "status_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "text_color"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(
                width = if (isAligned) 1.5.dp else 0.dp,
                color = if (isAligned) GoldAccent else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Navigation,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAligned) strings.qiblaAlignedMessage else if (relAngle > 0) strings.qiblaTurnRight(relAngle.roundToInt()) else strings.qiblaTurnLeft(-relAngle.roundToInt()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun QiblaCompassCanvas(
    dialRotation: Float,
    qiblaBearing: Float,
    isAligned: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val goldColor = GoldAccent
    val alignedColor = if (isAligned) GoldAccent else primaryColor

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 14.dp.toPx()

        // Outer Bezel Ring
        drawCircle(
            color = surfaceVariant.copy(alpha = 0.45f),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        if (isAligned) {
            // Glowing alignment ring
            drawCircle(
                color = goldColor.copy(alpha = 0.35f),
                radius = radius + 6.dp.toPx(),
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = goldColor.copy(alpha = 0.15f),
                radius = radius,
                center = center
            )
        }

        // Rotating Dial with Compass Directions & Ticks
        rotate(dialRotation, center) {
            // Draw 36 tick marks (every 10 degrees)
            for (i in 0 until 36) {
                val angleDeg = i * 10.0
                val angleRad = Math.toRadians(angleDeg)
                val isCardinal = i % 9 == 0
                val isSubCardinal = i % 3 == 0
                val tickLength = if (isCardinal) 16.dp.toPx() else if (isSubCardinal) 10.dp.toPx() else 6.dp.toPx()
                val tickColor = if (isCardinal) primaryColor else outlineColor.copy(alpha = if (isSubCardinal) 0.8f else 0.4f)
                val strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.2.dp.toPx()

                val startX = center.x + (radius - tickLength) * sin(angleRad).toFloat()
                val startY = center.y - (radius - tickLength) * cos(angleRad).toFloat()
                val endX = center.x + radius * sin(angleRad).toFloat()
                val endY = center.y - radius * cos(angleRad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Draw North Pointer (Red Arrow)
            val northPath = Path().apply {
                moveTo(center.x, center.y - radius + 16.dp.toPx())
                lineTo(center.x - 7.dp.toPx(), center.y - radius + 32.dp.toPx())
                lineTo(center.x + 7.dp.toPx(), center.y - radius + 32.dp.toPx())
                close()
            }
            drawPath(northPath, color = Color(0xFFE63946))

            // Draw Dedicated Qibla Pointer Arrow (Golden Kaaba arrow extending outward)
            rotate(qiblaBearing, center) {
                // Gold pointing needle toward Kaaba
                val qiblaArrowPath = Path().apply {
                    moveTo(center.x, center.y - radius + 8.dp.toPx())
                    lineTo(center.x - 8.dp.toPx(), center.y - radius + 28.dp.toPx())
                    lineTo(center.x - 3.dp.toPx(), center.y - 30.dp.toPx())
                    lineTo(center.x + 3.dp.toPx(), center.y - 30.dp.toPx())
                    lineTo(center.x + 8.dp.toPx(), center.y - radius + 28.dp.toPx())
                    close()
                }
                drawPath(qiblaArrowPath, color = goldColor)

                // Golden beacon circle at the tip of the Qibla arrow
                drawCircle(
                    color = goldColor,
                    radius = 9.dp.toPx(),
                    center = Offset(center.x, center.y - radius + 10.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF1E1B18),
                    radius = 4.dp.toPx(),
                    center = Offset(center.x, center.y - radius + 10.dp.toPx())
                )
            }
        }

        // Fixed Top Arrow (Phone Heading Direction)
        val fixedArrow = Path().apply {
            moveTo(center.x, 4.dp.toPx())
            lineTo(center.x - 8.dp.toPx(), 18.dp.toPx())
            lineTo(center.x + 8.dp.toPx(), 18.dp.toPx())
            close()
        }
        drawPath(fixedArrow, color = alignedColor)
    }
}

@Composable
private fun KaabaCenterIndicator(isAligned: Boolean) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                if (isAligned) GoldAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 2.dp,
                color = if (isAligned) GoldAccent else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Kaaba Cube Emblem
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KaabaBlack)
                .border(1.dp, GoldAccent, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            // Golden band around Kaaba
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                .background(GoldAccent)
            )
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
