package co.geoluker.app.points

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointNavigationTest {
    @Test
    fun `calcula distancia y rumbo aproximados entre dos puntos`() {
        val target = FieldPoint(
            id = 1,
            name = "Este",
            note = "",
            latitude = 4.0,
            longitude = -72.99,
            createdAtMillis = 0,
        )

        val navigation = navigationToPoint(4.0, -73.0, target)

        assertTrue(navigation.distanceMeters in 1_100.0..1_120.0)
        assertTrue(navigation.bearingDegrees in 89.0..91.0)
        assertEquals("E", navigation.cardinal)
    }

    @Test
    fun `detecta coordenadas practicamente duplicadas`() {
        val distance = distanceBetweenCoordinates(4.578214, -72.836150, 4.578220, -72.836150)
        assertTrue(distance < 1.0)
    }
}
