package co.geoluker.app.points

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PointNavigationInfo(
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val cardinal: String,
)

fun navigationToPoint(
    fromLatitude: Double,
    fromLongitude: Double,
    point: FieldPoint,
): PointNavigationInfo {
    val latitude1 = Math.toRadians(fromLatitude)
    val latitude2 = Math.toRadians(point.latitude)
    val deltaLatitude = latitude2 - latitude1
    val deltaLongitude = Math.toRadians(point.longitude - fromLongitude)
    val a = sin(deltaLatitude / 2).let { it * it } +
        cos(latitude1) * cos(latitude2) * sin(deltaLongitude / 2).let { it * it }
    val distance = EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    val y = sin(deltaLongitude) * cos(latitude2)
    val x = cos(latitude1) * sin(latitude2) -
        sin(latitude1) * cos(latitude2) * cos(deltaLongitude)
    val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    val directions = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return PointNavigationInfo(
        distanceMeters = distance,
        bearingDegrees = bearing,
        cardinal = directions[((bearing + 22.5) / 45.0).toInt() % directions.size],
    )
}

fun distanceBetweenCoordinates(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double,
): Double {
    val firstLatitude = Math.toRadians(latitude1)
    val secondLatitude = Math.toRadians(latitude2)
    val latitudeDelta = secondLatitude - firstLatitude
    val longitudeDelta = Math.toRadians(longitude2 - longitude1)
    val a = sin(latitudeDelta / 2).let { it * it } +
        cos(firstLatitude) * cos(secondLatitude) * sin(longitudeDelta / 2).let { it * it }
    return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private const val EARTH_RADIUS_METERS = 6_371_008.8
