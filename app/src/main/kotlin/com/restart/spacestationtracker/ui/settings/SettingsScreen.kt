package com.restart.spacestationtracker.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.Brightness6

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val settings by viewModel.settingsState.collectAsState()
    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateBottomPadding()
    )

    LazyColumn(
        contentPadding = screenPadding,
    ) {
        item {
            SettingsHeader(title = "Map Settings")
        }
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(vertical = 16.dp),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Units",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "Map Type",
                options = listOf("Normal", "Satellite", "Hybrid", "Terrain"),
                selectedOption = settings.mapType,
                onOptionSelected = viewModel::onMapTypeChanged
            )
        }
        item {
            SettingsHeader(title = "General Settings")
        }
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(top = 16.dp),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Units",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "Units",
                options = listOf("Metric", "Imperial"),
                selectedOption = settings.units,
                onOptionSelected = viewModel::onUnitsChanged
            )
        }
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(vertical = 16.dp),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "Theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "Theme",
                options = listOf("Follow System", "Light", "Dark"),
                selectedOption = settings.theme,
                onOptionSelected = viewModel::onThemeChanged,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlSetting(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingContent?.let {
                    it()
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                )
            }
        },
        supportingContent = {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == option
                    OutlinedButton(
                        onClick = { onOptionSelected(option) },
                        shape = when (index) {
                            0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                            options.lastIndex -> RoundedCornerShape(
                                topEndPercent = 50,
                                bottomEndPercent = 50
                            )

                            else -> RoundedCornerShape(0)
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .offset(x = (-1 * index).dp, y = 0.dp)
                            .zIndex(if (isSelected) 1f else 0f)
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun SwitchSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun SliderSetting(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = range,
                    steps = steps
                )
            }
        }
    )
}
