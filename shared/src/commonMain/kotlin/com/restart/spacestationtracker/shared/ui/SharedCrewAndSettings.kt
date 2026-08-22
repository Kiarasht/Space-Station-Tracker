package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.shared.passes.PassVisibility
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.*
import com.restart.spacestationtracker.shared.settings.AppSettings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

sealed interface SharedCrewItem {
    data class ExpeditionItem(val expedition: Expedition) : SharedCrewItem
    data class AstronautItem(val astronaut: Astronaut) : SharedCrewItem
    data class PlatformSlot(val id: String) : SharedCrewItem
}

@Composable
fun SharedCrewScreen(
    items: List<SharedCrewItem>,
    isLoading: Boolean,
    error: String?,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit,
    platformSlot: @Composable (String) -> Unit = {}
) {
    when {
        isLoading -> LoadingScreen(contentPadding)
        error != null || items.isEmpty() -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SentimentDissatisfied,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    stringResource(if (error != null) Res.string.unable_to_load_crew else Res.string.no_crew_data_available),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(if (error != null) Res.string.unable_to_load_crew_message else Res.string.no_crew_data_available_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.try_again))
                }
            }
        }
        else -> BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (maxWidth < 600.dp) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items, key = ::crewItemKey) { item ->
                        SharedCrewFeedItem(item, onOpenUrl, platformSlot)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalItemSpacing = 16.dp
                ) {
                    items(items, key = ::crewItemKey) { item ->
                        SharedCrewFeedItem(item, onOpenUrl, platformSlot)
                    }
                }
            }
        }
    }
}

private fun crewItemKey(item: SharedCrewItem): String = when (item) {
    is SharedCrewItem.ExpeditionItem -> "expedition-${item.expedition.number}"
    is SharedCrewItem.AstronautItem -> "astronaut-${item.astronaut.name}"
    is SharedCrewItem.PlatformSlot -> item.id
}

@Composable
private fun SharedCrewFeedItem(
    item: SharedCrewItem,
    onOpenUrl: (String) -> Unit,
    platformSlot: @Composable (String) -> Unit
) {
    when (item) {
        is SharedCrewItem.ExpeditionItem -> SharedExpeditionCard(item.expedition, onOpenUrl)
        is SharedCrewItem.AstronautItem -> SharedAstronautCard(item.astronaut, onOpenUrl)
        is SharedCrewItem.PlatformSlot -> platformSlot(item.id)
    }
}

@Composable
private fun SharedExpeditionCard(expedition: Expedition, onOpenUrl: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = expedition.url.isNotBlank()) {
            onOpenUrl(expedition.url)
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = wikimediaThumbnailUrl(expedition.imageUrl, widthPx = 960),
                contentDescription = stringResource(Res.string.expedition_format, expedition.number),
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = wikimediaThumbnailUrl(expedition.patchUrl, widthPx = 250),
                        contentDescription = stringResource(Res.string.expedition_patch),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(Res.string.expedition_format, expedition.number),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (expedition.startDate > 0) {
                    Text(
                        stringResource(Res.string.launched_format, formatEpochSeconds(expedition.startDate)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (expedition.endDate > 0) {
                    Text(
                        stringResource(Res.string.returns_format, formatEpochSeconds(expedition.endDate)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (expedition.bio.isNotBlank()) {
                    Text(expedition.bio, style = MaterialTheme.typography.bodySmall)
                }
                CrewSocialLinks(
                    links = buildList {
                        add(
                            CrewSocialLink(
                                label = "NASA",
                                url = "https://www.nasa.gov/mission/expedition-${expedition.number}/",
                                icon = Res.drawable.ic_social_nasa,
                                wide = true,
                                tintWithTheme = true
                            )
                        )
                        if (expedition.url.isNotBlank()) {
                            add(
                                CrewSocialLink(
                                    "Wikipedia",
                                    expedition.url,
                                    Res.drawable.ic_wikipedia_social_icon
                                )
                            )
                        }
                        add(
                            CrewSocialLink(
                                label = "Google",
                                url = googleSearchUrl("Expedition ${expedition.number}"),
                                icon = Res.drawable.ic_google_social_icon
                            )
                        )
                    },
                    onOpenUrl = onOpenUrl
                )
            }
        }
    }
}

@Composable
private fun SharedAstronautCard(astronaut: Astronaut, onOpenUrl: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = wikimediaThumbnailUrl(astronaut.profileImageUrl, widthPx = 960),
                contentDescription = astronaut.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://flagcdn.com/w80/${astronaut.flagCode.lowercase()}.png",
                        contentDescription = null,
                        modifier = Modifier
                            .width(40.dp)
                            .height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        astronaut.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    stringResource(Res.string.astronaut_role_format, astronaut.role, astronaut.craft),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (astronaut.launchDate > 0) {
                    Text(
                        stringResource(Res.string.launched_format, formatEpochSeconds(astronaut.launchDate)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (astronaut.bio.isNotBlank()) {
                    ExpandableAstronautBio(
                        astronautName = astronaut.name,
                        bio = astronaut.bio
                    )
                }
                CrewSocialLinks(
                    links = buildList {
                        astronaut.instagramUrl?.takeIf(String::isNotBlank)?.let {
                            add(CrewSocialLink("Instagram", it, Res.drawable.ic_instagram_social_icon))
                        }
                        astronaut.facebookUrl?.takeIf(String::isNotBlank)?.let {
                            add(CrewSocialLink("Facebook", it, Res.drawable.ic_facebook_social_icon))
                        }
                        add(
                            CrewSocialLink(
                                label = "Google",
                                url = googleSearchUrl(astronaut.name),
                                icon = Res.drawable.ic_google_social_icon
                            )
                        )
                        astronaut.twitterUrl?.takeIf(String::isNotBlank)?.let {
                            add(
                                CrewSocialLink(
                                    "X",
                                    it,
                                    Res.drawable.ic_x_social_icon,
                                    tintWithTheme = true
                                )
                            )
                        }
                        if (astronaut.bioUrl.isNotBlank()) {
                            add(
                                CrewSocialLink(
                                    "Wikipedia",
                                    astronaut.bioUrl,
                                    Res.drawable.ic_wikipedia_social_icon
                                )
                            )
                        }
                    },
                    onOpenUrl = onOpenUrl
                )
            }
        }
    }
}

@Composable
private fun ExpandableAstronautBio(
    astronautName: String,
    bio: String
) {
    var isExpanded by rememberSaveable(astronautName) { mutableStateOf(false) }
    var hasOverflow by remember(bio) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = bio,
            style = MaterialTheme.typography.bodySmall,
            maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_BIO_MAX_LINES,
            overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                hasOverflow = if (isExpanded) {
                    layoutResult.lineCount > COLLAPSED_BIO_MAX_LINES
                } else {
                    layoutResult.hasVisualOverflow
                }
            }
        )
        if (hasOverflow) {
            Text(
                text = stringResource(
                    if (isExpanded) Res.string.read_less else Res.string.read_more
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private const val COLLAPSED_BIO_MAX_LINES = 5

private data class CrewSocialLink(
    val label: String,
    val url: String,
    val icon: DrawableResource,
    val wide: Boolean = false,
    val tintWithTheme: Boolean = false
)

private fun wikimediaThumbnailUrl(url: String, widthPx: Int): String {
    val commonsPathMarker = "/wikipedia/commons/"
    if (!url.startsWith("https://upload.wikimedia.org") ||
        "/wikipedia/commons/thumb/" in url
    ) {
        return url
    }

    val commonsPathStart = url.indexOf(commonsPathMarker)
    if (commonsPathStart < 0) return url

    val encodedFilePath = url.substring(commonsPathStart + commonsPathMarker.length)
    val encodedFileName = encodedFilePath.substringAfterLast('/')
    if (encodedFileName.isBlank()) return url

    val thumbnailName = if (encodedFileName.endsWith(".svg", ignoreCase = true)) {
        "${widthPx}px-$encodedFileName.png"
    } else {
        "${widthPx}px-$encodedFileName"
    }
    return "https://upload.wikimedia.org/wikipedia/commons/thumb/" +
        "$encodedFilePath/$thumbnailName"
}

@Composable
private fun CrewSocialLinks(
    links: List<CrewSocialLink>,
    onOpenUrl: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        links.forEach { link ->
            IconButton(
                onClick = { onOpenUrl(link.url) },
                modifier = if (link.wide) {
                    Modifier.width(104.dp).height(48.dp)
                } else {
                    Modifier.size(48.dp)
                }
            ) {
                val iconModifier = if (link.wide) {
                    Modifier.width(104.dp).height(32.dp)
                } else {
                    Modifier.size(32.dp)
                }
                if (link.tintWithTheme) {
                    Icon(
                        painter = painterResource(link.icon),
                        contentDescription = link.label,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = iconModifier
                    )
                } else {
                    Image(
                        painter = painterResource(link.icon),
                        contentDescription = link.label,
                        modifier = iconModifier
                    )
                }
            }
        }
    }
}

private fun googleSearchUrl(query: String): String {
    return "https://www.google.com/search?q=${query.trim().replace(" ", "%20")}"
}

data class SharedSettingsPlatformState(
    val hasNotificationPermission: Boolean = true,
    val hasLocationPermission: Boolean = true,
    val isBackgroundUnrestricted: Boolean = true,
    val isLocationLookupInProgress: Boolean = false,
    val showPrivacyChoices: Boolean = false,
    val adsAvailable: Boolean = false,
    val isAdFree: Boolean = false,
    val purchasePriceText: String = "$9.99",
    val isPurchaseInProgress: Boolean = false,
    val isPurchaseAvailable: Boolean = true,
    val purchaseStatusCode: String? = null
)

@Composable
fun SharedFullSettingsScreen(
    settings: AppSettings,
    platformState: SharedSettingsPlatformState,
    contentPadding: PaddingValues,
    onAutomaticAlertsChanged: (Boolean) -> Unit,
    onMinVisibilityChanged: (String) -> Unit,
    onNotificationTimesChanged: (Set<String>) -> Unit,
    onUpdateLocation: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onMapTypeChanged: (String) -> Unit,
    onShowOrbitChanged: (Boolean) -> Unit,
    onThemeChanged: (String) -> Unit,
    onPrivacyChoices: () -> Unit
) {
    val hasLocation = settings.automaticPassAlertLatitude != null && settings.automaticPassAlertLongitude != null
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding)) {
        item { SettingsSection(stringResource(Res.string.settings_iss_pass_alerts)) }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.NotificationsActive, null) },
                headlineContent = { Text(stringResource(Res.string.automatic_good_pass_alerts)) },
                supportingContent = {
                    Text(
                        if (platformState.isLocationLookupInProgress) stringResource(Res.string.current_location)
                        else settings.automaticPassAlertLocationName ?: stringResource(Res.string.automatic_good_pass_alerts_description)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.automaticPassAlertsEnabled,
                        enabled = !platformState.isLocationLookupInProgress,
                        onCheckedChange = onAutomaticAlertsChanged
                    )
                }
            )
        }
        item {
            StatusListItem(
                title = stringResource(Res.string.alert_health),
                lines = listOf(
                    platformState.hasNotificationPermission to stringResource(if (platformState.hasNotificationPermission) Res.string.notifications_allowed else Res.string.notifications_need_permission),
                    platformState.hasLocationPermission to stringResource(if (platformState.hasLocationPermission) Res.string.location_permission_allowed else Res.string.location_permission_needed),
                    hasLocation to stringResource(if (hasLocation) Res.string.alert_location_saved else Res.string.alert_location_needs_update),
                    platformState.isBackgroundUnrestricted to stringResource(if (platformState.isBackgroundUnrestricted) Res.string.battery_unrestricted else Res.string.battery_may_pause_alerts)
                )
            )
        }
        if (settings.automaticPassAlertsEnabled) {
            item {
                SharedChoiceSetting(
                    title = stringResource(Res.string.minimum_visibility),
                    selected = settings.automaticPassAlertMinVisibility,
                    options = listOf(
                        "Faint" to stringResource(Res.string.visibility_faint),
                        "Moderate" to stringResource(Res.string.visibility_moderate),
                        "Bright" to stringResource(Res.string.visibility_bright),
                        "Very Bright" to stringResource(Res.string.visibility_very_bright)
                    ),
                    onSelected = onMinVisibilityChanged
                )
            }
            item {
                NotificationTimes(
                    selected = settings.automaticPassAlertNotificationTimes,
                    onChanged = onNotificationTimesChanged
                )
            }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                    headlineContent = { Text(stringResource(Res.string.alert_location)) },
                    supportingContent = { Text(settings.automaticPassAlertLocationName ?: stringResource(Res.string.no_location_saved)) },
                    trailingContent = { OutlinedButton(onClick = onUpdateLocation) { Text(stringResource(Res.string.update)) } }
                )
            }
            item {
                ListItem(
                    leadingContent = { Icon(if (platformState.isBackgroundUnrestricted) Icons.Default.CheckCircle else Icons.Default.Warning, null) },
                    headlineContent = { Text(stringResource(Res.string.notification_reliability)) },
                    supportingContent = { Text(stringResource(if (platformState.isBackgroundUnrestricted) Res.string.battery_optimization_off else Res.string.battery_may_pause_alerts_description)) },
                    trailingContent = if (platformState.isBackgroundUnrestricted) null else {
                        { OutlinedButton(onClick = onOpenBackgroundSettings) { Text(stringResource(Res.string.open)) } }
                    }
                )
            }
        }
        item { HorizontalDivider(); SettingsSection(stringResource(Res.string.settings_map_settings)) }
        item {
            SharedChoiceSetting(
                stringResource(Res.string.map_type),
                settings.mapType,
                listOf(
                    "Normal" to stringResource(Res.string.map_type_normal),
                    "Satellite" to stringResource(Res.string.map_type_satellite),
                    "Hybrid" to stringResource(Res.string.map_type_hybrid),
                    "Terrain" to stringResource(Res.string.map_type_terrain)
                ),
                onMapTypeChanged
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.show_orbit_path)) },
                supportingContent = { Text(stringResource(Res.string.show_orbit_path_description)) },
                trailingContent = { Switch(settings.showOrbit, onShowOrbitChanged) }
            )
        }
        item { HorizontalDivider(); SettingsSection(stringResource(Res.string.settings_general_settings)) }
        item {
            SharedChoiceSetting(
                stringResource(Res.string.theme),
                settings.theme,
                listOf(
                    "Follow System" to stringResource(Res.string.theme_follow_system),
                    "Light" to stringResource(Res.string.theme_light),
                    "Dark" to stringResource(Res.string.theme_dark)
                ),
                onThemeChanged
            )
        }
        if (platformState.showPrivacyChoices) {
            item { HorizontalDivider(); SettingsSection(stringResource(Res.string.settings_privacy)) }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Default.PrivacyTip, null) },
                    headlineContent = { Text(stringResource(Res.string.privacy_choices)) },
                    supportingContent = { Text(stringResource(Res.string.privacy_choices_description)) },
                    trailingContent = { OutlinedButton(onClick = onPrivacyChoices) { Text(stringResource(Res.string.manage)) } }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(title, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
}

@Composable
private fun SharedChoiceSetting(title: String, selected: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    OutlinedButton(onClick = { onSelected(value) }, modifier = Modifier.weight(1f)) {
                        Text(if (selected == value) "✓ $label" else label)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusListItem(title: String, lines: List<Pair<Boolean, String>>) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                lines.forEach { (healthy, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (healthy) Icons.Default.CheckCircle else Icons.Default.Warning, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        }
    )
}

@Composable
private fun NotificationTimes(selected: Set<String>, onChanged: (Set<String>) -> Unit) {
    val options = listOf(
        "At time of event" to stringResource(Res.string.alert_time_at_event),
        "10 minutes before" to stringResource(Res.string.alert_time_10_minutes_before),
        "1 hour before" to stringResource(Res.string.alert_time_1_hour_before),
        "12 hours before" to stringResource(Res.string.alert_time_12_hours_before),
        "1 day before" to stringResource(Res.string.alert_time_1_day_before),
        "1 week before" to stringResource(Res.string.alert_time_1_week_before)
    )
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(Res.string.alert_times), fontWeight = FontWeight.SemiBold)
        options.forEach { (value, label) ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    val next = selected.toMutableSet()
                    if (value in next && next.size > 1) next.remove(value) else next.add(value)
                    onChanged(next)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(value in selected, null)
                Text(label)
            }
        }
    }
}
