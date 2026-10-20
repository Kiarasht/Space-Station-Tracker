package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.overhead_format
import com.restart.spacestationtracker.shared.passes.CompassLabels
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val NightTop = Color(0xFF070C2B)
private val NightMiddle = Color(0xFF111A52)
private val NightBottom = Color(0xFF243B8F)

/** The night-sky gradient behind sky-path charts; also used for the whole next-pass hero card. */
internal val SkyNightBrush = Brush.verticalGradient(listOf(NightTop, NightMiddle, NightBottom))
private val PathStart = Color(0xFF2DD4BF)
private val PathEnd = Color(0xFFFFD84D)

/**
 * Side view of a pass on a night-sky panel: the curve follows the pass's real elevation over time,
 * from where it becomes visible (left) to where it fades (right), with the ISS at its highest point.
 */
@Composable
internal fun SkyPathChart(
    startCompass: String,
    endCompass: String,
    maxElevation: Double,
    elevationTrack: List<Double> = emptyList(),
    /** Azimuths at the same moments as [elevationTrack]; with them the curve follows the real sky path. */
    azimuthTrack: List<Double> = emptyList(),
    youLabel: String,
    modifier: Modifier = Modifier,
    sparkleSeed: Long = 0L,
    /** False when the chart sits on a surface that already has the night-sky gradient. */
    showBackground: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val labelFont = MaterialTheme.typography.labelMedium.fontFamily
    val track = remember(elevationTrack, maxElevation) {
        elevationTrack.takeIf { it.size >= 2 }
            ?: List(24) { index -> sin(PI * index / 23.0) * maxElevation }
    }
    val chartPoints = remember(track, azimuthTrack) {
        skyChartPoints(track, azimuthTrack.takeIf { it.size == track.size } ?: emptyList())
    }
    val compassStyle = TextStyle(
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = labelFont
    )
    val youStyle = TextStyle(
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = labelFont
    )
    val overheadText = stringResource(Res.string.overhead_format, 90)
    val startLabel = remember(startCompass) { CompassLabels.localize(startCompass) }
    val endLabel = remember(endCompass) { CompassLabels.localize(endCompass) }
    val overheadStyle = TextStyle(
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = labelFont
    )
    val peakStyle = TextStyle(
        color = PathEnd,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = labelFont
    )

    Box(
        modifier = if (showBackground) {
            modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SkyNightBrush)
        } else {
            modifier.fillMaxWidth()
        }
    ) {
        if (showBackground) {
            SkyPathCardSparkles(seed = sparkleSeed, modifier = Modifier.matchParentSize())
        }
        // The dome is a true half-circle as wide as the path, so the chart's height follows its width.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (showBackground) 14.dp else 0.dp, vertical = 10.dp)
        ) {
        val canvasHeight = (maxWidth - ChartInset * 2) / 2 + ChartLabelHeight + ChartTopRoom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
        ) {
            val labelHeight = ChartLabelHeight.toPx()
            val baselineY = size.height - labelHeight
            val inset = ChartInset.toPx()
            val left = inset
            val span = size.width - inset * 2f
            val center = Offset(size.width / 2f, baselineY)
            // Straight overhead is the top of the half-circle.
            val skyHeight = span / 2f

            // A faint "sky ceiling": horizon to straight overhead, stretched to the full width so every
            // path fits inside it. It shows at a glance how close to overhead a pass climbs.
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(left, baselineY - skyHeight),
                size = Size(span, skyHeight * 2f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
                )
            )
            // Name the dome's top, unless a near-overhead pass puts its own peak label there.
            if (maxElevation < 70.0) {
                val overhead = textMeasurer.measure(overheadText, overheadStyle)
                drawText(
                    overhead,
                    topLeft = Offset(
                        center.x - overhead.size.width / 2f,
                        baselineY - skyHeight - overhead.size.height - 3.dp.toPx()
                    )
                )
            }
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 1.5.dp.toPx()
            )

            // See skyChartPoints: the sky dome seen from the side, start direction on the left.
            fun pointAt(index: Int): Offset {
                val point = chartPoints[index]
                return Offset(
                    x = center.x + (skyHeight * point.x).toFloat(),
                    y = baselineY - (skyHeight * point.height).toFloat()
                )
            }

            val path = Path().apply {
                val first = pointAt(0)
                moveTo(first.x, first.y)
                for (index in 1 until track.size) {
                    val previous = pointAt(index - 1)
                    val current = pointAt(index)
                    val midX = (previous.x + current.x) / 2f
                    quadraticTo(previous.x, previous.y, midX, (previous.y + current.y) / 2f)
                    if (index == track.size - 1) lineTo(current.x, current.y)
                }
            }
            val pathBrush = Brush.horizontalGradient(
                colors = listOf(PathStart, PathEnd),
                startX = left,
                endX = left + span
            )
            // Soft glow under the path, then the path itself.
            drawPath(path, pathBrush, alpha = 0.25f, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, pathBrush, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            val start = pointAt(0)
            val end = pointAt(track.size - 1)
            drawCircle(PathStart, radius = 5.dp.toPx(), center = start)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = start)
            drawCircle(PathEnd, radius = 4.dp.toPx(), center = end)

            val peakIndex = track.indices.maxBy { track[it] }
            val peak = pointAt(peakIndex)
            drawCircle(PathEnd.copy(alpha = 0.22f), radius = 18.dp.toPx(), center = peak)
            drawIssSatellite(center = peak, bodyColor = PathEnd, panelColor = Color(0xFF9FB4FF))

            val peakText = textMeasurer.measure("${maxElevation.roundToInt()}°", peakStyle)
            val peakLabelY = (peak.y - 24.dp.toPx() - peakText.size.height).coerceAtLeast(0f)
            drawText(
                peakText,
                topLeft = Offset(
                    (peak.x - peakText.size.width / 2f).coerceIn(0f, size.width - peakText.size.width),
                    peakLabelY
                )
            )

            val startText = textMeasurer.measure(startLabel, compassStyle)
            val endText = textMeasurer.measure(endLabel, compassStyle)
            drawText(startText, topLeft = Offset(0f, baselineY + 4.dp.toPx()))
            drawText(endText, topLeft = Offset(size.width - endText.size.width, baselineY + 4.dp.toPx()))

            // The observer: a small marker on the horizon, so no path can run into it.
            drawCircle(Color.White.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = center)
            drawCircle(Color.White, radius = 3.5.dp.toPx(), center = center)
            // "You" sits under the horizon so even a low pass never runs into it.
            val youText = textMeasurer.measure(youLabel, youStyle)
            drawText(youText, topLeft = Offset(center.x - youText.size.width / 2f, baselineY + 4.dp.toPx()))
        }
        }
    }
}

private val ChartInset = 8.dp
private val ChartLabelHeight = 20.dp
// Room above the dome's top for the "Overhead" label.
private val ChartTopRoom = 22.dp

/** Where a pass sample sits on the chart: [x] from -1 (start side) to 1 (end side), [height] from 0 to 1. */
internal data class SkyChartPoint(val x: Double, val height: Double)

/**
 * The pass on the sky dome seen from the side, looking across the line from where it becomes visible to
 * where it fades (that line runs left to right). Each sample's left-right position comes from its real
 * direction along that line, so the curve has the shape of the actual path. Low passes are common, so
 * heights are eased upward to stay readable (12° draws about a third of the way up, overhead at the top);
 * horizontal reach shrinks with height to match, keeping every point inside the half-circle.
 *
 * Without [azimuths] (one per elevation) it falls back to spacing samples evenly in time.
 */
internal fun skyChartPoints(elevations: List<Double>, azimuths: List<Double>): List<SkyChartPoint> {
    if (elevations.isEmpty()) return emptyList()
    val heights = elevations.map { (it.coerceIn(0.0, 90.0) / 90.0).pow(0.55) }
    val reach = heights.map { sqrt(1.0 - it * it) }

    if (azimuths.size == elevations.size && azimuths.size >= 2) {
        // Ground-plane positions of the first and last samples set the left-right axis.
        fun ground(index: Int): Pair<Double, Double> {
            val azimuth = azimuths[index] * PI / 180.0
            val distance = cos(elevations[index].coerceIn(0.0, 90.0) * PI / 180.0)
            return distance * sin(azimuth) to distance * cos(azimuth)
        }
        val (startEast, startNorth) = ground(0)
        val (endEast, endNorth) = ground(azimuths.lastIndex)
        val axisEast = endEast - startEast
        val axisNorth = endNorth - startNorth
        val length = sqrt(axisEast * axisEast + axisNorth * axisNorth)
        if (length > 1e-6) {
            return elevations.indices.map { index ->
                val azimuth = azimuths[index] * PI / 180.0
                val along = (sin(azimuth) * axisEast + cos(azimuth) * axisNorth) / length
                SkyChartPoint(x = along * reach[index], height = heights[index])
            }
        }
    }

    return elevations.indices.map { index ->
        val fraction = if (elevations.size > 1) index.toDouble() / elevations.lastIndex else 0.5
        SkyChartPoint(x = cos(PI * (1.0 - fraction)) * reach[index], height = heights[index])
    }
}
