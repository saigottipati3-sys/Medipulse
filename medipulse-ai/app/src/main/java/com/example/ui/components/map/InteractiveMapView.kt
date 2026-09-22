package com.example.ui.components.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthcareCategory
import com.example.data.model.HealthcarePlace
import com.example.data.model.RouteInfo
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun InteractiveMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    userLocation: Pair<Double, Double>?,
    places: List<HealthcarePlace>,
    selectedPlace: HealthcarePlace?,
    activeRoute: RouteInfo?,
    isEmergencyMode: Boolean,
    onPlaceClick: (HealthcarePlace) -> Unit,
    onCenterChange: (Double, Double) -> Unit,
    onZoomChange: (Float) -> Unit,
    onRecenter: () -> Unit,
    onToggleEmergencyMode: () -> Unit,
    onToggleListView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Pulse animation for user GPS dot and emergency markers
    val infiniteTransition = rememberInfiniteTransition(label = "map_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Store measured screen marker positions for click hit-testing
    var markerHitBoxes by remember { mutableStateOf<List<Pair<HealthcarePlace, Offset>>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("interactive_map_container")
    ) {
        // Slippy Vector Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newZoom = (zoomLevel * zoom).coerceIn(11.0f, 18.5f)
                        onZoomChange(newZoom)

                        // Convert pan offset to lat/lon delta
                        val scale = 256.0 * 2.0.pow(newZoom.toDouble())
                        val dLon = -(pan.x / scale) * 360.0
                        val dLat = (pan.y / scale) * 180.0
                        onCenterChange(centerLat + dLat, centerLon + dLon)
                    }
                }
                .pointerInput(places, markerHitBoxes) {
                    detectTapGestures { tapOffset ->
                        // Find closest marker within touch radius (36dp equivalent)
                        val touchRadiusPx = 70f
                        val clicked = markerHitBoxes.firstOrNull { (_, offset) ->
                            val dx = offset.x - tapOffset.x
                            val dy = offset.y - tapOffset.y
                            (dx * dx + dy * dy) <= (touchRadiusPx * touchRadiusPx)
                        }
                        if (clicked != null) {
                            onPlaceClick(clicked.first)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Clean Healthcare Map Base (light pastel canvas with subtle medical road grid)
            drawMapBackground(width, height, centerLat, centerLon, zoomLevel)

            // 2. Draw Active Route Polyline if present
            if (activeRoute != null && activeRoute.polylinePoints.isNotEmpty()) {
                drawRoutePolyline(
                    points = activeRoute.polylinePoints,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    zoom = zoomLevel,
                    canvasWidth = width,
                    canvasHeight = height
                )
            }

            // 3. Draw User Location Marker
            if (userLocation != null) {
                val userOffset = latLonToScreenOffset(
                    lat = userLocation.first,
                    lon = userLocation.second,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    zoom = zoomLevel,
                    width = width,
                    height = height
                )

                if (userOffset.x in -100f..(width + 100f) && userOffset.y in -100f..(height + 100f)) {
                    // Pulsing animated outer ring
                    drawCircle(
                        color = Color(0xFF0284C7).copy(alpha = pulseAlpha),
                        radius = 24f * pulseScale,
                        center = userOffset
                    )
                    // Precision halo
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = userOffset
                    )
                    // Core GPS dot
                    drawCircle(
                        color = Color(0xFF0284C7),
                        radius = 8f,
                        center = userOffset
                    )
                }
            }

            // 4. Draw Facility Markers and collect click target hit boxes
            val currentMarkers = mutableListOf<Pair<HealthcarePlace, Offset>>()

            places.forEach { place ->
                val markerOffset = latLonToScreenOffset(
                    lat = place.latitude,
                    lon = place.longitude,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    zoom = zoomLevel,
                    width = width,
                    height = height
                )

                if (markerOffset.x in -50f..(width + 50f) && markerOffset.y in -50f..(height + 50f)) {
                    currentMarkers.add(Pair(place, markerOffset))

                    val isSelected = selectedPlace?.id == place.id
                    val isEmergency = place.isEmergencyAvailable || place.category == HealthcareCategory.EMERGENCY_DEPT

                    drawHealthcarePin(
                        offset = markerOffset,
                        place = place,
                        isSelected = isSelected,
                        isEmergency = isEmergency,
                        pulseScale = if (isEmergency) pulseScale else 1.0f,
                        pulseAlpha = if (isEmergency) pulseAlpha else 0f,
                        textMeasurer = textMeasurer
                    )
                }
            }

            markerHitBoxes = currentMarkers
        }

        // Floating Action Controls: Top Right and Bottom Right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Emergency Mode Toggle Button
            FloatingActionButton(
                onClick = onToggleEmergencyMode,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("map_emergency_mode_toggle"),
                shape = CircleShape,
                containerColor = if (isEmergencyMode) Color(0xFFDC2626) else MaterialTheme.colorScheme.surface,
                contentColor = if (isEmergencyMode) Color.White else Color(0xFFDC2626),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = "Toggle Emergency Priority",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Recenter to Current GPS Location
            SmallFloatingActionButton(
                onClick = onRecenter,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("map_recenter_button"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My GPS Location",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom In (+)
            SmallFloatingActionButton(
                onClick = { onZoomChange((zoomLevel + 1.0f).coerceAtMost(18.5f)) },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("map_zoom_in_btn"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // Zoom Out (-)
            SmallFloatingActionButton(
                onClick = { onZoomChange((zoomLevel - 1.0f).coerceAtLeast(11.0f)) },
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("map_zoom_out_btn"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            // List / Map Toggle
            FloatingActionButton(
                onClick = onToggleListView,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("map_list_view_toggle")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Switch to List View",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Active Emergency Mode Banner at top
        if (isEmergencyMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .testTag("active_emergency_mode_banner"),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFDC2626),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "🚨 EMERGENCY MODE: Prioritizing 24/7 ER & Ambulance",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Draws an elegant healthcare base map with subtle grid, water body representation, and road network
 */
private fun DrawScope.drawMapBackground(
    width: Float,
    height: Float,
    centerLat: Double,
    centerLon: Double,
    zoom: Float
) {
    // Light clean base canvas
    drawRect(color = Color(0xFFF8FAFC))

    val gridSize = (60f * (zoom / 13f)).coerceIn(40f, 140f)
    val offsetX = ((centerLon * 1000f) % gridSize).toFloat()
    val offsetY = ((centerLat * 1000f) % gridSize).toFloat()

    // Minor residential / local road grid lines
    var x = -gridSize + offsetX
    while (x < width + gridSize) {
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 2.dp.toPx()
        )
        x += gridSize
    }

    var y = -gridSize + offsetY
    while (y < height + gridSize) {
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 2.dp.toPx()
        )
        y += gridSize
    }

    // Major transit / arterial roads
    val arterialGrid = gridSize * 2.5f
    var ax = -arterialGrid + (offsetX % arterialGrid)
    while (ax < width + arterialGrid) {
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(ax, 0f),
            end = Offset(ax, height),
            strokeWidth = 4.dp.toPx()
        )
        ax += arterialGrid
    }

    var ay = -arterialGrid + (offsetY % arterialGrid)
    while (ay < height + arterialGrid) {
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(0f, ay),
            end = Offset(width, ay),
            strokeWidth = 4.dp.toPx()
        )
        ay += arterialGrid
    }

    // Subtle park / green space accents
    drawCircle(
        color = Color(0xFFDCFCE7).copy(alpha = 0.5f),
        radius = width * 0.25f,
        center = Offset(width * 0.15f, height * 0.2f)
    )
    drawCircle(
        color = Color(0xFFE0F2FE).copy(alpha = 0.5f),
        radius = width * 0.2f,
        center = Offset(width * 0.85f, height * 0.75f)
    )
}

/**
 * Draws an interactive custom healthcare pin marker with category color, letter badge, and selection halo
 */
private fun DrawScope.drawHealthcarePin(
    offset: Offset,
    place: HealthcarePlace,
    isSelected: Boolean,
    isEmergency: Boolean,
    pulseScale: Float,
    pulseAlpha: Float,
    textMeasurer: TextMeasurer
) {
    val pinColor = Color(place.category.hexColor)

    // Emergency pulse ring
    if (isEmergency && pulseAlpha > 0.05f) {
        drawCircle(
            color = Color(0xFFDC2626).copy(alpha = pulseAlpha),
            radius = 24f * pulseScale,
            center = offset
        )
    }

    // Selection Halo
    if (isSelected) {
        drawCircle(
            color = Color(0xFF0284C7).copy(alpha = 0.35f),
            radius = 32f,
            center = offset
        )
        drawCircle(
            color = Color.White,
            radius = 26f,
            center = offset
        )
    }

    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = 18f,
        center = Offset(offset.x, offset.y + 4f)
    )

    // Pin Body: Outer white border
    drawCircle(
        color = Color.White,
        radius = 18f,
        center = offset
    )

    // Pin Core: Category Color
    drawCircle(
        color = pinColor,
        radius = 15f,
        center = offset
    )

    // Category glyph inside pin
    val glyph = when (place.category) {
        HealthcareCategory.HOSPITAL -> "H"
        HealthcareCategory.EMERGENCY_DEPT -> "+"
        HealthcareCategory.CLINIC -> "C"
        HealthcareCategory.DOCTOR -> "Dr"
        HealthcareCategory.PHARMACY -> "Rx"
        HealthcareCategory.DIAGNOSTIC_LAB -> "L"
        HealthcareCategory.AMBULANCE_SERVICE -> "EMS"
        HealthcareCategory.BLOOD_BANK -> "B"
        HealthcareCategory.MEDICAL_STORE -> "M"
        else -> "H"
    }

    val textLayout = textMeasurer.measure(
        text = glyph,
        style = TextStyle(
            color = Color.White,
            fontSize = if (glyph.length > 2) 7.sp else 9.sp,
            fontWeight = FontWeight.Black
        )
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            offset.x - (textLayout.size.width / 2f),
            offset.y - (textLayout.size.height / 2f)
        )
    )
}

/**
 * Draws active route polyline on canvas
 */
private fun DrawScope.drawRoutePolyline(
    points: List<Pair<Double, Double>>,
    centerLat: Double,
    centerLon: Double,
    zoom: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    if (points.size < 2) return

    val path = Path()
    var isFirst = true

    points.forEach { point ->
        val screenPos = latLonToScreenOffset(
            lat = point.first,
            lon = point.second,
            centerLat = centerLat,
            centerLon = centerLon,
            zoom = zoom,
            width = canvasWidth,
            height = canvasHeight
        )
        if (isFirst) {
            path.moveTo(screenPos.x, screenPos.y)
            isFirst = false
        } else {
            path.lineTo(screenPos.x, screenPos.y)
        }
    }

    // Outer glow / contrast casing
    drawPath(
        path = path,
        color = Color.White,
        style = Stroke(
            width = 8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Core route line
    drawPath(
        path = path,
        color = Color(0xFF0284C7),
        style = Stroke(
            width = 5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Web Mercator Projection formula converting (lat, lon) -> screen (x, y) given center and zoom
 */
private fun latLonToScreenOffset(
    lat: Double,
    lon: Double,
    centerLat: Double,
    centerLon: Double,
    zoom: Float,
    width: Float,
    height: Float
): Offset {
    val scale = 256.0 * 2.0.pow(zoom.toDouble())

    // Mercator X
    val worldX = (lon + 180.0) / 360.0 * scale
    val centerWorldX = (centerLon + 180.0) / 360.0 * scale

    // Mercator Y
    val sinLat = sin(Math.toRadians(lat.coerceIn(-85.0, 85.0)))
    val worldY = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * scale

    val sinCenterLat = sin(Math.toRadians(centerLat.coerceIn(-85.0, 85.0)))
    val centerWorldY = (0.5 - ln((1.0 + sinCenterLat) / (1.0 - sinCenterLat)) / (4.0 * PI)) * scale

    val screenX = (worldX - centerWorldX + (width / 2.0)).toFloat()
    val screenY = (worldY - centerWorldY + (height / 2.0)).toFloat()

    return Offset(screenX, screenY)
}
