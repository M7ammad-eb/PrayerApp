package com.prayertimes.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prayertimes.data.models.UserLocation
import com.prayertimes.data.qibla.CompassAccuracy
import com.prayertimes.data.qibla.CompassSensorManager
import com.prayertimes.data.qibla.CompassState
import com.prayertimes.ui.locale.LocalAppStrings
import com.prayertimes.ui.theme.GoldAccent
import com.prayertimes.ui.theme.KaabaBlack
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ALIGNMENT_THRESHOLD = 3.5f

@Composable
fun QiblaScreen(
    compassSensorManager: CompassSensorManager,
    compassState: CompassState,
    location: UserLocation
) {
    val strings = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current

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
    var wasAligned by remember { mutableStateOf(false) }
    val isAligned = abs(compassState.relativeQiblaAngle) <= ALIGNMENT_THRESHOLD
    val hasClearlyLeftAlignment = abs(compassState.relativeQiblaAngle) >= 8f
    val animatedPointerRotation by animateFloatAsState(
        targetValue = compassState.qiblaBearing - compassState.azimuthUnwrapped,
        animationSpec = tween(durationMillis = 180),
        label = "qibla_pointer_rotation"
    )

    LaunchedEffect(isAligned, hasClearlyLeftAlignment) {
        if (isAligned && !wasAligned) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            wasAligned = true
        } else if (hasClearlyLeftAlignment) {
            wasAligned = false
        }
    }

    if (showCalibrationDialog) {
        CalibrationDialog(onDismiss = { showCalibrationDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("qibla_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (compassState.isSensorAvailable) {
            Text(
                text = strings.qiblaFindDirection,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = strings.qiblaCompassInstruction,
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            CompassHero(
                modifier = Modifier.weight(1f),
                compassState = compassState,
                pointerRotation = animatedPointerRotation,
                isAligned = isAligned
            )
            QiblaDetailsCard(
                location = location,
                distanceKm = compassState.distanceKm,
                accuracy = compassState.accuracy,
                onCalibrate = { showCalibrationDialog = true }
            )
        } else {
            SensorUnavailableCard()
        }
    }
}

@Composable
private fun CompassHero(
    modifier: Modifier = Modifier,
    compassState: CompassState,
    pointerRotation: Float,
    isAligned: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fills whatever vertical space is left between the header and the footer - the circle
        // itself is drawn from size.minDimension, so it naturally shrinks to fit short screens
        // instead of forcing the page to scroll.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxSize(0.88f),
                contentAlignment = Alignment.Center
            ) {
                MinimalQiblaCompass(pointerRotation = pointerRotation, isAligned = isAligned)
            }
        }
        DirectionStatus(isAligned = isAligned, relativeAngle = compassState.relativeQiblaAngle)
    }
}

@Composable
private fun MinimalQiblaCompass(pointerRotation: Float, isAligned: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val ringColor by animateColorAsState(
        targetValue = if (isAligned) primary else outline.copy(alpha = 0.28f),
        label = "qibla_ring_color"
    )
    val pointerColor by animateColorAsState(
        targetValue = if (isAligned) GoldAccent else primary,
        label = "qibla_pointer_color"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()

        drawCircle(color = ringColor.copy(alpha = if (isAligned) 0.10f else 0.08f), radius = radius, center = center)
        drawCircle(color = surface, radius = radius - 7.dp.toPx(), center = center)
        drawCircle(
            color = ringColor,
            radius = radius,
            center = center,
            style = Stroke(width = if (isAligned) 4.dp.toPx() else 2.dp.toPx())
        )
        drawCircle(
            color = outline.copy(alpha = 0.10f),
            radius = radius - 8.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        val topGuide = Path().apply {
            moveTo(center.x, 3.dp.toPx())
            lineTo(center.x - 7.dp.toPx(), 16.dp.toPx())
            lineTo(center.x + 7.dp.toPx(), 16.dp.toPx())
            close()
        }
        drawPath(topGuide, color = ringColor)

        rotate(pointerRotation, center) {
            val markerCenter = Offset(center.x, center.y - radius * 0.66f)
            drawLine(
                color = pointerColor,
                start = center,
                end = markerCenter,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = GoldAccent.copy(alpha = 0.18f), radius = 25.dp.toPx(), center = markerCenter)
            drawCircle(color = GoldAccent, radius = 19.dp.toPx(), center = markerCenter)
            rotate(-pointerRotation, markerCenter) {
                drawRoundRect(
                    color = KaabaBlack,
                    topLeft = Offset(markerCenter.x - 11.dp.toPx(), markerCenter.y - 12.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(22.dp.toPx(), 24.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
                drawLine(
                    color = GoldAccent,
                    start = Offset(markerCenter.x - 11.dp.toPx(), markerCenter.y - 5.dp.toPx()),
                    end = Offset(markerCenter.x + 11.dp.toPx(), markerCenter.y - 5.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        drawCircle(color = primary.copy(alpha = 0.10f), radius = 20.dp.toPx(), center = center)
        drawCircle(color = primary, radius = 8.dp.toPx(), center = center)
    }
}

@Composable
private fun DirectionStatus(isAligned: Boolean, relativeAngle: Float) {
    val strings = LocalAppStrings.current
    val degrees = abs(relativeAngle).roundToInt()
    val containerColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "qibla_status_container"
    )
    val contentColor = if (isAligned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isAligned) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = strings.qiblaAlignedShort,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        } else {
            Text(
                text = strings.formatNumber(degrees) + "°",
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Text(
                text = if (relativeAngle > 0f) strings.qiblaTurnRightShort else strings.qiblaTurnLeftShort,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QiblaDetailsCard(
    location: UserLocation,
    distanceKm: Double,
    accuracy: CompassAccuracy,
    onCalibrate: () -> Unit
) {
    val strings = LocalAppStrings.current
    val resources = LocalContext.current.resources
    val city = com.prayertimes.data.cities.CityDatabase.localizedName(resources, location)
    val country = com.prayertimes.data.cities.CityDatabase.localizedCountry(resources, location)
    val locationName = if (country.isNotEmpty() && !country.contains("°")) "$city, $country" else city
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${strings.distanceToKaabaLabel} ${strings.formatNumber(distanceKm.roundToInt())} ${strings.kmUnit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 5.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(9.dp).background(accuracyColor, CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
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
