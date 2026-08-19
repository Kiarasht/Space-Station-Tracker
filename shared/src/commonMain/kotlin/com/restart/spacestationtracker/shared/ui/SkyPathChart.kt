package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun SkyPathChart(
    startCompass: String,
    endCompass: String,
    maxElevation: Double,
    youLabel: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val foreground = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val muted = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(
        color = foreground,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = androidx.compose.material3.MaterialTheme.typography.labelMedium.fontFamily
    )
    val youStyle = TextStyle(
        color = muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontFamily
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .aspectRatio(1.9f)
    ) {
        val labelHeight = 22.dp.toPx()
        val baselineY = size.height - labelHeight
        val radius = min(size.width * 0.43f, baselineY * 0.86f)
        val center = Offset(size.width / 2f, baselineY)
        val stroke = 2.dp.toPx()

        listOf(
            Offset(size.width * 0.12f, size.height * 0.16f),
            Offset(size.width * 0.28f, size.height * 0.32f),
            Offset(size.width * 0.72f, size.height * 0.14f),
            Offset(size.width * 0.88f, size.height * 0.38f),
            Offset(size.width * 0.58f, size.height * 0.24f)
        ).forEachIndexed { index, star ->
            drawCircle(
                color = if (index % 2 == 0) accent.copy(alpha = 0.28f) else muted.copy(alpha = 0.22f),
                radius = if (index % 2 == 0) 1.8.dp.toPx() else 1.2.dp.toPx(),
                center = star
            )
        }

        drawLine(
            color = muted.copy(alpha = 0.65f),
            start = Offset(center.x - radius, baselineY),
            end = Offset(center.x + radius, baselineY),
            strokeWidth = stroke
        )
        drawArc(
            color = foreground,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke)
        )

        val elevation = maxElevation.coerceIn(0.0, 90.0)
        val angle = PI + elevation * PI / 180.0
        val satellite = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat()
        )
        drawLine(
            color = accent.copy(alpha = 0.48f),
            start = center,
            end = satellite,
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 7.dp.toPx()))
        )

        val startText = textMeasurer.measure(startCompass, labelStyle)
        val endText = textMeasurer.measure(endCompass, labelStyle)
        drawText(startText, topLeft = Offset(center.x - radius, baselineY + 4.dp.toPx()))
        drawText(
            endText,
            topLeft = Offset(center.x + radius - endText.size.width, baselineY + 4.dp.toPx())
        )

        val personY = baselineY - 20.dp.toPx()
        drawCircle(color = foreground, radius = 4.dp.toPx(), center = Offset(center.x, personY - 11.dp.toPx()))
        drawLine(foreground, Offset(center.x, personY - 7.dp.toPx()), Offset(center.x, personY + 7.dp.toPx()), 2.dp.toPx())
        drawLine(foreground, Offset(center.x, personY - 1.dp.toPx()), Offset(center.x - 7.dp.toPx(), personY + 4.dp.toPx()), 2.dp.toPx())
        drawLine(foreground, Offset(center.x, personY - 1.dp.toPx()), Offset(center.x + 7.dp.toPx(), personY + 4.dp.toPx()), 2.dp.toPx())
        drawLine(foreground, Offset(center.x, personY + 7.dp.toPx()), Offset(center.x - 5.dp.toPx(), personY + 15.dp.toPx()), 2.dp.toPx())
        drawLine(foreground, Offset(center.x, personY + 7.dp.toPx()), Offset(center.x + 5.dp.toPx(), personY + 15.dp.toPx()), 2.dp.toPx())
        val youText = textMeasurer.measure(youLabel, youStyle)
        drawText(youText, topLeft = Offset(center.x - youText.size.width / 2f, personY - 33.dp.toPx()))

        rotate(degrees = -12f, pivot = satellite) {
            val bodyWidth = 13.dp.toPx()
            val bodyHeight = 9.dp.toPx()
            drawRoundRect(
                color = accent,
                topLeft = Offset(satellite.x - bodyWidth / 2f, satellite.y - bodyHeight / 2f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            val panelWidth = 14.dp.toPx()
            val panelHeight = 7.dp.toPx()
            drawRect(
                color = Color(0xFF4B7BBE),
                topLeft = Offset(satellite.x - bodyWidth / 2f - panelWidth - 2.dp.toPx(), satellite.y - panelHeight / 2f),
                size = Size(panelWidth, panelHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawRect(
                color = Color(0xFF4B7BBE),
                topLeft = Offset(satellite.x + bodyWidth / 2f + 2.dp.toPx(), satellite.y - panelHeight / 2f),
                size = Size(panelWidth, panelHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawLine(
                color = accent,
                start = Offset(satellite.x, satellite.y - bodyHeight / 2f),
                end = Offset(satellite.x, satellite.y - bodyHeight),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
