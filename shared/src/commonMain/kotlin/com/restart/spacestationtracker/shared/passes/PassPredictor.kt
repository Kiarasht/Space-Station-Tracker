package com.restart.spacestationtracker.shared.passes

import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Visible-pass prediction settings. A moment counts as visible when the satellite is above
 * [minimumElevation], lit by the Sun, and the observer's sky is dark ([maximumSunElevation]).
 * The defaults reproduce the N2YO visual-pass service the app used before, so pass lists, visibility
 * labels and alert filters behave the same.
 */
data class PassPredictionSettings(
    val days: Int = 10,
    val minimumVisibleSeconds: Int = 300,
    /** Passes that never climb this high are too low to see well and are skipped, as N2YO did. */
    val minimumPeakElevation: Double = 10.0,
    val minimumElevation: Double = 0.0,
    val maximumSunElevation: Double = -6.0,
    /**
     * Standard magnitude at 1000 km and half phase. Physical estimates for the ISS are about -1.8; this
     * value is calibrated to N2YO's published magnitudes, which the app's visibility thresholds
     * (PassVisibility, automatic-alert minimum visibility) were tuned against.
     */
    val standardMagnitude: Double = 0.3
)

/** Predicts visible passes on the device from a TLE, replacing a pass-prediction web service. */
object PassPredictor {
    private const val COARSE_STEP_MILLIS = 30_000L
    private const val DETAIL_STEP_MILLIS = 5_000L
    private const val REFINE_PRECISION_MILLIS = 1_000L
    // Enough points for a smooth sky-path curve without bloating saved reminders.
    private const val TRACK_POINTS = 24
    // Start a little early so a pass already in progress is reported from its real start.
    private const val LOOKBACK_MILLIS = 15L * 60L * 1_000L

    fun visiblePasses(
        tle: Tle,
        observer: Observer,
        startMillis: Long,
        settings: PassPredictionSettings = PassPredictionSettings()
    ): List<IssPass> {
        val sky = SkyModel(Sgp4(tle), SkyGeometry(observer), settings)
        val searchStart = startMillis - LOOKBACK_MILLIS
        val searchEnd = startMillis + settings.days * MILLIS_PER_DAY.toLong()

        val passes = mutableListOf<IssPass>()
        var previousTime = searchStart
        var previousVisible = sky.sample(previousTime).isVisible
        var visibleSince: Long? = if (previousVisible) previousTime else null
        var time = searchStart + COARSE_STEP_MILLIS
        while (time <= searchEnd) {
            val visible = sky.sample(time).isVisible
            if (visible != previousVisible) {
                val boundary = refineBoundary(sky, previousTime, time, previousVisible)
                if (visible) {
                    visibleSince = boundary
                } else {
                    visibleSince?.let { start -> sky.describePass(start, boundary)?.let(passes::add) }
                    visibleSince = null
                }
            }
            previousTime = time
            previousVisible = visible
            time += COARSE_STEP_MILLIS
        }
        visibleSince?.let { start -> sky.describePass(start, searchEnd)?.let(passes::add) }

        return passes.filter { pass ->
            pass.durationInSeconds >= settings.minimumVisibleSeconds &&
                pass.maxElevation >= settings.minimumPeakElevation &&
                pass.startTimeMillis + pass.durationInSeconds * 1_000L > startMillis
        }
    }

    /** Bisects to the moment visibility changes between [from] (with [fromVisible]) and [to]. */
    private fun refineBoundary(sky: SkyModel, from: Long, to: Long, fromVisible: Boolean): Long {
        var low = from
        var high = to
        while (high - low > REFINE_PRECISION_MILLIS) {
            val middle = (low + high) / 2
            if (sky.sample(middle).isVisible == fromVisible) low = middle else high = middle
        }
        return high
    }

    private class Sample(
        val look: LookAngles,
        val isVisible: Boolean,
        val satelliteEcef: Vector3,
        val sunEcef: Vector3
    )

    private class SkyModel(
        private val propagator: Sgp4,
        private val geometry: SkyGeometry,
        private val settings: PassPredictionSettings
    ) {
        fun sample(epochMillis: Long): Sample {
            val state = propagator.propagate(epochMillis)
            val satelliteInertial = Vector3(state.x, state.y, state.z)
            val julianDate = SkyGeometry.julianDate(epochMillis)
            val siderealTime = SkyGeometry.greenwichSiderealTime(julianDate)
            val sunInertial = SkyGeometry.sunPosition(julianDate)
            val satelliteEcef = SkyGeometry.inertialToEcef(satelliteInertial, siderealTime)
            val sunEcef = SkyGeometry.inertialToEcef(sunInertial, siderealTime)
            val look = geometry.lookAngles(satelliteEcef)
            val visible = look.elevation > settings.minimumElevation &&
                geometry.lookAngles(sunEcef).elevation < settings.maximumSunElevation &&
                !SkyGeometry.isInEarthShadow(satelliteInertial, sunInertial)
            return Sample(look, visible, satelliteEcef, sunEcef)
        }

        fun describePass(startMillis: Long, endMillis: Long): IssPass? {
            if (endMillis <= startMillis) return null
            val first = sample(startMillis)
            val last = sample(endMillis - REFINE_PRECISION_MILLIS)
            var maxElevation = max(first.look.elevation, last.look.elevation)
            var brightest = min(magnitudeOf(first), magnitudeOf(last))
            val elevations = mutableListOf(first.look.elevation)
            val azimuths = mutableListOf(first.look.azimuth)
            var time = startMillis + DETAIL_STEP_MILLIS
            while (time < endMillis) {
                val sample = sample(time)
                maxElevation = max(maxElevation, sample.look.elevation)
                brightest = min(brightest, magnitudeOf(sample))
                elevations += sample.look.elevation
                azimuths += sample.look.azimuth
                time += DETAIL_STEP_MILLIS
            }
            elevations += last.look.elevation
            azimuths += last.look.azimuth
            return IssPass(
                startTimeMillis = startMillis,
                durationInSeconds = ((endMillis - startMillis) / 1_000.0).roundToLong().toInt(),
                magnitude = (brightest * 10.0).roundToLong() / 10.0,
                maxElevation = (maxElevation * 10.0).roundToLong() / 10.0,
                startAzimuthCompass = compassPoint(first.look.azimuth),
                endAzimuthCompass = compassPoint(last.look.azimuth),
                // Both tracks are sampled at the same indices, so they stay paired.
                elevationTrack = downsample(elevations, TRACK_POINTS),
                azimuthTrack = downsample(azimuths, TRACK_POINTS)
            )
        }

        private fun downsample(values: List<Double>, count: Int): List<Double> {
            if (values.size <= count) return values.map { (it * 10.0).roundToLong() / 10.0 }
            return List(count) { index ->
                val value = values[(index * (values.size - 1).toDouble() / (count - 1)).roundToLong().toInt()]
                (value * 10.0).roundToLong() / 10.0
            }
        }

        private fun magnitudeOf(sample: Sample): Double {
            return geometry.magnitude(sample.satelliteEcef, sample.sunEcef, settings.standardMagnitude)
        }
    }
}
