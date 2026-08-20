package co.geoluker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import co.geoluker.app.R
import co.geoluker.app.location.GpsUiState
import co.geoluker.app.map.GeoPdfUiState
import co.geoluker.app.orientation.HeadingUiState
import co.geoluker.app.points.FieldPoint
import co.geoluker.app.points.PointsUiState

private enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    MAP("Mapa", Icons.Outlined.Map),
    POINTS("Puntos", Icons.Outlined.Place),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoLukerApp(
    gpsState: GpsUiState,
    mapState: GeoPdfUiState,
    pointsState: PointsUiState,
    headingState: HeadingUiState,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBeginPoint: (Double, Double) -> Unit,
    onCancelPendingPoint: () -> Unit,
    onSavePendingPoint: (String, String) -> Unit,
    onDeletePoint: (Long) -> Unit,
    onUpdatePoint: (Long, String, String) -> Unit,
    onClearPointsMessage: () -> Unit,
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
                        Image(
                            painter = painterResource(R.drawable.luker_agricola_logo),
                            contentDescription = "Luker Agrícola",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.width(112.dp).height(42.dp),
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("GeoLuker", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Orientación en plantación",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                        icon = {
                            Icon(item.icon, contentDescription = null, modifier = Modifier.size(21.dp))
                        },
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
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            when (destination) {
                AppDestination.MAP -> GeoLukerMapScreen(
                    gpsState = gpsState,
                    mapState = mapState,
                    pointsState = pointsState,
                    headingState = headingState,
                    onRequestLocation = onRequestLocation,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onBeginPoint = onBeginPoint,
                    onCancelPendingPoint = onCancelPendingPoint,
                    onSavePendingPoint = onSavePendingPoint,
                    focusPoint = focusPoint,
                    focusPointRequest = focusPointRequest,
                    onUpdatePoint = onUpdatePoint,
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
                    onClearMessage = onClearPointsMessage,
                )
            }
        }
    }
}
