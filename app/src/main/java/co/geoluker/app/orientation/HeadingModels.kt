package co.geoluker.app.orientation

data class HeadingUiState(
    val trueHeadingDegrees: Float? = null,
    val sensorAvailable: Boolean = false,
    val accuracy: HeadingAccuracy = HeadingAccuracy.UNKNOWN,
    val source: HeadingSource = HeadingSource.UNAVAILABLE,
)

enum class HeadingSource {
    SENSOR,
    GPS_COURSE,
    UNAVAILABLE,
}

enum class HeadingAccuracy {
    UNKNOWN,
    LOW,
    MEDIUM,
    HIGH,
}

object HeadingMath {
    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    fun shortestDelta(fromDegrees: Float, toDegrees: Float): Float =
        ((toDegrees - fromDegrees + 540f) % 360f) - 180f

    /** Descarta sensores virtuales defectuosos que publican un cuaternión de cinco ceros. */
    fun hasUsableRotationVector(values: FloatArray): Boolean {
        if (values.size < 3 || values.take(minOf(values.size, 4)).any { !it.isFinite() }) return false
        if (values.size == 3) return true
        val quaternionNormSquared = values.take(4).sumOf { value ->
            (value * value).toDouble()
        }
        return quaternionNormSquared >= MIN_QUATERNION_NORM_SQUARED
    }

    fun usableGpsCourse(courseDegrees: Float?, speedMetersPerSecond: Float?): Float? {
        val course = courseDegrees?.takeIf(Float::isFinite) ?: return null
        val speed = speedMetersPerSecond?.takeIf(Float::isFinite) ?: return null
        if (speed < MIN_GPS_COURSE_SPEED_MPS) return null
        return normalize(course)
    }

    private const val MIN_QUATERNION_NORM_SQUARED = 0.25
    private const val MIN_GPS_COURSE_SPEED_MPS = 0.8f
}
