package co.georefer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.georefer.app.location.GpsViewModel
import co.georefer.app.location.LocationPermissionLevel
import co.georefer.app.map.GeoPdfViewModel
import co.georefer.app.map.OfflineMapsViewModeldit
import co.georefer.app.orientation.HeadingViewModel
import co.georefer.app.points.PointsViewModel
import co.georefer.app.points.PointFileFormat
import co.georefer.app.settings.MapPreferencesViewModel
import co.georefer.app.ui.GeoreferApp
import co.georefer.app.ui.theme.GeoreferTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent {
            GeoreferTheme {
                val gpsViewModel: GpsViewModel = viewModel()
                val gpsState by gpsViewModel.uiState.collectAsStateWithLifecycle()
                val geoPdfViewModel: GeoPdfViewModel = viewModel()
                val geoPdfState by geoPdfViewModel.uiState.collectAsStateWithLifecycle()
                val offlineMapsViewModel: OfflineMapsViewModel = viewModel()
                val offlineMapsState by offlineMapsViewModel.uiState.collectAsStateWithLifecycle()
                val pointsViewModel: PointsViewModel = viewModel()
                val pointsState by pointsViewModel.uiState.collectAsStateWithLifecycle()
                val headingViewModel: HeadingViewModel = viewModel()
                val headingState by headingViewModel.uiState.collectAsStateWithLifecycle()
                val mapPreferencesViewModel: MapPreferencesViewModel = viewModel()
                val northUpLocked by mapPreferencesViewModel.northUpLocked.collectAsStateWithLifecycle()
                var permissionLevel by remember { mutableStateOf(currentPermissionLevel()) }
                var pendingExportFormat by remember { mutableStateOf<PointFileFormat?>(null) }

                LaunchedEffect(gpsState.fix) {
                    gpsState.fix?.let { fix ->
                        headingViewModel.updateLocation(fix.latitude, fix.longitude)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    permissionLevel = currentPermissionLevel()
                    gpsViewModel.onForeground(permissionLevel)
                }

                val pdfLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    uri?.let { selectedUri ->
                        runCatching {
                            contentResolver.takePersistableUriPermission(
                                selectedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        geoPdfViewModel.import(selectedUri)
                        offlineMapsViewModel.activateGeoPdf()
                    }
                }

                val pointsExportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("*/*"),
                ) { uri ->
                    val format = pendingExportFormat
                    if (uri != null && format != null) pointsViewModel.exportPoints(uri, format)
                    pendingExportFormat = null
                }

                val pointsImportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    uri?.let(pointsViewModel::importPoints)
                }

                ObserveAppLifecycle(
                    permissionLevel = permissionLevel,
                    refreshPermission = {
                        permissionLevel = currentPermissionLevel()
                        permissionLevel
                    },
                    onForeground = { level ->
                        gpsViewModel.onForeground(level)
                        headingViewModel.start()
                    },
                    onBackground = {
                        gpsViewModel.onBackground()
                        headingViewModel.stop()
                    },
                )

                GeoreferApp(
                    gpsState = gpsState,
                    geoPdfState = geoPdfState,
                    offlineMapsState = offlineMapsState,
                    pointsState = pointsState,
                    headingState = headingState,
                    northUpLocked = northUpLocked,
                    onRequestLocation = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    onOpenLocationSettings = {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    onOpenAppSettings = {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                    onImportPdf = {
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    },
                    onDownloadOfflineMap = offlineMapsViewModel::download,
                    onActivateOfflineMap = offlineMapsViewModel::activate,
                    onActivateGeoPdf = offlineMapsViewModel::activateGeoPdf,
                    onPauseOrResumeOfflineMap = offlineMapsViewModel::pauseOrResume,
                    onDeleteOfflineMap = offlineMapsViewModel::delete,
                    onBeginPoint = pointsViewModel::beginPoint,
                    onCancelPendingPoint = pointsViewModel::cancelPendingPoint,
                    onSavePendingPoint = pointsViewModel::savePendingPoint,
                    onDeletePoint = pointsViewModel::deletePoint,
                    onUpdatePoint = pointsViewModel::updatePoint,
                    onExportPoints = { format ->
                        pendingExportFormat = format
                        pointsExportLauncher.launch("Georefer-puntos.${format.extension}")
                    },
                    onImportPoints = {
                        pointsImportLauncher.launch(
                            arrayOf(
                                "application/geo+json",
                                "application/json",
                                "application/vnd.google-earth.kml+xml",
                                "application/gpx+xml",
                                "text/csv",
                                "text/*",
                                "application/xml",
                            ),
                        )
                    },
                    onClearPointsMessage = pointsViewModel::clearMessage,
                    onNorthUpLockedChange = mapPreferencesViewModel::setNorthUpLocked,
                )
            }
        }
    }

    private fun currentPermissionLevel(): LocationPermissionLevel = when {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED -> LocationPermissionLevel.PRECISE

        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED -> LocationPermissionLevel.APPROXIMATE

        else -> LocationPermissionLevel.NONE
    }

}

@Composable
private fun ObserveAppLifecycle(
    permissionLevel: LocationPermissionLevel,
    refreshPermission: () -> LocationPermissionLevel,
    onForeground: (LocationPermissionLevel) -> Unit,
    onBackground: () -> Unit,
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, permissionLevel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> onForeground(refreshPermission())

                Lifecycle.Event.ON_STOP -> onBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            onForeground(refreshPermission())
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onBackground()
        }
    }
}
