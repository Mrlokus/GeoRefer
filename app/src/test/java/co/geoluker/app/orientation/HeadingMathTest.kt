package co.geoluker.app.orientation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingMathTest {
    @Test
    fun `normaliza angulos al rango de cero a 360`() {
        assertEquals(350f, HeadingMath.normalize(-10f), 0.001f)
        assertEquals(10f, HeadingMath.normalize(370f), 0.001f)
    }

    @Test
    fun `calcula la ruta corta al cruzar el norte`() {
        assertEquals(20f, HeadingMath.shortestDelta(350f, 10f), 0.001f)
        assertEquals(-20f, HeadingMath.shortestDelta(10f, 350f), 0.001f)
    }

    @Test
    fun `descarta el vector virtual compuesto solamente por ceros`() {
        assertFalse(HeadingMath.hasUsableRotationVector(floatArrayOf(0f, 0f, 0f, 0f, 0f)))
    }

    @Test
    fun `acepta un cuaternion de rotacion valido`() {
        assertTrue(HeadingMath.hasUsableRotationVector(floatArrayOf(0f, 0f, 0f, 1f, 0f)))
    }

    @Test
    fun `descarta componentes de rotacion no finitos`() {
        assertFalse(HeadingMath.hasUsableRotationVector(floatArrayOf(Float.NaN, 0f, 0f, 1f)))
    }

    @Test
    fun `usa rumbo gps solo cuando hay velocidad de marcha`() {
        assertNull(HeadingMath.usableGpsCourse(courseDegrees = 92f, speedMetersPerSecond = 0.3f))
        assertEquals(
            350f,
            HeadingMath.usableGpsCourse(courseDegrees = -10f, speedMetersPerSecond = 1.2f) ?: -1f,
            0.001f,
        )
    }
}
