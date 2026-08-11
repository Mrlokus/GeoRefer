package co.georefer.app.orientation

import org.junit.Assert.assertEquals
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
}
