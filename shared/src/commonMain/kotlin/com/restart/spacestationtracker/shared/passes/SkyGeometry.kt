package com.restart.spacestationtracker.shared.passes

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on Earth: geodetic latitude and longitude in degrees, altitude in meters above the WGS-84 ellipsoid. */
data class Observer(val latitude: Double, val longitude: Double, val altitudeMeters: Double)

/** Where to look: azimuth (degrees clockwise from north), elevation (degrees above the horizon), range in km. */
data class LookAngles(val azimuth: Double, val elevation: Double, val rangeKm: Double)

internal data class Vector3(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = Vector3(x * scale, y * scale, z * scale)
    infix fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    val length: Double get() = sqrt(this dot this)
}

/** Earth-fixed geometry for one observer. Accuracy targets visual pass prediction (well under a degree). */
internal class SkyGeometry(observer: Observer) {
    private val latitude = observer.latitude * DEG_TO_RAD
    private val longitude = observer.longitude * DEG_TO_RAD
    private val sinLat = sin(latitude)
    private val cosLat = cos(latitude)
    private val sinLon = sin(longitude)
    private val cosLon = cos(longitude)
    private val observerEcef: Vector3

    init {
        val altitudeKm = observer.altitudeMeters / 1000.0
        val n = WGS84_A / sqrt(1.0 - WGS84_E2 * sinLat * sinLat)
        observerEcef = Vector3(
            (n + altitudeKm) * cosLat * cosLon,
            (n + altitudeKm) * cosLat * sinLon,
            (n * (1.0 - WGS84_E2) + altitudeKm) * sinLat
        )
    }

    fun lookAngles(ecef: Vector3): LookAngles {
        val rho = ecef - observerEcef
        val south = sinLat * cosLon * rho.x + sinLat * sinLon * rho.y - cosLat * rho.z
        val east = -sinLon * rho.x + cosLon * rho.y
        val zenith = cosLat * cosLon * rho.x + cosLat * sinLon * rho.y + sinLat * rho.z
        val range = rho.length
        var azimuth = atan2(east, -south) * RAD_TO_DEG
        if (azimuth < 0.0) azimuth += 360.0
        return LookAngles(azimuth, asin(zenith / range) * RAD_TO_DEG, range)
    }

    /**
     * Apparent visual magnitude of a sunlit satellite, modelled as a diffusely reflecting sphere whose
     * standard magnitude is defined at 1000 km range and 90° phase angle (half illuminated).
     */
    fun magnitude(satelliteEcef: Vector3, sunEcef: Vector3, standardMagnitude: Double): Double {
        val toSun = sunEcef - satelliteEcef
        val toObserver = observerEcef - satelliteEcef
        val range = toObserver.length
        val cosPhase = ((toSun dot toObserver) / (toSun.length * range)).coerceIn(-1.0, 1.0)
        val phase = acos(cosPhase)
        val reflected = (sin(phase) + (PI - phase) * cos(phase)) / PI
        val reflectedAtHalfPhase = 1.0 / PI
        return standardMagnitude + 5.0 * log10(range / 1000.0) -
            2.5 * log10((reflected / reflectedAtHalfPhase).coerceAtLeast(1.0e-6))
    }

    companion object {
        private const val WGS84_A = 6378.137
        private const val WGS84_F = 1.0 / 298.257223563
        private const val WGS84_E2 = WGS84_F * (2.0 - WGS84_F)
        private const val EARTH_RADIUS_KM = 6378.137
        private const val AU_KM = 149_597_870.7

        fun julianDate(epochMillis: Long): Double = epochMillis / MILLIS_PER_DAY + UNIX_EPOCH_JULIAN_DATE

        /** Greenwich mean sidereal time in radians (IAU-82), the rotation from TEME to Earth-fixed. */
        fun greenwichSiderealTime(julianDate: Double): Double {
            val tut1 = (julianDate - 2_451_545.0) / 36_525.0
            var seconds = -6.2e-6 * tut1 * tut1 * tut1 + 0.093104 * tut1 * tut1 +
                (876_600.0 * 3600.0 + 8_640_184.812866) * tut1 + 67_310.54841
            seconds %= 86_400.0
            var angle = seconds / 240.0 * DEG_TO_RAD
            if (angle < 0.0) angle += 2.0 * PI
            return angle
        }

        /** Rotates an inertial (TEME, or mean-of-date) vector into the Earth-fixed frame. */
        fun inertialToEcef(inertial: Vector3, siderealTime: Double): Vector3 {
            val c = cos(siderealTime)
            val s = sin(siderealTime)
            return Vector3(c * inertial.x + s * inertial.y, -s * inertial.x + c * inertial.y, inertial.z)
        }

        /** Sun position in km, mean equator of date (Vallado's low-precision algorithm, about 0.01°). */
        fun sunPosition(julianDate: Double): Vector3 {
            val tut1 = (julianDate - 2_451_545.0) / 36_525.0
            val meanLongitude = (280.460 + 36_000.771 * tut1) % 360.0
            val meanAnomaly = ((357.5291092 + 35_999.05034 * tut1) % 360.0) * DEG_TO_RAD
            val eclipticLongitude = (meanLongitude + 1.914666471 * sin(meanAnomaly) +
                0.019994643 * sin(2.0 * meanAnomaly)) * DEG_TO_RAD
            val obliquity = (23.439291 - 0.0130042 * tut1) * DEG_TO_RAD
            val distance = (1.000140612 - 0.016708617 * cos(meanAnomaly) -
                0.000139589 * cos(2.0 * meanAnomaly)) * AU_KM
            return Vector3(
                distance * cos(eclipticLongitude),
                distance * cos(obliquity) * sin(eclipticLongitude),
                distance * sin(obliquity) * sin(eclipticLongitude)
            )
        }

        /** True when the satellite is inside Earth's cylindrical shadow. */
        fun isInEarthShadow(satellite: Vector3, sun: Vector3): Boolean {
            val sunDirection = sun * (1.0 / sun.length)
            val alongSun = satellite dot sunDirection
            if (alongSun >= 0.0) return false
            val perpendicular = satellite - sunDirection * alongSun
            return perpendicular.length < EARTH_RADIUS_KM
        }
    }
}

/** 16-point compass label (N, NNE, NE, ...) for an azimuth in degrees. */
internal fun compassPoint(azimuth: Double): String {
    val points = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((azimuth % 360.0 + 360.0) % 360.0 / 22.5 + 0.5).toInt() % 16
    return points[index]
}
