package co.geoluker.app.location

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtElapsedRealtimeNanos: Long,
    val speedMetersPerSecond: Float? = null,
    val courseDegrees: Float? = null,
)

data class GpsSnapshot(
    val fix: GpsFix? = null,
    val providerEnabled: Boolean = false,
    val satellitesVisible: Int = 0,
    val satellitesUsed: Int = 0,
    val errorMessage: String? = null,
)

enum class LocationPermissionLevel {
    NONE,
    APPROXIMATE,
    PRECISE,
}

enum class GpsAvailability {
    NEEDS_PERMISSION,
    APPROXIMATE_ONLY,
    LOCATION_DISABLED,
    ACTIVE,
    ERROR,
}

enum class GpsQuality(val rank: Int) {
    SEARCHING(0),
    LOW(1),
    ACCEPTABLE(2),
    GOOD(3),
}

data class GpsUiState(
    val availability: GpsAvailability = GpsAvailability.NEEDS_PERMISSION,
    val quality: GpsQuality = GpsQuality.SEARCHING,
    val fix: GpsFix? = null,
    val satellitesVisible: Int = 0,
    val satellitesUsed: Int = 0,
    val readingAgeSeconds: Long? = null,
    val errorMessage: String? = null,
)
