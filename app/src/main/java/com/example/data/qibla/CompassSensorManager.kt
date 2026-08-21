package com.example.data.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class CompassAccuracy {
    HIGH, MEDIUM, LOW, UNRELIABLE
}

data class CompassState(
    val azimuth: Float = 0f, // Device azimuth from True North (0..360)
    val qiblaBearing: Float = 0f, // Qibla bearing from True North (0..360)
    val relativeQiblaAngle: Float = 0f, // Angle to turn towards Kaaba (-180..180)
    val isAligned: Boolean = false, // Within ALIGNMENT_THRESHOLD_DEGREES of Qibla
    val accuracy: CompassAccuracy = CompassAccuracy.HIGH,
    val isSensorAvailable: Boolean = true,
    val distanceKm: Double = 0.0
)

class CompassSensorManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val ALIGNMENT_THRESHOLD_DEGREES = 3.5f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassState = MutableStateFlow(CompassState())
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private var userLat: Double = 0.0
    private var userLon: Double = 0.0
    private var qiblaBearing: Double = 0.0
    private var distanceKm: Double = 0.0
    private var declination: Float = 0f

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var smoothedAzimuth = 0f
    private val alpha = 0.2f // Low pass filter factor

    // Whether orientation is being driven by the rotation vector sensor - if so, the raw
    // accelerometer/magnetometer readings below are used only for their onAccuracyChanged
    // callback (compass calibration status), not fused into the orientation calculation.
    private var usingRotationVector = false

    fun setLocation(latitude: Double, longitude: Double) {
        userLat = latitude
        userLon = longitude
        qiblaBearing = QiblaCalculator.calculateQiblaBearing(latitude, longitude)
        distanceKm = QiblaCalculator.calculateDistanceToKaabaKm(latitude, longitude)

        try {
            val geoField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            )
            declination = geoField.declination
        } catch (e: Exception) {
            declination = 0f
        }

        updateCalculatedState(smoothedAzimuth)
    }

    fun start() {
        val hasRotation = rotationSensor != null && sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI
        )
        usingRotationVector = hasRotation

        // The rotation vector sensor rarely fires onAccuracyChanged on its own, which left the
        // accuracy badge stuck at its HIGH default forever. Registering the magnetometer here
        // (even when it isn't used for orientation) gives a real, live calibration signal.
        val hasMag = magnetSensor != null && sensorManager.registerListener(
            this,
            magnetSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        if (!hasRotation) {
            val hasAccel = accelSensor != null && sensorManager.registerListener(
                this,
                accelSensor,
                SensorManager.SENSOR_DELAY_UI
            )

            if (!hasAccel || !hasMag) {
                _compassState.value = _compassState.value.copy(
                    isSensorAvailable = false,
                    qiblaBearing = qiblaBearing.toFloat(),
                    distanceKm = distanceKm
                )
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawAzimuth = 0f

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            remapForDisplayRotation(rotationMatrix, remappedRotationMatrix)
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
            rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + declination
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && !usingRotationVector) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD && !usingRotationVector) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                gravityValues,
                geomagneticValues
            )
            if (success) {
                remapForDisplayRotation(rotationMatrix, remappedRotationMatrix)
                SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
                rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + declination
                hasGravity = false
                hasGeomagnetic = false
            }
        }

        // Normalize rawAzimuth to 0..360
        rawAzimuth = (rawAzimuth + 360f) % 360f

        // Smooth angle transition across 0/360 boundary
        val diff = (rawAzimuth - smoothedAzimuth + 540f) % 360f - 180f
        smoothedAzimuth = (smoothedAzimuth + alpha * diff + 360f) % 360f

        updateCalculatedState(smoothedAzimuth)
    }

    // Raw sensor axes are fixed to the device's physical orientation, not the "up" direction the
    // user is currently holding the screen at - without remapping, rotating the phone to landscape
    // (this Activity isn't orientation-locked) would report an azimuth 90 degrees off from what's
    // actually pointing at the top of the visible compass dial. DisplayManager (not
    // Context.display, which throws on a non-UI context like the Application context this class is
    // constructed with) works from any context.
    private fun remapForDisplayRotation(input: FloatArray, output: FloatArray) {
        val rotation = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        val (newX, newY) = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(input, newX, newY, output)
    }

    private fun updateCalculatedState(azimuth: Float) {
        val qibla = qiblaBearing.toFloat()
        // Relative angle: How many degrees clockwise to turn towards Qibla
        var relAngle = (qibla - azimuth + 540f) % 360f - 180f
        val isAligned = abs(relAngle) <= ALIGNMENT_THRESHOLD_DEGREES

        _compassState.value = _compassState.value.copy(
            azimuth = azimuth,
            qiblaBearing = qibla,
            relativeQiblaAngle = relAngle,
            isAligned = isAligned,
            isSensorAvailable = true,
            distanceKm = distanceKm
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val accEnum = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            else -> CompassAccuracy.UNRELIABLE
        }
        _compassState.value = _compassState.value.copy(accuracy = accEnum)
    }
}
