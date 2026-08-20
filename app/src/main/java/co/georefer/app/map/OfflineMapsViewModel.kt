package co.georefer.app.map

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import androidx.lifecycle.AndroidViewModel
import org.json.JSONObject
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineMapsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val manager = OfflineManager.getInstance(application)
    private val regions = mutableMapOf<Long, OfflineRegion>()
    private val downloadsInCreation = mutableSetOf<DownloadRequestKey>()
    private val _uiState = MutableStateFlow(
        OfflineMapsUiState(
            activeMapKey = preferences.getString(KEY_ACTIVE_MAP, null),
            availableStorageBytes = availableStorageBytes(application),
        ),
    )
    val uiState: StateFlow<OfflineMapsUiState> = _uiState.asStateFlow()

    init {
        manager.setOfflineMapboxTileCountLimit(MAX_TILE_COUNT)
        cacheBundledSatelliteStyle()
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                regions.clear()
                val regionsToObserve = mutableListOf<OfflineRegion>()
                var removedActiveRegion = false
                val entries = offlineRegions.orEmpty().mapNotNull { region ->
                    val entry = entryFrom(region) ?: return@mapNotNull null
                    if (isLegacySatelliteRegion(region, entry)) {
                        removedActiveRegion = removedActiveRegion ||
                            _uiState.value.activeMapKey == OfflineMapsUiState.REGION_KEY_PREFIX + region.id
                        removeLegacySatelliteRegion(region)
                        return@mapNotNull null
                    }
                    regions[region.id] = region
                    regionsToObserve += region
                    entry
                }
                if (removedActiveRegion) preferences.edit().remove(KEY_ACTIVE_MAP).apply()
                val sortedEntries = entries.sortedByDescending { it.regionId }
                _uiState.value = _uiState.value.copy(
                    maps = sortedEntries,
                    activeMapKey = if (removedActiveRegion) null else _uiState.value.activeMapKey,
                    isLoading = false,
                    availableStorageBytes = availableStorageBytes(),
                    managedMapBytes = sortedEntries.sumOf { it.downloadedBytes },
                )
                regionsToObserve.forEach(::observeAndReadStatus)
            }

            override fun onError(error: String) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error)
            }
        })
    }

    fun download(
        name: String,
        style: OfflineMapStyle,
        area: MapArea,
        minZoom: Double,
        maxZoom: Double,
    ) {
        if (!area.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = "El rectángulo seleccionado no es válido.")
            return
        }
        val existingMap = _uiState.value.maps.firstOrNull {
            it.matchesDownload(style, area, minZoom, maxZoom)
        }
        if (existingMap != null) {
            val message = when (existingMap.state) {
                OfflineMapDownloadState.COMPLETE -> "Este mapa ya está descargado y listo para usar."
                OfflineMapDownloadState.DOWNLOADING -> "Este mapa ya se está descargando."
                OfflineMapDownloadState.PAUSED -> "Este mapa ya existe. Reanuda la descarga guardada."
                OfflineMapDownloadState.WAITING -> "Este mapa ya está en preparación."
                OfflineMapDownloadState.ERROR -> "Este mapa ya existe. Reintenta la descarga guardada."
            }
            _uiState.value = _uiState.value.copy(errorMessage = message)
            return
        }
        val estimatedTiles = estimateTileCount(area, minZoom.toInt(), maxZoom.toInt())
        if (estimatedTiles > MAX_TILE_COUNT) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El área requiere aproximadamente $estimatedTiles teselas. Reduce el rectángulo o el nivel de detalle.",
            )
            return
        }
        val estimatedBytes = estimateDownloadBytes(style, estimatedTiles)
        val usableSpace = availableStorageBytes()
        if (estimatedBytes > (usableSpace - MIN_FREE_STORAGE_BYTES).coerceAtLeast(0L)) {
            _uiState.value = _uiState.value.copy(
                availableStorageBytes = usableSpace,
                errorMessage = "No hay espacio suficiente. Libera almacenamiento o selecciona un área menor.",
            )
            return
        }
        val requestKey = DownloadRequestKey(style, area, minZoom, maxZoom)
        if (!downloadsInCreation.add(requestKey)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Este mapa ya se está preparando.")
            return
        }
        if (style == OfflineMapStyle.SATELLITE_2025 && !cacheBundledSatelliteStyle()) {
            downloadsInCreation.remove(requestKey)
            _uiState.value = _uiState.value.copy(
                errorMessage = "No fue posible preparar el estilo Satélite 2025 incluido en la aplicación.",
            )
            return
        }

        val bounds = LatLngBounds.from(area.north, area.east, area.south, area.west)
        val definition = OfflineTilePyramidRegionDefinition(
            style.styleUrl,
            bounds,
            minZoom,
            maxZoom,
            1f,
            false,
        )
        val metadata = JSONObject()
            .put("name", name)
            .put("style", style.name)
            .put("north", area.north)
            .put("east", area.east)
            .put("south", area.south)
            .put("west", area.west)
            .put("minZoom", minZoom)
            .put("maxZoom", maxZoom)
            .put("estimatedBytes", estimatedBytes)
            .put("createdAtMillis", System.currentTimeMillis())
            .toString()
            .toByteArray(Charsets.UTF_8)

        _uiState.value = _uiState.value.copy(errorMessage = null)
        val createRegion = {
            manager.createOfflineRegion(
                definition,
                metadata,
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        downloadsInCreation.remove(requestKey)
                        regions[offlineRegion.id] = offlineRegion
                        entryFrom(offlineRegion)?.let(::upsert)
                        observeAndReadStatus(offlineRegion)
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    }

                    override fun onError(error: String) {
                        downloadsInCreation.remove(requestKey)
                        _uiState.value = _uiState.value.copy(errorMessage = error)
                    }
                },
            )
        }
        if (style == OfflineMapStyle.SATELLITE_2025) {
            Handler(Looper.getMainLooper()).postDelayed(createRegion, SATELLITE_STYLE_CACHE_DELAY_MS)
        } else {
            createRegion()
        }
    }

    fun activate(regionId: Long) {
        val key = OfflineMapsUiState.REGION_KEY_PREFIX + regionId
        preferences.edit().putString(KEY_ACTIVE_MAP, key).apply()
        _uiState.value = _uiState.value.copy(activeMapKey = key)
    }

    fun activateGeoPdf() {
        preferences.edit().putString(KEY_ACTIVE_MAP, OfflineMapsUiState.GEO_PDF_KEY).apply()
        _uiState.value = _uiState.value.copy(activeMapKey = OfflineMapsUiState.GEO_PDF_KEY)
    }

    fun pauseOrResume(regionId: Long) {
        val region = regions[regionId] ?: return
        val entry = _uiState.value.maps.firstOrNull { it.regionId == regionId } ?: return
        val nextState = if (entry.state == OfflineMapDownloadState.DOWNLOADING) {
            OfflineRegion.STATE_INACTIVE
        } else {
            OfflineRegion.STATE_ACTIVE
        }
        region.setDownloadState(nextState)
    }

    fun delete(regionId: Long) {
        val region = regions[regionId] ?: return
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                regions.remove(regionId)
                val wasActive = _uiState.value.activeMapKey == OfflineMapsUiState.REGION_KEY_PREFIX + regionId
                if (wasActive) preferences.edit().remove(KEY_ACTIVE_MAP).apply()
                _uiState.value = _uiState.value.copy(
                    maps = _uiState.value.maps.filterNot { it.regionId == regionId },
                    activeMapKey = if (wasActive) null else _uiState.value.activeMapKey,
                    availableStorageBytes = availableStorageBytes(),
                    managedMapBytes = _uiState.value.maps.filterNot { it.regionId == regionId }
                        .sumOf { it.downloadedBytes },
                )
            }

            override fun onError(error: String) {
                _uiState.value = _uiState.value.copy(errorMessage = error)
            }
        })
    }

    private fun observeAndReadStatus(region: OfflineRegion) {
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                updateStatus(region, status)
                if (status.isComplete) {
                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                }
            }

            override fun onError(error: OfflineRegionError) {
                updateEntry(region.id) { entry ->
                    entry.copy(
                        state = OfflineMapDownloadState.ERROR,
                        errorMessage = error.toString(),
                    )
                }
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                updateEntry(region.id) { entry ->
                    entry.copy(
                        state = OfflineMapDownloadState.ERROR,
                        errorMessage = "Se alcanzó el límite de $limit teselas. Reduce el área.",
                    )
                }
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
            }
        })
        region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
            override fun onStatus(status: OfflineRegionStatus?) {
                status?.let { updateStatus(region, it) }
            }

            override fun onError(error: String?) {
                updateEntry(region.id) { entry ->
                    entry.copy(errorMessage = error ?: "No fue posible leer el estado de la descarga.")
                }
            }
        })
    }

    private fun updateStatus(region: OfflineRegion, status: OfflineRegionStatus) {
        updateEntry(region.id) { entry ->
            entry.copy(
                state = when {
                    status.isComplete -> OfflineMapDownloadState.COMPLETE
                    status.downloadState == OfflineRegion.STATE_ACTIVE -> OfflineMapDownloadState.DOWNLOADING
                    status.completedResourceCount > 0 -> OfflineMapDownloadState.PAUSED
                    else -> OfflineMapDownloadState.WAITING
                },
                completedResources = status.completedResourceCount,
                requiredResources = status.requiredResourceCount,
            downloadedBytes = status.completedResourceSize,
                errorMessage = null,
            )
        }
    }

    private fun entryFrom(region: OfflineRegion): OfflineMapEntry? = runCatching {
        val metadata = JSONObject(String(region.metadata, Charsets.UTF_8))
        val definition = region.definition
        val bounds = definition.bounds ?: return null
        OfflineMapEntry(
            regionId = region.id,
            name = metadata.optString("name", "Mapa descargado"),
            style = OfflineMapStyle.valueOf(metadata.optString("style", OfflineMapStyle.RURAL.name)),
            area = MapArea(
                north = metadata.optDouble("north", bounds.latitudeNorth),
                east = metadata.optDouble("east", bounds.longitudeEast),
                south = metadata.optDouble("south", bounds.latitudeSouth),
                west = metadata.optDouble("west", bounds.longitudeWest),
            ),
            minZoom = metadata.optDouble("minZoom", definition.minZoom),
            maxZoom = metadata.optDouble("maxZoom", definition.maxZoom),
            estimatedBytes = metadata.optLong(
                "estimatedBytes",
                estimateDownloadBytes(
                    OfflineMapStyle.valueOf(metadata.optString("style", OfflineMapStyle.RURAL.name)),
                    estimateTileCount(
                        MapArea(
                            north = metadata.optDouble("north", bounds.latitudeNorth),
                            east = metadata.optDouble("east", bounds.longitudeEast),
                            south = metadata.optDouble("south", bounds.latitudeSouth),
                            west = metadata.optDouble("west", bounds.longitudeWest),
                        ),
                        metadata.optDouble("minZoom", definition.minZoom).toInt(),
                        metadata.optDouble("maxZoom", definition.maxZoom).toInt(),
                    ),
                ),
            ),
            createdAtMillis = metadata.optLong("createdAtMillis", region.id),
            state = OfflineMapDownloadState.WAITING,
        )
    }.getOrNull()

    private fun upsert(entry: OfflineMapEntry) {
        val maps = _uiState.value.maps.filterNot { it.regionId == entry.regionId } + entry
        val sortedMaps = maps.sortedByDescending { it.regionId }
        _uiState.value = _uiState.value.copy(
            maps = sortedMaps,
            availableStorageBytes = availableStorageBytes(),
            managedMapBytes = sortedMaps.sumOf { it.downloadedBytes },
        )
    }

    private fun updateEntry(regionId: Long, transform: (OfflineMapEntry) -> OfflineMapEntry) {
        val maps = _uiState.value.maps.map { entry ->
                if (entry.regionId == regionId) transform(entry) else entry
            }
        _uiState.value = _uiState.value.copy(
            maps = maps,
            availableStorageBytes = availableStorageBytes(),
            managedMapBytes = maps.sumOf { it.downloadedBytes },
        )
    }

    private fun cacheBundledSatelliteStyle(): Boolean = runCatching {
        val styleBytes = getApplication<Application>().assets
            .open(SATELLITE_STYLE_ASSET_PATH)
            .use { it.readBytes() }
        val expiresAtSeconds = System.currentTimeMillis() / 1_000L + SATELLITE_STYLE_CACHE_SECONDS
        manager.putResourceWithUrl(
            OfflineMapStyle.SATELLITE_2025.styleUrl,
            styleBytes,
            0L,
            expiresAtSeconds,
            null,
            false,
        )
    }.isSuccess

    private fun isLegacySatelliteRegion(region: OfflineRegion, entry: OfflineMapEntry): Boolean =
        entry.style == OfflineMapStyle.SATELLITE_2025 &&
            region.definition.styleURL != OfflineMapStyle.SATELLITE_2025.styleUrl

    private fun removeLegacySatelliteRegion(region: OfflineRegion) {
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() = Unit

            override fun onError(error: String) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Elimina la descarga satelital anterior y vuelve a intentarlo: $error",
                )
            }
        })
    }

    private data class DownloadRequestKey(
        val style: OfflineMapStyle,
        val area: MapArea,
        val minZoom: Double,
        val maxZoom: Double,
    )

    private fun availableStorageBytes(): Long = availableStorageBytes(getApplication())

    private fun availableStorageBytes(application: Application): Long = runCatching {
        val storageManager = application.getSystemService(StorageManager::class.java)
        val storageUuid = storageManager.getUuidForPath(application.filesDir)
        storageManager.getAllocatableBytes(storageUuid)
    }.getOrDefault(application.filesDir.freeSpace)

    private companion object {
        const val PREFERENCES_NAME = "georefer_app"
        const val KEY_ACTIVE_MAP = "active_map"
        const val MAX_TILE_COUNT = 6_000L
        const val MIN_FREE_STORAGE_BYTES = 150L * 1024L * 1024L
        const val SATELLITE_STYLE_ASSET_PATH = "styles/satellite_2025.json"
        const val SATELLITE_STYLE_CACHE_DELAY_MS = 250L
        const val SATELLITE_STYLE_CACHE_SECONDS = 10L * 365L * 24L * 60L * 60L
    }
}
