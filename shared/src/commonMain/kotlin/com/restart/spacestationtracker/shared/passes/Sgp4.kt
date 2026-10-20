package com.restart.spacestationtracker.shared.passes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A two-line element set, the orbit description published for every tracked satellite. */
data class Tle(val line1: String, val line2: String) {
    val epochMillis: Long
    internal val bstar: Double
    internal val inclination: Double
    internal val rightAscension: Double
    internal val eccentricity: Double
    internal val argumentOfPerigee: Double
    internal val meanAnomaly: Double
    /** Mean motion in radians per minute (Kozai convention, as published). */
    internal val meanMotion: Double

    init {
        require(line1.length >= 69 && line1[0] == '1') { "Invalid TLE line 1" }
        require(line2.length >= 69 && line2[0] == '2') { "Invalid TLE line 2" }

        val epochYear = line1.substring(18, 20).trim().toInt()
        val epochDays = line1.substring(20, 32).trim().toDouble()
        val year = if (epochYear < 57) 2000 + epochYear else 1900 + epochYear
        epochMillis = ((julianDateOfJanuaryFirst(year) + epochDays - 1.0 - UNIX_EPOCH_JULIAN_DATE) * MILLIS_PER_DAY)
            .toLong()
        bstar = parseImpliedExponent(line1.substring(53, 61))

        inclination = line2.substring(8, 16).trim().toDouble() * DEG_TO_RAD
        rightAscension = line2.substring(17, 25).trim().toDouble() * DEG_TO_RAD
        eccentricity = "0.${line2.substring(26, 33).trim()}".toDouble()
        argumentOfPerigee = line2.substring(34, 42).trim().toDouble() * DEG_TO_RAD
        meanAnomaly = line2.substring(43, 51).trim().toDouble() * DEG_TO_RAD
        meanMotion = line2.substring(52, 63).trim().toDouble() * 2.0 * PI / MINUTES_PER_DAY
    }

    private companion object {
        // Fields like " 15012-3" mean 0.15012e-3.
        fun parseImpliedExponent(field: String): Double {
            val value = field.trim()
            if (value.isEmpty()) return 0.0
            val sign = if (value.startsWith('-')) -1.0 else 1.0
            val digits = value.trimStart('-', '+')
            val exponentStart = digits.indexOfAny(charArrayOf('-', '+'), startIndex = 1)
            if (exponentStart < 0) return sign * "0.$digits".toDouble()
            val mantissa = "0.${digits.substring(0, exponentStart)}".toDouble()
            val exponent = digits.substring(exponentStart).toInt()
            return sign * mantissa * 10.0.pow(exponent)
        }

        fun julianDateOfJanuaryFirst(year: Int): Double {
            return 367.0 * year - floor(7.0 * (year + floor(10.0 / 12.0)) / 4.0) +
                floor(275.0 / 9.0) + 1.0 + 1_721_013.5
        }
    }
}

/** Position (km) and velocity (km/s) in the TEME frame SGP4 produces. */
data class TemeState(
    val x: Double,
    val y: Double,
    val z: Double,
    val vx: Double,
    val vy: Double,
    val vz: Double
)

/**
 * SGP4 orbit propagator for near-Earth satellites (period under 225 minutes, which includes the ISS),
 * ported from the reference implementation in Vallado et al., "Revisiting Spacetrack Report #3" (2006),
 * with WGS-72 constants as the element sets assume.
 */
class Sgp4(private val tle: Tle) {
    private val ecco = tle.eccentricity
    private val inclo = tle.inclination
    private val nodeo = tle.rightAscension
    private val argpo = tle.argumentOfPerigee
    private val mo = tle.meanAnomaly
    private val bstar = tle.bstar

    private val noUnkozai: Double
    private val ao: Double
    private val isimp: Boolean
    private val con41: Double
    private val cc1: Double
    private val cc4: Double
    private val cc5: Double
    private val d2: Double
    private val d3: Double
    private val d4: Double
    private val delmo: Double
    private val eta: Double
    private val argpdot: Double
    private val omgcof: Double
    private val sinmao: Double
    private val t2cof: Double
    private val t3cof: Double
    private val t4cof: Double
    private val t5cof: Double
    private val x1mth2: Double
    private val x7thm1: Double
    private val mdot: Double
    private val nodedot: Double
    private val xlcof: Double
    private val xmcof: Double
    private val nodecf: Double
    private val aycof: Double

    init {
        // initl: recover the original mean motion and semi-major axis from the Kozai mean motion.
        val eccsq = ecco * ecco
        val omeosq = 1.0 - eccsq
        val rteosq = sqrt(omeosq)
        val cosio = cos(inclo)
        val cosio2 = cosio * cosio
        val ak = (XKE / tle.meanMotion).pow(X2O3)
        val d1 = 0.75 * J2 * (3.0 * cosio2 - 1.0) / (rteosq * omeosq)
        var del = d1 / (ak * ak)
        val adel = ak * (1.0 - del * del - del * (1.0 / 3.0 + 134.0 * del * del / 81.0))
        del = d1 / (adel * adel)
        noUnkozai = tle.meanMotion / (1.0 + del)
        require(2.0 * PI / noUnkozai < 225.0) { "Deep-space orbits are not supported" }

        ao = (XKE / noUnkozai).pow(X2O3)
        val sinio = sin(inclo)
        val po = ao * omeosq
        val con42 = 1.0 - 5.0 * cosio2
        con41 = -con42 - cosio2 - cosio2
        val posq = po * po
        val rp = ao * (1.0 - ecco)

        // sgp4init
        val ss = 78.0 / RADIUS_EARTH_KM + 1.0
        val qzms2t = ((120.0 - 78.0) / RADIUS_EARTH_KM).pow(4)
        isimp = rp < 220.0 / RADIUS_EARTH_KM + 1.0
        var sfour = ss
        var qzms24 = qzms2t
        val perige = (rp - 1.0) * RADIUS_EARTH_KM
        if (perige < 156.0) {
            sfour = if (perige < 98.0) 20.0 else perige - 78.0
            qzms24 = ((120.0 - sfour) / RADIUS_EARTH_KM).pow(4)
            sfour = sfour / RADIUS_EARTH_KM + 1.0
        }
        val pinvsq = 1.0 / posq
        val tsi = 1.0 / (ao - sfour)
        eta = ao * ecco * tsi
        val etasq = eta * eta
        val eeta = ecco * eta
        val psisq = abs(1.0 - etasq)
        val coef = qzms24 * tsi.pow(4)
        val coef1 = coef / psisq.pow(3.5)
        val cc2 = coef1 * noUnkozai * (ao * (1.0 + 1.5 * etasq + eeta * (4.0 + etasq)) +
            0.375 * J2 * tsi / psisq * con41 * (8.0 + 3.0 * etasq * (8.0 + etasq)))
        cc1 = bstar * cc2
        val cc3 = if (ecco > 1.0e-4) -2.0 * coef * tsi * J3OJ2 * noUnkozai * sinio / ecco else 0.0
        x1mth2 = 1.0 - cosio2
        cc4 = 2.0 * noUnkozai * coef1 * ao * omeosq * (eta * (2.0 + 0.5 * etasq) + ecco * (0.5 + 2.0 * etasq) -
            J2 * tsi / (ao * psisq) * (-3.0 * con41 * (1.0 - 2.0 * eeta + etasq * (1.5 - 0.5 * eeta)) +
                0.75 * x1mth2 * (2.0 * etasq - eeta * (1.0 + etasq)) * cos(2.0 * argpo)))
        cc5 = 2.0 * coef1 * ao * omeosq * (1.0 + 2.75 * (etasq + eeta) + eeta * etasq)
        val cosio4 = cosio2 * cosio2
        val temp1 = 1.5 * J2 * pinvsq * noUnkozai
        val temp2 = 0.5 * temp1 * J2 * pinvsq
        val temp3 = -0.46875 * J4 * pinvsq * pinvsq * noUnkozai
        mdot = noUnkozai + 0.5 * temp1 * rteosq * con41 +
            0.0625 * temp2 * rteosq * (13.0 - 78.0 * cosio2 + 137.0 * cosio4)
        argpdot = -0.5 * temp1 * con42 + 0.0625 * temp2 * (7.0 - 114.0 * cosio2 + 395.0 * cosio4) +
            temp3 * (3.0 - 36.0 * cosio2 + 49.0 * cosio4)
        val xhdot1 = -temp1 * cosio
        nodedot = xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * cosio2) + 2.0 * temp3 * (3.0 - 7.0 * cosio2)) * cosio
        omgcof = bstar * cc3 * cos(argpo)
        xmcof = if (ecco > 1.0e-4) -X2O3 * coef * bstar / eeta else 0.0
        nodecf = 3.5 * omeosq * xhdot1 * cc1
        t2cof = 1.5 * cc1
        xlcof = if (abs(cosio + 1.0) > 1.5e-12) {
            -0.25 * J3OJ2 * sinio * (3.0 + 5.0 * cosio) / (1.0 + cosio)
        } else {
            -0.25 * J3OJ2 * sinio * (3.0 + 5.0 * cosio) / 1.5e-12
        }
        aycof = -0.5 * J3OJ2 * sinio
        delmo = (1.0 + eta * cos(mo)).pow(3)
        sinmao = sin(mo)
        x7thm1 = 7.0 * cosio2 - 1.0

        if (!isimp) {
            val cc1sq = cc1 * cc1
            d2 = 4.0 * ao * tsi * cc1sq
            val temp = d2 * tsi * cc1 / 3.0
            d3 = (17.0 * ao + sfour) * temp
            d4 = 0.5 * temp * ao * tsi * (221.0 * ao + 31.0 * sfour) * cc1
            t3cof = d2 + 2.0 * cc1sq
            t4cof = 0.25 * (3.0 * d3 + cc1 * (12.0 * d2 + 10.0 * cc1sq))
            t5cof = 0.2 * (3.0 * d4 + 12.0 * cc1 * d3 + 6.0 * d2 * d2 + 15.0 * cc1sq * (2.0 * d2 + cc1sq))
        } else {
            d2 = 0.0; d3 = 0.0; d4 = 0.0; t3cof = 0.0; t4cof = 0.0; t5cof = 0.0
        }
    }

    /** State at [epochMillis] (Unix time); throws if the orbit has decayed or the elements are invalid. */
    fun propagate(epochMillis: Long): TemeState {
        return propagateMinutes((epochMillis - tle.epochMillis) / 60_000.0)
    }

    fun propagateMinutes(tsince: Double): TemeState {
        val xmdf = mo + mdot * tsince
        val argpdf = argpo + argpdot * tsince
        val nodedf = nodeo + nodedot * tsince
        var argpm = argpdf
        var mm = xmdf
        val t2 = tsince * tsince
        var nodem = nodedf + nodecf * t2
        var tempa = 1.0 - cc1 * tsince
        var tempe = bstar * cc4 * tsince
        var templ = t2cof * t2

        if (!isimp) {
            val delomg = omgcof * tsince
            val delm = xmcof * ((1.0 + eta * cos(xmdf)).pow(3) - delmo)
            val temp = delomg + delm
            mm = xmdf + temp
            argpm = argpdf - temp
            val t3 = t2 * tsince
            val t4 = t3 * tsince
            tempa = tempa - d2 * t2 - d3 * t3 - d4 * t4
            tempe += bstar * cc5 * (sin(mm) - sinmao)
            templ = templ + t3cof * t3 + t4 * (t4cof + tsince * t5cof)
        }

        var nm = noUnkozai
        var em = ecco
        val inclm = inclo
        check(nm > 0.0) { "SGP4: mean motion is not positive" }
        val am = (XKE / nm).pow(X2O3) * tempa * tempa
        nm = XKE / am.pow(1.5)
        em -= tempe
        check(em < 1.0 && em >= -0.001) { "SGP4: eccentricity out of range" }
        if (em < 1.0e-6) em = 1.0e-6
        mm += noUnkozai * templ
        var xlm = mm + argpm + nodem
        nodem %= TWO_PI
        argpm %= TWO_PI
        xlm %= TWO_PI
        mm = (xlm - argpm - nodem) % TWO_PI

        val sinim = sin(inclm)
        val cosim = cos(inclm)

        // Long-period periodics.
        val axnl = em * cos(argpm)
        var temp = 1.0 / (am * (1.0 - em * em))
        val aynl = em * sin(argpm) + temp * aycof
        val xl = mm + argpm + nodem + temp * xlcof * axnl

        // Kepler's equation.
        val u = (xl - nodem) % TWO_PI
        var eo1 = u
        var tem5 = 9999.9
        var iteration = 1
        var sineo1 = 0.0
        var coseo1 = 0.0
        while (abs(tem5) >= 1.0e-12 && iteration <= 10) {
            sineo1 = sin(eo1)
            coseo1 = cos(eo1)
            tem5 = 1.0 - coseo1 * axnl - sineo1 * aynl
            tem5 = (u - aynl * coseo1 + axnl * sineo1 - eo1) / tem5
            if (abs(tem5) >= 0.95) tem5 = if (tem5 > 0.0) 0.95 else -0.95
            eo1 += tem5
            iteration++
        }

        // Short-period periodics.
        val ecose = axnl * coseo1 + aynl * sineo1
        val esine = axnl * sineo1 - aynl * coseo1
        val el2 = axnl * axnl + aynl * aynl
        val pl = am * (1.0 - el2)
        check(pl >= 0.0) { "SGP4: semi-latus rectum is negative" }
        val rl = am * (1.0 - ecose)
        val rdotl = sqrt(am) * esine / rl
        val rvdotl = sqrt(pl) / rl
        val betal = sqrt(1.0 - el2)
        temp = esine / (1.0 + betal)
        val sinu = am / rl * (sineo1 - aynl - axnl * temp)
        val cosu = am / rl * (coseo1 - axnl + aynl * temp)
        var su = atan2(sinu, cosu)
        val sin2u = (cosu + cosu) * sinu
        val cos2u = 1.0 - 2.0 * sinu * sinu
        temp = 1.0 / pl
        val temp1 = 0.5 * J2 * temp
        val temp2 = temp1 * temp

        val mrt = rl * (1.0 - 1.5 * temp2 * betal * con41) + 0.5 * temp1 * x1mth2 * cos2u
        su -= 0.25 * temp2 * x7thm1 * sin2u
        val xnode = nodem + 1.5 * temp2 * cosim * sin2u
        val xinc = inclm + 1.5 * temp2 * cosim * sinim * cos2u
        val mvt = rdotl - nm * temp1 * x1mth2 * sin2u / XKE
        val rvdot = rvdotl + nm * temp1 * (x1mth2 * cos2u + 1.5 * con41) / XKE
        check(mrt >= 1.0) { "SGP4: satellite has decayed" }

        val sinsu = sin(su)
        val cossu = cos(su)
        val snod = sin(xnode)
        val cnod = cos(xnode)
        val sini = sin(xinc)
        val cosi = cos(xinc)
        val xmx = -snod * cosi
        val xmy = cnod * cosi
        val ux = xmx * sinsu + cnod * cossu
        val uy = xmy * sinsu + snod * cossu
        val uz = sini * sinsu
        val vx = xmx * cossu - cnod * sinsu
        val vy = xmy * cossu - snod * sinsu
        val vz = sini * cossu

        return TemeState(
            x = mrt * ux * RADIUS_EARTH_KM,
            y = mrt * uy * RADIUS_EARTH_KM,
            z = mrt * uz * RADIUS_EARTH_KM,
            vx = (mvt * ux + rvdot * vx) * VELOCITY_KM_PER_SEC,
            vy = (mvt * uy + rvdot * vy) * VELOCITY_KM_PER_SEC,
            vz = (mvt * uz + rvdot * vz) * VELOCITY_KM_PER_SEC
        )
    }

    private companion object {
        // WGS-72 constants used to generate the published element sets.
        const val MU = 398_600.8
        const val RADIUS_EARTH_KM = 6378.135
        val XKE = 60.0 / sqrt(RADIUS_EARTH_KM * RADIUS_EARTH_KM * RADIUS_EARTH_KM / MU)
        const val J2 = 0.001082616
        const val J3 = -0.00000253881
        const val J4 = -0.00000165597
        const val J3OJ2 = J3 / J2
        const val X2O3 = 2.0 / 3.0
        val VELOCITY_KM_PER_SEC = RADIUS_EARTH_KM * XKE / 60.0
        const val TWO_PI = 2.0 * PI
    }
}

internal const val DEG_TO_RAD = PI / 180.0
internal const val RAD_TO_DEG = 180.0 / PI
internal const val MINUTES_PER_DAY = 1440.0
internal const val MILLIS_PER_DAY = 86_400_000.0
internal const val UNIX_EPOCH_JULIAN_DATE = 2_440_587.5
