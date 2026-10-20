package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.shared.passes.CompassLabels
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.add_to_calendar
import com.restart.spacestationtracker.shared.resources.countdown_days_format
import com.restart.spacestationtracker.shared.resources.countdown_hours_format
import com.restart.spacestationtracker.shared.resources.countdown_minutes_format
import com.restart.spacestationtracker.shared.resources.duration_min_sec_format
import com.restart.spacestationtracker.shared.resources.later_passes
import com.restart.spacestationtracker.shared.resources.next_visible_pass
import com.restart.spacestationtracker.shared.resources.peak_elevation_format
import com.restart.spacestationtracker.shared.resources.schedule_notification
import com.restart.spacestationtracker.shared.resources.share
import com.restart.spacestationtracker.shared.resources.visible_now
import com.restart.spacestationtracker.shared.resources.you
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** "Sun, Oct 19" in the device's language. */
internal fun formatPassDate(epochMillis: Long): String = formatLocalizedDate(epochMillis, "EEEMMMd")

/** "6:48 AM", or "06:48" where 24-hour time is the norm. */
internal fun formatPassTime(epochMillis: Long): String = formatLocalizedDate(epochMillis, "jmm")

/** The next visible pass, with a live countdown, on the gradient hero card. */
@Composable
fun SharedNextPassHero(
    pass: IssPass,
    onScheduleNotification: (IssPass) -> Unit,
    onAddToCalendar: (IssPass) -> Unit,
    onShare: (IssPass) -> Unit,
    modifier: Modifier = Modifier,
    dateLabel: String = formatPassDate(pass.startTimeMillis),
    timeLabel: String = formatPassTime(pass.startTimeMillis)
) {
    var nowMillis by remember { mutableLongStateOf(currentTimeMillis()) }
    LaunchedEffect(pass.startTimeMillis) {
        while (true) {
            nowMillis = currentTimeMillis()
            delay(30_000)
        }
    }
    val heroText = MaterialTheme.spaceColors.onHero
    // One night sky across the whole card, with the chart drawn straight onto it.
    SpaceHeroCard(
        modifier = modifier,
        background = SkyNightBrush,
        decoration = {
            SkyPathCardSparkles(seed = pass.startTimeMillis, modifier = Modifier.matchParentSize())
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.next_visible_pass).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = heroText.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            SpacePill(
                text = countdownLabel(pass, nowMillis),
                color = MaterialTheme.spaceColors.gold,
                filled = true,
                contentColor = MaterialTheme.spaceColors.onGold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(timeLabel, style = MaterialTheme.typography.headlineMedium, color = heroText)
            Text(dateLabel, style = MaterialTheme.typography.titleMedium, color = heroText.copy(alpha = 0.85f))
        }
        SkyPathChart(
            startCompass = pass.startAzimuthCompass,
            endCompass = pass.endAzimuthCompass,
            maxElevation = pass.maxElevation,
            elevationTrack = pass.elevationTrack,
            youLabel = stringResource(Res.string.you),
            showBackground = false
        )
        PassFacts(pass = pass, onHero = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PassActions(
                pass = pass,
                onScheduleNotification = onScheduleNotification,
                onAddToCalendar = onAddToCalendar,
                onShare = onShare,
                container = Color.White.copy(alpha = 0.16f),
                content = heroText
            )
        }
    }
}

/** A later pass on the same night-sky card as the hero: date and time, the sky path, and colored facts. */
@Composable
fun SharedIssPassCard(
    pass: IssPass,
    onScheduleNotification: (IssPass) -> Unit,
    onAddToCalendar: (IssPass) -> Unit,
    onShare: (IssPass) -> Unit,
    dateLabel: String = formatPassDate(pass.startTimeMillis),
    timeLabel: String = formatPassTime(pass.startTimeMillis),
    modifier: Modifier = Modifier
) {
    val heroText = MaterialTheme.spaceColors.onHero
    SpaceHeroCard(
        modifier = modifier,
        contentPadding = 16.dp,
        background = SkyNightBrush,
        decoration = {
            SkyPathCardSparkles(seed = pass.startTimeMillis, modifier = Modifier.matchParentSize())
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = heroText
                )
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.spaceColors.gold
                )
            }
            PassActions(
                pass = pass,
                onScheduleNotification = onScheduleNotification,
                onAddToCalendar = onAddToCalendar,
                onShare = onShare,
                container = Color.White.copy(alpha = 0.16f),
                content = heroText
            )
        }
        SkyPathChart(
            startCompass = pass.startAzimuthCompass,
            endCompass = pass.endAzimuthCompass,
            maxElevation = pass.maxElevation,
            elevationTrack = pass.elevationTrack,
            youLabel = stringResource(Res.string.you),
            showBackground = false
        )
        PassFacts(pass = pass, onHero = true)
    }
}

@Composable
fun SkyPathSectionTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.later_passes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassFacts(pass: IssPass, onHero: Boolean) {
    val neutral = if (onHero) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpacePill(
            text = "${CompassLabels.localize(pass.startAzimuthCompass)} → ${CompassLabels.localize(pass.endAzimuthCompass)}",
            color = neutral,
            icon = Icons.Default.Explore
        )
        SpacePill(
            text = stringResource(Res.string.peak_elevation_format, pass.maxElevation.roundToInt()),
            color = neutral,
            icon = Icons.Default.Height
        )
        SpacePill(
            text = stringResource(
                Res.string.duration_min_sec_format,
                pass.durationInSeconds / 60,
                pass.durationInSeconds % 60
            ),
            color = neutral,
            icon = Icons.Default.Timer
        )
        SpacePill(
            text = visibilityLabel(pass.magnitude),
            color = neutral,
            icon = Icons.Default.Visibility
        )
    }
}

@Composable
private fun PassActions(
    pass: IssPass,
    onScheduleNotification: (IssPass) -> Unit,
    onAddToCalendar: (IssPass) -> Unit,
    onShare: (IssPass) -> Unit,
    container: Color,
    content: Color
) {
    val colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = container,
        contentColor = content
    )
    PassActionButton(Icons.Default.Notifications, stringResource(Res.string.schedule_notification), colors) {
        onScheduleNotification(pass)
    }
    Spacer(Modifier.size(6.dp))
    PassActionButton(Icons.Default.CalendarMonth, stringResource(Res.string.add_to_calendar), colors) {
        onAddToCalendar(pass)
    }
    Spacer(Modifier.size(6.dp))
    PassActionButton(Icons.Default.Share, stringResource(Res.string.share), colors) {
        onShare(pass)
    }
}

@Composable
private fun PassActionButton(
    icon: ImageVector,
    label: String,
    colors: androidx.compose.material3.IconButtonColors,
    onClick: () -> Unit
) {
    FilledTonalIconButton(onClick = onClick, colors = colors, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun countdownLabel(pass: IssPass, nowMillis: Long): String {
    val untilStart = pass.startTimeMillis - nowMillis
    val endMillis = pass.startTimeMillis + pass.durationInSeconds * 1_000L
    if (untilStart <= 0 && nowMillis < endMillis) return stringResource(Res.string.visible_now)
    val totalMinutes = (untilStart.coerceAtLeast(0) / 60_000).toInt()
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> stringResource(Res.string.countdown_days_format, days, hours)
        hours > 0 -> stringResource(Res.string.countdown_hours_format, hours, minutes)
        else -> stringResource(Res.string.countdown_minutes_format, minutes.coerceAtLeast(1))
    }
}

private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
