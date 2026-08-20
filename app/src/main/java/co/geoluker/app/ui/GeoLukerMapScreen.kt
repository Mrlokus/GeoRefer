package co.geoluker.app.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import co.geoluker.app.location.GpsAvailability
import co.geoluker.app.location.GpsUiState
import co.geoluker.app.map.GeoPdfMap
import co.geoluker.app.map.GeoPdfPagePosition
import co.geoluker.app.map.GeoPdfStatus
import co.geoluker.app.map.GeoPdfTile
import co.geoluker.app.map.GeoPdfTileKey
import co.geoluker.app.map.GeoPdfTileRenderer
import co.geoluker.app.map.BoundaryStatusStabilizer
import co.geoluker.app.map.GeoPdfUiState
import co.geoluker.app.map.LotCatalog
import co.geoluker.app.map.LotLocation
import co.geoluker.app.orientation.HeadingUiState
import co.geoluker.app.orientation.HeadingSource
import co.geoluker.app.points.FieldPoint
import co.geoluker.app.points.PointCandidate
import co.geoluker.app.points.PointsUiState
import co.geoluker.app.ui.theme.GpsBlue
import co.geoluker.app.ui.theme.LukerBrown
import co.geoluker.app.ui.theme.LotRed
import co.geoluker.app.ui.theme.PointGold
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GeoLukerMapScreen(
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
    focusPoint: FieldPoint?,
    focusPointRequest: Int,
    onUpdatePoint: (Long, String, String) -> Unit,
) {
    when (mapState.status) {
        GeoPdfStatus.READY -> mapState.map?.let { map ->
            GeoLukerMapView(
                map = map,
                gpsState = gpsState,
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
        }

        GeoPdfStatus.ERROR -> MapLoadState(
            icon = Icons.Outlined.WarningAmber,
            title = "No se pudo abrir el mapa oficial",
            body = mapState.errorMessage ?: "Verifica que el recurso de Luker Agrícola esté incluido.",
            loading = false,
        )

        else -> MapLoadState(
            icon = Icons.Outlined.Map,
            title = "Preparando Luker Agrícola",
            body = "Optimizando el mapa para conservar la legibilidad al ampliar.",
            loading = true,
        )
    }
}

@Composable
private fun MapLoadState(
    icon: ImageVector,
    title: String,
    body: String,
    loading: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.error)
        }
        Text(title, modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GeoLukerMapView(
    map: GeoPdfMap,
    gpsState: GpsUiState,
    pointsState: PointsUiState,
    headingState: HeadingUiState,
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lots = remember { LotCatalog.load(context) }
    var scale by remember(map) { mutableFloatStateOf(1f) }
    var translation by remember(map) { mutableStateOf(Offset.Zero) }
    var rotation by remember(map) { mutableFloatStateOf(0f) }
    var viewportSize by remember(map) { mutableStateOf(IntSize.Zero) }
    var locateRequest by remember(map) { mutableIntStateOf(0) }
    var handledFocusPointRequest by remember(map) { mutableIntStateOf(0) }
    var locationMode by remember(map) { mutableIntStateOf(0) }
    var autoLocationHandled by remember(map) { mutableStateOf(false) }
    var selectedPoint by remember(map) { mutableStateOf<FieldPoint?>(null) }
    var selectedLot by remember(map) { mutableStateOf<LotLocation?>(null) }
    var lotFocusRequest by remember(map) { mutableIntStateOf(0) }
    var handledLotFocusRequest by remember(map) { mutableIntStateOf(0) }
    var showLotSearch by remember { mutableStateOf(false) }
    var clearlyOutside by remember(map) { mutableStateOf(false) }
    val boundaryStabilizer = remember(map) { BoundaryStatusStabilizer() }
    val renderedTiles = remember(map) { mutableStateMapOf<GeoPdfTileKey, GeoPdfTile>() }

    val gpsPagePosition = remember(map.reference, gpsState.fix) {
        gpsState.fix?.let { map.reference.pagePosition(it.latitude, it.longitude) }
    }
    LaunchedEffect(map.reference, gpsState.fix) {
        val fix = gpsState.fix
        if (fix == null) {
            boundaryStabilizer.reset()
            clearlyOutside = false
        } else {
            clearlyOutside = boundaryStabilizer.update(
                isClearlyOutside = !map.reference.isInsideOrNear(
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.accuracyMeters,
                ),
            )
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
        pointsState.pendingPoint?.let {
            map.reference.pagePosition(it.latitude, it.longitude)?.takeIf { position -> position.insideMap }
        }
    }
    val focusPagePosition = remember(map.reference, focusPoint) {
        focusPoint?.let {
            map.reference.pagePosition(it.latitude, it.longitude)?.takeIf { position -> position.insideMap }
        }
    }

    val tileLongEdge = remember(map, viewportSize, scale) {
        selectTileLongEdge(map, viewportSize, scale)
    }
    LaunchedEffect(map, viewportSize, scale, translation, rotation, tileLongEdge) {
        delay(TILE_RENDER_DEBOUNCE_MS)
        val previewLongEdge = max(map.bitmap.width, map.bitmap.height)
        val desired = if (tileLongEdge <= previewLongEdge) emptyList() else visibleTileKeys(
            map = map,
            viewportSize = viewportSize,
            scale = scale,
            translation = translation,
            rotationDegrees = rotation,
            longEdgePixels = tileLongEdge,
        )
        renderedTiles.keys.toList().filterNot(desired::contains).forEach(renderedTiles::remove)
        desired.filterNot(renderedTiles::containsKey).forEach { key ->
            runCatching { map.tileRenderer.render(key) }
                .onSuccess { tile -> renderedTiles[key] = tile }
        }
    }

    LaunchedEffect(gpsPagePosition, viewportSize) {
        if (!autoLocationHandled && gpsPagePosition?.insideMap == true && viewportSize != IntSize.Zero) {
            locationMode = 1
            locateRequest += 1
            autoLocationHandled = true
        }
    }

    LaunchedEffect(gpsPagePosition, viewportSize, locationMode, locateRequest) {
        val position = gpsPagePosition?.takeIf { it.insideMap } ?: return@LaunchedEffect
        if (locationMode == 0 || viewportSize == IntSize.Zero) return@LaunchedEffect
        val targetRotation = if (locationMode == 2 && headingState.trueHeadingDegrees != null) {
            normalizeSignedAngle(-headingState.trueHeadingDegrees)
        } else {
            rotation
        }
        val targetScale = max(scale, LOCATION_ZOOM)
        val centered = centerGeoPdfPosition(map, position, viewportSize, targetScale, targetRotation)
        val target = MapTransform(
            scale = centered.scale,
            rotation = targetRotation,
            translation = constrainTranslation(
                map,
                viewportSize,
                centered.scale,
                targetRotation,
                centered.translation,
            ),
        )
        if (locationMode == 2) {
            scale = target.scale
            translation = target.translation
            rotation = target.rotation
            return@LaunchedEffect
        }
        animateMapTransform(MapTransform(scale, translation, rotation), target, FOLLOW_ANIMATION_MS) {
            scale = it.scale
            translation = it.translation
            rotation = it.rotation
        }
    }

    LaunchedEffect(locationMode, headingState.trueHeadingDegrees, gpsPagePosition, viewportSize) {
        if (locationMode != 2 || viewportSize == IntSize.Zero) return@LaunchedEffect
        val position = gpsPagePosition?.takeIf { it.insideMap }
        if (position == null) {
            locationMode = 1
            return@LaunchedEffect
        }
        val heading = headingState.trueHeadingDegrees
        if (heading == null) {
            val centered = centerGeoPdfPosition(map, position, viewportSize, scale, 0f)
            val target = MapTransform(
                scale = centered.scale,
                translation = constrainTranslation(
                    map,
                    viewportSize,
                    centered.scale,
                    0f,
                    centered.translation,
                ),
                rotation = 0f,
            )
            animateMapTransform(
                from = MapTransform(scale, translation, rotation),
                to = target,
                durationMillis = CAMERA_ANIMATION_MS,
            ) {
                scale = it.scale
                translation = it.translation
                rotation = it.rotation
            }
            locationMode = 1
            return@LaunchedEffect
        }
        val targetRotation = normalizeSignedAngle(-heading)
        val centered = centerGeoPdfPosition(map, position, viewportSize, scale, targetRotation)
        rotation = targetRotation
        translation = constrainTranslation(
            map,
            viewportSize,
            scale,
            targetRotation,
            centered.translation,
        )
    }

    LaunchedEffect(focusPointRequest, focusPagePosition, viewportSize) {
        val position = focusPagePosition ?: return@LaunchedEffect
        if (focusPointRequest <= handledFocusPointRequest || viewportSize == IntSize.Zero) return@LaunchedEffect
        val transform = centerGeoPdfPosition(map, position, viewportSize, POINT_ZOOM, 0f)
        val target = MapTransform(
            transform.scale,
            constrainTranslation(map, viewportSize, transform.scale, 0f, transform.translation),
            0f,
        )
        locationMode = 0
        animateMapTransform(MapTransform(scale, translation, rotation), target, CAMERA_ANIMATION_MS) {
            scale = it.scale
            translation = it.translation
            rotation = it.rotation
        }
        handledFocusPointRequest = focusPointRequest
    }

    LaunchedEffect(lotFocusRequest, selectedLot, viewportSize) {
        val lot = selectedLot ?: return@LaunchedEffect
        if (lotFocusRequest <= handledLotFocusRequest || viewportSize == IntSize.Zero) return@LaunchedEffect
        val position = GeoPdfPagePosition(lot.xFraction, lot.yFraction, insideMap = true)
        val transform = centerGeoPdfPosition(map, position, viewportSize, LOT_ZOOM, 0f)
        val target = MapTransform(
            transform.scale,
            constrainTranslation(map, viewportSize, transform.scale, 0f, transform.translation),
            0f,
        )
        locationMode = 0
        animateMapTransform(MapTransform(scale, translation, rotation), target, CAMERA_ANIMATION_MS) {
            scale = it.scale
            translation = it.translation
            rotation = it.rotation
        }
        handledLotFocusRequest = lotFocusRequest
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(map, viewportSize, savedPointPositions) {
                detectTapGestures(
                    onDoubleTap = { touch ->
                        val nextScale = (scale * DOUBLE_TAP_ZOOM_FACTOR).coerceAtMost(MAX_MAP_SCALE)
                        val candidate = translationForZoomAtPoint(
                            focalPoint = touch,
                            viewportSize = viewportSize,
                            translation = translation,
                            oldScale = scale,
                            newScale = nextScale,
                        )
                        translation = constrainTranslation(
                            map,
                            viewportSize,
                            nextScale,
                            rotation,
                            candidate,
                        )
                        scale = nextScale
                        locationMode = 0
                    },
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
                        )?.let { onBeginPoint(it.latitude, it.longitude) }
                    },
                )
            }
            .pointerInput(map, viewportSize) {
                detectTransformGestures { centroid, pan, zoomChange, rotationChange ->
                    val oldScale = scale
                    val nextScale = (oldScale * zoomChange).coerceIn(MIN_MAP_SCALE, MAX_MAP_SCALE)
                    val effectiveZoom = nextScale / oldScale
                    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val focalFromTransformOrigin = centroid - center - translation
                    val transformedFocal = rotateOffset(
                        focalFromTransformOrigin * effectiveZoom,
                        rotationChange,
                    )

                    val nextRotation = normalizeSignedAngle(rotation + rotationChange)
                    val candidate = centroid + pan - center - transformedFocal
                    translation = constrainTranslation(
                        map,
                        viewportSize,
                        nextScale,
                        nextRotation,
                        candidate,
                    )
                    scale = nextScale
                    rotation = nextRotation
                    if (
                        pan != Offset.Zero ||
                        abs(rotationChange) > 0.05f ||
                        abs(effectiveZoom - 1f) > 0.002f
                    ) {
                        locationMode = 0
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = translation.x
                translationY = translation.y
                rotationZ = rotation
            },
        ) {
            Image(
                bitmap = map.bitmap.asImageBitmap(),
                contentDescription = "Mapa oficial de Luker Agrícola",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val layout = imageLayout(map, IntSize(size.width.toInt(), size.height.toInt()))
                renderedTiles.values.forEach { tile ->
                    val left = layout.left + tile.pixelLeft.toFloat() / tile.fullWidthPixels * layout.width
                    val top = layout.top + tile.pixelTop.toFloat() / tile.fullHeightPixels * layout.height
                    val width = tile.bitmap.width.toFloat() / tile.fullWidthPixels * layout.width
                    val height = tile.bitmap.height.toFloat() / tile.fullHeightPixels * layout.height
                    withTransform({
                        translate(left, top)
                        scale(
                            scaleX = width / tile.bitmap.width.toFloat(),
                            scaleY = height / tile.bitmap.height.toFloat(),
                            pivot = Offset.Zero,
                        )
                    }) {
                        drawImage(
                            image = tile.bitmap.asImageBitmap(),
                            topLeft = Offset.Zero,
                        )
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            val layout = imageLayout(map, canvasSize)
            fun screenPosition(position: GeoPdfPagePosition): Offset = transformedViewportPoint(
                point = layout.toOffset(position),
                viewportSize = canvasSize,
                scale = scale,
                translation = translation,
                rotationDegrees = rotation,
            )

            gpsPagePosition?.takeIf { it.insideMap }?.let { position ->
                val center = screenPosition(position)
                gpsState.fix?.let { fix ->
                    rotate(rotation, center) {
                        drawAccuracyOnGeoPdf(
                            map = map,
                            latitude = fix.latitude,
                            longitude = fix.longitude,
                            accuracyMeters = fix.accuracyMeters,
                            position = position,
                            center = center,
                            layout = layout,
                            displayScale = scale,
                        )
                    }
                }
                drawGpsDirectionIndicator(
                    center = center,
                    headingDegrees = headingState.trueHeadingDegrees?.plus(rotation),
                    source = headingState.source,
                )
            }
            savedPointPositions.forEach { (point, position) ->
                val markerPosition = screenPosition(position)
                drawFieldPointMarker(markerPosition, pending = false)
                if (scale >= POINT_LABEL_MIN_SCALE) {
                    drawFieldPointLabel(markerPosition, point.name)
                }
            }
            pendingPointPosition?.let { drawFieldPointMarker(screenPosition(it), pending = true) }
            selectedLot?.let { lot ->
                drawLotIndicator(
                    screenPosition(GeoPdfPagePosition(lot.xFraction, lot.yFraction, true)),
                )
            }
        }

        SmallFloatingActionButton(
            onClick = { showLotSearch = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = "Buscar lote")
        }

        SmallFloatingActionButton(
            onClick = {
                locationMode = 0
                selectedLot = null
                coroutineScope.launch {
                    animateMapTransform(
                        from = MapTransform(scale, translation, rotation),
                        to = MapTransform(1f, Offset.Zero, 0f),
                        durationMillis = CAMERA_ANIMATION_MS,
                    ) {
                        scale = it.scale
                        translation = it.translation
                        rotation = it.rotation
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Outlined.Fullscreen, contentDescription = "Ver mapa completo")
        }

        if (abs(rotation) > 0.5f) {
            SmallFloatingActionButton(
                onClick = {
                    locationMode = 0
                    val targetTranslation = constrainTranslation(
                        map,
                        viewportSize,
                        scale,
                        0f,
                        translation,
                    )
                    coroutineScope.launch {
                        animateMapTransform(
                            from = MapTransform(scale, translation, rotation),
                            to = MapTransform(scale, targetTranslation, 0f),
                            durationMillis = CAMERA_ANIMATION_MS,
                        ) {
                            scale = it.scale
                            translation = it.translation
                            rotation = it.rotation
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 64.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.Explore, contentDescription = "Restablecer norte arriba")
            }
        }

        selectedLot?.let { lot ->
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(start = 13.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Lote ${lot.code}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    IconButton(onClick = { selectedLot = null }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cerrar selección", modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        if (clearlyOutside) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp, start = 70.dp, end = 70.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f),
            ) {
                Text(
                    "Estás fuera de Luker Agricola",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
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
                        locationMode = if (locationMode == 1 && headingState.trueHeadingDegrees != null) 2 else 1
                        locateRequest += 1
                    },
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 84.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                if (locationMode == 2) Icons.Outlined.Navigation else Icons.Outlined.MyLocation,
                contentDescription = when {
                    locationMode != 2 -> "Buscar mi ubicación"
                    headingState.source == HeadingSource.GPS_COURSE -> "Orientado según dirección de marcha"
                    else -> "Orientado con la brújula"
                },
            )
        }

        GpsStatusCard(
            state = gpsState,
            headingState = headingState,
            onRequestLocation = onRequestLocation,
            onOpenLocationSettings = onOpenLocationSettings,
            onOpenAppSettings = onOpenAppSettings,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).widthIn(max = 620.dp),
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
    if (showLotSearch) {
        LotSearchDialog(
            lots = lots,
            onDismiss = { showLotSearch = false },
            onSelect = { lot ->
                selectedLot = lot
                lotFocusRequest += 1
                showLotSearch = false
            },
        )
    }
}

@Composable
private fun LotSearchDialog(
    lots: List<LotLocation>,
    onDismiss: () -> Unit,
    onSelect: (LotLocation) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val normalized = query.trim().uppercase(Locale.ROOT).replace(" ", "")
    val results = remember(normalized, lots) {
        if (normalized.isBlank()) lots.take(16)
        else lots.filter { it.code.contains(normalized) }.take(20)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        title = { Text("Buscar lote") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Código del lote") },
                    placeholder = { Text("Ej. D30, F18R o AP20") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${lots.size} lotes indexados desde el mapa oficial",
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp)) {
                    items(results, key = { it.code }) { lot ->
                        TextButton(
                            onClick = { onSelect(lot) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null)
                            Text(
                                "Lote ${lot.code}",
                                modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    if (results.isEmpty()) {
                        item {
                            Text(
                                "No se encontró un lote con ese código.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

private data class ImageLayout(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    fun toOffset(position: GeoPdfPagePosition): Offset = Offset(
        x = left + position.xFraction * width,
        y = top + position.yFraction * height,
    )
}

private data class MapTransform(
    val scale: Float,
    val translation: Offset,
    val rotation: Float,
)

private suspend fun animateMapTransform(
    from: MapTransform,
    to: MapTransform,
    durationMillis: Int,
    onFrame: (MapTransform) -> Unit,
) {
    val rotationDelta = normalizeSignedAngle(to.rotation - from.rotation)
    animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
    ) { progress, _ ->
        onFrame(
            MapTransform(
                scale = from.scale + (to.scale - from.scale) * progress,
                translation = Offset(
                    from.translation.x + (to.translation.x - from.translation.x) * progress,
                    from.translation.y + (to.translation.y - from.translation.y) * progress,
                ),
                rotation = normalizeSignedAngle(from.rotation + rotationDelta * progress),
            ),
        )
    }
}

private fun imageLayout(map: GeoPdfMap, viewportSize: IntSize): ImageLayout {
    val width = viewportSize.width.toFloat()
    val height = viewportSize.height.toFloat()
    val imageAspect = map.bitmap.width.toFloat() / map.bitmap.height.toFloat()
    val viewportAspect = if (height > 0f) width / height else imageAspect
    val imageWidth: Float
    val imageHeight: Float
    if (imageAspect > viewportAspect) {
        imageWidth = width
        imageHeight = imageWidth / imageAspect
    } else {
        imageHeight = height
        imageWidth = imageHeight * imageAspect
    }
    return ImageLayout(
        left = (width - imageWidth) / 2f,
        top = (height - imageHeight) / 2f,
        width = imageWidth,
        height = imageHeight,
    )
}

private fun constrainTranslation(
    map: GeoPdfMap,
    viewportSize: IntSize,
    scale: Float,
    rotationDegrees: Float,
    candidate: Offset,
): Offset {
    if (viewportSize == IntSize.Zero) return candidate
    val layout = imageLayout(map, viewportSize)
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosine = abs(kotlin.math.cos(radians)).toFloat()
    val sine = abs(kotlin.math.sin(radians)).toFloat()
    val scaledWidth = layout.width * scale
    val scaledHeight = layout.height * scale
    val boundsWidth = scaledWidth * cosine + scaledHeight * sine
    val boundsHeight = scaledWidth * sine + scaledHeight * cosine
    val maxX = max(0f, (boundsWidth - viewportSize.width) / 2f)
    val maxY = max(0f, (boundsHeight - viewportSize.height) / 2f)
    return Offset(
        x = candidate.x.coerceIn(-maxX, maxX),
        y = candidate.y.coerceIn(-maxY, maxY),
    )
}

private fun selectTileLongEdge(map: GeoPdfMap, viewportSize: IntSize, scale: Float): Int {
    if (viewportSize == IntSize.Zero) return GeoPdfTileRenderer.MIN_LONG_EDGE_PX
    val layout = imageLayout(map, viewportSize)
    val requested = max(layout.width, layout.height) * scale * TILE_OVERSAMPLE
    return TILE_LONG_EDGES.firstOrNull { it >= requested }
        ?: GeoPdfTileRenderer.MAX_LONG_EDGE_PX
}

private fun visibleTileKeys(
    map: GeoPdfMap,
    viewportSize: IntSize,
    scale: Float,
    translation: Offset,
    rotationDegrees: Float,
    longEdgePixels: Int,
): List<GeoPdfTileKey> {
    if (viewportSize == IntSize.Zero || scale <= 0f) return emptyList()
    val layout = imageLayout(map, viewportSize)
    val corners = listOf(
        Offset.Zero,
        Offset(viewportSize.width.toFloat(), 0f),
        Offset(0f, viewportSize.height.toFloat()),
        Offset(viewportSize.width.toFloat(), viewportSize.height.toFloat()),
    ).mapNotNull { corner ->
        transformedContentTouch(corner, viewportSize, scale, translation, rotationDegrees)
    }
    if (corners.isEmpty()) return emptyList()

    val minFractionX = ((corners.minOf { it.x } - layout.left) / layout.width).coerceIn(0f, 1f)
    val maxFractionX = ((corners.maxOf { it.x } - layout.left) / layout.width).coerceIn(0f, 1f)
    val minFractionY = ((corners.minOf { it.y } - layout.top) / layout.height).coerceIn(0f, 1f)
    val maxFractionY = ((corners.maxOf { it.y } - layout.top) / layout.height).coerceIn(0f, 1f)
    if (minFractionX >= maxFractionX || minFractionY >= maxFractionY) return emptyList()

    val grid = map.tileRenderer.grid(longEdgePixels)
    val firstColumn = (floor(minFractionX * grid.fullWidthPixels / GeoPdfTileRenderer.TILE_SIZE_PX).toInt() - 1)
        .coerceIn(0, grid.columns - 1)
    val lastColumn = (floor(maxFractionX * grid.fullWidthPixels / GeoPdfTileRenderer.TILE_SIZE_PX).toInt() + 1)
        .coerceIn(0, grid.columns - 1)
    val firstRow = (floor(minFractionY * grid.fullHeightPixels / GeoPdfTileRenderer.TILE_SIZE_PX).toInt() - 1)
        .coerceIn(0, grid.rows - 1)
    val lastRow = (floor(maxFractionY * grid.fullHeightPixels / GeoPdfTileRenderer.TILE_SIZE_PX).toInt() + 1)
        .coerceIn(0, grid.rows - 1)
    val centerColumn = (firstColumn + lastColumn) / 2f
    val centerRow = (firstRow + lastRow) / 2f

    return buildList {
        for (row in firstRow..lastRow) {
            for (column in firstColumn..lastColumn) {
                add(GeoPdfTileKey(grid.longEdgePixels, column, row))
            }
        }
    }.sortedBy { key ->
        val dx = key.column - centerColumn
        val dy = key.row - centerRow
        dx * dx + dy * dy
    }
}

private fun geoPdfCoordinateAtTouch(
    map: GeoPdfMap,
    touch: Offset,
    viewportSize: IntSize,
    scale: Float,
    translation: Offset,
    rotationDegrees: Float,
): PointCandidate? {
    val contentTouch = transformedContentTouch(touch, viewportSize, scale, translation, rotationDegrees) ?: return null
    val layout = imageLayout(map, viewportSize)
    val xFraction = (contentTouch.x - layout.left) / layout.width
    val yFraction = (contentTouch.y - layout.top) / layout.height
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
    val contentTouch = transformedContentTouch(touch, viewportSize, scale, translation, rotationDegrees) ?: return null
    val layout = imageLayout(map, viewportSize)
    val hitRadius = 34f / scale
    return points.minByOrNull { (_, position) ->
        val marker = layout.toOffset(position)
        hypot(contentTouch.x - marker.x, contentTouch.y - marker.y)
    }?.takeIf { (_, position) ->
        val marker = layout.toOffset(position)
        hypot(contentTouch.x - marker.x, contentTouch.y - marker.y) <= hitRadius
    }?.first
}

private fun transformedContentTouch(
    touch: Offset,
    viewportSize: IntSize,
    scale: Float,
    translation: Offset,
    rotationDegrees: Float,
): Offset? {
    if (viewportSize == IntSize.Zero || scale <= 0f) return null
    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val translated = Offset(touch.x - center.x - translation.x, touch.y - center.y - translation.y)
    val unrotated = rotateOffset(translated, -rotationDegrees)
    return Offset(unrotated.x / scale + center.x, unrotated.y / scale + center.y)
}

private fun translationForZoomAtPoint(
    focalPoint: Offset,
    viewportSize: IntSize,
    translation: Offset,
    oldScale: Float,
    newScale: Float,
): Offset {
    if (oldScale <= 0f || viewportSize == IntSize.Zero) return translation
    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val zoomRatio = newScale / oldScale
    return focalPoint - center - (focalPoint - center - translation) * zoomRatio
}

private fun transformedViewportPoint(
    point: Offset,
    viewportSize: IntSize,
    scale: Float,
    translation: Offset,
    rotationDegrees: Float,
): Offset {
    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val relative = Offset((point.x - center.x) * scale, (point.y - center.y) * scale)
    val transformed = rotateOffset(relative, rotationDegrees)
    return center + transformed + translation
}

private data class GeoPdfCenterTransform(val scale: Float, val translation: Offset)

private fun centerGeoPdfPosition(
    map: GeoPdfMap,
    position: GeoPdfPagePosition,
    viewportSize: IntSize,
    targetScale: Float,
    targetRotation: Float,
): GeoPdfCenterTransform {
    val layout = imageLayout(map, viewportSize)
    val point = layout.toOffset(position)
    val centered = Offset(
        x = (viewportSize.width / 2f - point.x) * targetScale,
        y = (viewportSize.height / 2f - point.y) * targetScale,
    )
    return GeoPdfCenterTransform(targetScale, rotateOffset(centered, targetRotation))
}

private fun DrawScope.drawAccuracyOnGeoPdf(
    map: GeoPdfMap,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    position: GeoPdfPagePosition,
    center: Offset,
    layout: ImageLayout,
    displayScale: Float,
) {
    val accuracy = accuracyMeters.toDouble()
    if (!accuracy.isFinite() || accuracy <= 0.0) return
    val latitudeDelta = accuracy / METERS_PER_LATITUDE_DEGREE
    val longitudeScale = cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
    val longitudeDelta = accuracy / (METERS_PER_LATITUDE_DEGREE * longitudeScale)
    val north = map.reference.pagePosition(latitude + latitudeDelta, longitude) ?: return
    val east = map.reference.pagePosition(latitude, longitude + longitudeDelta) ?: return
    drawGpsAccuracyEllipse(
        center = center,
        radiusX = abs(east.xFraction - position.xFraction) * layout.width * displayScale,
        radiusY = abs(north.yFraction - position.yFraction) * layout.height * displayScale,
    )
}

private fun DrawScope.drawGpsDirectionIndicator(
    center: Offset,
    headingDegrees: Float?,
    source: HeadingSource,
) {
    if (headingDegrees != null) {
        val arrowColor = if (source == HeadingSource.GPS_COURSE) PointGold else GpsBlue
        rotate(degrees = headingDegrees, pivot = center) {
            val arrow = Path().apply {
                moveTo(center.x, center.y - 22.dp.toPx())
                lineTo(center.x + 5.2.dp.toPx(), center.y - 8.dp.toPx())
                lineTo(center.x, center.y - 11.dp.toPx())
                lineTo(center.x - 5.2.dp.toPx(), center.y - 8.dp.toPx())
                close()
            }
            drawPath(arrow, Color.White, style = Stroke(width = 1.4.dp.toPx()))
            drawPath(arrow, arrowColor)
        }
    }
    drawCircle(GpsBlue.copy(alpha = 0.17f), radius = 9.dp.toPx(), center = center)
    drawCircle(Color.White, radius = 6.dp.toPx(), center = center)
    drawCircle(GpsBlue, radius = 4.dp.toPx(), center = center)
}

private fun DrawScope.drawGpsAccuracyEllipse(center: Offset, radiusX: Float, radiusY: Float) {
    if (!radiusX.isFinite() || !radiusY.isFinite() || radiusX <= 0f || radiusY <= 0f) return
    val minimumRadius = 14.dp.toPx()
    val safeX = radiusX.coerceAtLeast(minimumRadius).coerceAtMost(size.width * 4f)
    val safeY = radiusY.coerceAtLeast(minimumRadius).coerceAtMost(size.height * 4f)
    val topLeft = Offset(center.x - safeX, center.y - safeY)
    val ellipseSize = Size(safeX * 2f, safeY * 2f)
    drawOval(GpsBlue.copy(alpha = 0.12f), topLeft, ellipseSize)
    drawOval(GpsBlue.copy(alpha = 0.62f), topLeft, ellipseSize, style = Stroke(width = 1.dp.toPx()))
}

private fun DrawScope.drawFieldPointMarker(tip: Offset, pending: Boolean) {
    val width = if (pending) 8.dp.toPx() else 7.dp.toPx()
    val height = if (pending) 18.dp.toPx() else 16.dp.toPx()
    val centerY = tip.y - height * 0.62f
    val marker = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(tip.x - width * 0.25f, tip.y - height * 0.23f, tip.x - width, tip.y - height * 0.52f, tip.x - width, centerY)
        cubicTo(tip.x - width, centerY - width, tip.x + width, centerY - width, tip.x + width, centerY)
        cubicTo(tip.x + width, tip.y - height * 0.52f, tip.x + width * 0.25f, tip.y - height * 0.23f, tip.x, tip.y)
        close()
    }
    drawPath(marker, if (pending) PointGold else LukerBrown)
    drawPath(marker, Color.White, style = Stroke(width = 1.dp.toPx()))
    drawCircle(
        Color.White,
        radius = if (pending) 3.dp.toPx() else 2.5.dp.toPx(),
        center = Offset(tip.x, centerY),
    )
}

private fun DrawScope.drawFieldPointLabel(anchor: Offset, name: String) {
    val label = name.trim().ifBlank { "Punto" }.take(24)
    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(87, 47, 19)
        textSize = 11.dp.toPx()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val horizontalPadding = 6.dp.toPx()
    val labelHeight = 20.dp.toPx()
    val labelWidth = textPaint.measureText(label) + horizontalPadding * 2
    val left = anchor.x + 9.dp.toPx()
    val top = anchor.y - 23.dp.toPx()
    drawRoundRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset(left, top),
        size = Size(labelWidth, labelHeight),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        color = LukerBrown.copy(alpha = 0.35f),
        topLeft = Offset(left, top),
        size = Size(labelWidth, labelHeight),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(width = 0.8.dp.toPx()),
    )
    drawContext.canvas.nativeCanvas.drawText(
        label,
        left + horizontalPadding,
        top + 14.dp.toPx(),
        textPaint,
    )
}

private fun DrawScope.drawLotIndicator(tip: Offset) {
    val halfWidth = 10.dp.toPx()
    val height = 24.dp.toPx()
    val bodyCenter = Offset(tip.x, tip.y - height * 0.62f)
    val marker = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(
            tip.x - halfWidth * 0.32f,
            tip.y - height * 0.24f,
            tip.x - halfWidth,
            tip.y - height * 0.48f,
            tip.x - halfWidth,
            bodyCenter.y,
        )
        cubicTo(
            tip.x - halfWidth,
            bodyCenter.y - halfWidth,
            tip.x + halfWidth,
            bodyCenter.y - halfWidth,
            tip.x + halfWidth,
            bodyCenter.y,
        )
        cubicTo(
            tip.x + halfWidth,
            tip.y - height * 0.48f,
            tip.x + halfWidth * 0.32f,
            tip.y - height * 0.24f,
            tip.x,
            tip.y,
        )
        close()
    }

    drawCircle(LotRed.copy(alpha = 0.20f), radius = 15.dp.toPx(), center = bodyCenter)
    drawPath(marker, LotRed)
    drawPath(marker, Color.White, style = Stroke(width = 2.dp.toPx()))
    drawCircle(Color.White, radius = 5.dp.toPx(), center = bodyCenter)
    drawCircle(LotRed, radius = 2.dp.toPx(), center = bodyCenter)
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
                    String.format(Locale.US, "%.6f, %.6f", candidate.latitude, candidate.longitude),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Nota opcional") }, minLines = 2, maxLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, note) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
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
        title = { Text("Editar ${point.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Nota") }, minLines = 2, maxLines = 4)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(point.id, name, note); onDismiss() }) { Text("Guardar") }
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
    headingState: HeadingUiState,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        when (state.availability) {
            GpsAvailability.NEEDS_PERMISSION -> CompactGpsAction(
                Icons.Outlined.GpsNotFixed,
                "Activa la ubicación precisa",
                "Activar",
                onRequestLocation,
            )
            GpsAvailability.APPROXIMATE_ONLY -> CompactGpsAction(
                Icons.Outlined.WarningAmber,
                "Autoriza la ubicación precisa",
                "Ajustes",
                onOpenAppSettings,
            )
            GpsAvailability.LOCATION_DISABLED -> CompactGpsAction(
                Icons.Outlined.LocationDisabled,
                "Enciende el GPS del teléfono",
                "Encender",
                onOpenLocationSettings,
            )
            GpsAvailability.ERROR -> CompactGpsAction(
                Icons.Outlined.WarningAmber,
                state.errorMessage ?: "Revisa el permiso de ubicación",
                "Ajustes",
                onOpenAppSettings,
            )
            GpsAvailability.ACTIVE -> CompactActiveGpsStatus(state, headingState)
        }
    }
}

@Composable
private fun CompactGpsAction(icon: ImageVector, title: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.labelLarge)
        FilledTonalButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun CompactActiveGpsStatus(state: GpsUiState, headingState: HeadingUiState) {
    val orientationLabel = when (headingState.source) {
        HeadingSource.SENSOR -> "Brújula"
        HeadingSource.GPS_COURSE -> "Rumbo GPS"
        HeadingSource.UNAVAILABLE -> "Rumbo al caminar"
    }
    Row(
        modifier = Modifier
            .semantics {
                contentDescription =
                    "${state.satellitesUsed} de ${state.satellitesVisible} satélites. $orientationLabel"
            }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Outlined.SatelliteAlt,
                contentDescription = null,
                modifier = Modifier.padding(7.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                "${state.satellitesUsed}/${state.satellitesVisible} Satélites",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                state.fix?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) }
                    ?: "Esperando coordenadas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                state.fix?.let { "±${formatAccuracy(it.accuracyMeters)} m" } ?: "—",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                orientationLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (headingState.source == HeadingSource.GPS_COURSE) {
                    PointGold
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

private fun rotateOffset(offset: Offset, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    val cosine = cos(radians)
    val sine = sin(radians)
    return Offset(offset.x * cosine - offset.y * sine, offset.x * sine + offset.y * cosine)
}

private fun normalizeSignedAngle(degrees: Float): Float = ((degrees + 540f) % 360f) - 180f

private fun formatAccuracy(value: Float): String =
    if (value < 10f) String.format(Locale.forLanguageTag("es-CO"), "%.1f", value) else value.toInt().toString()

private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
private const val MIN_MAP_SCALE = 1f
private const val MAX_MAP_SCALE = 16f
private const val DOUBLE_TAP_ZOOM_FACTOR = 2f
private const val LOCATION_ZOOM = 6f
private const val POINT_ZOOM = 6.5f
private const val LOT_ZOOM = 7f
private const val POINT_LABEL_MIN_SCALE = 3f
private const val TILE_OVERSAMPLE = 1.25f
private const val TILE_RENDER_DEBOUNCE_MS = 140L
private const val FOLLOW_ANIMATION_MS = 280
private const val CAMERA_ANIMATION_MS = 430
private val TILE_LONG_EDGES = listOf(2_048, 4_096, 8_192, 16_384)
