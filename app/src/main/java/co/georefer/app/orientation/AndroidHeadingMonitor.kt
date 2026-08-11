package co.georefer.app.orientation

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
    private var smoothedHeading: Float? = null
    private var sensorAccuracy = HeadingAccuracy.UNKNOWN
    private var started = false

    private val _state = MutableStateFlow(
        HeadingUiState(sensorAvailable = hasUsableSensors()),
    )
    val state: StateFlow<HeadingUiState> = _state.asStateFlow()

    fun start() {
        if (started) return
        started = true
        val registered = rotationVector?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        } ?: run {
            val accelerometerRegistered = accelerometer?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            } ?: false
            val magnetometerRegistered = magnetometer?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            } ?: false
            accelerometerRegistered && magnetometerRegistered
        }
        _state.value = _state.value.copy(sensorAvailable = registered)
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
        smoothedHeading = null
        acceleration = null
        magneticField = null
        _state.value = _state.value.copy(trueHeadingDegrees = null)
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        if (!latitude.isFinite() || !longitude.isFinite()) return
        declinationDegrees = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis(),
        ).declination
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
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
        _state.value = _state.value.copy(accuracy = sensorAccuracy)
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
        val previous = smoothedHeading
        val smoothed = if (previous == null) {
            trueDegrees
        } else {
            HeadingMath.normalize(
                previous + HeadingMath.shortestDelta(previous, trueDegrees) * SMOOTHING_FACTOR,
            )
        }
        smoothedHeading = smoothed
        _state.value = HeadingUiState(
            trueHeadingDegrees = smoothed,
            sensorAvailable = true,
            accuracy = sensorAccuracy,
        )
    }

    private fun hasUsableSensors(): Boolean =
        rotationVector != null || (accelerometer != null && magnetometer != null)

    private companion object {
        const val SMOOTHING_FACTOR = 0.16f
    }
}
