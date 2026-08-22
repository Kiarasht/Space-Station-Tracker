package com.restart.spacestationtracker.shared.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

internal val IssSatellitePanelColor = Color(0xFF4B7BBE)

internal fun DrawScope.drawIssSatellite(
    center: Offset,
    bodyColor: Color,
    panelColor: Color = IssSatellitePanelColor,
    rotationDegrees: Float = -12f,
    scale: Float = 1f
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        val bodyWidth = 13.dp.toPx() * scale
        val bodyHeight = 9.dp.toPx() * scale
        val panelWidth = 14.dp.toPx() * scale
        val panelHeight = 7.dp.toPx() * scale
        val panelGap = 2.dp.toPx() * scale
        val panelStroke = 1.5.dp.toPx() * scale

        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(center.x - bodyWidth / 2f, center.y - bodyHeight / 2f),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(2.dp.toPx() * scale)
        )
        drawRect(
            color = panelColor,
            topLeft = Offset(
                center.x - bodyWidth / 2f - panelWidth - panelGap,
                center.y - panelHeight / 2f
            ),
            size = Size(panelWidth, panelHeight),
            style = Stroke(width = panelStroke)
        )
        drawRect(
            color = panelColor,
            topLeft = Offset(
                center.x + bodyWidth / 2f + panelGap,
                center.y - panelHeight / 2f
            ),
            size = Size(panelWidth, panelHeight),
            style = Stroke(width = panelStroke)
        )
        drawLine(
            color = bodyColor,
            start = Offset(center.x, center.y - bodyHeight / 2f),
            end = Offset(center.x, center.y - bodyHeight),
            strokeWidth = panelStroke
        )
    }
}
