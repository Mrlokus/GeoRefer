package co.georefer.app.points

data class FieldPoint(
    val id: Long,
    val name: String,
    val note: String,
    val latitude: Double,
    val longitude: Double,
    val createdAtMillis: Long,
)

data class PointCandidate(
    val latitude: Double,
    val longitude: Double,
)

data class PointsUiState(
    val points: List<FieldPoint> = emptyList(),
    val pendingPoint: PointCandidate? = null,
    val message: String? = null,
)

enum class PointFileFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
) {
    GEOJSON("GeoJSON", "geojson", "application/geo+json"),
    KML("KML", "kml", "application/vnd.google-earth.kml+xml"),
    GPX("GPX", "gpx", "application/gpx+xml"),
    CSV("CSV", "csv", "text/csv"),
}

data class ImportedPoint(
    val name: String,
    val note: String,
    val latitude: Double,
    val longitude: Double,
)
