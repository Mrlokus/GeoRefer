package co.georefer.app.ui

import android.graphics.PointF
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import co.georefer.app.location.GpsFix
import co.georefer.app.map.MapArea
import co.georefer.app.points.FieldPoint
import co.georefer.app.points.PointCandidate
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.sin

@Composable
fun MapLibreMapView(
    styleUrl: String,
    initialArea: MapArea,
    modifier: Modifier = Modifier,
    gpsFix: GpsFix? = null,
    headingDegrees: Float? = null,
    northUpLocked: Boolean = false,
    locateRequest: Int = 0,
    locateZoom: Double = 16.0,
    orientToHeading: Boolean = false,
    fieldPoints: List<FieldPoint> = emptyList(),
    pendingPoint: PointCandidate? = null,
    focusPoint: FieldPoint? = null,
    focusPointRequest: Int = 0,
    onMapLongPress: ((Double, Double) -> Unit)? = null,
    onFieldPointClick: ((FieldPoint) -> Unit)? = null,
    onUserGesture: (() -> Unit)? = null,
    showSelectionRectangle: Boolean = false,
    onVisibleAreaChanged: ((MapArea) -> Unit)? = null,
) {
    val mapView = rememberMapViewWithLifecycle()
    var mapInstance by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    var cameraInitialized by remember(mapView, initialArea) { mutableStateOf(false) }
    var loadedStyle by remember(mapView) { mutableStateOf<String?>(null) }
    var handledLocateRequest by remember(mapView) { mutableIntStateOf(0) }
    var handledFocusPointRequest by remember(mapView) { mutableIntStateOf(0) }

    DisposableEffect(mapInstance, onMapLongPress) {
        val map = mapInstance
        val callback = onMapLongPress
        if (map == null || callback == null) return@DisposableEffect onDispose { }
        val listener = MapLibreMap.OnMapLongClickListener { coordinate ->
            callback(coordinate.latitude, coordinate.longitude)
            true
        }
        map.addOnMapLongClickListener(listener)
        onDispose { map.removeOnMapLongClickListener(listener) }
    }

    DisposableEffect(mapInstance, fieldPoints, onFieldPointClick) {
        val map = mapInstance
        val callback = onFieldPointClick
        if (map == null || callback == null) return@DisposableEffect onDispose { }
        val listener = MapLibreMap.OnMapClickListener { coordinate ->
            val screenPoint = map.projection.toScreenLocation(coordinate)
            val feature = map.queryRenderedFeatures(screenPoint, FIELD_POINT_LAYER_ID).firstOrNull()
            val pointId = feature?.getStringProperty("pointId")?.toLongOrNull()
            val point = pointId?.let { id -> fieldPoints.firstOrNull { it.id == id } }
            if (point != null) callback(point)
            point != null
        }
        map.addOnMapClickListener(listener)
        onDispose { map.removeOnMapClickListener(listener) }
    }

    LaunchedEffect(mapInstance, gpsFix, locateRequest, orientToHeading, headingDegrees) {
        val map = mapInstance
        val fix = gpsFix
        if (map != null && fix != null && locateRequest > handledLocateRequest) {
            val camera = CameraPosition.Builder(map.cameraPosition)
                .target(LatLng(fix.latitude, fix.longitude))
                .zoom(locateZoom)
                .apply {
                    if (orientToHeading && headingDegrees != null && !northUpLocked) {
                        bearing(headingDegrees.toDouble())
                    }
                }
                .build()
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(camera),
                800,
            )
            handledLocateRequest = locateRequest
        }
    }

    LaunchedEffect(mapInstance, focusPoint, focusPointRequest) {
        val map = mapInstance
        val point = focusPoint
        if (map != null && point != null && focusPointRequest > handledFocusPointRequest) {
            val camera = CameraPosition.Builder(map.cameraPosition)
                .target(LatLng(point.latitude, point.longitude))
                .zoom(locateZoom)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 800)
            handledFocusPointRequest = focusPointRequest
        }
    }

    DisposableEffect(mapInstance, onUserGesture) {
        val map = mapInstance
        val callback = onUserGesture
        if (map == null || callback == null) return@DisposableEffect onDispose { }
        val listener = MapLibreMap.OnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                callback()
            }
        }
        map.addOnCameraMoveStartedListener(listener)
        onDispose { map.removeOnCameraMoveStartedListener(listener) }
    }

    LaunchedEffect(mapInstance, northUpLocked) {
        val map = mapInstance ?: return@LaunchedEffect
        map.uiSettings.isRotateGesturesEnabled = !northUpLocked
        map.uiSettings.isTiltGesturesEnabled = !northUpLocked
        map.uiSettings.isCompassEnabled = !northUpLocked
        if (northUpLocked && map.cameraPosition.bearing != 0.0) {
            map.animateCamera(CameraUpdateFactory.bearingTo(0.0), 450)
        }
    }

    DisposableEffect(mapInstance, onVisibleAreaChanged) {
        val map = mapInstance
        if (map == null || onVisibleAreaChanged == null) return@DisposableEffect onDispose { }
        val listener = MapLibreMap.OnCameraIdleListener {
            if (mapView.width <= 0 || mapView.height <= 0) return@OnCameraIdleListener
            val density = mapView.resources.displayMetrics.density
            val horizontalInset = 32f * density
            val verticalInset = 56f * density
            val northWest = map.projection.fromScreenLocation(
                PointF(horizontalInset, verticalInset),
            )
            val southEast = map.projection.fromScreenLocation(
                PointF(
                    (mapView.width - horizontalInset).coerceAtLeast(horizontalInset),
                    (mapView.height - verticalInset).coerceAtLeast(verticalInset),
                ),
            )
            onVisibleAreaChanged(
                MapArea(
                    north = maxOf(northWest.latitude, southEast.latitude),
                    east = maxOf(northWest.longitude, southEast.longitude),
                    south = minOf(northWest.latitude, southEast.latitude),
                    west = minOf(northWest.longitude, southEast.longitude),
                ),
            )
        }
        map.addOnCameraIdleListener(listener)
        listener.onCameraIdle()
        onDispose { map.removeOnCameraIdleListener(listener) }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    mapInstance = map
                    if (loadedStyle != styleUrl) {
                        loadedStyle = styleUrl
                        map.setStyle(styleUrl) { loadedMapStyle ->
                            updateGpsIndicator(loadedMapStyle, gpsFix, headingDegrees)
                            updateFieldPointIndicators(loadedMapStyle, fieldPoints, pendingPoint)
                        }
                    } else {
                        map.style?.let { loadedMapStyle ->
                            updateGpsIndicator(loadedMapStyle, gpsFix, headingDegrees)
                            updateFieldPointIndicators(loadedMapStyle, fieldPoints, pendingPoint)
                        }
                    }
                    if (!cameraInitialized) {
                        val center = LatLng(
                            (initialArea.north + initialArea.south) / 2.0,
                            (initialArea.east + initialArea.west) / 2.0,
                        )
                        map.cameraPosition = CameraPosition.Builder()
                            .target(center)
                            .zoom(zoomFor(initialArea))
                            .build()
                        cameraInitialized = true
                    }
                    map.uiSettings.isRotateGesturesEnabled = !northUpLocked
                    map.uiSettings.isTiltGesturesEnabled = !northUpLocked
                    map.uiSettings.isCompassEnabled = !northUpLocked
                }
            },
        )

        if (showSelectionRectangle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 56.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFD79B32),
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
        }
    }
}

private fun updateFieldPointIndicators(
    style: Style,
    points: List<FieldPoint>,
    pendingPoint: PointCandidate?,
) {
    if (style.getImage(FIELD_POINT_IMAGE_ID) == null) {
        style.addImage(FIELD_POINT_IMAGE_ID, createPointMarkerBitmap("#D79B32"))
    }
    if (style.getImage(PENDING_POINT_IMAGE_ID) == null) {
        style.addImage(PENDING_POINT_IMAGE_ID, createPointMarkerBitmap("#F2B84B"))
    }

    val savedFeatures = FeatureCollection.fromFeatures(
        points.map { point ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                addStringProperty("name", point.name)
                addStringProperty("pointId", point.id.toString())
            }
        },
    )
    val savedSource = style.getSourceAs<GeoJsonSource>(FIELD_POINT_SOURCE_ID)
    if (savedSource == null) {
        style.addSource(GeoJsonSource(FIELD_POINT_SOURCE_ID, savedFeatures))
    } else {
        savedSource.setGeoJson(savedFeatures)
    }
    if (style.getLayer(FIELD_POINT_LAYER_ID) == null) {
        style.addLayer(
            SymbolLayer(FIELD_POINT_LAYER_ID, FIELD_POINT_SOURCE_ID).withProperties(
                iconImage(FIELD_POINT_IMAGE_ID),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconSize(0.72f),
            ),
        )
    }

    val pendingFeatures = pendingPoint?.let { point ->
        FeatureCollection.fromFeature(
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)),
        )
    } ?: EMPTY_GPS_FEATURES
    val pendingSource = style.getSourceAs<GeoJsonSource>(PENDING_POINT_SOURCE_ID)
    if (pendingSource == null) {
        style.addSource(GeoJsonSource(PENDING_POINT_SOURCE_ID, pendingFeatures))
    } else {
        pendingSource.setGeoJson(pendingFeatures)
    }
    if (style.getLayer(PENDING_POINT_LAYER_ID) == null) {
        style.addLayer(
            SymbolLayer(PENDING_POINT_LAYER_ID, PENDING_POINT_SOURCE_ID).withProperties(
                iconImage(PENDING_POINT_IMAGE_ID),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconSize(0.9f),
            ),
        )
    }
}

private fun createPointMarkerBitmap(color: String): Bitmap {
    val bitmap = Bitmap.createBitmap(44, 56, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val marker = Path().apply {
        moveTo(22f, 54f)
        cubicTo(18f, 45f, 5f, 34f, 5f, 20f)
        cubicTo(5f, 10.6f, 12.6f, 3f, 22f, 3f)
        cubicTo(31.4f, 3f, 39f, 10.6f, 39f, 20f)
        cubicTo(39f, 34f, 26f, 45f, 22f, 54f)
        close()
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.parseColor(color)
        style = Paint.Style.FILL
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val center = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.parseColor("#174F3D")
        style = Paint.Style.FILL
    }
    canvas.drawPath(marker, fill)
    canvas.drawPath(marker, outline)
    canvas.drawCircle(22f, 20f, 6f, center)
    return bitmap
}

private fun updateGpsIndicator(style: Style, fix: GpsFix?, headingDegrees: Float?) {
    val source = style.getSourceAs<GeoJsonSource>(GPS_SOURCE_ID)
    val accuracySource = style.getSourceAs<GeoJsonSource>(GPS_ACCURACY_SOURCE_ID)
    if (fix == null) {
        source?.setGeoJson(EMPTY_GPS_FEATURES)
        accuracySource?.setGeoJson(EMPTY_GPS_FEATURES)
        return
    }

    val feature = Feature.fromGeometry(Point.fromLngLat(fix.longitude, fix.latitude))
    if (source == null) {
        style.addSource(GeoJsonSource(GPS_SOURCE_ID, feature))
    } else {
        source.setGeoJson(feature)
    }

    val accuracyFeature = accuracyCircleFeature(fix)
    if (accuracySource == null) {
        style.addSource(GeoJsonSource(GPS_ACCURACY_SOURCE_ID, accuracyFeature))
    } else {
        accuracySource.setGeoJson(accuracyFeature)
    }
    if (style.getLayer(GPS_ACCURACY_LAYER_ID) == null) {
        style.addLayer(
            FillLayer(GPS_ACCURACY_LAYER_ID, GPS_ACCURACY_SOURCE_ID).withProperties(
                fillColor(GPS_COLOR),
                fillOpacity(0.12f),
                fillOutlineColor(GPS_COLOR),
            ),
        )
    }

    if (style.getLayer(GPS_HALO_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(GPS_HALO_LAYER_ID, GPS_SOURCE_ID).withProperties(
                circleRadius(6.5f),
                circleColor(GPS_COLOR),
                circleOpacity(0.15f),
            ),
        )
    }
    if (style.getLayer(GPS_DOT_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(GPS_DOT_LAYER_ID, GPS_SOURCE_ID).withProperties(
                circleRadius(3f),
                circleColor(GPS_COLOR),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(1.25f),
            ),
        )
    }
    if (style.getImage(GPS_HEADING_IMAGE_ID) == null) {
        style.addImage(GPS_HEADING_IMAGE_ID, createHeadingIndicatorBitmap())
    }
    val headingLayer = style.getLayerAs<SymbolLayer>(GPS_HEADING_LAYER_ID)
    if (headingLayer == null) {
        style.addLayerBelow(
            SymbolLayer(GPS_HEADING_LAYER_ID, GPS_SOURCE_ID).withProperties(
                iconImage(GPS_HEADING_IMAGE_ID),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                iconRotate(headingDegrees ?: 0f),
                iconOpacity(if (headingDegrees == null) 0f else 1f),
                iconSize(0.72f),
            ),
            GPS_DOT_LAYER_ID,
        )
    } else {
        headingLayer.setProperties(
            iconRotate(headingDegrees ?: 0f),
            iconOpacity(if (headingDegrees == null) 0f else 1f),
        )
    }
}

private fun accuracyCircleFeature(fix: GpsFix): FeatureCollection {
    val radiusMeters = fix.accuracyMeters.toDouble()
    if (!radiusMeters.isFinite() || radiusMeters <= 0.0) return EMPTY_GPS_FEATURES
    val angularDistance = radiusMeters.coerceAtMost(MAX_ACCURACY_RADIUS_METERS) / EARTH_RADIUS_METERS
    val latitude = Math.toRadians(fix.latitude)
    val longitude = Math.toRadians(fix.longitude)
    val ring = (0..ACCURACY_CIRCLE_SEGMENTS).map { index ->
        val bearing = 2.0 * PI * index / ACCURACY_CIRCLE_SEGMENTS
        val targetLatitude = asin(
            sin(latitude) * cos(angularDistance) +
                cos(latitude) * sin(angularDistance) * cos(bearing),
        )
        val targetLongitude = longitude + atan2(
            sin(bearing) * sin(angularDistance) * cos(latitude),
            cos(angularDistance) - sin(latitude) * sin(targetLatitude),
        )
        Point.fromLngLat(Math.toDegrees(targetLongitude), Math.toDegrees(targetLatitude))
    }
    return FeatureCollection.fromFeature(Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))))
}

private fun createHeadingIndicatorBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val arrow = Path().apply {
        moveTo(20f, 2f)
        lineTo(29f, 19f)
        lineTo(23f, 17f)
        lineTo(20f, 23f)
        lineTo(17f, 17f)
        lineTo(11f, 19f)
        close()
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(GPS_COLOR)
        style = Paint.Style.FILL
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
    }
    canvas.drawPath(arrow, fill)
    canvas.drawPath(arrow, outline)
    return bitmap
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(null) } }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        onDispose {
            lifecycle.removeObserver(observer)
            runCatching { mapView.onPause() }
            runCatching { mapView.onStop() }
            runCatching { mapView.onDestroy() }
        }
    }
    return mapView
}

private fun zoomFor(area: MapArea): Double {
    val span = max(area.north - area.south, area.east - area.west).coerceAtLeast(0.0001)
    return (8.0 - log2(span)).coerceIn(4.5, 15.0)
}

private val EMPTY_GPS_FEATURES = FeatureCollection.fromFeatures(emptyList())
private const val GPS_SOURCE_ID = "georefer-gps-source"
private const val GPS_ACCURACY_SOURCE_ID = "georefer-gps-accuracy-source"
private const val GPS_ACCURACY_LAYER_ID = "georefer-gps-accuracy-layer"
private const val GPS_HALO_LAYER_ID = "georefer-gps-halo"
private const val GPS_DOT_LAYER_ID = "georefer-gps-dot"
private const val GPS_HEADING_LAYER_ID = "georefer-gps-heading"
private const val GPS_HEADING_IMAGE_ID = "georefer-gps-heading-image"
private const val GPS_COLOR = "#2D75D5"
private const val EARTH_RADIUS_METERS = 6_371_008.8
private const val MAX_ACCURACY_RADIUS_METERS = 20_000.0
private const val ACCURACY_CIRCLE_SEGMENTS = 64
private const val FIELD_POINT_SOURCE_ID = "georefer-field-points-source"
private const val FIELD_POINT_LAYER_ID = "georefer-field-points-layer"
private const val FIELD_POINT_IMAGE_ID = "georefer-field-point-marker"
private const val PENDING_POINT_SOURCE_ID = "georefer-pending-point-source"
private const val PENDING_POINT_LAYER_ID = "georefer-pending-point-layer"
private const val PENDING_POINT_IMAGE_ID = "georefer-pending-point-marker"
