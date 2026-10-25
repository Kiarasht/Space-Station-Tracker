package com.restart.spacestationtracker.shared.ui.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
actual fun PlatformIssMap(
    currentLocation: IssLocation?,
    orbit: List<IssMapPoint>,
    mapType: String,
    modifier: Modifier,
    bottomInset: Dp,
    satellites: List<SatelliteMapMarker>,
    tracks: List<SatelliteMapTrack>,
    orbitVisible: Boolean,
    focus: MapFocusRequest?,
    onSatelliteClick: (String?) -> Unit
) {
    val context = LocalContext.current
    // Lines (with their arrows and labels) fade in and out when the selection changes.
    val orbitAlpha by animateFloatAsState(if (orbitVisible) 1f else 0f, tween(LINE_FADE_MILLIS))
    val density = context.resources.displayMetrics.density
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }
    var hasCentered by remember { mutableStateOf(false) }
    val orderedOrbit = remember(orbit) {
        orbit
            .filter { it.timestamp > 0L }
            .distinctBy(IssMapPoint::timestamp)
            .sortedBy(IssMapPoint::timestamp)
    }
    val orbitCues = remember(orderedOrbit, currentLocation?.timestamp) {
        buildOrbitCues(currentLocation?.timestamp ?: 0L, orderedOrbit)
    }
    val exoTypeface = remember(context) { loadExoTypeface(context) }
    LaunchedEffect(currentLocation) {
        if (!hasCentered && currentLocation != null) {
            cameraState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation.latitude, currentLocation.longitude),
                    2f
                )
            )
            hasCentered = true
        }
    }
    LaunchedEffect(focus?.serial) {
        val target = focus ?: return@LaunchedEffect
        cameraState.animate(CameraUpdateFactory.newLatLng(LatLng(target.latitude, target.longitude)))
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        contentPadding = PaddingValues(bottom = bottomInset),
        properties = MapProperties(
            mapType = when (mapType) {
                "Satellite" -> MapType.SATELLITE
                "Hybrid" -> MapType.HYBRID
                "Terrain" -> MapType.TERRAIN
                else -> MapType.NORMAL
            }
        ),
        // No map toolbar: it would appear bottom right, where the spacecraft switcher sits.
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
        onMapClick = { onSatelliteClick(null) }
    ) {
        val directionMarker = remember { createDirectionMarker() }
        val timeMarkers = remember(exoTypeface) {
            (30..120 step 30).associateWith { minute ->
                createTimeMarker("+${minute}m", exoTypeface)
            }
        }
        tracks.forEach { track ->
            key(track.id) {
                val alpha by animateFloatAsState(if (track.visible) 1f else 0f, tween(LINE_FADE_MILLIS))
                if (track.points.size > 1 && alpha > 0f) {
                    val trackColor = track.colorArgb.toInt()
                    Polyline(
                        points = track.points.map { LatLng(it.latitude, it.longitude) },
                        color = Color(trackColor).copy(alpha = alpha),
                        width = 5f,
                        geodesic = true,
                        zIndex = 1f
                    )
                    // The same arrows and +30m to +120m labels as the ISS line, in the satellite's color.
                    val cues = remember(track.points) {
                        buildOrbitCues(track.points.first().timestamp, track.points)
                    }
                    val trackDirectionMarker = remember(trackColor) { createDirectionMarker(trackColor) }
                    val trackTimeMarkers = remember(trackColor, exoTypeface) {
                        (30..120 step 30).associateWith { minute ->
                            createTimeMarker("+${minute}m", exoTypeface, trackColor)
                        }
                    }
                    cues.forEach { cue ->
                        key(cue.minuteOffset) {
                            Marker(
                                state = rememberUpdatedMarkerState(LatLng(cue.point.latitude, cue.point.longitude)),
                                icon = if (cue.minuteOffset % 30 == 0) trackTimeMarkers[cue.minuteOffset] else trackDirectionMarker,
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                flat = cue.minuteOffset % 30 != 0,
                                rotation = if (cue.minuteOffset % 30 == 0) 0f else cue.bearing,
                                alpha = alpha,
                                zIndex = 4f
                            )
                        }
                    }
                }
            }
        }
        satellites.forEach { satellite -> key(satellite.id) {
            val icon = remember(satellite.label, satellite.colorArgb, satellite.isStation, satellite.isSelected, exoTypeface) {
                createSatelliteMarker(satellite, density, exoTypeface)
            }
            Marker(
                state = rememberUpdatedMarkerState(LatLng(satellite.latitude, satellite.longitude)),
                icon = icon.descriptor,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, icon.anchorY),
                title = satellite.label,
                zIndex = if (satellite.isSelected) 5f else 2.5f,
                onClick = {
                    onSatelliteClick(satellite.id)
                    true
                }
            )
        } }
        if (orderedOrbit.size > 1 && orbitAlpha > 0f) {
            Polyline(
                points = orderedOrbit.map { LatLng(it.latitude, it.longitude) },
                color = Color.Red.copy(alpha = orbitAlpha),
                width = 5f,
                geodesic = true,
                zIndex = 1f
            )
            orbitCues.forEach { cue ->
                key(cue.minuteOffset) {
                    if (cue.minuteOffset % 30 == 0) {
                        Marker(
                            state = rememberUpdatedMarkerState(
                                LatLng(cue.point.latitude, cue.point.longitude)
                            ),
                            icon = timeMarkers[cue.minuteOffset],
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            alpha = orbitAlpha,
                            zIndex = 4f
                        )
                    } else {
                        Marker(
                            state = rememberUpdatedMarkerState(
                                LatLng(cue.point.latitude, cue.point.longitude)
                            ),
                            icon = directionMarker,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            flat = true,
                            rotation = cue.bearing,
                            alpha = orbitAlpha,
                            zIndex = 4f
                        )
                    }
                }
            }
        }
    }
}

@Composable
actual fun PlatformNativeAd(
    slotId: String,
    modifier: Modifier
) = Unit

@Composable
actual fun PlatformBannerAd(
    modifier: Modifier
) = Unit

private const val LINE_FADE_MILLIS = 300

private class SatelliteMarkerIcon(val descriptor: BitmapDescriptor, val anchorY: Float)

/** A colored badge with a station or telescope glyph, and the satellite's name in a pill underneath. */
private fun createSatelliteMarker(marker: SatelliteMapMarker, density: Float, typeface: Typeface): SatelliteMarkerIcon {
    // Same size selected or not, so a tap doesn't make the badge jump; the label's border shows the selection.
    val radius = 14f * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        this.typeface = Typeface.create(typeface, Typeface.BOLD)
    }
    val labelWidth = textPaint.measureText(marker.label) + 14f * density
    val labelHeight = 18f * density
    val gap = 3f * density
    val ring = 2f * density
    val width = maxOf(labelWidth, (radius + ring) * 2f).toInt() + 2
    val height = ((radius + ring) * 2f + gap + labelHeight).toInt() + 2
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = width / 2f
    val centerY = radius + ring
    val accent = marker.colorArgb.toInt()

    canvas.drawCircle(centerX, centerY, radius + ring, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
    canvas.drawCircle(centerX, centerY, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
    val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }
    val unit = radius / 14f
    if (marker.isStation) {
        canvas.drawRect(centerX - 3f * unit, centerY - 4f * unit, centerX + 3f * unit, centerY + 4f * unit, glyph)
        canvas.drawRect(centerX - 11f * unit, centerY - 3f * unit, centerX - 5f * unit, centerY + 3f * unit, glyph)
        canvas.drawRect(centerX + 5f * unit, centerY - 3f * unit, centerX + 11f * unit, centerY + 3f * unit, glyph)
        canvas.drawRect(centerX - 5f * unit, centerY - 0.6f * unit, centerX + 5f * unit, centerY + 0.6f * unit, glyph)
    } else {
        canvas.save()
        canvas.rotate(-35f, centerX, centerY)
        canvas.drawRoundRect(RectF(centerX - 9f * unit, centerY - 3.5f * unit, centerX + 7f * unit, centerY + 3.5f * unit), 2f * unit, 2f * unit, glyph)
        canvas.drawRect(centerX + 7f * unit, centerY - 2.5f * unit, centerX + 10f * unit, centerY + 2.5f * unit, glyph)
        canvas.drawRect(centerX - 3f * unit, centerY - 9f * unit, centerX - 1f * unit, centerY - 3.5f * unit, glyph)
        canvas.drawRect(centerX - 3f * unit, centerY + 3.5f * unit, centerX - 1f * unit, centerY + 9f * unit, glyph)
        canvas.restore()
    }

    val labelTop = centerY + radius + ring + gap
    val label = RectF(centerX - labelWidth / 2f, labelTop, centerX + labelWidth / 2f, labelTop + labelHeight)
    canvas.drawRoundRect(label, labelHeight / 2f, labelHeight / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(235, 12, 18, 68)
    })
    if (marker.isSelected) {
        canvas.drawRoundRect(label, labelHeight / 2f, labelHeight / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        })
    }
    val baseline = label.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(marker.label, centerX, baseline, textPaint)

    return SatelliteMarkerIcon(BitmapDescriptorFactory.fromBitmap(bitmap), anchorY = centerY / height)
}

private fun createDirectionMarker(fillColor: Int = AndroidColor.RED): BitmapDescriptor {
    val size = 44
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(0, 0, 32)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeJoin = Paint.Join.ROUND
    }
    val path = Path().apply {
        moveTo(22f, 3f)
        lineTo(38f, 37f)
        lineTo(22f, 29f)
        lineTo(6f, 37f)
        close()
    }
    canvas.drawPath(path, fill)
    canvas.drawPath(path, outline)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun createTimeMarker(
    label: String,
    typeface: Typeface,
    borderColor: Int = AndroidColor.rgb(255, 221, 64)
): BitmapDescriptor {
    val width = 116
    val height = 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(235, 12, 18, 68)
        style = Paint.Style.FILL
    }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 23f
        textAlign = Paint.Align.CENTER
        this.typeface = Typeface.create(typeface, Typeface.BOLD)
    }
    val bounds = RectF(2f, 2f, width - 2f, height - 2f)
    canvas.drawRoundRect(bounds, 16f, 16f, background)
    canvas.drawRoundRect(bounds, 16f, 16f, border)
    val baseline = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(label, width / 2f, baseline, textPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun loadExoTypeface(context: Context): Typeface {
    val resourceId = context.resources.getIdentifier(
        "exo_variable",
        "font",
        context.packageName
    )
    return if (resourceId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { context.resources.getFont(resourceId) }.getOrDefault(Typeface.DEFAULT)
    } else {
        Typeface.DEFAULT
    }
}

private data class OrbitCue(
    val point: IssMapPoint,
    val minuteOffset: Int,
    val bearing: Float
)

private fun buildOrbitCues(
    currentTimestamp: Long,
    orbit: List<IssMapPoint>
): List<OrbitCue> {
    if (currentTimestamp <= 0L) return emptyList()
    val future = orbit.filter { it.timestamp > currentTimestamp }
    if (future.size < 2) return emptyList()

    return (15..120 step 15).mapNotNull { minuteOffset ->
        val targetTimestamp = currentTimestamp + minuteOffset * 60L
        val index = future.indices.minByOrNull { index ->
            abs(future[index].timestamp - targetTimestamp)
        } ?: return@mapNotNull null
        val point = future[index]
        if (abs(point.timestamp - targetTimestamp) > 90L) return@mapNotNull null
        val next = future.getOrNull(index + 1)
        val previous = future.getOrNull(index - 1)
        val bearing = when {
            next != null -> bearingDegrees(point, next)
            previous != null -> bearingDegrees(previous, point)
            else -> 0f
        }
        OrbitCue(point, minuteOffset, bearing)
    }
}

private fun bearingDegrees(from: IssMapPoint, to: IssMapPoint): Float {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val y = sin(longitudeDelta) * cos(toLatitude)
    val x = cos(fromLatitude) * sin(toLatitude) -
        sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
}

private fun normalizeLongitude(longitude: Double): Double =
    ((longitude + 540.0) % 360.0) - 180.0
