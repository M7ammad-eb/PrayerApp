package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.cities.CityDatabase
import com.example.data.models.UserLocation
import com.example.data.qibla.QiblaCalculator
import com.example.ui.locale.LocalAppStrings
import java.time.ZoneId
import java.util.Locale

@Composable
fun ManualCoordinatesDialog(
    currentLocation: UserLocation,
    onSaveLocation: (UserLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var latText by remember {
        mutableStateOf(
            if (currentLocation.isGps || currentLocation.latitude != 0.0)
                String.format(Locale.US, "%.5f", currentLocation.latitude)
            else ""
        )
    }
    var lonText by remember {
        mutableStateOf(
            if (currentLocation.isGps || currentLocation.longitude != 0.0)
                String.format(Locale.US, "%.5f", currentLocation.longitude)
            else ""
        )
    }
    var locationNameText by remember {
        mutableStateOf(
            if (currentLocation.name.isNotEmpty() && currentLocation.name != "Makkah")
                currentLocation.name
            else if (strings.isArabic) "إحداثيات مخصصة" else "Custom Coordinates"
        )
    }

    var selectedTimeZone by remember {
        mutableStateOf(
            if (currentLocation.timeZoneId.isNotEmpty()) currentLocation.timeZoneId
            else ZoneId.systemDefault().id
        )
    }

    var showTimeZoneDropdown by remember { mutableStateOf(false) }

    val latParsed = latText.trim().toDoubleOrNull()
    val lonParsed = lonText.trim().toDoubleOrNull()

    val isLatValid = latParsed != null && latParsed >= -90.0 && latParsed <= 90.0
    val isLonValid = lonParsed != null && lonParsed >= -180.0 && lonParsed <= 180.0
    val isValid = isLatValid && isLonValid

    val nearestCityResult by remember(latParsed, lonParsed) {
        derivedStateOf {
            if (latParsed != null && lonParsed != null && isLatValid && isLonValid) {
                CityDatabase.findNearestCity(latParsed, lonParsed)
            } else null
        }
    }

    val qiblaBearing by remember(latParsed, lonParsed) {
        derivedStateOf {
            if (latParsed != null && lonParsed != null && isLatValid && isLonValid) {
                QiblaCalculator.calculateQiblaBearing(latParsed, lonParsed)
            } else null
        }
    }

    val commonTimezones = remember {
        listOf(
            ZoneId.systemDefault().id,
            "Asia/Riyadh",
            "Asia/Dubai",
            "Africa/Cairo",
            "Europe/London",
            "Europe/Paris",
            "Europe/Istanbul",
            "Asia/Karachi",
            "Asia/Dhaka",
            "Asia/Jakarta",
            "Asia/Kuala_Lumpur",
            "Asia/Singapore",
            "America/New_York",
            "America/Chicago",
            "America/Los_Angeles",
            "America/Toronto",
            "Australia/Sydney",
            "UTC"
        ).distinct()
    }

    val presetCoordinates = remember(strings.isArabic) {
        listOf(
            PresetCoord(if (strings.isArabic) "مكة المكرمة" else "Makkah", 21.4225, 39.8262, "Asia/Riyadh", "Saudi Arabia"),
            PresetCoord(if (strings.isArabic) "المدينة المنورة" else "Madinah", 24.4672, 39.6111, "Asia/Riyadh", "Saudi Arabia"),
            PresetCoord(if (strings.isArabic) "القدس الشريف" else "Jerusalem", 31.7683, 35.2137, "Asia/Jerusalem", "Palestine"),
            PresetCoord(if (strings.isArabic) "القاهرة" else "Cairo", 30.0444, 31.2357, "Africa/Cairo", "Egypt"),
            PresetCoord(if (strings.isArabic) "دبي" else "Dubai", 25.2048, 55.2708, "Asia/Dubai", "UAE"),
            PresetCoord(if (strings.isArabic) "إسطنبول" else "Istanbul", 41.0082, 28.9784, "Europe/Istanbul", "Turkey"),
            PresetCoord(if (strings.isArabic) "لندن" else "London", 51.5074, -0.1278, "Europe/London", "United Kingdom"),
            PresetCoord(if (strings.isArabic) "نيويورك" else "New York", 40.7128, -74.0060, "America/New_York", "United States")
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("manual_coordinates_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
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
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.customCoordinates,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.enterCoordinates,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_coordinates_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.manualCoordinatesDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips
                Text(
                    text = strings.quickPresets,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCoordinates.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    latText = String.format(Locale.US, "%.4f", preset.lat)
                                    lonText = String.format(Locale.US, "%.4f", preset.lon)
                                    locationNameText = preset.name
                                    selectedTimeZone = preset.tz
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inputs Row: Latitude and Longitude
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Latitude Field
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text(strings.latitudeLabel) },
                            placeholder = { Text("24.7136") },
                            isError = latText.isNotBlank() && !isLatValid,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("latitude_input_field")
                        )
                        if (latText.isNotBlank() && !isLatValid) {
                            Text(
                                text = "-90.0° .. +90.0°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    // Longitude Field
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text(strings.longitudeLabel) },
                            placeholder = { Text("46.6753") },
                            isError = lonText.isNotBlank() && !isLonValid,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("longitude_input_field")
                        )
                        if (lonText.isNotBlank() && !isLonValid) {
                            Text(
                                text = "-180.0° .. +180.0°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location Label / Name Field
                OutlinedTextField(
                    value = locationNameText,
                    onValueChange = { locationNameText = it },
                    label = { Text(strings.locationNameLabel) },
                    placeholder = { Text("Desert Camp") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_name_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Time Zone Selector Box
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.timeZoneLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showTimeZoneDropdown = true }
                                .testTag("timezone_selector_button"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedTimeZone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (strings.isArabic) "تغيير" else "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTimeZoneDropdown,
                            onDismissRequest = { showTimeZoneDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            commonTimezones.forEach { tz ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (tz == ZoneId.systemDefault().id) "$tz (${if (strings.isArabic) "الافتراضي" else "Device Default"})" else tz,
                                            fontWeight = if (tz == selectedTimeZone) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedTimeZone = tz
                                        showTimeZoneDropdown = false
                                    },
                                    leadingIcon = {
                                        if (tz == selectedTimeZone) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nearest Reference City / Calculated Bearing Feedback Card
                if (nearestCityResult != null || qiblaBearing != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (nearestCityResult != null) {
                                val (nearCity, distKm) = nearestCityResult!!
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.coords_near_city_format,
                                            stringResource(nearCity.nameRes),
                                            stringResource(nearCity.countryRes),
                                            distKm.toInt()
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (qiblaBearing != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${strings.qiblaBearingLabel}: ${String.format(Locale.US, "%.1f°", qiblaBearing)} (${strings.cardinalDirection(qiblaBearing!!)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_coordinates_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }

                    Button(
                        onClick = {
                            if (isValid && latParsed != null && lonParsed != null) {
                                val finalName = locationNameText.trim().ifEmpty {
                                    "Custom (${String.format(Locale.US, "%.2f, %.2f", latParsed, lonParsed)})"
                                }
                                val finalCountry = nearestCityResult?.let {
                                    context.getString(R.string.coords_near_city_short_format, context.getString(it.first.nameRes))
                                } ?: "Manual Coordinates"
                                val newLocation = UserLocation(
                                    name = finalName,
                                    country = finalCountry,
                                    latitude = latParsed,
                                    longitude = lonParsed,
                                    timeZoneId = selectedTimeZone,
                                    isGps = false
                                )
                                onSaveLocation(newLocation)
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_coordinates_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(strings.applyCoordinates)
                    }
                }
            }
        }
    }
}

private data class PresetCoord(
    val name: String,
    val lat: Double,
    val lon: Double,
    val tz: String,
    val country: String
)
