package com.restart.spacestationtracker.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One destination in [SpaceFloatingNavBar]. */
@Immutable
data class SpaceNavItem(
    val key: String,
    val label: String,
    val icon: Painter
)

/**
 * The bottom area both apps share: an optional banner ad above a floating, pill-shaped navigation
 * bar. Screens draw underneath it and add its measured height to their bottom padding.
 */
@Composable
fun SpaceBottomChrome(
    modifier: Modifier = Modifier,
    banner: (@Composable () -> Unit)? = null,
    navigation: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (banner != null) {
            banner()
            Spacer(Modifier.height(10.dp))
        }
        navigation()
    }
}

/** Floating pill navigation: the selected tab is tinted with the accent color on a soft capsule. */
@Composable
fun SpaceFloatingNavBar(
    items: List<SpaceNavItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.spaceColors
    Surface(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(0.9f),
        shape = RoundedCornerShape(percent = 50),
        color = colors.navBarContainer,
        contentColor = colors.navBarUnselected,
        border = BorderStroke(1.dp, colors.navBarBorder),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                SpaceNavBarItem(
                    item = item,
                    selected = item.key == selectedKey,
                    onClick = { onSelect(item.key) },
                    selectedColor = colors.navBarSelected,
                    unselectedColor = colors.navBarUnselected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpaceNavBarItem(
    item: SpaceNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(if (selected) selectedColor else unselectedColor)
    val capsuleColor by animateColorAsState(
        if (selected) selectedColor.copy(alpha = 0.16f) else Color.Transparent
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // The selected capsule is the only feedback; no ripple.
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(capsuleColor)
                .padding(horizontal = 14.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = item.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
