package co.georefer.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import co.georefer.app.map.DepartmentArea
import co.georefer.app.map.GeoPdfStatus
import co.georefer.app.map.GeoPdfUiState
import co.georefer.app.map.MapArea
import co.georefer.app.map.OfflineMapDownloadState
import co.georefer.app.map.OfflineMapEntry
import co.georefer.app.map.OfflineMapIntegrity
import co.georefer.app.map.OfflineMapStyle
import co.georefer.app.map.OfflineMapsUiState
import co.georefer.app.map.estimateTileCount
import co.georefer.app.map.estimateDownloadBytes
import co.georefer.app.map.matchesDownload
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

private enum class AreaSelectionMode {
    DEPARTMENT,
    RECTANGLE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    state: OfflineMapsUiState,
    geoPdfState: GeoPdfUiState,
    onDownload: (String, OfflineMapStyle, MapArea, Double, Double) -> Unit,
    onActivate: (Long) -> Unit,
    onActivateGeoPdf: () -> Unit,
    onPauseOrResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onImportPdf: () -> Unit,
) {
    var mode by remember { mutableStateOf(AreaSelectionMode.DEPARTMENT) }
    var department by remember { mutableStateOf(DepartmentArea.META) }
    var departmentMenuExpanded by remember { mutableStateOf(false) }
    var style by remember { mutableStateOf(OfflineMapStyle.RURAL) }
    var rectangle by remember { mutableStateOf(DepartmentArea.META.bounds) }

    val selectedArea = if (mode == AreaSelectionMode.DEPARTMENT) department.bounds else rectangle
    val minZoom = if (mode == AreaSelectionMode.DEPARTMENT) 6 else 7
    val maxZoom = if (mode == AreaSelectionMode.DEPARTMENT) 11 else 14
    val estimatedTiles = estimateTileCount(selectedArea, minZoom, maxZoom)
    val estimatedBytes = estimateDownloadBytes(style, estimatedTiles)
    val existingMap = state.maps.firstOrNull {
        it.matchesDownload(style, selectedArea, minZoom.toDouble(), maxZoom.toDouble())
    }
    val isExistingMapActive = existingMap?.let {
        state.activeMapKey == OfflineMapsUiState.REGION_KEY_PREFIX + it.regionId
    } == true
    val hasEnoughStorage = state.availableStorageBytes - estimatedBytes >= 150L * 1024L * 1024L
    val areaCanBeDownloaded = selectedArea.isValid && estimatedTiles <= 6_000L && hasEnoughStorage

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Mapas offline", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Elige una zona y guárdala para trabajar sin señal.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(13.dp)) {
                    Text("Almacenamiento de mapas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${formatBytes(state.managedMapBytes)} usados · ${formatBytes(state.availableStorageBytes)} libres",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.availableStorageBytes < 500L * 1024L * 1024L) {
                        Text(
                            "Espacio bajo: elimina mapas que ya no uses.",
                            modifier = Modifier.padding(top = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "ÁREA") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == AreaSelectionMode.DEPARTMENT,
                        onClick = { mode = AreaSelectionMode.DEPARTMENT },
                        label = { Text("Departamento") },
                        leadingIcon = { Icon(Icons.Outlined.Map, contentDescription = null) },
                    )
                    FilterChip(
                        selected = mode == AreaSelectionMode.RECTANGLE,
                        onClick = { mode = AreaSelectionMode.RECTANGLE },
                        label = { Text("Rectángulo") },
                        leadingIcon = { Icon(Icons.Outlined.CropFree, contentDescription = null) },
                    )
                }

                if (mode == AreaSelectionMode.DEPARTMENT) {
                    ExposedDropdownMenuBox(
                        expanded = departmentMenuExpanded,
                        onExpandedChange = { departmentMenuExpanded = !departmentMenuExpanded },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = department.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Departamento") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentMenuExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = departmentMenuExpanded,
                            onDismissRequest = { departmentMenuExpanded = false },
                        ) {
                            DepartmentArea.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        department = option
                                        departmentMenuExpanded = false
                                    },
                                    leadingIcon = if (department == option) {
                                        { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Mueve y amplía el mapa. El marco dorado será el área descargada.",
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MapLibreMapView(
                        styleUrl = style.styleUrl,
                        initialArea = DepartmentArea.META.bounds,
                        northUpLocked = true,
                        showSelectionRectangle = true,
                        onVisibleAreaChanged = { rectangle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                }
            }
        }

        item {
            SectionCard(title = "VISTA") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OfflineMapStyle.entries.forEach { option ->
                        FilterChip(
                            selected = style == option,
                            onClick = { style = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text(
                    style.description,
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (style == OfflineMapStyle.SATELLITE_2025) {
                    Text(
                        "Licencia CC BY-NC-SA 4.0: requiere atribución y uso exclusivamente no comercial.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (mode == AreaSelectionMode.DEPARTMENT) department.label
                                else "Zona personalizada",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${style.label} · zoom $minZoom–$maxZoom · ~${formatCount(estimatedTiles)} teselas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Tamaño estimado: ${formatBytes(estimatedBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            enabled = when (existingMap?.state) {
                                OfflineMapDownloadState.COMPLETE -> !isExistingMapActive
                                OfflineMapDownloadState.PAUSED,
                                OfflineMapDownloadState.ERROR,
                                -> true
                                OfflineMapDownloadState.WAITING,
                                OfflineMapDownloadState.DOWNLOADING,
                                -> false
                                null -> areaCanBeDownloaded
                            },
                            onClick = {
                                when (existingMap?.state) {
                                    OfflineMapDownloadState.COMPLETE -> onActivate(existingMap.regionId)
                                    OfflineMapDownloadState.PAUSED,
                                    OfflineMapDownloadState.ERROR,
                                    -> onPauseOrResume(existingMap.regionId)
                                    OfflineMapDownloadState.WAITING,
                                    OfflineMapDownloadState.DOWNLOADING,
                                    -> Unit
                                    null -> {
                                        val areaName = if (mode == AreaSelectionMode.DEPARTMENT) {
                                            department.label
                                        } else {
                                            "Zona personalizada"
                                        }
                                        onDownload(
                                            "$areaName · ${style.label}",
                                            style,
                                            selectedArea,
                                            minZoom.toDouble(),
                                            maxZoom.toDouble(),
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                if (existingMap == null) Icons.Outlined.Download else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                when (existingMap?.state) {
                                    OfflineMapDownloadState.COMPLETE -> if (isExistingMapActive) "Activo" else "Usar mapa"
                                    OfflineMapDownloadState.PAUSED -> "Reanudar"
                                    OfflineMapDownloadState.ERROR -> "Reintentar"
                                    OfflineMapDownloadState.WAITING -> "Preparando"
                                    OfflineMapDownloadState.DOWNLOADING -> "Descargando"
                                    null -> "Descargar"
                                },
                            )
                        }
                    }
                    if (existingMap != null) {
                        Text(
                            "Esta combinación ya está guardada; no se descargará de nuevo.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else if (!hasEnoughStorage) {
                        Text(
                            "No hay espacio suficiente: la app conserva al menos 150 MB libres.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (!areaCanBeDownloaded) {
                        Text(
                            "El área es demasiado grande para este detalle. Amplía el mapa y selecciona un rectángulo menor.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Mis mapas",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        item {
            GeoPdfDownloadCard(
                state = geoPdfState,
                isActive = state.isGeoPdfActive,
                onActivate = onActivateGeoPdf,
                onImport = onImportPdf,
            )
        }

        if (state.maps.isEmpty()) {
            item {
                Text(
                    "Aún no has descargado zonas cartográficas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.maps, key = { it.regionId }) { map ->
                OfflineMapCard(
                    map = map,
                    isActive = state.activeMapKey == OfflineMapsUiState.REGION_KEY_PREFIX + map.regionId,
                    onActivate = { onActivate(map.regionId) },
                    onPauseOrResume = { onPauseOrResume(map.regionId) },
                    onDelete = { onDelete(map.regionId) },
                )
            }
        }

        state.errorMessage?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            Text(
                if (style == OfflineMapStyle.SATELLITE_2025) {
                    "EOxCloudless por EOX IT Services GmbH · Datos Copernicus Sentinel modificados 2025. " +
                        "La descarga requiere Internet; después funciona sin conexión."
                } else {
                    "Datos cartográficos: OpenStreetMap/OpenMapTiles. La descarga requiere Internet; " +
                        "después funciona sin conexión."
                },
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        content()
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun GeoPdfDownloadCard(
    state: GeoPdfUiState,
    isActive: Boolean,
    onActivate: () -> Unit,
    onImport: () -> Unit,
) {
    OutlinedCard(shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.FileOpen,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(state.map?.displayName ?: "GeoPDF local", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.status == GeoPdfStatus.READY) "Disponible en el teléfono" else "Importa un mapa desde tu almacenamiento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.status == GeoPdfStatus.READY) {
                FilledTonalButton(onClick = onActivate, enabled = !isActive) {
                    Text(if (isActive) "Activo" else "Usar")
                }
                IconButton(onClick = onImport) {
                    Icon(Icons.Outlined.FileOpen, contentDescription = "Cambiar archivo GeoPDF")
                }
            } else {
                OutlinedButton(onClick = onImport) { Text("Importar") }
            }
        }
    }
}

@Composable
private fun OfflineMapCard(
    map: OfflineMapEntry,
    isActive: Boolean,
    onActivate: () -> Unit,
    onPauseOrResume: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (map.state == OfflineMapDownloadState.COMPLETE) Icons.Outlined.CheckCircle
                    else Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(map.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatBytes(map.downloadedBytes)} · ${downloadLabel(map.state)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${integrityLabel(map.integrity)} · zoom ${map.minZoom.toInt()}–${map.maxZoom.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (map.integrity == OfflineMapIntegrity.ERROR) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    if (map.createdAtMillis > 0L) {
                        Text(
                            "Guardado ${formatMapDate(map.createdAtMillis)} · estimado ${formatBytes(map.estimatedBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                if (map.state == OfflineMapDownloadState.COMPLETE) {
                    FilledTonalButton(onClick = onActivate, enabled = !isActive) {
                        Text(if (isActive) "Activo" else "Usar")
                    }
                } else {
                    IconButton(onClick = onPauseOrResume) {
                        Icon(
                            if (map.state == OfflineMapDownloadState.DOWNLOADING) Icons.Outlined.Pause
                            else Icons.Outlined.PlayArrow,
                            contentDescription = "Pausar o reanudar",
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar mapa")
                }
            }
            if (map.state != OfflineMapDownloadState.COMPLETE) {
                LinearProgressIndicator(
                    progress = { map.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
            map.errorMessage?.let { message ->
                Text(
                    message,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun downloadLabel(state: OfflineMapDownloadState): String = when (state) {
    OfflineMapDownloadState.WAITING -> "Preparando"
    OfflineMapDownloadState.DOWNLOADING -> "Descargando"
    OfflineMapDownloadState.COMPLETE -> "Disponible sin conexión"
    OfflineMapDownloadState.PAUSED -> "Pausado"
    OfflineMapDownloadState.ERROR -> "Revisa la descarga"
}

private fun integrityLabel(integrity: OfflineMapIntegrity): String = when (integrity) {
    OfflineMapIntegrity.VERIFIED -> "Integridad verificada"
    OfflineMapIntegrity.PARTIAL -> "Descarga parcial"
    OfflineMapIntegrity.IN_PROGRESS -> "Verificando recursos"
    OfflineMapIntegrity.ERROR -> "Descarga con error"
}

private fun formatMapDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-CO")).format(Date(value))

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatCount(value: Long): String = String.format(Locale.US, "%,d", value)
