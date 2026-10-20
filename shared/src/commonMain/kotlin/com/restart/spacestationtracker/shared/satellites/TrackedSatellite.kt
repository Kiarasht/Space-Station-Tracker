package com.restart.spacestationtracker.shared.satellites

import com.restart.spacestationtracker.shared.passes.RAD_TO_DEG
import com.restart.spacestationtracker.shared.passes.Sgp4
import com.restart.spacestationtracker.shared.passes.SkyGeometry
import com.restart.spacestationtracker.shared.passes.Tle
import com.restart.spacestationtracker.shared.passes.Vector3
import com.restart.spacestationtracker.shared.ui.platform.IssMapPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Spacecraft the map can show. [id] is stable across versions; it keys selections and map markers. */
enum class TrackedSatellite(val id: String, val noradId: Int) {
    ISS("iss", 25544),
    TIANGONG("tiangong", 48274),
    HUBBLE("hubble", 20580);

    companion object {
        fun fromId(id: String?): TrackedSatellite? = entries.firstOrNull { it.id == id }
    }
}

/** Where a satellite is now, and the ground track of its next orbit. */
data class SatelliteSnapshot(
    val satellite: TrackedSatellite,
    val latitude: Double,
    val longitude: Double,
    val altitudeKm: Double,
    val speedKmh: Double,
    val periodMinutes: Int,
    val timestampSeconds: Long,
    /** True while the satellite is inside Earth's shadow. */
    val isInEarthShadow: Boolean,
    /** [TRACK_MINUTES] ahead in one-minute steps, starting at the current position; timestamps in seconds. */
    val track: List<IssMapPoint>
)

/** How far ahead a satellite's path is drawn; matches the ISS orbit line and its +30m to +120m labels. */
const val TRACK_MINUTES = 120

/** Supplies current orbital elements for the satellites on the map. */
interface SatelliteElementsSource {
    /** Elements for each satellite that could be loaded; a failed one is simply missing. */
    suspend fun satelliteElements(satellites: List<TrackedSatellite>): Map<TrackedSatellite, Tle>
}

/** Turns orbital elements into map positions with SGP4; cheap enough to run on every map refresh. */
object SatelliteTracker {
    fun snapshot(satellite: TrackedSatellite, tle: Tle, nowMillis: Long): SatelliteSnapshot {
        val propagator = Sgp4(tle)
        val periodMinutes = (2.0 * PI / tle.meanMotion).roundToInt()
        val now = position(propagator, nowMillis)
        val track = (0..TRACK_MINUTES).map { minute ->
            val millis = nowMillis + minute * 60_000L
            val point = if (minute == 0) now else position(propagator, millis)
            IssMapPoint(latitude = point.latitude, longitude = point.longitude, timestamp = millis / 1_000L)
        }
        return SatelliteSnapshot(
            satellite = satellite,
            latitude = now.latitude,
            longitude = now.longitude,
            altitudeKm = now.altitudeKm,
            speedKmh = now.speedKmh,
            periodMinutes = periodMinutes,
            timestampSeconds = nowMillis / 1_000L,
            isInEarthShadow = now.isInEarthShadow,
            track = track
        )
    }

    private class Position(
        val latitude: Double,
        val longitude: Double,
        val altitudeKm: Double,
        val speedKmh: Double,
        val isInEarthShadow: Boolean
    )

    private fun position(propagator: Sgp4, epochMillis: Long): Position {
        val state = propagator.propagate(epochMillis)
        val julianDate = SkyGeometry.julianDate(epochMillis)
        val ecef = SkyGeometry.inertialToEcef(
            Vector3(state.x, state.y, state.z),
            SkyGeometry.greenwichSiderealTime(julianDate)
        )
        val speedKmh = sqrt(state.vx * state.vx + state.vy * state.vy + state.vz * state.vz) * 3_600.0
        val inShadow = SkyGeometry.isInEarthShadow(Vector3(state.x, state.y, state.z), SkyGeometry.sunPosition(julianDate))
        return geodetic(ecef, speedKmh, inShadow)
    }

    /** WGS-84 latitude, longitude and height from an Earth-fixed position in km (Bowring's iteration). */
    private fun geodetic(ecef: Vector3, speedKmh: Double, inShadow: Boolean): Position {
        val longitude = atan2(ecef.y, ecef.x)
        val horizontal = sqrt(ecef.x * ecef.x + ecef.y * ecef.y)
        var latitude = atan2(ecef.z, horizontal * (1.0 - WGS84_E2))
        var radiusOfCurvature = WGS84_A
        repeat(5) {
            val sinLatitude = sin(latitude)
            radiusOfCurvature = WGS84_A / sqrt(1.0 - WGS84_E2 * sinLatitude * sinLatitude)
            latitude = atan2(ecef.z + WGS84_E2 * radiusOfCurvature * sinLatitude, horizontal)
        }
        val altitude = horizontal / cos(latitude) - radiusOfCurvature
        return Position(latitude * RAD_TO_DEG, normalizeLongitude(longitude * RAD_TO_DEG), altitude, speedKmh, inShadow)
    }

    private fun normalizeLongitude(degrees: Double): Double = ((degrees + 540.0) % 360.0) - 180.0

    private const val WGS84_A = 6378.137
    private const val WGS84_F = 1.0 / 298.257223563
    private const val WGS84_E2 = WGS84_F * (2.0 - WGS84_F)
}

/**
 * Keeps the map's satellites current. Elements are reloaded every [REFRESH_MILLIS] (the source caches
 * them for hours), or every [RETRY_MILLIS] while some are missing, e.g. offline; positions are
 * recomputed on every call.
 */
class SatelliteLayer(
    private val source: SatelliteElementsSource,
    private val satellites: List<TrackedSatellite> = TrackedSatellite.entries
) {
    private var elements: Map<TrackedSatellite, Tle> = emptyMap()
    private var lastLoadMillis: Long? = null

    suspend fun snapshots(nowMillis: Long): List<SatelliteSnapshot> {
        val wait = if (elements.size < satellites.size) RETRY_MILLIS else REFRESH_MILLIS
        val lastLoad = lastLoadMillis
        if (lastLoad == null || nowMillis - lastLoad >= wait) {
            lastLoadMillis = nowMillis
            // Keep what loaded before if a refresh fails.
            elements = elements + source.satelliteElements(satellites)
        }
        return satellites.mapNotNull { satellite ->
            elements[satellite]?.let { tle -> runCatching { SatelliteTracker.snapshot(satellite, tle, nowMillis) }.getOrNull() }
        }
    }

    private companion object {
        const val REFRESH_MILLIS = 30L * 60L * 1_000L
        const val RETRY_MILLIS = 60L * 1_000L
    }
}
