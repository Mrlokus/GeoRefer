package co.georefer.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import co.georefer.app.location.GpsAvailability
import co.georefer.app.location.GpsQuality
import co.georefer.app.location.GpsUiState
import co.georefer.app.map.GeoPdfMap
import co.georefer.app.map.GeoPdfPagePosition
import co.georefer.app.map.GeoPdfStatus
import co.georefer.app.map.GeoPdfUiState
import co.georefer.app.map.OfflineMapDownloadState
import co.georefer.app.map.OfflineMapEntry
import co.georefer.app.map.OfflineMapStyle
import co.georefer.app.map.OfflineMapsUiState
import co.georefer.app.orientation.HeadingUiState
import co.georefer.app.points.FieldPoint
import co.georefer.app.points.PointCandidate
import co.georefer.app.points.PointsUiState
import co.georefer.app.ui.theme.AlertTerracotta
import co.georefer.app.ui.theme.FieldGold
import co.georefer.app.ui.theme.ForestPrimary
import co.georefer.app.ui.theme.GpsBlue
import co.georefer.app.ui.theme.Moss
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun MapScreen(
    gpsState: GpsUiState,
    geoPdfState: GeoPdfUiState,
    offlineMapsState: OfflineMapsUiState,
    pointsState: PointsUiState,
    headingState: HeadingUiState,
    northUpLocked: Boolean,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDownloadMap: () -> Unit,
    onImportPdf: () -> Unit,
    onBeginPoint: (Double, Double) -> Unit,
    onCancelPendingPoint: () -> Unit,
    onSavePendingPoint: (String, String) -> Unit,
    focusPoint: FieldPoint?,
    focusPointRequest: Int,
    onUpdatePoint: (Long, String, String) -> Unit,
) {
    val activeOfflineMap = offlineMapsState.activeOfflineMap
        ?.takeIf { it.state == OfflineMapDownloadState.COMPLETE }
    if (activeOfflineMap != null) {
        OfflineVectorMapView(
            map = activeOfflineMap,
            gpsState = gpsState,
            pointsState = pointsState,
            headingState = headingState,
            northUpLocked = northUpLocked,
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
        return
    }

    val activeMap = geoPdfState.map
    val shouldShowGeoPdf = offlineMapsState.isGeoPdfActive || offlineMapsState.activeMapKey == null
    if (shouldShowGeoPdf && geoPdfState.status == GeoPdfStatus.READY && activeMap != null) {
        GeoPdfMapView(
            map = activeMap,
            gpsState = gpsState,
            pointsState = pointsState,
            headingState = headingState,
            northUpLocked = northUpLocked,
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
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyMapCard(
                state = geoPdfState,
                onDownloadMap = onDownloadMap,
                onImportPdf = onImportPdf,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            GpsStatusCard(
                state = gpsState,
                onRequestLocation = onRequestLocation,
                onOpenLocationSettings = onOpenLocationSettings,
                onOpenAppSettings = onOpenAppSettings,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyMapCard(
    state: GeoPdfUiState,
    onDownloadMap: () -> Unit,
    onImportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = if (state.status == GeoPdfStatus.ERROR) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ) {
                if (state.status == GeoPdfStatus.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(13.dp).size(26.dp),
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        if (state.status == GeoPdfStatus.ERROR) Icons.Outlined.WarningAmber
                        else Icons.Outlined.Map,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = if (state.status == GeoPdfStatus.ERROR) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when (state.status) {
                    GeoPdfStatus.LOADING -> "Preparando GeoPDF"
                    GeoPdfStatus.ERROR -> "No se pudo abrir el mapa"
                    else -> "Aún no tienes mapas"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                when (state.status) {
                    GeoPdfStatus.LOADING -> "La app está renderizando la página y leyendo sus coordenadas."
                    GeoPdfStatus.ERROR -> state.errorMessage ?: "El archivo no es un GeoPDF compatible."
                    else -> "Elige una zona o carga un GeoPDF georreferenciado para comenzar."
                },
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state.status == GeoPdfStatus.LOADING) return@Column
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = onDownloadMap,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Descargar mapa")
            }
            OutlinedButton(
                onClick = onImportPdf,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importar GeoPDF")
            }
        }
    }
}

@Composable
private fun GeoPdfMapView(
    map: GeoPdfMap,
    gpsState: GpsUiState,
    pointsState: PointsUiState,
    headingState: HeadingUiState,
    northUpLocked: Boolean,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBeginPoint: (Double, Double) -> Unit,
    onCancelPendingPoint: () -> Unit,
    onSavePendingPoint: (String, String) -> Unit,
    focusPoint: FieldPoint?,
    focusPointRequest: Int,
    onUpdatePoint: (Long, String, String) -> Unit,
) {
    var scale by remember(map) { mutableFloatStateOf(1f) }
    var translation by remember(map) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(map) { mutableStateOf(IntSize.Zero) }
    var locateRequest by remember(map) { mutableIntStateOf(0) }
    var handledLocateRequest by remember(map) { mutableIntStateOf(0) }
    var rotation by remember(map) { mutableFloatStateOf(0f) }
    var locationMode by remember(map) { mutableIntStateOf(0) }
    var handledFocusPointRequest by remember(map) { mutableIntStateOf(0) }
    var selectedPoint by remember(map) { mutableStateOf<FieldPoint?>(null) }
    val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 6f)
        scale = nextScale
        translation = if (nextScale == 1f) Offset.Zero else translation + panChange
        if (!northUpLocked) rotation = normalizeSignedAngle(rotation + rotationChange)
        if (panChange.x != 0f || panChange.y != 0f || abs(rotationChange) > 0.05f) {
            locationMode = 0
        }
    }
    val pagePosition = remember(map.reference, gpsState.fix) {
        gpsState.fix?.let { fix ->
            map.reference.pagePosition(fix.latitude, fix.longitude)
        }
    }
    val savedPointPositions = remember(map.reference, pointsState.points) {
        pointsState.points.mapNotNull { point ->
            map.reference.pagePosition(point.latitude, point.longitude)
                ?.takeIf { it.insideMap }
                ?.let { point to it }
        }
    }
    val pendingPointPosition = remember(map.reference, pointsState.pendingPoint) {
        pointsState.pendingPoint?.let { point ->
            map.reference.pagePosition(point.latitude, point.longitude)?.takeIf { it.insideMap }
        }
    }
    val focusPagePosition = remember(map.reference, focusPoint) {
        focusPoint?.let { point ->
            map.reference.pagePosition(point.latitude, point.longitude)?.takeIf { it.insideMap }
        }
    }

    LaunchedEffect(northUpLocked) {
        if (northUpLocked) {
            rotation = 0f
            locationMode = locationMode.coerceAtMost(1)
        }
    }

    LaunchedEffect(locationMode, headingState.trueHeadingDegrees) {
        val heading = headingState.trueHeadingDegrees
        if (locationMode == 2 && heading != null && !northUpLocked) {
            rotation = normalizeSignedAngle(-heading)
        }
    }

    LaunchedEffect(
        locateRequest,
        pagePosition,
        viewportSize,
        locationMode,
        headingState.trueHeadingDegrees,
    ) {
        val position = pagePosition?.takeIf { it.insideMap } ?: return@LaunchedEffect
        if (locateRequest <= handledLocateRequest || viewportSize == IntSize.Zero) return@LaunchedEffect

        val width = viewportSize.width.toFloat()
        val height = viewportSize.height.toFloat()
        val imageAspect = map.bitmap.width.toFloat() / map.bitmap.height.toFloat()
        val viewportAspect = width / height
        val imageWidth: Float
        val imageHeight: Float
        if (imageAspect > viewportAspect) {
            imageWidth = width
            imageHeight = imageWidth / imageAspect
        } else {
            imageHeight = height
            imageWidth = imageHeight * imageAspect
        }
        val point = Offset(
            x = (width - imageWidth) / 2f + position.xFraction * imageWidth,
            y = (height - imageHeight) / 2f + position.yFraction * imageHeight,
        )
        val targetScale = 3f
        val targetRotation = if (
            locationMode == 2 && headingState.trueHeadingDegrees != null && !northUpLocked
        ) {
            normalizeSignedAngle(-headingState.trueHeadingDegrees)
        } else {
            rotation
        }
        val centerTranslation = Offset(
            x = (width / 2f - point.x) * targetScale,
            y = (height / 2f - point.y) * targetScale,
        )
        scale = targetScale
        rotation = targetRotation
        translation = rotateOffset(centerTranslation, targetRotation)
        handledLocateRequest = locateRequest
    }

    LaunchedEffect(focusPointRequest, focusPagePosition, viewportSize) {
        val position = focusPagePosition ?: return@LaunchedEffect
        if (focusPointRequest <= handledFocusPointRequest || viewportSize == IntSize.Zero) return@LaunchedEffect
        val centered = centerGeoPdfPosition(map, position, viewportSize, targetScale = 3f)
        scale = centered.scale
        rotation = 0f
        translation = centered.translation
        locationMode = 0
        handledFocusPointRequest = focusPointRequest
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .clipToBounds()
            .background(Color(0xFFDDE5DD))
            .pointerInput(map, scale, translation, rotation, viewportSize) {
                detectTapGestures(
                    onTap = { touch ->
                        geoPdfPointAtTouch(
                            map = map,
                            touch = touch,
                            viewportSize = viewportSize,
                            scale = scale,
                            translation = translation,
                            rotationDegrees = rotation,
                            points = savedPointPositions,
                        )?.let { selectedPoint = it }
                    },
                    onLongPress = { touch ->
                        geoPdfCoordinateAtTouch(
                            map = map,
                            touch = touch,
                            viewportSize = viewportSize,
                            scale = scale,
                            translation = translation,
                            rotationDegrees = rotation,
                        )?.let { coordinate ->
                            onBeginPoint(coordinate.latitude, coordinate.longitude)
                        }
                    },
                )
            }
            .transformable(transformState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = translation.x
                    translationY = translation.y
                    rotationZ = rotation
                },
        ) {
            Image(
                bitmap = map.bitmap.asImageBitmap(),
                contentDescription = "Mapa ${map.displayName}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val imageAspect = map.bitmap.width.toFloat() / map.bitmap.height.toFloat()
                val availableAspect = size.width / size.height
                val imageWidth: Float
                val imageHeight: Float
                if (imageAspect > availableAspect) {
                    imageWidth = size.width
                    imageHeight = imageWidth / imageAspect
                } else {
                    imageHeight = size.height
                    imageWidth = imageHeight * imageAspect
                }
                val left = (size.width - imageWidth) / 2f
                val top = (size.height - imageHeight) / 2f
                pagePosition?.takeIf { it.insideMap }?.let { position ->
                    val center = Offset(
                        x = left + position.xFraction * imageWidth,
                        y = top + position.yFraction * imageHeight,
                    )
                    gpsState.fix?.let { fix ->
                        val accuracy = fix.accuracyMeters.toDouble()
                        if (accuracy.isFinite() && accuracy > 0.0) {
                            val latitudeDelta = accuracy / METERS_PER_LATITUDE_DEGREE
                            val longitudeScale = cos(Math.toRadians(fix.latitude)).coerceAtLeast(0.01)
                            val longitudeDelta = accuracy / (METERS_PER_LATITUDE_DEGREE * longitudeScale)
                            val northPosition = map.reference.pagePosition(
                                fix.latitude + latitudeDelta,
                                fix.longitude,
                            )
                            val eastPosition = map.reference.pagePosition(
                                fix.latitude,
                                fix.longitude + longitudeDelta,
                            )
                            if (northPosition != null && eastPosition != null) {
                                drawGpsAccuracyEllipse(
                                    center = center,
                                    radiusX = abs(eastPosition.xFraction - position.xFraction) * imageWidth,
                                    radiusY = abs(northPosition.yFraction - position.yFraction) * imageHeight,
                                )
                            }
                        }
                    }
                    drawGpsDirectionIndicator(
                        center = center,
                        headingDegrees = headingState.trueHeadingDegrees,
                    )
                }
                savedPointPositions.forEach { (_, position) ->
                    drawFieldPointMarker(
                        tip = Offset(
                            x = left + position.xFraction * imageWidth,
                            y = top + position.yFraction * imageHeight,
                        ),
                        pending = false,
                    )
                }
                pendingPointPosition?.let { position ->
                    drawFieldPointMarker(
                        tip = Offset(
                            x = left + position.xFraction * imageWidth,
                            y = top + position.yFraction * imageHeight,
                        ),
                        pending = true,
                    )
                }
            }
        }

        if (pagePosition != null && !pagePosition.insideMap) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 18.dp, end = 18.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.96f),
            ) {
                Text(
                    "Tu ubicación está fuera de los límites de este mapa.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        SmallFloatingActionButton(
            onClick = {
                runLocateAction(
                    state = gpsState,
                    onRequestLocation = onRequestLocation,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onLocate = {
                        locationMode = if (
                            locationMode == 1 &&
                            headingState.trueHeadingDegrees != null &&
                            !northUpLocked
                        ) 2 else 1
                        locateRequest += 1
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                if (locationMode == 2) Icons.Outlined.Navigation else Icons.Outlined.MyLocation,
                contentDescription = if (locationMode == 2) {
                    "Mapa orientado según el teléfono"
                } else {
                    "Buscar mi ubicación en el mapa"
                },
            )
        }

        if (!northUpLocked && abs(rotation) > 0.5f) {
            SmallFloatingActionButton(
                onClick = {
                    rotation = 0f
                    locationMode = 0
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.Explore, contentDescription = "Restablecer norte arriba")
            }
        }

        GpsStatusCard(
            state = gpsState,
            onRequestLocation = onRequestLocation,
            onOpenLocationSettings = onOpenLocationSettings,
            onOpenAppSettings = onOpenAppSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .widthIn(max = 720.dp),
        )
    }
    PointSaveDialog(
        candidate = pointsState.pendingPoint,
        nextPointNumber = pointsState.points.size + 1,
        onCancel = onCancelPendingPoint,
        onSave = onSavePendingPoint,
    )
    PointEditDialog(
        point = selectedPoint,
        onDismiss = { selectedPoint = null },
        onSave = onUpdatePoint,
    )
}

@Composable
private fun OfflineVectorMapView(
    map: OfflineMapEntry,
    gpsState: GpsUiState,
    pointsState: PointsUiState,
    headingState: HeadingUiState,
    northUpLocked: Boolean,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBeginPoint: (Double, Double) -> Unit,
    onCancelPendingPoint: () -> Unit,
    onSavePendingPoint: (String, String) -> Unit,
    focusPoint: FieldPoint?,
    focusPointRequest: Int,
    onUpdatePoint: (Long, String, String) -> Unit,
) {
    var locateRequest by remember(map.regionId) { mutableIntStateOf(0) }
    var locationMode by remember(map.regionId) { mutableIntStateOf(0) }
    var selectedPoint by remember(map.regionId) { mutableStateOf<FieldPoint?>(null) }

    LaunchedEffect(northUpLocked) {
        if (northUpLocked) locationMode = locationMode.coerceAtMost(1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibreMapView(
            styleUrl = map.style.styleUrl,
            initialArea = map.area,
            gpsFix = gpsState.fix?.takeIf { gpsState.availability == GpsAvailability.ACTIVE },
            headingDegrees = headingState.trueHeadingDegrees,
            northUpLocked = northUpLocked,
            locateRequest = locateRequest,
            locateZoom = map.maxZoom.coerceAtMost(16.0),
            orientToHeading = locationMode == 2,
            fieldPoints = pointsState.points,
            pendingPoint = pointsState.pendingPoint,
            focusPoint = focusPoint,
            focusPointRequest = focusPointRequest,
            onMapLongPress = onBeginPoint,
            onFieldPointClick = { selectedPoint = it },
            onUserGesture = { locationMode = 0 },
            modifier = Modifier.fillMaxSize(),
        )
        if (map.style == OfflineMapStyle.SATELLITE_2025) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.68f),
            ) {
                Text(
                    "EOxCloudless · Sentinel-2 2025 · Uso no comercial",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
        SmallFloatingActionButton(
            onClick = {
                runLocateAction(
                    state = gpsState,
                    onRequestLocation = onRequestLocation,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onLocate = {
                        locationMode = if (
                            locationMode == 1 &&
                            headingState.trueHeadingDegrees != null &&
                            !northUpLocked
                        ) 2 else 1
                        locateRequest += 1
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                if (locationMode == 2) Icons.Outlined.Navigation else Icons.Outlined.MyLocation,
                contentDescription = if (locationMode == 2) {
                    "Mapa orientado según el teléfono"
                } else {
                    "Buscar mi ubicación en el mapa"
                },
            )
        }
        GpsStatusCard(
            state = gpsState,
            onRequestLocation = onRequestLocation,
            onOpenLocationSettings = onOpenLocationSettings,
            onOpenAppSettings = onOpenAppSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .widthIn(max = 520.dp),
        )
    }
    PointSaveDialog(
        candidate = pointsState.pendingPoint,
        nextPointNumber = pointsState.points.size + 1,
        onCancel = onCancelPendingPoint,
        onSave = onSavePendingPoint,
    )
    PointEditDialog(
        point = selectedPoint,
        onDismiss = { selectedPoint = null },
        onSave = onUpdatePoint,
    )
}

private fun geoPdfCoordinateAtTouch(
    map: GeoPdfMap,
    touch: Offset,
    viewportSize: IntSize,
    scale: Float,
    translation: Offset,
    rotationDegrees: Float,
): PointCandidate? {
    if (viewportSize == IntSize.Zero || scale <= 0f) return null
    val viewportCenter = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val translatedTouch = Offset(
        x = touch.x - viewportCenter.x - translation.x,
        y = touch.y - viewportCenter.y - translation.y,
    )
    val unrotatedTouch = rotateOffset(translatedTouch, -rotationDegrees)
    val contentTouch = Offset(
        x = unrotatedTouch.x / scale + viewportCenter.x,
        y = unrotatedTouch.y / scale + viewportCenter.y,
    )
    val width = viewportSize.width.toFloat()
    val height = viewportSize.height.toFloat()
    val imageAspect = map.bitmap.width.toFloat() / map.bitmap.height.toFloat()
    val viewportAspect = width / height
    val imageWidth: Float
    val imageHeight: Float
    if (imageAspect > viewportAspect) {
        imageWidth = width
        imageHeight = imageWidth / imageAspect
    } else {
        imageHeight = height
        imageWidth = imageHeight * imageAspect
    }
    val left = (width - imageWidth) / 2f
    val top = (height - imageHeight) / 2f
    val xFraction = (contentTouch.x - left) / imageWidth
    val yFraction = (contentTouch.y - top) / imageHeight
    if (xFraction !in 0f..1f || yFraction !in 0f..1f) return null
    val coordinate = map.reference.coordinateAtPagePosition(xFraction, yFraction) ?: return null
    return PointCandidate(coordinate.latitude, coordinate.longitude)
}

private fun geoPdfPointAtTouch(
    map: GeoPdfMap,
    touch: Offset,
    viewportSize: IntSize,
    scale: Float,
    translation: Offset,
    rotationDegrees: Float,
    points: List<Pair<FieldPoint, GeoPdfPagePosition>>,
): FieldPoint? {
    if (viewportSize == IntSize.Zero || scale <= 0f) return null
    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val translated = Offset(
        x = touch.x - center.x - translation.x,
        y = touch.y - center.y - translation.y,
    )
    val unrotated = rotateOffset(translated, -rotationDegrees)
    val contentTouch = Offset(
        x = unrotated.x / scale + center.x,
        y = unrotated.y / scale + center.y,
    )
    val width = viewportSize.width.toFloat()
    val height = viewportSize.height.toFloat()
    val imageAspect = map.bitmap.width.toFloat() / map.bitmap.height.toFloat()
    val imageWidth: Float
    val imageHeight: Float
    if (imageAspect > width / height) {
        imageWidth = width
        imageHeight = imageWidth / imageAspect
    } else {
        imageHeight = height
        imageWidth = imageHeight * imageAspect
    }
    val left = (width - imageWidth) / 2f
    val top = (height - imageHeight) / 2f
    val hitRadius = 32f / scale
    return points.minByOrNull { (_, position) ->
        hypot(
            contentTouch.x - (left + position.xFraction * imageWidth),
            contentTouch.y - (top + position.yFraction * imageHeight),
        )
    }?.takeIf { (_, position) ->
        hypot(
            contentTouch.x - (left + position.xFraction * imageWidth),
            contentTouch.y - (top + position.yFraction * imageHeight),
        ) <= hitRadius
    }?.first
}

private data class GeoPdfCenterTransform(
    val scale: Float,
    val translation: Offset,
)

private fun centerGeoPdfPosition(
    map: GeoPdfMap,
    position: GeoPdfPagePosition,
    viewportSize: IntSize,
    targetScale: Float,
): GeoPdfCenterTransform {
    val width = viewportSize.width.toFloat()
    val height = viewportSize.height.toFloat()
    val imageAspect = map.bitmap.width.toFloat() / map.bitmap.height.toFloat()
    val imageWidth: Float
    val imageHeight: Float
    if (imageAspect > width / height) {
        imageWidth = width
        imageHeight = imageWidth / imageAspect
    } else {
        imageHeight = height
        imageWidth = imageHeight * imageAspect
    }
    val point = Offset(
        x = (width - imageWidth) / 2f + position.xFraction * imageWidth,
        y = (height - imageHeight) / 2f + position.yFraction * imageHeight,
    )
    return GeoPdfCenterTransform(
        scale = targetScale,
        translation = Offset(
            x = (width / 2f - point.x) * targetScale,
            y = (height / 2f - point.y) * targetScale,
        ),
    )
}

private fun DrawScope.drawGpsDirectionIndicator(
    center: Offset,
    headingDegrees: Float?,
) {
    if (headingDegrees != null) {
        rotate(degrees = headingDegrees, pivot = center) {
            val cone = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x - 7f, center.y - 20f)
                lineTo(center.x + 7f, center.y - 20f)
                close()
            }
            drawPath(cone, color = GpsBlue.copy(alpha = 0.18f))

            val arrow = Path().apply {
                moveTo(center.x, center.y - 18f)
                lineTo(center.x + 4.2f, center.y - 7f)
                lineTo(center.x, center.y - 9.5f)
                lineTo(center.x - 4.2f, center.y - 7f)
                close()
            }
            drawPath(arrow, color = Color.White, style = Stroke(width = 2.2f))
            drawPath(arrow, color = GpsBlue)
        }
    }
    drawCircle(GpsBlue.copy(alpha = 0.15f), radius = 8f, center = center)
    drawCircle(Color.White, radius = 4.5f, center = center)
    drawCircle(GpsBlue, radius = 3f, center = center)
}

private fun DrawScope.drawGpsAccuracyEllipse(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
) {
    if (!radiusX.isFinite() || !radiusY.isFinite() || radiusX <= 0f || radiusY <= 0f) return
    val safeRadiusX = radiusX.coerceAtMost(size.width * 4f)
    val safeRadiusY = radiusY.coerceAtMost(size.height * 4f)
    val topLeft = Offset(center.x - safeRadiusX, center.y - safeRadiusY)
    val ellipseSize = Size(safeRadiusX * 2f, safeRadiusY * 2f)
    drawOval(
        color = GpsBlue.copy(alpha = 0.11f),
        topLeft = topLeft,
        size = ellipseSize,
    )
    drawOval(
        color = GpsBlue.copy(alpha = 0.5f),
        topLeft = topLeft,
        size = ellipseSize,
        style = Stroke(width = 1.2f),
    )
}

private fun rotateOffset(offset: Offset, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    val cosine = cos(radians)
    val sine = sin(radians)
    return Offset(
        x = offset.x * cosine - offset.y * sine,
        y = offset.x * sine + offset.y * cosine,
    )
}

private fun normalizeSignedAngle(degrees: Float): Float =
    ((degrees + 540f) % 360f) - 180f

private const val METERS_PER_LATITUDE_DEGREE = 111_320.0

private fun DrawScope.drawFieldPointMarker(tip: Offset, pending: Boolean) {
    val width = if (pending) 11f else 9f
    val height = if (pending) 18f else 15f
    val centerY = tip.y - height * 0.62f
    val marker = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(
            tip.x - width * 0.25f,
            tip.y - height * 0.23f,
            tip.x - width,
            tip.y - height * 0.52f,
            tip.x - width,
            centerY,
        )
        cubicTo(
            tip.x - width,
            centerY - width,
            tip.x + width,
            centerY - width,
            tip.x + width,
            centerY,
        )
        cubicTo(
            tip.x + width,
            tip.y - height * 0.52f,
            tip.x + width * 0.25f,
            tip.y - height * 0.23f,
            tip.x,
            tip.y,
        )
        close()
    }
    val fill = if (pending) Color(0xFFF2B84B) else FieldGold
    drawPath(marker, color = fill)
    drawPath(marker, color = Color.White, style = Stroke(width = 1.5f))
    drawCircle(
        color = ForestPrimary,
        radius = if (pending) 3.2f else 2.7f,
        center = Offset(tip.x, centerY),
    )
}

@Composable
private fun PointSaveDialog(
    candidate: PointCandidate?,
    nextPointNumber: Int,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    if (candidate == null) return
    var name by remember(candidate) { mutableStateOf("Punto $nextPointNumber") }
    var note by remember(candidate) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.Place, contentDescription = null) },
        title = { Text("Guardar punto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        candidate.latitude,
                        candidate.longitude,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota opcional") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, note) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
    )
}

@Composable
private fun PointEditDialog(
    point: FieldPoint?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String) -> Unit,
) {
    if (point == null) return
    var name by remember(point.id) { mutableStateOf(point.name) }
    var note by remember(point.id) { mutableStateOf(point.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Place, contentDescription = null) },
        title = { Text(point.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    String.format(Locale.US, "%.6f, %.6f", point.latitude, point.longitude),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(point.id, name, note)
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

private fun runLocateAction(
    state: GpsUiState,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onLocate: () -> Unit,
) {
    when (state.availability) {
        GpsAvailability.NEEDS_PERMISSION -> onRequestLocation()
        GpsAvailability.APPROXIMATE_ONLY,
        GpsAvailability.ERROR,
        -> onOpenAppSettings()
        GpsAvailability.LOCATION_DISABLED -> onOpenLocationSettings()
        GpsAvailability.ACTIVE -> onLocate()
    }
}

@Composable
private fun GpsStatusCard(
    state: GpsUiState,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shadowElevation = 1.dp,
    ) {
        when (state.availability) {
            GpsAvailability.NEEDS_PERMISSION -> CompactGpsAction(
                icon = Icons.Outlined.GpsNotFixed,
                title = "Ubicación desactivada",
                body = "Activa el GPS preciso para posicionarte.",
                actionLabel = "Activar",
                onAction = onRequestLocation,
            )

            GpsAvailability.APPROXIMATE_ONLY -> CompactGpsAction(
                icon = Icons.Outlined.WarningAmber,
                title = "GPS aproximado",
                body = "Autoriza precisión para trabajar en campo.",
                actionLabel = "Ajustes",
                onAction = onOpenAppSettings,
            )

            GpsAvailability.LOCATION_DISABLED -> CompactGpsAction(
                icon = Icons.Outlined.LocationDisabled,
                title = "GPS desactivado",
                body = "Enciende la ubicación del teléfono.",
                actionLabel = "Encender",
                onAction = onOpenLocationSettings,
            )

            GpsAvailability.ERROR -> CompactGpsAction(
                icon = Icons.Outlined.WarningAmber,
                title = "Error de GPS",
                body = state.errorMessage ?: "Revisa el permiso de ubicación.",
                actionLabel = "Ajustes",
                onAction = onOpenAppSettings,
            )

            GpsAvailability.ACTIVE -> CompactActiveGpsStatus(state)
        }
    }
}

@Composable
private fun CompactGpsAction(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                body,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun CompactActiveGpsStatus(state: GpsUiState) {
    val satelliteText = "${state.satellitesUsed}/${state.satellitesVisible} Satélites"
    Row(
        modifier = Modifier
            .semantics {
                contentDescription =
                    "${state.satellitesUsed} de ${state.satellitesVisible} satélites"
            }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Outlined.SatelliteAlt,
                contentDescription = null,
                modifier = Modifier.padding(7.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = satelliteText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                state.fix?.let {
                    String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude)
                } ?: "Buscando coordenadas…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            state.fix?.let { "±${formatAccuracy(it.accuracyMeters)} m" } ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PermissionMessage(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
                }
            }
        }
    }
}

@Composable
private fun ActiveGpsStatus(state: GpsUiState) {
    val presentation = qualityPresentation(state.quality)
    val description = buildString {
        append("Calidad GPS ${presentation.label}")
        state.fix?.let { append(", precisión más o menos ${it.accuracyMeters.toInt()} metros") }
    }

    Column(
        modifier = Modifier
            .semantics { contentDescription = description }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = presentation.color.copy(alpha = 0.14f)) {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = presentation.color,
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = presentation.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = presentation.color,
                )
                Text(
                    text = presentation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.fix?.let { "± ${formatAccuracy(it.accuracyMeters)} m" } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = presentation.color,
                )
                Text(
                    text = "precisión estimada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        QualityScale(current = state.quality)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.fix?.let {
                            String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude)
                        } ?: "Buscando coordenadas…",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = buildString {
                            append("WGS 84 · GPS interno")
                            if (state.satellitesVisible > 0) {
                                append(" · ${state.satellitesUsed}/${state.satellitesVisible} satélites")
                            }
                            state.readingAgeSeconds?.takeIf { it > 1 }?.let { append(" · hace ${it}s") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Outlined.SatelliteAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun QualityScale(current: GpsQuality) {
    val qualities = listOf(
        GpsQuality.SEARCHING,
        GpsQuality.LOW,
        GpsQuality.ACCEPTABLE,
        GpsQuality.GOOD,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        qualities.forEach { quality ->
            val active = quality == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (active) 6.dp else 4.dp)
                    .background(
                        color = if (active) qualityPresentation(quality).color
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

private data class QualityPresentation(
    val label: String,
    val description: String,
    val color: Color,
    val icon: ImageVector,
)

private fun qualityPresentation(quality: GpsQuality): QualityPresentation = when (quality) {
    GpsQuality.SEARCHING -> QualityPresentation(
        label = "Buscando señal",
        description = "Espera una lectura reciente del GPS.",
        color = Moss,
        icon = Icons.Outlined.LocationSearching,
    )

    GpsQuality.LOW -> QualityPresentation(
        label = "Calidad baja",
        description = "Busca cielo abierto antes de confiar en la posición.",
        color = AlertTerracotta,
        icon = Icons.Outlined.WarningAmber,
    )

    GpsQuality.ACCEPTABLE -> QualityPresentation(
        label = "Calidad aceptable",
        description = "Útil para orientación general en campo.",
        color = FieldGold,
        icon = Icons.Outlined.GpsFixed,
    )

    GpsQuality.GOOD -> QualityPresentation(
        label = "Calidad buena",
        description = "La señal está estable para orientación.",
        color = ForestPrimary,
        icon = Icons.Outlined.CheckCircle,
    )
}

private fun formatAccuracy(value: Float): String =
    if (value < 10f) String.format(Locale.forLanguageTag("es-CO"), "%.1f", value)
    else value.toInt().toString()
