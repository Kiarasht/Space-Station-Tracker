package com.restart.spacestationtracker.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

private const val SKY_PATH_SPARKLE_COUNT = 7

private data class SkyPathSparkle(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float
)

@Composable
internal fun SkyPathCardSparkles(
    seed: Long,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        repeat(SKY_PATH_SPARKLE_COUNT) { index ->
            RandomSkyPathSparkle(
                seed = seed + index * 1_103_515_245L,
                startDelayMillis = index * 220L
            )
        }
    }
}

@Composable
private fun RandomSkyPathSparkle(
    seed: Long,
    startDelayMillis: Long
) {
    val random = remember(seed) { Random(seed) }
    var sparkle by remember(seed) {
        mutableStateOf(randomSkyPathSparkle(random, previous = null))
    }
    val alpha = remember { Animatable(0f) }
    val sparkleColor = Color(0xFFFFD166)

    LaunchedEffect(seed) {
        delay(startDelayMillis)
        while (isActive) {
            sparkle = randomSkyPathSparkle(random, previous = sparkle)
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = LinearEasing)
            )
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1_250, easing = LinearEasing)
            )
            delay((260 + random.nextInt(680)).toLong())
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val pulse = alpha.value
        val center = Offset(
            x = size.width * sparkle.xFraction,
            y = size.height * sparkle.yFraction
        )
        val radius = sparkle.radiusDp.dp.toPx() * (0.75f + pulse * 0.45f)
        val starAlpha = pulse * 0.28f
        val starColor = sparkleColor.copy(alpha = starAlpha)

        drawCircle(
            color = starColor.copy(alpha = starAlpha * 0.45f),
            radius = radius * 1.8f,
            center = center
        )
        drawLine(
            color = starColor,
            start = Offset(center.x - radius * 2.2f, center.y),
            end = Offset(center.x + radius * 2.2f, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = starColor,
            start = Offset(center.x, center.y - radius * 2.2f),
            end = Offset(center.x, center.y + radius * 2.2f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun randomSkyPathSparkle(
    random: Random,
    previous: SkyPathSparkle?
): SkyPathSparkle {
    repeat(8) {
        val candidate = SkyPathSparkle(
            xFraction = 0.06f + random.nextFloat() * 0.88f,
            yFraction = 0.08f + random.nextFloat() * 0.78f,
            radiusDp = 1f + random.nextFloat()
        )
        if (previous == null || distanceSquared(candidate, previous) > 0.025f) {
            return candidate
        }
    }

    return SkyPathSparkle(
        xFraction = 0.06f + random.nextFloat() * 0.88f,
        yFraction = 0.08f + random.nextFloat() * 0.78f,
        radiusDp = 1f + random.nextFloat()
    )
}

private fun distanceSquared(first: SkyPathSparkle, second: SkyPathSparkle): Float {
    val xDistance = first.xFraction - second.xFraction
    val yDistance = first.yFraction - second.yFraction
    return xDistance * xDistance + yDistance * yDistance
}
