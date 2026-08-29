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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    val animatedRingRotation by animateFloatAsState(
        targetValue = -compassState.azimuthUnwrapped,
        animationSpec = tween(durationMillis = 180),
        label = "qibla_ring_rotation"
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
                ringRotation = animatedRingRotation,
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
    ringRotation: Float,
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
                modifier = Modifier.fillMaxSize(0.99f),
                contentAlignment = Alignment.Center
            ) {
                MinimalQiblaCompass(
                    pointerRotation = pointerRotation,
                    ringRotation = ringRotation,
                    isAligned = isAligned
                )
            }
        }
        DirectionStatus(isAligned = isAligned, relativeAngle = compassState.relativeQiblaAngle)
    }
}

@Composable
private fun MinimalQiblaCompass(pointerRotation: Float, ringRotation: Float, isAligned: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val faceColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val ringLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val badgeColor by animateColorAsState(
        targetValue = if (isAligned) GoldAccent else primary,
        label = "qibla_badge_color"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isAligned) 0.45f else 0.26f,
        label = "qibla_glow_alpha"
    )
    val labelStyle = remember(ringLabelColor) {
        TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ringLabelColor)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val faceRadius = size.minDimension / 2f * 0.82f
        val glowRadius = faceRadius * 1.2f
        val dialRadius = faceRadius * 0.75f
        val badgeRadius = faceRadius * 0.155f

        // Warm glow ring hugging the face's edge (like a CSS box-shadow blur), rather than a
        // wash from the center - transparent at the center and at the outer edge, peaking right
        // where the white face ends.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to GoldAccent.copy(alpha = 0f),
                    (faceRadius / glowRadius) to GoldAccent.copy(alpha = glowAlpha),
                    1f to GoldAccent.copy(alpha = 0f)
                ),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )

        // White face with a thin outline - no drop shadow, the reference relies only on the
        // warm glow for depth.
        drawCircle(color = faceColor, radius = faceRadius, center = center)
        drawCircle(color = ringLabelColor.copy(alpha = 0.18f), radius = faceRadius, center = center, style = Stroke(width = 1.dp.toPx()))

        // Fine dashed inner ring with cardinal labels sitting right on it - both stay inside the
        // face so the orbiting badge (out at faceRadius) never overlaps a letter. The ring is
        // drawn as four arcs, one per gap between labels, each trimmed short of the label's own
        // angular width so the dashes stop next to the letter instead of running through it.
        rotate(ringRotation, center) {
            val cardinals = listOf("N" to -90f, "E" to 0f, "S" to 90f, "W" to 180f)
            val labelLayouts = cardinals.map { (label, _) -> textMeasurer.measure(label, labelStyle) }
            val labelHalfAngles = labelLayouts.map { layout ->
                val halfArcLength = layout.size.width / 2f + 5.dp.toPx()
                Math.toDegrees((halfArcLength / dialRadius).toDouble()).toFloat()
            }
            val dashStyle = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
            val dialTopLeft = Offset(center.x - dialRadius, center.y - dialRadius)
            val dialSize = Size(dialRadius * 2, dialRadius * 2)
            cardinals.indices.forEach { i ->
                val nextI = (i + 1) % cardinals.size
                val startAngle = cardinals[i].second + labelHalfAngles[i]
                val endAngleRaw = cardinals[nextI].second
                val endAngle = (if (endAngleRaw <= cardinals[i].second) endAngleRaw + 360f else endAngleRaw) - labelHalfAngles[nextI]
                val sweep = endAngle - startAngle
                if (sweep > 0f) {
                    drawArc(
                        color = ringLabelColor.copy(alpha = 0.30f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = dialTopLeft,
                        size = dialSize,
                        style = dashStyle
                    )
                }
            }
            cardinals.forEachIndexed { i, (_, angleDeg) ->
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val labelCenter = Offset(
                    center.x + dialRadius * cos(angleRad).toFloat(),
                    center.y + dialRadius * sin(angleRad).toFloat()
                )
                val layout = labelLayouts[i]
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(labelCenter.x - layout.size.width / 2f, labelCenter.y - layout.size.height / 2f)
                )
            }
        }

        // Center marker
        drawCircle(color = GoldAccent, radius = 6.dp.toPx(), center = center)

        // Kaaba badge orbiting the rim at the current bearing to Qibla
        val badgeAngleRad = Math.toRadians((pointerRotation - 90).toDouble())
        val badgeCenter = Offset(
            center.x + faceRadius * cos(badgeAngleRad).toFloat(),
            center.y + faceRadius * sin(badgeAngleRad).toFloat()
        )
        drawCircle(color = faceColor, radius = badgeRadius + 3.dp.toPx(), center = badgeCenter)
        drawCircle(color = badgeColor, radius = badgeRadius, center = badgeCenter)

        // Upright isometric cube inside the badge - a "corner" view of the Kaaba (three visible
        // faces meeting at the near corner), scaled to the badge's own radius. badgeCenter is
        // already an absolute position (from cos/sin), not the result of a rotate() transform,
        // so the icon itself must be drawn plain here - wrapping it in
        // rotate(-pointerRotation, ...) would spin it as the badge orbits instead of keeping it
        // upright.
        val cx = badgeCenter.x
        val cy = badgeCenter.y
        val iconScale = badgeRadius / 24.dp.toPx()
        // Silhouette: a short flat top cap, then a much taller body that narrows to a near-point
        // at the bottom - not a regular hexagon. The body itself is left unfilled (the badge's
        // own dark background already shows through), matching the reference's solid-cube style
        // without needing a separate fill or seam line.
        val w = 12.dp.toPx() * iconScale
        val topY = cy - 11.dp.toPx() * iconScale
        val shoulderY = cy - 6.dp.toPx() * iconScale
        val nearY = cy - 4.dp.toPx() * iconScale
        val lowerShoulderY = cy + 7.dp.toPx() * iconScale
        val bottomY = cy + 9.dp.toPx() * iconScale

        val top = Offset(cx, topY)
        val upperRight = Offset(cx + w, shoulderY)
        val lowerRight = Offset(cx + w, lowerShoulderY)
        val bottom = Offset(cx, bottomY)
        val lowerLeft = Offset(cx - w, lowerShoulderY)
        val upperLeft = Offset(cx - w, shoulderY)
        val near = Offset(cx, nearY)

        val outlinePath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(upperRight.x, upperRight.y)
            lineTo(lowerRight.x, lowerRight.y)
            lineTo(bottom.x, bottom.y)
            lineTo(lowerLeft.x, lowerLeft.y)
            lineTo(upperLeft.x, upperLeft.y)
            close()
        }
        val lineWidth = 1.4.dp.toPx() * iconScale
        drawPath(path = outlinePath, color = onPrimary, style = Stroke(width = lineWidth))

        // Flat top face, dipping down slightly to its near vertex like the reference's kite shape
        val topFacePath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(upperRight.x, upperRight.y)
            lineTo(near.x, near.y)
            lineTo(upperLeft.x, upperLeft.y)
            close()
        }
        drawPath(path = topFacePath, color = onPrimary)

        // Belt bands wrapping the body, each bowing down slightly at the center seam
        fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        fun chevron(frac: Float, bow: Float, width: Float) {
            val left = lerp(upperLeft, lowerLeft, frac)
            val right = lerp(upperRight, lowerRight, frac)
            val dip = Offset(cx, ((left.y + right.y) / 2f) + bow)
            val path = Path().apply {
                moveTo(left.x, left.y)
                lineTo(dip.x, dip.y)
                lineTo(right.x, right.y)
            }
            drawPath(path = path, color = onPrimary, style = Stroke(width = width))
        }
        val bow = 1.6.dp.toPx() * iconScale
        chevron(frac = 0.42f, bow = bow, width = lineWidth * 1.3f)
        chevron(frac = 0.78f, bow = bow * 0.7f, width = lineWidth)
        chevron(frac = 0.87f, bow = bow * 0.6f, width = lineWidth)
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

    // Fixed height regardless of which variant is shown below - the aligned and unaligned
    // content have different intrinsic heights, and letting the pill grow/shrink with them
    // would resize the compass above it too, since it fills whatever space is left over.
    Column(
        modifier = Modifier
            .height(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
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
