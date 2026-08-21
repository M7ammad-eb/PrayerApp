package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.models.UserLocation
import com.example.data.qibla.CompassAccuracy
import com.example.data.qibla.CompassSensorManager
import com.example.data.qibla.CompassState
import com.example.ui.locale.LocalAppStrings
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.KaabaBlack
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ALIGNMENT_THRESHOLD = 3.5f

@Composable
fun QiblaScreen(
    compassSensorManager: CompassSensorManager,
    compassState: CompassState,
    location: UserLocation
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(compassSensorManager, lifecycleOwner) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            compassSensorManager.start()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> compassSensorManager.start()
                Lifecycle.Event.ON_PAUSE -> compassSensorManager.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            compassSensorManager.stop()
        }
    }

    var showCalibrationDialog by remember { mutableStateOf(false) }
    val isAligned = abs(compassState.relativeQiblaAngle) <= ALIGNMENT_THRESHOLD
    val animatedDialRotation by animateFloatAsState(
        targetValue = -compassState.azimuthUnwrapped,
        animationSpec = tween(durationMillis = 220),
        label = "qibla_dial_rotation"
    )

    if (showCalibrationDialog) {
        CalibrationDialog(onDismiss = { showCalibrationDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("qibla_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LocationSummary(location = location, distanceKm = compassState.distanceKm)

        if (compassState.isSensorAvailable) {
            CompassHero(
                compassState = compassState,
                dialRotation = animatedDialRotation,
                isAligned = isAligned
            )
            AccuracyRow(
                accuracy = compassState.accuracy,
                onCalibrate = { showCalibrationDialog = true }
            )
        } else {
            SensorUnavailableCard()
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LocationSummary(location: UserLocation, distanceKm: Double) {
    val strings = LocalAppStrings.current
    val resources = LocalContext.current.resources
    val city = com.example.data.cities.CityDatabase.localizedName(resources, location)
    val country = com.example.data.cities.CityDatabase.localizedCountry(resources, location)
    val locationName = if (country.isNotEmpty() && !country.contains("°")) "$city, $country" else city

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = "${strings.distanceToKaabaLabel} ${strings.formatNumber(distanceKm.roundToInt())} ${strings.kmUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompassHero(
    compassState: CompassState,
    dialRotation: Float,
    isAligned: Boolean
) {
    val strings = LocalAppStrings.current
    val borderColor by animateColorAsState(
        targetValue = if (isAligned) GoldAccent.copy(alpha = 0.75f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
        label = "qibla_hero_border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    dialRotation = dialRotation,
                    qiblaBearing = compassState.qiblaBearing,
                    isAligned = isAligned
                )
                CardinalLabels(dialRotation = dialRotation)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${compassState.azimuth.roundToInt()}°",
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.cardinalDirection(compassState.azimuth.toDouble()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DirectionStatus(isAligned = isAligned, relativeAngle = compassState.relativeQiblaAngle)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.qiblaBearingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${compassState.qiblaBearing.roundToInt()}° ${strings.cardinalDirection(compassState.qiblaBearing.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }
        }
    }
}

@Composable
private fun CompassDial(dialRotation: Float, qiblaBearing: Float, isAligned: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val alignedRingColor by animateColorAsState(
        targetValue = if (isAligned) GoldAccent else outline.copy(alpha = 0.7f),
        label = "qibla_alignment_ring"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 10.dp.toPx()

        drawCircle(color = surfaceVariant.copy(alpha = 0.32f), radius = radius, center = center)
        drawCircle(
            color = alignedRingColor,
            radius = radius,
            center = center,
            style = Stroke(width = if (isAligned) 3.dp.toPx() else 2.dp.toPx())
        )
        drawCircle(
            color = outline.copy(alpha = 0.18f),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        rotate(dialRotation, center) {
            for (index in 0 until 72) {
                val angle = Math.toRadians(index * 5.0)
                val isCardinal = index % 18 == 0
                val isMajor = index % 6 == 0
                val tickLength = when {
                    isCardinal -> 15.dp.toPx()
                    isMajor -> 10.dp.toPx()
                    else -> 5.dp.toPx()
                }
                val tickColor = when {
                    index == 0 -> Color(0xFFE25555)
                    isCardinal -> primary
                    isMajor -> outline.copy(alpha = 0.7f)
                    else -> outline.copy(alpha = 0.35f)
                }
                val strokeWidth = when {
                    isCardinal -> 2.5.dp.toPx()
                    isMajor -> 1.6.dp.toPx()
                    else -> 1.dp.toPx()
                }
                val startRadius = radius - tickLength - 3.dp.toPx()
                val endRadius = radius - 3.dp.toPx()

                drawLine(
                    color = tickColor,
                    start = Offset(
                        x = center.x + startRadius * sin(angle).toFloat(),
                        y = center.y - startRadius * cos(angle).toFloat()
                    ),
                    end = Offset(
                        x = center.x + endRadius * sin(angle).toFloat(),
                        y = center.y - endRadius * cos(angle).toFloat()
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            rotate(qiblaBearing, center) {
                val markerCenter = Offset(center.x, center.y - radius + 27.dp.toPx())

                drawCircle(
                    color = GoldAccent.copy(alpha = 0.18f),
                    radius = 23.dp.toPx(),
                    center = markerCenter
                )
                drawCircle(color = GoldAccent, radius = 17.dp.toPx(), center = markerCenter)

                val kaabaWidth = 21.dp.toPx()
                val kaabaHeight = 24.dp.toPx()
                val kaabaTopLeft = Offset(
                    markerCenter.x - kaabaWidth / 2f,
                    markerCenter.y - kaabaHeight / 2f
                )
                drawRoundRect(
                    color = KaabaBlack,
                    topLeft = kaabaTopLeft,
                    size = Size(kaabaWidth, kaabaHeight),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawRoundRect(
                    color = GoldAccent,
                    topLeft = Offset(kaabaTopLeft.x, kaabaTopLeft.y + 6.dp.toPx()),
                    size = Size(kaabaWidth, 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
        }

        val pointerColor = if (isAligned) GoldAccent else primary
        val fixedPointer = Path().apply {
            moveTo(center.x, 1.dp.toPx())
            lineTo(center.x - 8.dp.toPx(), 17.dp.toPx())
            lineTo(center.x + 8.dp.toPx(), 17.dp.toPx())
            close()
        }
        drawPath(fixedPointer, color = pointerColor)

        drawCircle(color = surface, radius = 54.dp.toPx(), center = center)
        drawCircle(
            color = if (isAligned) GoldAccent.copy(alpha = 0.55f) else primary.copy(alpha = 0.28f),
            radius = 54.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
private fun CardinalLabels(dialRotation: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(31.dp)
            .rotate(dialRotation)
    ) {
        CardinalLabel("N", Color(0xFFE25555), Modifier.align(Alignment.TopCenter))
        CardinalLabel("E", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.align(Alignment.CenterEnd))
        CardinalLabel("S", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.align(Alignment.BottomCenter))
        CardinalLabel("W", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.align(Alignment.CenterStart))
    }
}

@Composable
private fun CardinalLabel(text: String, color: Color, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = color
    )
}

@Composable
private fun DirectionStatus(isAligned: Boolean, relativeAngle: Float) {
    val strings = LocalAppStrings.current
    val containerColor by animateColorAsState(
        targetValue = if (isAligned) GoldAccent.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        label = "qibla_status_container"
    )
    val contentColor = if (isAligned) GoldAccent else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Navigation,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = when {
                isAligned -> strings.qiblaAlignedMessage
                relativeAngle > 0f -> strings.qiblaTurnRight(relativeAngle.roundToInt())
                else -> strings.qiblaTurnLeft(-relativeAngle.roundToInt())
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AccuracyRow(accuracy: CompassAccuracy, onCalibrate: () -> Unit) {
    val strings = LocalAppStrings.current
    val accuracyText = when (accuracy) {
        CompassAccuracy.HIGH -> strings.qiblaAccuracyHigh
        CompassAccuracy.MEDIUM -> strings.qiblaAccuracyMedium
        CompassAccuracy.LOW -> strings.qiblaAccuracyLow
        CompassAccuracy.UNRELIABLE -> strings.qiblaAccuracyUnreliable
    }
    val accuracyColor = when (accuracy) {
        CompassAccuracy.HIGH -> MaterialTheme.colorScheme.primary
        CompassAccuracy.MEDIUM -> GoldAccent
        CompassAccuracy.LOW, CompassAccuracy.UNRELIABLE -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(9.dp).background(accuracyColor, CircleShape))
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "${strings.qiblaSensorLabel} $accuracyText",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onCalibrate) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(strings.qiblaCalibrateButton)
            }
        }
    }
}

@Composable
private fun SensorUnavailableCard() {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = strings.qiblaSensorUnavailable,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalibrationDialog(onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CompassCalibration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(strings.compassCalibTitle, fontWeight = FontWeight.Bold) },
        text = { Text(strings.compassCalibText, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.gotIt)
            }
        }
    )
}
