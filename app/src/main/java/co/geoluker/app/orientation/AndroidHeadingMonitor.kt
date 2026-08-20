package co.geoluker.app.orientation

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.display.DisplayManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.Display
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI

class AndroidHeadingMonitor(context: Context) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var acceleration: FloatArray? = null
    private var magneticField: FloatArray? = null
    private var declinationDegrees = 0f
    private var smoothedSensorHeading: Float? = null
    private var smoothedGpsCourse: Float? = null
    private var sensorAccuracy = HeadingAccuracy.UNKNOWN
    private var invalidRotationSamples = 0
    private var fallbackRegistered = false
    private var started = false

    private val _state = MutableStateFlow(HeadingUiState())
    val state: StateFlow<HeadingUiState> = _state.asStateFlow()

    fun start() {
        if (started) return
        started = true
        invalidRotationSamples = 0
        val rotationRegistered = rotationVector?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        } ?: false
        if (!rotationRegistered) registerFallbackSensors()
        publishEffectiveHeading()
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
        fallbackRegistered = false
        invalidRotationSamples = 0
        smoothedSensorHeading = null
        smoothedGpsCourse = null
        acceleration = null
        magneticField = null
        _state.value = HeadingUiState()
    }

    fun updateLocation(
        latitude: Double,
        longitude: Double,
        courseDegrees: Float?,
        speedMetersPerSecond: Float?,
    ) {
        if (!latitude.isFinite() || !longitude.isFinite()) return
        declinationDegrees = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis(),
        ).declination
        smoothedGpsCourse = HeadingMath.usableGpsCourse(
            courseDegrees = courseDegrees,
            speedMetersPerSecond = speedMetersPerSecond,
        )?.let { course ->
            smooth(previous = smoothedGpsCourse, current = course)
        }
        publishEffectiveHeading()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                if (!HeadingMath.hasUsableRotationVector(event.values)) {
                    invalidRotationSamples += 1
                    if (invalidRotationSamples >= INVALID_ROTATION_SAMPLE_LIMIT) {
                        smoothedSensorHeading = null
                        if (registerFallbackSensors()) {
                            rotationVector?.let { sensor ->
                                sensorManager.unregisterListener(this, sensor)
                            }
                            invalidRotationSamples = 0
                        }
                        publishEffectiveHeading()
                    }
                    return
                }
                invalidRotationSamples = 0
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                publishHeading(rotationMatrix)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                acceleration = event.values.copyOf()
                calculateFallbackHeading()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticField = event.values.copyOf()
                calculateFallbackHeading()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type != Sensor.TYPE_MAGNETIC_FIELD && sensor?.type != Sensor.TYPE_ROTATION_VECTOR) {
            return
        }
        sensorAccuracy = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HeadingAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> HeadingAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> HeadingAccuracy.LOW
            else -> HeadingAccuracy.UNKNOWN
        }
        publishEffectiveHeading()
    }

    private fun calculateFallbackHeading() {
        val gravity = acceleration ?: return
        val geomagnetic = magneticField ?: return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            publishHeading(rotationMatrix)
        }
    }

    private fun publishHeading(sourceMatrix: FloatArray) {
        val displayRotation = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation
            ?: Surface.ROTATION_0
        val (axisX, axisY) = when (displayRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(sourceMatrix, axisX, axisY, adjustedMatrix)) return
        SensorManager.getOrientation(adjustedMatrix, orientation)
        val magneticDegrees = (orientation[0] * 180f / PI.toFloat())
        val trueDegrees = HeadingMath.normalize(magneticDegrees + declinationDegrees)
        smoothedSensorHeading = smooth(previous = smoothedSensorHeading, current = trueDegrees)
        publishEffectiveHeading()
    }

    private fun registerFallbackSensors(): Boolean {
        if (fallbackRegistered) return true
        val accelerationSensor = accelerometer ?: return false
        val magneticSensor = magnetometer ?: return false
        val accelerometerRegistered = sensorManager.registerListener(
            this,
            accelerationSensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
        val magnetometerRegistered = sensorManager.registerListener(
            this,
            magneticSensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
        if (!accelerometerRegistered || !magnetometerRegistered) {
            sensorManager.unregisterListener(this, accelerationSensor)
            sensorManager.unregisterListener(this, magneticSensor)
            return false
        }
        fallbackRegistered = true
        return true
    }

    private fun smooth(previous: Float?, current: Float): Float = if (previous == null) {
        current
    } else {
        HeadingMath.normalize(
            previous + HeadingMath.shortestDelta(previous, current) * SMOOTHING_FACTOR,
        )
    }

    private fun publishEffectiveHeading() {
        val sensorHeading = smoothedSensorHeading
        val gpsCourse = smoothedGpsCourse
        _state.value = when {
            sensorHeading != null -> HeadingUiState(
                trueHeadingDegrees = sensorHeading,
                sensorAvailable = true,
                accuracy = sensorAccuracy,
                source = HeadingSource.SENSOR,
            )

            gpsCourse != null -> HeadingUiState(
                trueHeadingDegrees = gpsCourse,
                sensorAvailable = false,
                accuracy = HeadingAccuracy.UNKNOWN,
                source = HeadingSource.GPS_COURSE,
            )

            else -> HeadingUiState()
        }
    }

    private companion object {
        const val SMOOTHING_FACTOR = 0.16f
        const val INVALID_ROTATION_SAMPLE_LIMIT = 15
    }
}
