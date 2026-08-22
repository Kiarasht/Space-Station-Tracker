package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private enum class GuideSymbol {
    Horizon,
    SkyArc,
    HighestPoint,
    Directions
}

@Composable
fun SkyPathVisualGuide(
    youLabel: String,
    horizonLabel: String,
    skyArcLabel: String,
    highestPointLabel: String,
    directionsLabel: String,
    overheadExplanation: String,
    lowerExplanation: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkyPathChart(
                startCompass = "WNW",
                endCompass = "NE",
                maxElevation = 58.0,
                youLabel = youLabel
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            GuideLegendRow(GuideSymbol.Horizon, horizonLabel)
            GuideLegendRow(GuideSymbol.SkyArc, skyArcLabel)
            GuideLegendRow(GuideSymbol.HighestPoint, highestPointLabel)
            GuideLegendRow(GuideSymbol.Directions, directionsLabel)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(overheadExplanation, style = MaterialTheme.typography.bodySmall)
                    Text(lowerExplanation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GuideLegendRow(symbol: GuideSymbol, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GuideSymbolGraphic(symbol)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuideSymbolGraphic(symbol: GuideSymbol) {
    val accent = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier.size(width = 34.dp, height = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (symbol == GuideSymbol.Directions) {
            Text(
                text = "WNW",
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        } else {
            Canvas(modifier = Modifier.size(width = 34.dp, height = 24.dp)) {
                val stroke = 2.dp.toPx()
                when (symbol) {
                    GuideSymbol.Horizon -> drawLine(
                        color = foreground,
                        start = Offset(2.dp.toPx(), size.height / 2f),
                        end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                        strokeWidth = stroke
                    )

                    GuideSymbol.SkyArc -> drawArc(
                        color = foreground,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
                        size = Size(size.width - 4.dp.toPx(), size.height * 1.55f),
                        style = Stroke(width = stroke)
                    )

                    GuideSymbol.HighestPoint -> {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawIssSatellite(
                            center = center,
                            bodyColor = accent,
                            scale = 0.68f
                        )
                    }

                    GuideSymbol.Directions -> Unit
                }
            }
        }
    }
}
