package co.geoluker.app

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
import co.geoluker.app.location.GpsViewModel
import co.geoluker.app.location.LocationPermissionLevel
import co.geoluker.app.map.GeoPdfViewModel
import co.geoluker.app.orientation.HeadingViewModel
import co.geoluker.app.points.PointsViewModel
import co.geoluker.app.ui.GeoLukerApp
import co.geoluker.app.ui.theme.GeoLukerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoLukerTheme {
                val gpsViewModel: GpsViewModel = viewModel()
                val gpsState by gpsViewModel.uiState.collectAsStateWithLifecycle()
                val mapViewModel: GeoPdfViewModel = viewModel()
                val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()
                val pointsViewModel: PointsViewModel = viewModel()
                val pointsState by pointsViewModel.uiState.collectAsStateWithLifecycle()
                val headingViewModel: HeadingViewModel = viewModel()
                val headingState by headingViewModel.uiState.collectAsStateWithLifecycle()
                var permissionLevel by remember { mutableStateOf(currentPermissionLevel()) }

                LaunchedEffect(gpsState.fix) {
                    gpsState.fix?.let { fix ->
                        headingViewModel.updateLocation(
                            latitude = fix.latitude,
                            longitude = fix.longitude,
                            courseDegrees = fix.courseDegrees,
                            speedMetersPerSecond = fix.speedMetersPerSecond,
                        )
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    permissionLevel = currentPermissionLevel()
                    gpsViewModel.onForeground(permissionLevel)
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

                GeoLukerApp(
                    gpsState = gpsState,
                    mapState = mapState,
                    pointsState = pointsState,
                    headingState = headingState,
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
                    onBeginPoint = pointsViewModel::beginPoint,
                    onCancelPendingPoint = pointsViewModel::cancelPendingPoint,
                    onSavePendingPoint = pointsViewModel::savePendingPoint,
                    onDeletePoint = pointsViewModel::deletePoint,
                    onUpdatePoint = pointsViewModel::updatePoint,
                    onClearPointsMessage = pointsViewModel::clearMessage,
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
                Lifecycle.Event.ON_RESUME,
                -> onForeground(refreshPermission())

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
