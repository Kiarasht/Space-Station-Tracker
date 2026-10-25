package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.shared.passes.CompassLabels
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.got_it
import com.restart.spacestationtracker.shared.resources.nav_sky_path
import com.restart.spacestationtracker.shared.resources.sky_path_info_arc
import com.restart.spacestationtracker.shared.resources.sky_path_info_colors
import com.restart.spacestationtracker.shared.resources.sky_path_info_horizon
import com.restart.spacestationtracker.shared.resources.sky_path_info_iss_icon
import com.restart.spacestationtracker.shared.resources.sky_path_info_labels
import com.restart.spacestationtracker.shared.resources.sky_path_info_shadow
import com.restart.spacestationtracker.shared.resources.you
import org.jetbrains.compose.resources.stringResource

/** The Sky Path help dialog, shared by both apps. */
@Composable
fun SkyPathInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nav_sky_path)) },
        text = { SkyPathVisualGuide() },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.got_it))
            }
        }
    )
}

private enum class GuideSymbol {
    Horizon,
    Overhead,
    Colors,
    HighestPoint,
    Directions
}

// A typical evening pass, computed like a real one: it rises in the west-northwest, peaks at 47°, then
// fades into Earth's shadow about 20° above the east. It shows every part of the chart without crowding.
private val ExampleElevations = listOf(
    0.0, 4.8, 9.6, 14.4, 19.0, 23.5, 27.8, 31.9, 35.7, 39.1, 42.0, 44.3,
    46.0, 46.8, 46.9, 46.2, 44.7, 42.5, 39.7, 36.5, 32.8, 28.7, 24.5, 20.0
)
private val ExampleAzimuths = listOf(
    292.5, 297.0, 301.6, 306.3, 311.2, 316.4, 322.0, 328.0, 334.6, 341.7, 349.5, 358.0,
    7.1, 16.6, 26.3, 35.9, 45.1, 53.7, 61.7, 69.0, 75.6, 81.7, 87.4, 92.7
)

/** An example chart, a one-line key to each part of it, and the one thing the chart can't show by itself. */
@Composable
private fun SkyPathVisualGuide(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SkyPathChart(
            startCompass = "WNW",
            endCompass = "E",
            maxElevation = 47.0,
            elevationTrack = ExampleElevations,
            azimuthTrack = ExampleAzimuths,
            youLabel = stringResource(Res.string.you),
            sparkleSeed = 7L
        )

        GuideLegendRow(GuideSymbol.Horizon, stringResource(Res.string.sky_path_info_horizon))
        GuideLegendRow(GuideSymbol.Overhead, stringResource(Res.string.sky_path_info_arc))
        GuideLegendRow(GuideSymbol.Colors, stringResource(Res.string.sky_path_info_colors))
        GuideLegendRow(GuideSymbol.HighestPoint, stringResource(Res.string.sky_path_info_iss_icon))
        GuideLegendRow(GuideSymbol.Directions, stringResource(Res.string.sky_path_info_labels))

        Text(
            stringResource(Res.string.sky_path_info_shadow),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuideLegendRow(symbol: GuideSymbol, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GuideSymbolGraphic(symbol)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Each symbol is drawn the way the chart draws that part. */
@Composable
private fun GuideSymbolGraphic(symbol: GuideSymbol) {
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        // Wide enough for written-out directions such as Arabic's.
        modifier = Modifier.widthIn(min = 34.dp).height(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (symbol == GuideSymbol.Directions) {
            Text(
                text = CompassLabels.localize("WNW"),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall,
                color = foreground
            )
        } else {
            Canvas(modifier = Modifier.size(width = 34.dp, height = 24.dp)) {
                val stroke = 2.dp.toPx()
                val middle = size.height / 2f
                when (symbol) {
                    // The horizon, with you as the dot in the middle.
                    GuideSymbol.Horizon -> {
                        drawLine(
                            color = foreground,
                            start = Offset(2.dp.toPx(), middle),
                            end = Offset(size.width - 2.dp.toPx(), middle),
                            strokeWidth = stroke
                        )
                        drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(size.width / 2f, middle))
                    }

                    GuideSymbol.Overhead -> drawArc(
                        color = foreground,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
                        size = Size(size.width - 4.dp.toPx(), size.height * 1.55f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                        )
                    )

                    GuideSymbol.Colors -> {
                        val start = Offset(4.dp.toPx(), middle)
                        val end = Offset(size.width - 4.dp.toPx(), middle)
                        drawLine(
                            brush = Brush.horizontalGradient(listOf(SkyPathStart, SkyPathEnd), start.x, end.x),
                            start = start,
                            end = end,
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(SkyPathStart, radius = 4.dp.toPx(), center = start)
                        drawCircle(Color.White, radius = 1.5.dp.toPx(), center = start)
                        drawCircle(SkyPathEnd, radius = 3.dp.toPx(), center = end)
                    }

                    GuideSymbol.HighestPoint -> drawIssSatellite(
                        center = Offset(size.width / 2f, middle),
                        bodyColor = SkyPathEnd,
                        panelColor = Color(0xFF9FB4FF),
                        scale = 0.68f
                    )

                    GuideSymbol.Directions -> Unit
                }
            }
        }
    }
}
