package co.georefer.app.map

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

data class MapArea(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double,
) {
    val isValid: Boolean
        get() = north > south && east > west &&
            north in -85.0..85.0 && south in -85.0..85.0 &&
            east in -180.0..180.0 && west in -180.0..180.0
}

enum class OfflineMapStyle(
    val label: String,
    val description: String,
    val styleUrl: String,
) {
    RURAL(
        label = "Rural",
        description = "Vías, caminos, predios y referencias de campo.",
        styleUrl = "https://tiles.openfreemap.org/styles/liberty",
    ),
    CARTOGRAPHIC(
        label = "Cartográfico",
        description = "Lectura clara de poblaciones, vías y límites.",
        styleUrl = "https://tiles.openfreemap.org/styles/bright",
    ),
    MINIMAL(
        label = "Minimalista",
        description = "Menos elementos para destacar tus puntos.",
        styleUrl = "https://tiles.openfreemap.org/styles/positron",
    ),
    HIGH_CONTRAST(
        label = "Alto contraste",
        description = "Vista oscura para lectura cómoda con poca luz.",
        styleUrl = "https://tiles.openfreemap.org/styles/fiord",
    ),
    SATELLITE_2025(
        label = "Satélite 2025",
        description = "Mosaico Sentinel-2 sin nubes para uso no comercial.",
        styleUrl = "https://offline.georefer.app/styles/satellite_2025.json",
    ),
}

enum class DepartmentArea(
    val daneCode: String,
    val label: String,
    val bounds: MapArea,
) {
    // Envolventes del Marco Geoestadístico Nacional integrado 2018 del DANE.
    AMAZONAS(
        daneCode = "91",
        label = "Amazonas",
        bounds = MapArea(north = 0.097257, east = -69.395496, south = -4.229406, west = -74.396344),
    ),
    ANTIOQUIA(
        daneCode = "05",
        label = "Antioquia",
        bounds = MapArea(north = 8.873974, east = -73.881282, south = 5.418558, west = -77.127832),
    ),
    ARAUCA(
        daneCode = "81",
        label = "Arauca",
        bounds = MapArea(north = 7.104381, east = -69.427555, south = 6.036228, west = -72.366624),
    ),
    ATLANTICO(
        daneCode = "08",
        label = "Atlántico",
        bounds = MapArea(north = 11.105370, east = -74.718330, south = 10.253286, west = -75.249527),
    ),
    BOGOTA_DC(
        daneCode = "11",
        label = "Bogotá, D. C.",
        bounds = MapArea(north = 4.836827, east = -73.986126, south = 3.730633, west = -74.450850),
    ),
    BOLIVAR(
        daneCode = "13",
        label = "Bolívar",
        bounds = MapArea(north = 10.801467, east = -73.745780, south = 6.999160, west = -76.190631),
    ),
    BOYACA(
        daneCode = "15",
        label = "Boyacá",
        bounds = MapArea(north = 7.055557, east = -71.948854, south = 4.655196, west = -74.664960),
    ),
    CALDAS(
        daneCode = "17",
        label = "Caldas",
        bounds = MapArea(north = 5.779182, east = -74.627456, south = 4.799700, west = -75.922510),
    ),
    CAQUETA(
        daneCode = "18",
        label = "Caquetá",
        bounds = MapArea(north = 2.964148, east = -71.253846, south = -0.705840, west = -76.306221),
    ),
    CASANARE(
        daneCode = "85",
        label = "Casanare",
        bounds = MapArea(north = 6.346111, east = -69.835910, south = 4.287476, west = -73.077769),
    ),
    CAUCA(
        daneCode = "19",
        label = "Cauca",
        bounds = MapArea(north = 3.328941, east = -75.747824, south = 0.958028, west = -77.928338),
    ),
    CESAR(
        daneCode = "20",
        label = "Cesar",
        bounds = MapArea(north = 10.867672, east = -72.885751, south = 7.674350, west = -74.139160),
    ),
    CHOCO(
        daneCode = "27",
        label = "Chocó",
        bounds = MapArea(north = 8.677730, east = -76.001846, south = 3.964883, west = -77.883776),
    ),
    CORDOBA(
        daneCode = "23",
        label = "Córdoba",
        bounds = MapArea(north = 9.447748, east = -74.780945, south = 7.347087, west = -76.514530),
    ),
    CUNDINAMARCA(
        daneCode = "25",
        label = "Cundinamarca",
        bounds = MapArea(north = 5.837258, east = -73.052557, south = 3.730129, west = -74.890629),
    ),
    GUAINIA(
        daneCode = "94",
        label = "Guainía",
        bounds = MapArea(north = 4.045026, east = -66.847215, south = 1.165633, west = -70.942492),
    ),
    GUAVIARE(
        daneCode = "95",
        label = "Guaviare",
        bounds = MapArea(north = 2.924987, east = -69.995107, south = 0.655413, west = -73.663909),
    ),
    HUILA(
        daneCode = "41",
        label = "Huila",
        bounds = MapArea(north = 3.843208, east = -74.413032, south = 1.552125, west = -76.624663),
    ),
    LA_GUAJIRA(
        daneCode = "44",
        label = "La Guajira",
        bounds = MapArea(north = 12.459443, east = -71.112958, south = 10.396759, west = -73.664941),
    ),
    MAGDALENA(
        daneCode = "47",
        label = "Magdalena",
        bounds = MapArea(north = 11.348912, east = -73.541838, south = 8.936489, west = -74.946600),
    ),
    META(
        daneCode = "50",
        label = "Meta",
        bounds = MapArea(north = 4.899101, east = -71.077526, south = 1.604238, west = -74.899207),
    ),
    NARINO(
        daneCode = "52",
        label = "Nariño",
        bounds = MapArea(north = 2.683898, east = -76.833681, south = 0.361348, west = -79.010211),
    ),
    NORTE_DE_SANTANDER(
        daneCode = "54",
        label = "Norte de Santander",
        bounds = MapArea(north = 9.290847, east = -72.047606, south = 6.872201, west = -73.633792),
    ),
    PUTUMAYO(
        daneCode = "86",
        label = "Putumayo",
        bounds = MapArea(north = 1.467315, east = -73.841318, south = -0.562278, west = -77.186806),
    ),
    QUINDIO(
        daneCode = "63",
        label = "Quindío",
        bounds = MapArea(north = 4.721360, east = -75.383172, south = 4.075311, west = -75.895796),
    ),
    RISARALDA(
        daneCode = "66",
        label = "Risaralda",
        bounds = MapArea(north = 5.567754, east = -75.375499, south = 4.663173, west = -76.211427),
    ),
    SAN_ANDRES_Y_PROVIDENCIA(
        daneCode = "88",
        label = "San Andrés y Providencia",
        bounds = MapArea(north = 13.394728, east = -81.349095, south = 12.480296, west = -81.735621),
    ),
    SANTANDER(
        daneCode = "68",
        label = "Santander",
        bounds = MapArea(north = 8.145010, east = -72.477059, south = 5.707536, west = -74.528954),
    ),
    SUCRE(
        daneCode = "70",
        label = "Sucre",
        bounds = MapArea(north = 10.145483, east = -74.532711, south = 8.277882, west = -75.706026),
    ),
    TOLIMA(
        daneCode = "73",
        label = "Tolima",
        bounds = MapArea(north = 5.319342, east = -74.474825, south = 2.871081, west = -76.105741),
    ),
    VALLE_DEL_CAUCA(
        daneCode = "76",
        label = "Valle del Cauca",
        bounds = MapArea(north = 5.047394, east = -75.707236, south = 3.091239, west = -77.549773),
    ),
    VAUPES(
        daneCode = "97",
        label = "Vaupés",
        bounds = MapArea(north = 2.080401, east = -69.115641, south = -1.227884, west = -72.033192),
    ),
    VICHADA(
        daneCode = "99",
        label = "Vichada",
        bounds = MapArea(north = 6.324317, east = -67.409803, south = 2.737109, west = -71.077931),
    ),
}

enum class OfflineMapDownloadState {
    WAITING,
    DOWNLOADING,
    COMPLETE,
    PAUSED,
    ERROR,
}

data class OfflineMapEntry(
    val regionId: Long,
    val name: String,
    val style: OfflineMapStyle,
    val area: MapArea,
    val minZoom: Double,
    val maxZoom: Double,
    val state: OfflineMapDownloadState,
    val completedResources: Long = 0,
    val requiredResources: Long = 0,
    val downloadedBytes: Long = 0,
    val estimatedBytes: Long = 0,
    val createdAtMillis: Long = 0,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = if (requiredResources > 0) {
            (completedResources.toFloat() / requiredResources.toFloat()).coerceIn(0f, 1f)
        } else if (state == OfflineMapDownloadState.COMPLETE) 1f else 0f

    val integrity: OfflineMapIntegrity
        get() = when {
            state == OfflineMapDownloadState.ERROR -> OfflineMapIntegrity.ERROR
            state == OfflineMapDownloadState.COMPLETE &&
                (requiredResources == 0L || completedResources >= requiredResources) -> OfflineMapIntegrity.VERIFIED
            state == OfflineMapDownloadState.PAUSED -> OfflineMapIntegrity.PARTIAL
            else -> OfflineMapIntegrity.IN_PROGRESS
        }
}

enum class OfflineMapIntegrity {
    VERIFIED,
    PARTIAL,
    IN_PROGRESS,
    ERROR,
}

fun OfflineMapEntry.matchesDownload(
    style: OfflineMapStyle,
    area: MapArea,
    minZoom: Double,
    maxZoom: Double,
): Boolean = this.style == style &&
    abs(this.area.north - area.north) <= DOWNLOAD_MATCH_TOLERANCE &&
    abs(this.area.east - area.east) <= DOWNLOAD_MATCH_TOLERANCE &&
    abs(this.area.south - area.south) <= DOWNLOAD_MATCH_TOLERANCE &&
    abs(this.area.west - area.west) <= DOWNLOAD_MATCH_TOLERANCE &&
    abs(this.minZoom - minZoom) <= DOWNLOAD_MATCH_TOLERANCE &&
    abs(this.maxZoom - maxZoom) <= DOWNLOAD_MATCH_TOLERANCE

data class OfflineMapsUiState(
    val maps: List<OfflineMapEntry> = emptyList(),
    val activeMapKey: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val availableStorageBytes: Long = 0,
    val managedMapBytes: Long = 0,
) {
    val activeOfflineMap: OfflineMapEntry?
        get() = activeMapKey
            ?.removePrefix(REGION_KEY_PREFIX)
            ?.toLongOrNull()
            ?.let { id -> maps.firstOrNull { it.regionId == id } }

    val isGeoPdfActive: Boolean
        get() = activeMapKey == GEO_PDF_KEY

    companion object {
        const val GEO_PDF_KEY = "geopdf"
        const val REGION_KEY_PREFIX = "region:"
    }
}

fun estimateDownloadBytes(style: OfflineMapStyle, tileCount: Long): Long {
    if (tileCount == Long.MAX_VALUE) return Long.MAX_VALUE
    val averageTileBytes = if (style == OfflineMapStyle.SATELLITE_2025) 48L * 1024L else 26L * 1024L
    return (tileCount * averageTileBytes * 1.08).toLong()
}

fun estimateTileCount(area: MapArea, minZoom: Int, maxZoom: Int): Long {
    if (!area.isValid || minZoom < 0 || maxZoom < minZoom) return Long.MAX_VALUE
    var total = 0L
    for (zoom in minZoom..maxZoom) {
        val tiles = 2.0.pow(zoom).toInt()
        val xMin = floor((area.west + 180.0) / 360.0 * tiles).toInt().coerceIn(0, tiles - 1)
        val xMax = floor((area.east + 180.0) / 360.0 * tiles).toInt().coerceIn(0, tiles - 1)
        val yMin = latitudeToTileY(area.north, zoom).coerceIn(0, tiles - 1)
        val yMax = latitudeToTileY(area.south, zoom).coerceIn(0, tiles - 1)
        total += (xMax - xMin + 1L) * (yMax - yMin + 1L)
    }
    return total
}

private fun latitudeToTileY(latitude: Double, zoom: Int): Int {
    val radians = Math.toRadians(latitude.coerceIn(-85.05112878, 85.05112878))
    val value = (1.0 - ln(tan(radians) + 1.0 / kotlin.math.cos(radians)) / PI) / 2.0
    return floor(value * 2.0.pow(zoom)).toInt()
}

private const val DOWNLOAD_MATCH_TOLERANCE = 0.00001
