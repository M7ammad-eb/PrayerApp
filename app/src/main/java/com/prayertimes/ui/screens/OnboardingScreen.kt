package com.prayertimes.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.prayertimes.R
import com.prayertimes.audio.AthanAudioEngine
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.NotificationPrayerConfig
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.UserLocation
import com.prayertimes.data.models.suggestCalculationMethod
import com.prayertimes.data.places.PlaceEntity
import com.prayertimes.data.places.PlaceRepository
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.locale.LocalAppStrings
import com.prayertimes.ui.theme.ExpressiveMotion
import kotlinx.coroutines.delay
import java.util.Locale

enum class OnboardingStep(val stepNumber: Int) {
    LANGUAGE(1),
    LOCATION(2),
    CALCULATION_METHOD(3),
    NOTIFICATIONS(4),
    ATHAN_SOUNDS(5),
    STYLE(6)
}

@Composable
fun OnboardingScreen(
    settings: AppPrayerSettings,
    isGpsLoading: Boolean,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onSelectCity: (UserLocation) -> Unit,
    onRequestGps: () -> Unit,
    onUpdateCalculationMethod: (CalculationMethod) -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit,
    onUpdateLiveCountdownSettings: (Boolean, Int) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (AppColorPreset) -> Unit,
    onUpdateFollowSystemColors: (Boolean) -> Unit,
    onCompleteOnboarding: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardingStep.LANGUAGE) }
    val strings = LocalAppStrings.current

    if (onSkip != null) {
        BackHandler { onSkip() }
    }

    // Re-suggest a calculation method whenever the location actually changes during this
    // onboarding session (GPS resolves asynchronously, so this can't just be a callback wrapped
    // around onSelectCity). Keyed on the actual coordinates rather than timeZoneId: many distinct
    // locations share a timezone (e.g. Makkah and Madinah are both Asia/Riyadh), so keying on the
    // zone id alone would silently miss a real location change and leave a stale suggestion
    // selected on the Calculation Method step. On a true first-run (onSkip == null - see MainScreen,
    // which only supplies onSkip when the wizard was reopened from Settings) the suggestion is also
    // applied for the initial default location, so the very first location shown and the
    // Calculation Method step's pre-selected radio agree. When the wizard was reopened from an
    // already-configured location instead, the first firing is skipped so a deliberate prior choice
    // isn't silently overridden.
    var isFirstLocationComposition by remember { mutableStateOf(true) }
    LaunchedEffect(settings.location.latitude, settings.location.longitude) {
        if (isFirstLocationComposition) {
            isFirstLocationComposition = false
            if (onSkip == null) {
                onUpdateCalculationMethod(suggestCalculationMethod(settings.location.timeZoneId))
            }
        } else {
            onUpdateCalculationMethod(suggestCalculationMethod(settings.location.timeZoneId))
        }
    }

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

    // Once the OS has permanently suppressed the permission dialog (after a prior denial outside
    // this screen, or a second denial within it), calling launch() again silently no-ops - no
    // dialog, no callback the user can see. shouldShowRequestPermissionRationale() reliably tells
    // us this only once we've already asked at least once, which is exactly the case right after
    // our own callback reports a denial, so the "Grant Permission" button can then switch to
    // opening the app's system settings page instead of retrying a request that will never show.
    var canRequestNotifDirectly by remember { mutableStateOf(true) }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            canRequestNotifDirectly = activity == null ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        }
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.testTag("onboarding_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                            shape = MaterialTheme.shapes.extraLarge,
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
                            shape = MaterialTheme.shapes.extraLarge,
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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

            // Step Progress Indicator (+ an escape hatch when this wizard was reopened from
            // Settings, in case "Re-run Setup Wizard" was tapped by mistake)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnboardingStep.values().forEach { step ->
                        val isPast = step.ordinal < currentStep.ordinal
                        val isCurrent = step == currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
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

                if (onSkip != null) {
                    TextButton(onClick = onSkip, modifier = Modifier.testTag("onboarding_skip_button")) {
                        Text(text = strings.skipBtn, fontWeight = FontWeight.SemiBold)
                    }
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
                        (slideInHorizontally(animationSpec = ExpressiveMotion.emphasized()) { width -> if (strings.isArabic) -width else width } + fadeIn(animationSpec = ExpressiveMotion.standard())).togetherWith(
                            slideOutHorizontally(animationSpec = ExpressiveMotion.emphasized()) { width -> if (strings.isArabic) width else -width } + fadeOut(animationSpec = ExpressiveMotion.standard())
                        )
                    } else {
                        (slideInHorizontally(animationSpec = ExpressiveMotion.emphasized()) { width -> if (strings.isArabic) width else -width } + fadeIn(animationSpec = ExpressiveMotion.standard())).togetherWith(
                            slideOutHorizontally(animationSpec = ExpressiveMotion.emphasized()) { width -> if (strings.isArabic) -width else width } + fadeOut(animationSpec = ExpressiveMotion.standard())
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
                    OnboardingStep.CALCULATION_METHOD -> {
                        OnboardingCalculationMethodStep(
                            selectedMethod = settings.calculationMethod,
                            suggestedMethod = suggestCalculationMethod(settings.location.timeZoneId),
                            onSelectMethod = onUpdateCalculationMethod
                        )
                    }
                    OnboardingStep.NOTIFICATIONS -> {
                        OnboardingNotificationStep(
                            hasPermission = hasNotifPermission,
                            canRequestDirectly = canRequestNotifDirectly,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    hasNotifPermission = true
                                }
                            },
                            onOpenAppSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    OnboardingStep.ATHAN_SOUNDS -> {
                        OnboardingAthanStep(
                            settings = settings,
                            onUpdateNotificationConfig = onUpdateNotificationConfig,
                            onPreviewSound = onPreviewSound,
                            onUpdateLiveCountdownSettings = onUpdateLiveCountdownSettings
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
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
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
    var searchResults by remember { mutableStateOf<List<PlaceEntity>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
        } else {
            delay(200)
            searchResults = PlaceRepository.search(context, searchQuery)
        }
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
                        if (currentLocation.isGps) {
                            Text(
                                text = String.format(Locale.US, "%.4f°, %.4f°", currentLocation.latitude, currentLocation.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentLocation.nearestPlaceDistanceKm != null) {
                                Text(
                                    text = strings.gpsLocationDisclaimer,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
            placeholder = { Text(strings.locationSearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = strings.clear)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Cities List
        if (searchQuery.isNotBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(searchResults, key = { it.geonameId }) { place ->
                    val isSelected = !currentLocation.isGps &&
                        currentLocation.latitude == place.latitude &&
                        currentLocation.longitude == place.longitude
                    val placeName = if (strings.isArabic) place.nameAr ?: place.nameEn else place.nameEn
                    val countryName = Locale("", place.countryCode)
                        .getDisplayCountry(if (strings.isArabic) Locale("ar") else Locale.ENGLISH)
                    Card(
                        onClick = {
                            onSelectCity(
                                UserLocation(
                                    name = placeName,
                                    country = countryName,
                                    latitude = place.latitude,
                                    longitude = place.longitude,
                                    timeZoneId = place.timeZoneId,
                                    isGps = false
                                )
                            )
                        },
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
                                    text = placeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$countryName • ${place.timeZoneId}",
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
}

// -------------------------------------------------------------
// STEP 2b: CALCULATION METHOD (pre-selected from the chosen location)
// -------------------------------------------------------------
@Composable
private fun OnboardingCalculationMethodStep(
    selectedMethod: CalculationMethod,
    suggestedMethod: CalculationMethod,
    onSelectMethod: (CalculationMethod) -> Unit
) {
    val strings = LocalAppStrings.current

    Column(modifier = Modifier.fillMaxSize().testTag("onboarding_step_calc_method")) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.stepCalcMethodTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.stepCalcMethodDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Suggestion banner - not everyone knows which method applies to them, so this makes the
        // pre-selected choice (already applied to selectedMethod) visible and explained.
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.calcMethodSuggestedBanner(strings.calcMethodName(suggestedMethod)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CalculationMethod.values()) { method ->
                val isSelected = selectedMethod == method
                Card(
                    onClick = { onSelectMethod(method) },
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.calcMethodName(method),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val ishaDesc = if ((method.ishaMinutesAfterMaghrib ?: 0) > 0) "${method.ishaMinutesAfterMaghrib}m" else "${method.ishaAngle}°"
                            Text(
                                text = "${strings.prayerName(PrayerType.FAJR)}: ${method.fajrAngle}° • ${strings.prayerName(PrayerType.ISHA)}: $ishaDesc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(selected = isSelected, onClick = { onSelectMethod(method) })
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
    canRequestDirectly: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
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
                    if (canRequestDirectly) {
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.grantNotificationPermissionBtn, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // The OS will no longer show the permission dialog for this app (denied
                        // once already) - the only way forward is the system settings page.
                        Text(
                            text = strings.permissionDeniedOpenSettingsDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.openAppSettingsBtn, fontWeight = FontWeight.Bold)
                        }
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
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit,
    onUpdateLiveCountdownSettings: (Boolean, Int) -> Unit
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

        Spacer(modifier = Modifier.height(14.dp))

        // Live Prayer Countdown - the same feature as Settings > Notifications' "Live Athan
        // Countdown" card, surfaced here too since most people don't know it exists until they
        // stumble into Settings; fixed at 15 minutes here, adjustable later in Settings.
        Card(
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.live_countdown_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.live_countdown_section_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.liveCountdownEnabled,
                    onCheckedChange = { isChecked ->
                        onUpdateLiveCountdownSettings(isChecked, settings.liveCountdownMinutesBefore)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
        items(AppColorPreset.values().filter {
            it != AppColorPreset.SYSTEM_DYNAMIC && it != AppColorPreset.CUSTOM
        }) { preset ->
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
