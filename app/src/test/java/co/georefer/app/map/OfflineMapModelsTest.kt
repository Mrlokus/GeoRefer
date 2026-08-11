package co.georefer.app.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapModelsTest {
    @Test
    fun `incluye todos los departamentos y bogota`() {
        assertEquals(33, DepartmentArea.entries.size)
        assertEquals(33, DepartmentArea.entries.map { it.daneCode }.distinct().size)
    }

    @Test
    fun `el estilo satelital usa una url valida para el motor offline`() {
        assertTrue(OfflineMapStyle.SATELLITE_2025.styleUrl.startsWith("https://"))
    }

    @Test
    fun `los departamentos caben dentro del limite de descarga`() {
        DepartmentArea.entries.forEach { department ->
            val count = estimateTileCount(department.bounds, 6, 11)
            assertTrue("${department.label}: $count", count in 1..6_000)
        }
    }

    @Test
    fun `un rectangulo menor necesita menos teselas`() {
        val large = MapArea(north = 5.0, east = -71.0, south = 2.0, west = -74.0)
        val small = MapArea(north = 4.7, east = -72.2, south = 4.4, west = -72.6)
        assertTrue(estimateTileCount(small, 7, 14) < estimateTileCount(large, 7, 14))
    }

    @Test
    fun `un area invalida no se puede estimar`() {
        val invalid = MapArea(north = 4.0, east = -73.0, south = 5.0, west = -72.0)
        assertEquals(Long.MAX_VALUE, estimateTileCount(invalid, 7, 14))
    }

    @Test
    fun `la vista satelital estima mas almacenamiento que la vectorial`() {
        val tiles = 1_000L
        assertTrue(
            estimateDownloadBytes(OfflineMapStyle.SATELLITE_2025, tiles) >
                estimateDownloadBytes(OfflineMapStyle.RURAL, tiles),
        )
    }

    @Test
    fun `una descarga completa informa integridad verificada`() {
        val map = downloadedMetaMap().copy(
            completedResources = 120,
            requiredResources = 120,
        )
        assertEquals(OfflineMapIntegrity.VERIFIED, map.integrity)
    }

    @Test
    fun `reconoce una descarga existente con la misma configuracion`() {
        val entry = downloadedMetaMap()

        assertTrue(
            entry.matchesDownload(
                style = OfflineMapStyle.RURAL,
                area = DepartmentArea.META.bounds.copy(north = DepartmentArea.META.bounds.north + 0.000001),
                minZoom = 6.0,
                maxZoom = 11.0,
            ),
        )
    }

    @Test
    fun `una vista diferente no es una descarga duplicada`() {
        assertFalse(
            downloadedMetaMap().matchesDownload(
                style = OfflineMapStyle.CARTOGRAPHIC,
                area = DepartmentArea.META.bounds,
                minZoom = 6.0,
                maxZoom = 11.0,
            ),
        )
    }

    private fun downloadedMetaMap() = OfflineMapEntry(
        regionId = 1L,
        name = "Meta · Rural",
        style = OfflineMapStyle.RURAL,
        area = DepartmentArea.META.bounds,
        minZoom = 6.0,
        maxZoom = 11.0,
        state = OfflineMapDownloadState.COMPLETE,
    )
}
