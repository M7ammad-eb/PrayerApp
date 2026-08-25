package com.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.ui.MainScreen
import com.prayertimes.ui.theme.MyApplicationTheme
import com.prayertimes.ui.viewmodel.PrayerViewModel

class MainActivity : ComponentActivity() {

    private val prayerViewModel: PrayerViewModel by viewModels()

    val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            prayerViewModel.requestGpsLocation(this)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            prayerViewModel.rescheduleAllAlarms()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        (application as PrayerApplication).prewarmUserCaches()

        requestAppPermissionsIfNeeded()

        setContent {
            val settings by prayerViewModel.settings.collectAsState()
            MyApplicationTheme(
                themeMode = settings.themeMode,
                colorPreset = settings.colorPreset,
                followSystemColors = settings.followSystemColors
            ) {
                MainScreen(
                    viewModel = prayerViewModel,
                    onRequestLocationPermission = { requestLocationPermission() },
                    onRequestNotificationPermission = { requestNotificationPermission() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        prayerViewModel.setForeground(true)
    }

    override fun onPause() {
        super.onPause()
        prayerViewModel.setForeground(false)
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun requestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            prayerViewModel.requestGpsLocation(this)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun requestAppPermissionsIfNeeded() {
        // Request notification permission on Android 13+, but only for a returning user who has
        // already been through onboarding - a fresh install must not be prompted before reaching
        // the dedicated notifications step (page 4) of the setup wizard, which owns this request
        // for first-run users. Read via the synchronous fast-cache path since the ViewModel's
        // DataStore-backed settings flow hasn't necessarily emitted yet at this point in onCreate().
        val initialSettings = PrayerPreferences.getInitialSettings(this)
        val onboardingCompleted = initialSettings.onboardingCompleted
        if (onboardingCompleted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // A manual city remains authoritative even if location permission is still granted.
        // Only an already GPS-based selection participates in the lightweight startup check.
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if ((fineGranted || coarseGranted) && initialSettings.location.isGps) {
            prayerViewModel.requestGpsLocation(this)
        }
    }
}


