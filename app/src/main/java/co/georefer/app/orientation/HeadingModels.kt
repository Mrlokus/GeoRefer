package co.georefer.app.orientation

data class HeadingUiState(
    val trueHeadingDegrees: Float? = null,
    val sensorAvailable: Boolean = true,
    val accuracy: HeadingAccuracy = HeadingAccuracy.UNKNOWN,
)

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
}
