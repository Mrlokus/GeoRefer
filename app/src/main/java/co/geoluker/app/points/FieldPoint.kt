package co.geoluker.app.points

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
