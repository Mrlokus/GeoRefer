package co.geoluker.app.map

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

data class GeoControlPoint(
    val localX: Double,
    val localY: Double,
    val latitude: Double,
    val longitude: Double,
)

data class GeoPdfReference(
    val pageWidthPoints: Double,
    val pageHeightPoints: Double,
    val viewportBox: List<Double>,
    val controlPoints: List<GeoControlPoint>,
) {
    fun pagePosition(latitude: Double, longitude: Double): GeoPdfPagePosition? {
        if (viewportBox.size != 4 || controlPoints.size < 4) return null

        val p00 = corner(0.0, 0.0) ?: return null
        val p10 = corner(1.0, 0.0) ?: return null
        val p01 = corner(0.0, 1.0) ?: return null
        val p11 = corner(1.0, 1.0) ?: return null

        val latitudeModel = BilinearModel(
            p00.latitude,
            p10.latitude,
            p01.latitude,
            p11.latitude,
        )
        val longitudeModel = BilinearModel(
            p00.longitude,
            p10.longitude,
            p01.longitude,
            p11.longitude,
        )

        var u = 0.5
        var v = 0.5
        repeat(12) {
            val latitudeError = latitudeModel.value(u, v) - latitude
            val longitudeError = longitudeModel.value(u, v) - longitude
            val determinant =
                latitudeModel.du(v) * longitudeModel.dv(u) -
                    latitudeModel.dv(u) * longitudeModel.du(v)
            if (abs(determinant) < 1e-14) return null

            val deltaU = (
                latitudeError * longitudeModel.dv(u) -
                    latitudeModel.dv(u) * longitudeError
                ) / determinant
            val deltaV = (
                latitudeModel.du(v) * longitudeError -
                    latitudeError * longitudeModel.du(v)
                ) / determinant

            u -= deltaU
            v -= deltaV
            if (!u.isFinite() || !v.isFinite()) return null
        }

        val xPdf = viewportBox[0] + u * (viewportBox[2] - viewportBox[0])
        val yPdf = viewportBox[1] + v * (viewportBox[3] - viewportBox[1])
        val xFraction = xPdf / pageWidthPoints
        val yFraction = 1.0 - (yPdf / pageHeightPoints)

        return GeoPdfPagePosition(
            xFraction = xFraction.toFloat(),
            yFraction = yFraction.toFloat(),
            insideMap = u in 0.0..1.0 && v in 0.0..1.0,
        )
    }

    fun coordinateAtPagePosition(xFraction: Float, yFraction: Float): GeoCoordinate? {
        if (viewportBox.size != 4 || controlPoints.size < 4) return null
        if (!xFraction.isFinite() || !yFraction.isFinite()) return null

        val viewportWidth = viewportBox[2] - viewportBox[0]
        val viewportHeight = viewportBox[3] - viewportBox[1]
        if (abs(viewportWidth) < 1e-12 || abs(viewportHeight) < 1e-12) return null

        val xPdf = xFraction * pageWidthPoints
        val yPdf = (1.0 - yFraction) * pageHeightPoints
        val u = (xPdf - viewportBox[0]) / viewportWidth
        val v = (yPdf - viewportBox[1]) / viewportHeight
        if (u !in 0.0..1.0 || v !in 0.0..1.0) return null

        val p00 = corner(0.0, 0.0) ?: return null
        val p10 = corner(1.0, 0.0) ?: return null
        val p01 = corner(0.0, 1.0) ?: return null
        val p11 = corner(1.0, 1.0) ?: return null

        return GeoCoordinate(
            latitude = BilinearModel(
                p00.latitude,
                p10.latitude,
                p01.latitude,
                p11.latitude,
            ).value(u, v),
            longitude = BilinearModel(
                p00.longitude,
                p10.longitude,
                p01.longitude,
                p11.longitude,
            ).value(u, v),
        )
    }

    fun isInsideOrNear(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        extraToleranceMeters: Double = 10.0,
    ): Boolean {
        if (pagePosition(latitude, longitude)?.insideMap == true) return true
        val reportedAccuracy = accuracyMeters.toDouble().takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val toleranceMeters = max(reportedAccuracy, MIN_BOUNDARY_TOLERANCE_METERS) + extraToleranceMeters
        val latitudeDelta = toleranceMeters / METERS_PER_LATITUDE_DEGREE
        val longitudeDelta = toleranceMeters /
            (METERS_PER_LATITUDE_DEGREE * cos(Math.toRadians(latitude)).coerceAtLeast(0.01))
        val samples = listOf(
            latitude + latitudeDelta to longitude,
            latitude - latitudeDelta to longitude,
            latitude to longitude + longitudeDelta,
            latitude to longitude - longitudeDelta,
            latitude + latitudeDelta * DIAGONAL_FACTOR to longitude + longitudeDelta * DIAGONAL_FACTOR,
            latitude + latitudeDelta * DIAGONAL_FACTOR to longitude - longitudeDelta * DIAGONAL_FACTOR,
            latitude - latitudeDelta * DIAGONAL_FACTOR to longitude + longitudeDelta * DIAGONAL_FACTOR,
            latitude - latitudeDelta * DIAGONAL_FACTOR to longitude - longitudeDelta * DIAGONAL_FACTOR,
        )
        return samples.any { (sampleLatitude, sampleLongitude) ->
            pagePosition(sampleLatitude, sampleLongitude)?.insideMap == true
        }
    }

    private fun corner(x: Double, y: Double): GeoControlPoint? = controlPoints
        .minByOrNull { point ->
            val dx = point.localX - x
            val dy = point.localY - y
            dx * dx + dy * dy
        }
        ?.takeIf { point ->
            abs(point.localX - x) <= 0.05 && abs(point.localY - y) <= 0.05
        }
}

class BoundaryStatusStabilizer(
    private val outsideConfirmations: Int = 3,
    private val insideConfirmations: Int = 2,
) {
    private var outside = false
    private var candidate: Boolean? = null
    private var consecutive = 0

    fun update(isClearlyOutside: Boolean): Boolean {
        if (isClearlyOutside == outside) {
            candidate = null
            consecutive = 0
            return outside
        }
        if (candidate == isClearlyOutside) consecutive += 1 else {
            candidate = isClearlyOutside
            consecutive = 1
        }
        val required = if (isClearlyOutside) outsideConfirmations else insideConfirmations
        if (consecutive >= required) {
            outside = isClearlyOutside
            candidate = null
            consecutive = 0
        }
        return outside
    }

    fun reset() {
        outside = false
        candidate = null
        consecutive = 0
    }
}

data class GeoPdfPagePosition(
    val xFraction: Float,
    val yFraction: Float,
    val insideMap: Boolean,
)

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
)

private data class BilinearModel(
    val c0: Double,
    val c10: Double,
    val c01: Double,
    val c11: Double,
) {
    private val a0 = c0
    private val a1 = c10 - c0
    private val a2 = c01 - c0
    private val a3 = c11 - c10 - c01 + c0

    fun value(u: Double, v: Double): Double = a0 + a1 * u + a2 * v + a3 * u * v
    fun du(v: Double): Double = a1 + a3 * v
    fun dv(u: Double): Double = a2 + a3 * u
}

data class GeoPdfMap(
    val displayName: String,
    val bitmap: Bitmap,
    val reference: GeoPdfReference,
    val tileRenderer: GeoPdfTileRenderer,
)

enum class GeoPdfStatus {
    EMPTY,
    LOADING,
    READY,
    ERROR,
}

data class GeoPdfUiState(
    val status: GeoPdfStatus = GeoPdfStatus.EMPTY,
    val map: GeoPdfMap? = null,
    val pendingName: String? = null,
    val errorMessage: String? = null,
)

private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
private const val MIN_BOUNDARY_TOLERANCE_METERS = 8.0
private const val DIAGONAL_FACTOR = 0.70710678118
