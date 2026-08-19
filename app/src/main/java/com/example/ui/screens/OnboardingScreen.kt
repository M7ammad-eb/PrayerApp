package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.audio.AthanAudioEngine
import com.example.data.cities.CityDatabase
import com.example.data.models.AppColorPreset
import com.example.data.models.AppLanguage
import com.example.data.models.AppThemeMode
import com.example.data.models.NotificationPrayerConfig
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import com.example.data.models.UserLocation
import com.example.data.preferences.AppPrayerSettings
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.ProvideAppLocale

enum class OnboardingStep(val stepNumber: Int) {
    LANGUAGE(1),
    LOCATION(2),
    NOTIFICATIONS(3),
    ATHAN_SOUNDS(4),
    STYLE(5)
}

@Composable
fun OnboardingScreen(
    settings: AppPrayerSettings,
    isGpsLoading: Boolean,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onSelectCity: (UserLocation) -> Unit,
    onRequestGps: () -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (AppColorPreset) -> Unit,
    onUpdateFollowSystemColors: (Boolean) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardingStep.LANGUAGE) }
    val strings = LocalAppStrings.current

    // Check OS notification permission status
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifPermission = isGranted
    }

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            onRequestGps()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Bottom Action Navigation Bar (Back & Next / Finish)
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep != OnboardingStep.LANGUAGE) {
                        OutlinedButton(
                            onClick = {
                                val prevOrdinal = currentStep.ordinal - 1
                                if (prevOrdinal >= 0) {
                                    AthanAudioEngine.stop()
                                    currentStep = OnboardingStep.values()[prevOrdinal]
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("onboarding_prev_button")
                        ) {
                            Icon(
                                imageVector = if (strings.isArabic) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.previousStepBtn, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    if (currentStep == OnboardingStep.STYLE) {
                        Button(
                            onClick = {
                                AthanAudioEngine.stop()
                                onCompleteOnboarding()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("onboarding_finish_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.getStartedBtn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val nextOrdinal = currentStep.ordinal + 1
                                if (nextOrdinal < OnboardingStep.values().size) {
                                    AthanAudioEngine.stop()
                                    currentStep = OnboardingStep.values()[nextOrdinal]
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = strings.nextStepBtn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (strings.isArabic) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingStep.values().forEach { step ->
                    val isPast = step.ordinal < currentStep.ordinal
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step Label Header
            Text(
                text = "${strings.stepIndicatorText} ${currentStep.stepNumber} ${strings.ofStepText} ${OnboardingStep.values().size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Step Container
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> if (strings.isArabic) -width else width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> if (strings.isArabic) width else -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> if (strings.isArabic) width else -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> if (strings.isArabic) -width else width } + fadeOut()
                        )
                    }
                },
                label = "OnboardingStepAnimation",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.LANGUAGE -> {
                        OnboardingLanguageStep(
                            currentLanguage = settings.language,
                            onSelectLanguage = onUpdateLanguage
                        )
                    }
                    OnboardingStep.LOCATION -> {
                        OnboardingLocationStep(
                            currentLocation = settings.location,
                            isGpsLoading = isGpsLoading,
                            onSelectCity = onSelectCity,
                            onRequestGps = {
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (fine || coarse) {
                                    onRequestGps()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            }
                        )
                    }
                    OnboardingStep.NOTIFICATIONS -> {
                        OnboardingNotificationStep(
                            hasPermission = hasNotifPermission,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    hasNotifPermission = true
                                }
                            }
                        )
                    }
                    OnboardingStep.ATHAN_SOUNDS -> {
                        OnboardingAthanStep(
                            settings = settings,
                            onUpdateNotificationConfig = onUpdateNotificationConfig,
                            onPreviewSound = onPreviewSound
                        )
                    }
                    OnboardingStep.STYLE -> {
                        OnboardingStyleStep(
                            settings = settings,
                            onUpdateThemeMode = onUpdateThemeMode,
                            onUpdateColorPreset = onUpdateColorPreset,
                            onUpdateFollowSystemColors = onUpdateFollowSystemColors
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: LANGUAGE SELECTION
// -------------------------------------------------------------
@Composable
private fun OnboardingLanguageStep(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_step_language")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Icon & Titles
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = strings.welcomeToApp,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.stepLanguageDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Language Options
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Arabic Option
            LanguageOptionCard(
                nativeTitle = "العربية",
                englishSubtitle = "Arabic",
                flagEmoji = "🇸🇦",
                isSelected = currentLanguage == AppLanguage.ARABIC,
                onClick = { onSelectLanguage(AppLanguage.ARABIC) }
            )

            // English Option
            LanguageOptionCard(
                nativeTitle = "English",
                englishSubtitle = "الإنجليزية",
                flagEmoji = "🇬🇧",
                isSelected = currentLanguage == AppLanguage.ENGLISH,
                onClick = { onSelectLanguage(AppLanguage.ENGLISH) }
            )

            // System Default Option
            LanguageOptionCard(
                nativeTitle = strings.systemDefaultLangTitle,
                englishSubtitle = strings.systemDefaultLangSubtitle,
                flagEmoji = "🌐",
                isSelected = currentLanguage == AppLanguage.SYSTEM,
                onClick = { onSelectLanguage(AppLanguage.SYSTEM) }
            )
        }
    }
}

@Composable
private fun LanguageOptionCard(
    nativeTitle: String,
    englishSubtitle: String,
    flagEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flagEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = nativeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = englishSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2: LOCATION SELECTION (GPS + City Search, NO Lat/Lon)
// -------------------------------------------------------------
@Composable
private fun OnboardingLocationStep(
    currentLocation: UserLocation,
    isGpsLoading: Boolean,
    onSelectCity: (UserLocation) -> Unit,
    onRequestGps: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) CityDatabase.popularCities
        else CityDatabase.searchCities(context, searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_step_location")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title & Description
        Text(
            text = strings.stepLocationTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.stepLocationDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Guarantee Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = strings.privacyNoticeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.privacyNoticeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Current Selection Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (currentLocation.isGps) Icons.Default.MyLocation else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.selectedLocationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currentLocation.name}, ${currentLocation.country}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Automatic GPS Button
                Button(
                    onClick = onRequestGps,
                    enabled = !isGpsLoading,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isGpsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "GPS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // City Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.searchCityPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Cities List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredCities) { city ->
                val isSelected = !currentLocation.isGps &&
                    (currentLocation.name.equals(com.example.util.LocalizedStrings.forLanguage(context, false).getString(city.nameRes), ignoreCase = true) ||
                        currentLocation.name == com.example.util.LocalizedStrings.forLanguage(context, true).getString(city.nameRes))
                Card(
                    onClick = { onSelectCity(city.toUserLocation(context.resources)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(city.nameRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${stringResource(city.countryRes)} • ${city.timeZoneId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: NOTIFICATIONS PERMISSION & EXPLANATION
// -------------------------------------------------------------
@Composable
private fun OnboardingNotificationStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_step_notifications")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title & Description
        Text(
            text = strings.stepNotificationsTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.stepNotificationsDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Status Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (hasPermission) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (hasPermission) strings.permissionGrantedStatus else strings.stepNotificationsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.grantNotificationPermissionBtn, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Detailed Explanation of Why Permissions Are Essential
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = strings.stepNotificationsExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: ATHAN SOUND SELECTION PER PRAYER
// -------------------------------------------------------------
@Composable
private fun OnboardingAthanStep(
    settings: AppPrayerSettings,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedPrayerForDialog by remember { mutableStateOf<PrayerType?>(null) }

    if (selectedPrayerForDialog != null) {
        val prayer = selectedPrayerForDialog!!
        val currentConfig = settings.prayerConfigs[prayer] ?: NotificationPrayerConfig()
        SoundPickerDialog(
            prayerType = prayer,
            currentSound = currentConfig.soundType,
            onSelectSound = { sound ->
                onUpdateNotificationConfig(prayer, currentConfig.enabled, sound, currentConfig.preReminderMinutes)
            },
            onPreviewSound = { sound ->
                onPreviewSound(sound, prayer)
            },
            onDismiss = {
                selectedPrayerForDialog = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_step_athan")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.stepAthanSoundsTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.stepAthanSoundsDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PrayerType.values()) { prayer ->
                val config = settings.prayerConfigs[prayer] ?: NotificationPrayerConfig()
                Card(
                    onClick = { selectedPrayerForDialog = prayer },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = strings.prayerName(prayer),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.soundTypeName(config.soundType),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { selectedPrayerForDialog = prayer },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = strings.change, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5: STYLE SELECTION (Theme Mode & Colors)
// -------------------------------------------------------------
@Composable
private fun OnboardingStyleStep(
    settings: AppPrayerSettings,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (AppColorPreset) -> Unit,
    onUpdateFollowSystemColors: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_step_style"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.stepStyleTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = strings.stepStyleDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.themeModeTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Theme Options (System, Light, Dark)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeMiniCard(
                    title = strings.themeModeName(AppThemeMode.SYSTEM),
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = settings.themeMode == AppThemeMode.SYSTEM,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateThemeMode(AppThemeMode.SYSTEM) }
                )
                ThemeMiniCard(
                    title = strings.themeModeName(AppThemeMode.LIGHT),
                    icon = Icons.Default.LightMode,
                    isSelected = settings.themeMode == AppThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateThemeMode(AppThemeMode.LIGHT) }
                )
                ThemeMiniCard(
                    title = strings.themeModeName(AppThemeMode.DARK),
                    icon = Icons.Default.DarkMode,
                    isSelected = settings.themeMode == AppThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateThemeMode(AppThemeMode.DARK) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.colorPaletteSection,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Follow System Dynamic Colors Switch
        item {
            Card(
                onClick = { onUpdateFollowSystemColors(!settings.followSystemColors) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.followSystemColors) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                border = if (settings.followSystemColors) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (settings.followSystemColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (settings.followSystemColors) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = strings.followSystemColorsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.followSystemColorsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.followSystemColors,
                        onCheckedChange = { onUpdateFollowSystemColors(it) }
                    )
                }
            }
        }

        // Color Presets
        items(AppColorPreset.values().filter { it != AppColorPreset.SYSTEM_DYNAMIC }) { preset ->
            val isSelected = !settings.followSystemColors && settings.colorPreset == preset
            val isDark = settings.themeMode == AppThemeMode.DARK || (settings.themeMode == AppThemeMode.SYSTEM && isSystemInDarkTheme())
            val primaryColorLong = if (isDark) preset.primaryDark else preset.primaryLight
            val secondaryColorLong = if (isDark) preset.secondaryDark else preset.secondaryLight

            Card(
                onClick = {
                    if (settings.followSystemColors) {
                        onUpdateFollowSystemColors(false)
                    }
                    onUpdateColorPreset(preset)
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(primaryColorLong)),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(secondaryColorLong))
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = strings.colorPresetName(preset),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.colorPresetName(preset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            if (settings.followSystemColors) {
                                onUpdateFollowSystemColors(false)
                            }
                            onUpdateColorPreset(preset)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ThemeMiniCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
