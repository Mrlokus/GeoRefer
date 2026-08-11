package co.georefer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import co.georefer.app.location.GpsUiState
import co.georefer.app.map.GeoPdfUiState
import co.georefer.app.map.MapArea
import co.georefer.app.map.OfflineMapStyle
import co.georefer.app.map.OfflineMapsUiState
import co.georefer.app.orientation.HeadingUiState
import co.georefer.app.points.PointsUiState
import co.georefer.app.points.FieldPoint
import co.georefer.app.points.PointFileFormat

private enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    MAP("Mapa", Icons.Outlined.Map),
    DOWNLOADS("Descargas", Icons.Outlined.Download),
    POINTS("Puntos", Icons.Outlined.Place),
    SETTINGS("Ajustes", Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoreferApp(
    gpsState: GpsUiState,
    geoPdfState: GeoPdfUiState,
    offlineMapsState: OfflineMapsUiState,
    pointsState: PointsUiState,
    headingState: HeadingUiState,
    northUpLocked: Boolean,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onImportPdf: () -> Unit,
    onDownloadOfflineMap: (String, OfflineMapStyle, MapArea, Double, Double) -> Unit,
    onActivateOfflineMap: (Long) -> Unit,
    onActivateGeoPdf: () -> Unit,
    onPauseOrResumeOfflineMap: (Long) -> Unit,
    onDeleteOfflineMap: (Long) -> Unit,
    onBeginPoint: (Double, Double) -> Unit,
    onCancelPendingPoint: () -> Unit,
    onSavePendingPoint: (String, String) -> Unit,
    onDeletePoint: (Long) -> Unit,
    onUpdatePoint: (Long, String, String) -> Unit,
    onExportPoints: (PointFileFormat) -> Unit,
    onImportPoints: () -> Unit,
    onClearPointsMessage: () -> Unit,
    onNorthUpLockedChange: (Boolean) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.MAP) }
    var focusPoint by remember { mutableStateOf<FieldPoint?>(null) }
    var focusPointRequest by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(text = "Georefer", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(68.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(21.dp)) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    }
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (destination) {
                AppDestination.MAP -> MapScreen(
                    gpsState = gpsState,
                    geoPdfState = geoPdfState,
                    offlineMapsState = offlineMapsState,
                    pointsState = pointsState,
                    headingState = headingState,
                    northUpLocked = northUpLocked,
                    onRequestLocation = onRequestLocation,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onDownloadMap = { destination = AppDestination.DOWNLOADS },
                    onImportPdf = onImportPdf,
                    onBeginPoint = onBeginPoint,
                    onCancelPendingPoint = onCancelPendingPoint,
                    onSavePendingPoint = onSavePendingPoint,
                    focusPoint = focusPoint,
                    focusPointRequest = focusPointRequest,
                    onUpdatePoint = onUpdatePoint,
                )

                AppDestination.DOWNLOADS -> DownloadsScreen(
                    state = offlineMapsState,
                    geoPdfState = geoPdfState,
                    onDownload = onDownloadOfflineMap,
                    onActivate = onActivateOfflineMap,
                    onActivateGeoPdf = onActivateGeoPdf,
                    onPauseOrResume = onPauseOrResumeOfflineMap,
                    onDelete = onDeleteOfflineMap,
                    onImportPdf = onImportPdf,
                )
                AppDestination.POINTS -> PointsScreen(
                    state = pointsState,
                    gpsFix = gpsState.fix,
                    onDeletePoint = onDeletePoint,
                    onUpdatePoint = onUpdatePoint,
                    onLocatePoint = { point ->
                        focusPoint = point
                        focusPointRequest += 1
                        destination = AppDestination.MAP
                    },
                    onExport = onExportPoints,
                    onImport = onImportPoints,
                    onClearMessage = onClearPointsMessage,
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    northUpLocked = northUpLocked,
                    onNorthUpLockedChange = onNorthUpLockedChange,
                )
            }
        }
    }
}
