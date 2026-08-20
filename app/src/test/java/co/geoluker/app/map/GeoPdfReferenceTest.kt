package co.geoluker.app.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoPdfReferenceTest {
    private val reference = GeoPdfReference(
        pageWidthPoints = 792.0,
        pageHeightPoints = 612.0,
        viewportBox = listOf(23.5202, 591.12128, 765.60645, 26.43575),
        controlPoints = listOf(
            GeoControlPoint(0.0, 1.0, 4.53347, -72.89813),
            GeoControlPoint(0.0, 0.0, 4.62352, -72.89798),
            GeoControlPoint(1.0, 0.0, 4.62331, -72.78005),
            GeoControlPoint(1.0, 1.0, 4.53326, -72.78022),
        ),
    )

    @Test
    fun `convierte una posicion de pagina nuevamente a coordenadas`() {
        val originalLatitude = 4.578214
        val originalLongitude = -72.836150
        val page = requireNotNull(reference.pagePosition(originalLatitude, originalLongitude))
        val coordinate = requireNotNull(
            reference.coordinateAtPagePosition(page.xFraction, page.yFraction),
        )

        assertEquals(originalLatitude, coordinate.latitude, 0.000001)
        assertEquals(originalLongitude, coordinate.longitude, 0.000001)
    }

    @Test
    fun `proyecta la esquina noroccidental en el viewport del pdf`() {
        val position = reference.pagePosition(4.62352, -72.89798)
        assertNotNull(position)
        assertTrue(position!!.insideMap)
        assertEquals((23.5202 / 792.0).toFloat(), position.xFraction, 0.0001f)
        assertEquals((1.0 - 591.12128 / 612.0).toFloat(), position.yFraction, 0.0001f)
    }

    @Test
    fun `proyecta la esquina suroriental en el viewport del pdf`() {
        val position = reference.pagePosition(4.53326, -72.78022)
        assertNotNull(position)
        assertTrue(position!!.insideMap)
        assertEquals((765.60645 / 792.0).toFloat(), position.xFraction, 0.0001f)
        assertEquals((1.0 - 26.43575 / 612.0).toFloat(), position.yFraction, 0.0001f)
    }

    @Test
    fun `detecta una ubicacion por fuera del mapa`() {
        val position = reference.pagePosition(5.0, -73.2)
        assertNotNull(position)
        assertFalse(position!!.insideMap)
    }

    @Test
    fun `considera la incertidumbre gps cerca del borde`() {
        val justOutside = reference.pagePosition(4.62360, -72.89798)
        assertNotNull(justOutside)
        assertFalse(justOutside!!.insideMap)
        assertTrue(reference.isInsideOrNear(4.62360, -72.89798, accuracyMeters = 20f))
    }

    @Test
    fun `estabiliza cambios intermitentes del limite`() {
        val stabilizer = BoundaryStatusStabilizer(outsideConfirmations = 3, insideConfirmations = 2)
        assertFalse(stabilizer.update(true))
        assertFalse(stabilizer.update(false))
        assertFalse(stabilizer.update(true))
        assertFalse(stabilizer.update(true))
        assertTrue(stabilizer.update(true))
        assertTrue(stabilizer.update(false))
        assertFalse(stabilizer.update(false))
    }
}
