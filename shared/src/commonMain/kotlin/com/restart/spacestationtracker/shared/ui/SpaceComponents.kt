package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.shared.passes.PassVisibility

/** Navy-to-blue gradient card used for the most important item on a screen; reads as "space" in both modes. */
@Composable
fun SpaceHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 20.dp,
    /** Defaults to the navy-to-blue hero gradient. */
    background: Brush? = null,
    /** Drawn behind the content across the whole card, such as twinkling stars. */
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.spaceColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(background ?: Brush.linearGradient(colors.heroGradient))
    ) {
        decoration?.invoke(this)
        CompositionLocalProvider(LocalContentColor provides colors.onHero) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** Small rounded label tinted with [color]; [filled] uses a solid background for emphasis. */
@Composable
fun SpacePill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
    contentColor: Color = if (filled) Color.White else color
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (filled) color else color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Accent color for a pass brightness level, from gold (very bright) to slate (very faint). */
@Composable
fun visibilityColor(magnitude: Double): Color {
    val colors = MaterialTheme.spaceColors
    return when (PassVisibility.fromMagnitude(magnitude)) {
        PassVisibility.VERY_BRIGHT -> colors.visibilityVeryBright
        PassVisibility.BRIGHT -> colors.visibilityBright
        PassVisibility.MODERATE -> colors.visibilityModerate
        PassVisibility.FAINT -> colors.visibilityFaint
        PassVisibility.VERY_FAINT -> colors.visibilityVeryFaint
    }
}

/** Rounded square behind an icon, tinted with the icon's color. */
@Composable
fun SpaceIconTile(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** A titled group of rows on one rounded card, the settings layout used across the app. */
@Composable
fun SpaceSectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 1.dp
        ) {
            Column(content = content)
        }
    }
}
