package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
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
    data class SectionHeader(val section: CrewSection) : SharedCrewItem
    /** [elsewhere] counts people off the ISS per spacecraft, in list order. */
    data class Summary(val total: Int, val onIss: Int, val elsewhere: List<Pair<String, Int>> = emptyList()) : SharedCrewItem
}

sealed interface CrewSection {
    data object Iss : CrewSection
    /** People aboard another spacecraft; a blank [name] falls back to "Elsewhere in space". */
    data class Craft(val name: String) : CrewSection
}

private val Astronaut.craftName: String get() = craft.trim()

/**
 * Adds an "Aboard the ISS" header and one header per other spacecraft (such as "Shenzhou 23") when
 * anyone listed is not on the ISS. Expects ISS crew first and everyone else grouped by craft (the
 * repository sorts them that way); without anyone elsewhere the list is unchanged.
 */
internal fun withStationHeaders(items: List<SharedCrewItem>): List<SharedCrewItem> {
    val astronauts = items.filterIsInstance<SharedCrewItem.AstronautItem>()
    if (astronauts.none { !it.astronaut.isOnIss }) return items
    val result = mutableListOf<SharedCrewItem>()
    if (astronauts.any { it.astronaut.isOnIss }) result += SharedCrewItem.SectionHeader(CrewSection.Iss)
    // Headers double as list keys, so each craft gets exactly one.
    val addedCrafts = mutableSetOf<String>()
    items.forEach { item ->
        if (item is SharedCrewItem.AstronautItem && !item.astronaut.isOnIss && addedCrafts.add(item.astronaut.craftName)) {
            result += SharedCrewItem.SectionHeader(CrewSection.Craft(item.astronaut.craftName))
        }
        result += item
    }
    return result
}

/** Head count for the top of the crew list, with people off the ISS counted per spacecraft. */
internal fun crewSummary(items: List<SharedCrewItem>): SharedCrewItem.Summary {
    val astronauts = items.filterIsInstance<SharedCrewItem.AstronautItem>().map { it.astronaut }
    return SharedCrewItem.Summary(
        total = astronauts.size,
        onIss = astronauts.count { it.isOnIss },
        elsewhere = astronauts.filterNot { it.isOnIss }
            .groupingBy { it.craftName }
            .eachCount()
            .toList()
    )
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
        else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val displayItems = remember(items) {
                listOf(crewSummary(items)) + withStationHeaders(items)
            }
            // Edge to edge: content starts below the status bar and scrolls under it and the floating bar.
            val listPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp + contentPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = 16.dp + contentPadding.calculateBottomPadding()
            )
            if (maxWidth < 600.dp) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayItems, key = ::crewItemKey) { item ->
                        SharedCrewFeedItem(item, onOpenUrl, platformSlot)
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalItemSpacing = 16.dp
                ) {
                    items(
                        displayItems,
                        key = ::crewItemKey,
                        span = { item ->
                            if (item is SharedCrewItem.SectionHeader || item is SharedCrewItem.Summary) {
                                StaggeredGridItemSpan.FullLine
                            } else {
                                StaggeredGridItemSpan.SingleLane
                            }
                        }
                    ) { item ->
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
    is SharedCrewItem.SectionHeader -> when (val section = item.section) {
        CrewSection.Iss -> "section-iss"
        is CrewSection.Craft -> "section-craft-${section.name}"
    }
    is SharedCrewItem.Summary -> "summary"
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
        is SharedCrewItem.SectionHeader -> {
            val color = stationColor(item.section == CrewSection.Iss)
            Row(
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when (val section = item.section) {
                        CrewSection.Iss -> stringResource(Res.string.crew_section_iss)
                        is CrewSection.Craft -> craftLabel(section.name)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        is SharedCrewItem.Summary -> CrewSummary(item)
    }
}

/** The spacecraft's name from the feed, or "Elsewhere in space" when the feed leaves it out. */
@Composable
private fun craftLabel(name: String): String = name.ifBlank { stringResource(Res.string.crew_section_elsewhere) }

/** [stationColor] for the gradient cards: a light sky blue for ISS crew, coral for everyone else. */
private fun heroStationColor(onIss: Boolean): Color = if (onIss) HeroIssBlue else HeroCoral

private val HeroIssBlue = Color(0xFF9DBBFF)
private val HeroCoral = Color(0xFFFF7A7A)

/** ISS crew use the accent color; people aboard other spacecraft use coral. */
@Composable
private fun stationColor(onIss: Boolean): Color =
    if (onIss) MaterialTheme.colorScheme.primary else MaterialTheme.spaceColors.live

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CrewSummary(summary: SharedCrewItem.Summary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = summary.total.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(Res.string.people_in_space_now),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (summary.onIss != summary.total) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpacePill(
                        text = "${stringResource(Res.string.crew_section_iss)} · ${summary.onIss}",
                        color = stationColor(true)
                    )
                    summary.elsewhere.forEach { (craft, count) ->
                        SpacePill(
                            text = "${craftLabel(craft)} · $count",
                            color = stationColor(false)
                        )
                    }
                }
            }
        }
    }
}

/** Days since [launchEpochSeconds], counting launch day as day 1. */
private fun dayInSpace(launchEpochSeconds: Long): Int? {
    if (launchEpochSeconds <= 0) return null
    val nowSeconds = kotlin.time.Clock.System.now().epochSeconds
    return ((nowSeconds - launchEpochSeconds) / 86_400L).toInt().plus(1).takeIf { it > 0 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedExpeditionCard(expedition: Expedition, onOpenUrl: (String) -> Unit) {
    val heroText = MaterialTheme.spaceColors.onHero
    SpaceHeroCard(contentPadding = 0.dp) {
        if (expedition.imageUrl.isNotBlank()) {
            AsyncImage(
                model = wikimediaThumbnailUrl(expedition.imageUrl, widthPx = 960),
                contentDescription = stringResource(Res.string.expedition_format, expedition.number),
                modifier = Modifier.fillMaxWidth().aspectRatio(2f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
        }
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = wikimediaThumbnailUrl(expedition.patchUrl, widthPx = 250),
                    contentDescription = stringResource(Res.string.expedition_patch),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(Res.string.expedition_format, expedition.number),
                        style = MaterialTheme.typography.headlineSmall,
                        color = heroText
                    )
                    if (expedition.startDate > 0) {
                        Text(
                            stringResource(Res.string.launched_format, formatEpochSeconds(expedition.startDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = heroText.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayInSpace(expedition.startDate)?.let { day ->
                    SpacePill(
                        text = stringResource(Res.string.day_in_space_format, day),
                        color = MaterialTheme.spaceColors.gold,
                        filled = true,
                        contentColor = MaterialTheme.spaceColors.onGold
                    )
                }
                if (expedition.endDate > 0) {
                    SpacePill(
                        text = stringResource(Res.string.returns_format, formatEpochSeconds(expedition.endDate)),
                        color = Color.White
                    )
                }
            }
            if (expedition.bio.isNotBlank()) {
                ExpandableAstronautBio(
                    astronautName = "expedition-${expedition.number}",
                    bio = expedition.bio,
                    textColor = heroText.copy(alpha = 0.9f),
                    linkColor = MaterialTheme.spaceColors.gold
                )
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
                        add(CrewSocialLink("Wikipedia", expedition.url, Res.drawable.ic_wikipedia_social_icon))
                    }
                    add(
                        CrewSocialLink(
                            label = "Google",
                            url = googleSearchUrl("Expedition ${expedition.number}"),
                            icon = Res.drawable.ic_google_social_icon
                        )
                    )
                },
                onOpenUrl = onOpenUrl,
                onHero = true
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedAstronautCard(astronaut: Astronaut, onOpenUrl: (String) -> Unit) {
    val accent = heroStationColor(astronaut.isOnIss)
    val heroText = MaterialTheme.spaceColors.onHero
    // Same gradient card as the expedition, so the whole list reads as one set.
    SpaceHeroCard(contentPadding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = wikimediaThumbnailUrl(astronaut.profileImageUrl, widthPx = 330),
                    contentDescription = astronaut.name,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(2.5.dp, accent), CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        astronaut.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = heroText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (astronaut.flagCode.isNotBlank()) {
                            AsyncImage(
                                model = "https://flagcdn.com/w80/${astronaut.flagCode.lowercase()}.png",
                                contentDescription = null,
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(15.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            astronaut.role,
                            style = MaterialTheme.typography.bodyMedium,
                            color = heroText.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (astronaut.craft.isNotBlank()) {
                            SpacePill(text = astronaut.craft, color = accent, icon = Icons.Default.RocketLaunch)
                        }
                        dayInSpace(astronaut.launchDate)?.let { day ->
                            SpacePill(
                                text = stringResource(Res.string.day_in_space_format, day),
                                color = MaterialTheme.spaceColors.gold
                            )
                        }
                    }
                }
            }
            if (astronaut.bio.isNotBlank()) {
                ExpandableAstronautBio(
                    astronautName = astronaut.name,
                    bio = astronaut.bio,
                    textColor = heroText.copy(alpha = 0.9f),
                    linkColor = MaterialTheme.spaceColors.gold
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
                        add(CrewSocialLink("X", it, Res.drawable.ic_x_social_icon, tintWithTheme = true))
                    }
                    if (astronaut.bioUrl.isNotBlank()) {
                        add(CrewSocialLink("Wikipedia", astronaut.bioUrl, Res.drawable.ic_wikipedia_social_icon))
                    }
                },
                onOpenUrl = onOpenUrl,
                onHero = true
            )
    }
}

@Composable
private fun ExpandableAstronautBio(
    astronautName: String,
    bio: String,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    linkColor: Color = MaterialTheme.colorScheme.primary
) {
    var isExpanded by rememberSaveable(astronautName) { mutableStateOf(false) }
    var hasOverflow by remember(bio) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = bio,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
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
                color = linkColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private const val COLLAPSED_BIO_MAX_LINES = 3

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
    onOpenUrl: (String) -> Unit,
    onHero: Boolean = false
) {
    val container = if (onHero) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val themedTint = if (onHero) Color(0xFF0B1640) else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        links.forEach { link ->
            Surface(
                onClick = { onOpenUrl(link.url) },
                shape = RoundedCornerShape(percent = 50),
                color = container,
                modifier = if (link.wide) Modifier.width(84.dp).height(40.dp) else Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val iconModifier = if (link.wide) Modifier.width(60.dp).height(20.dp) else Modifier.size(22.dp)
                    if (link.tintWithTheme) {
                        Icon(
                            painter = painterResource(link.icon),
                            contentDescription = link.label,
                            tint = themedTint,
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
    val colors = MaterialTheme.spaceColors
    val visibilityOptions = listOf(
        "Faint" to stringResource(Res.string.visibility_faint),
        "Moderate" to stringResource(Res.string.visibility_moderate),
        "Bright" to stringResource(Res.string.visibility_bright),
        "Very Bright" to stringResource(Res.string.visibility_very_bright)
    )
    val alertTimeOptions = listOf(
        "At time of event" to stringResource(Res.string.alert_time_at_event),
        "10 minutes before" to stringResource(Res.string.alert_time_10_minutes_before),
        "1 hour before" to stringResource(Res.string.alert_time_1_hour_before),
        "12 hours before" to stringResource(Res.string.alert_time_12_hours_before),
        "1 day before" to stringResource(Res.string.alert_time_1_day_before),
        "1 week before" to stringResource(Res.string.alert_time_1_week_before)
    )
    val mapTypeOptions = listOf(
        "Normal" to stringResource(Res.string.map_type_normal),
        "Satellite" to stringResource(Res.string.map_type_satellite),
        "Hybrid" to stringResource(Res.string.map_type_hybrid),
        "Terrain" to stringResource(Res.string.map_type_terrain)
    )
    val themeOptions = listOf(
        "Follow System" to stringResource(Res.string.theme_follow_system),
        "Light" to stringResource(Res.string.theme_light),
        "Dark" to stringResource(Res.string.theme_dark)
    )
    // Which choice dialog is open, if any.
    var openChoice by rememberSaveable { mutableStateOf<String?>(null) }
    when (openChoice) {
        CHOICE_VISIBILITY -> SingleChoiceDialog(
            title = stringResource(Res.string.minimum_visibility),
            options = visibilityOptions,
            selected = settings.automaticPassAlertMinVisibility,
            onSelected = onMinVisibilityChanged,
            onDismiss = { openChoice = null }
        )
        CHOICE_ALERT_TIMES -> MultiChoiceDialog(
            title = stringResource(Res.string.alert_times),
            options = alertTimeOptions,
            selected = settings.automaticPassAlertNotificationTimes,
            onChanged = onNotificationTimesChanged,
            onDismiss = { openChoice = null }
        )
        CHOICE_MAP_TYPE -> SingleChoiceDialog(
            title = stringResource(Res.string.map_type),
            options = mapTypeOptions,
            selected = settings.mapType,
            onSelected = onMapTypeChanged,
            onDismiss = { openChoice = null }
        )
        CHOICE_THEME -> SingleChoiceDialog(
            title = stringResource(Res.string.theme),
            options = themeOptions,
            selected = settings.theme,
            onSelected = onThemeChanged,
            onDismiss = { openChoice = null }
        )
    }
    LazyColumn(
        // Edge to edge: content starts below the status bar and scrolls under it and the floating bar.
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp + contentPadding.calculateTopPadding(),
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SpaceSectionCard(stringResource(Res.string.settings_iss_pass_alerts)) {
                SettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(Res.string.automatic_good_pass_alerts),
                    subtitle = if (platformState.isLocationLookupInProgress) {
                        stringResource(Res.string.current_location)
                    } else {
                        settings.automaticPassAlertLocationName
                            ?: stringResource(Res.string.automatic_good_pass_alerts_description)
                    }
                ) {
                    Switch(
                        checked = settings.automaticPassAlertsEnabled,
                        enabled = !platformState.isLocationLookupInProgress,
                        onCheckedChange = onAutomaticAlertsChanged
                    )
                }
                // Alert health and the alert options only matter while automatic alerts are on.
                if (settings.automaticPassAlertsEnabled) {
                    SettingsDivider()
                    AlertHealth(
                        lines = listOf(
                            platformState.hasNotificationPermission to stringResource(if (platformState.hasNotificationPermission) Res.string.notifications_allowed else Res.string.notifications_need_permission),
                            platformState.hasLocationPermission to stringResource(if (platformState.hasLocationPermission) Res.string.location_permission_allowed else Res.string.location_permission_needed),
                            hasLocation to stringResource(if (hasLocation) Res.string.alert_location_saved else Res.string.alert_location_needs_update),
                            platformState.isBackgroundUnrestricted to stringResource(if (platformState.isBackgroundUnrestricted) Res.string.battery_unrestricted else Res.string.battery_may_pause_alerts)
                        )
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        icon = Icons.Default.Visibility,
                        tint = colors.visibilityVeryBright,
                        title = stringResource(Res.string.minimum_visibility),
                        value = visibilityOptions.labelFor(settings.automaticPassAlertMinVisibility),
                        onClick = { openChoice = CHOICE_VISIBILITY }
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        icon = Icons.Default.Schedule,
                        tint = colors.visibilityFaint,
                        title = stringResource(Res.string.alert_times),
                        value = alertTimesSummary(alertTimeOptions, settings.automaticPassAlertNotificationTimes),
                        onClick = { openChoice = CHOICE_ALERT_TIMES }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.LocationOn,
                        tint = MaterialTheme.colorScheme.tertiary,
                        title = stringResource(Res.string.alert_location),
                        subtitle = settings.automaticPassAlertLocationName ?: stringResource(Res.string.no_location_saved)
                    ) {
                        FilledTonalButton(onClick = onUpdateLocation) { Text(stringResource(Res.string.update)) }
                    }
                    SettingsDivider()
                    SettingsRow(
                        icon = if (platformState.isBackgroundUnrestricted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        tint = if (platformState.isBackgroundUnrestricted) colors.success else colors.warning,
                        title = stringResource(Res.string.notification_reliability),
                        subtitle = stringResource(if (platformState.isBackgroundUnrestricted) Res.string.battery_optimization_off else Res.string.battery_may_pause_alerts_description)
                    ) {
                        if (!platformState.isBackgroundUnrestricted) {
                            FilledTonalButton(onClick = onOpenBackgroundSettings) { Text(stringResource(Res.string.open)) }
                        }
                    }
                }
            }
        }
        item {
            SpaceSectionCard(stringResource(Res.string.settings_map_settings)) {
                SettingsChoiceRow(
                    icon = Icons.Default.Layers,
                    tint = colors.visibilityModerate,
                    title = stringResource(Res.string.map_type),
                    value = mapTypeOptions.labelFor(settings.mapType),
                    onClick = { openChoice = CHOICE_MAP_TYPE }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Timeline,
                    tint = colors.live,
                    title = stringResource(Res.string.show_orbit_path),
                    subtitle = stringResource(Res.string.show_orbit_path_description)
                ) {
                    Switch(settings.showOrbit, onShowOrbitChanged)
                }
            }
        }
        item {
            SpaceSectionCard(stringResource(Res.string.settings_general_settings)) {
                SettingsChoiceRow(
                    icon = Icons.Default.DarkMode,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(Res.string.theme),
                    value = themeOptions.labelFor(settings.theme),
                    onClick = { openChoice = CHOICE_THEME }
                )
            }
        }
        if (platformState.showPrivacyChoices) {
            item {
                SpaceSectionCard(stringResource(Res.string.settings_privacy)) {
                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        tint = MaterialTheme.colorScheme.tertiary,
                        title = stringResource(Res.string.privacy_choices),
                        subtitle = stringResource(Res.string.privacy_choices_description)
                    ) {
                        FilledTonalButton(onClick = onPrivacyChoices) { Text(stringResource(Res.string.manage)) }
                    }
                }
            }
        }
    }
}

/** One settings row: tinted icon tile, title and subtitle, and a trailing control. */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceIconTile(icon = icon, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

private const val CHOICE_VISIBILITY = "visibility"
private const val CHOICE_ALERT_TIMES = "alert_times"
private const val CHOICE_MAP_TYPE = "map_type"
private const val CHOICE_THEME = "theme"

private fun List<Pair<String, String>>.labelFor(value: String): String =
    firstOrNull { it.first == value }?.second ?: value

/** Selected alert times in list order: "At time of event, 10 minutes before +2" when there are many. */
private fun alertTimesSummary(options: List<Pair<String, String>>, selected: Set<String>): String {
    val labels = options.filter { it.first in selected }.map { it.second }
    return if (labels.size <= 2) labels.joinToString(", ") else labels.take(2).joinToString(", ") + " +${labels.size - 2}"
}

/** A settings row showing the current choice in the accent color; tapping it opens the options. */
@Composable
private fun SettingsChoiceRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceIconTile(icon = icon, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Pick one option; choosing applies it and closes the dialog. */
@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    ChoiceDialogRow(
                        label = label,
                        onClick = {
                            onSelected(value)
                            onDismiss()
                        }
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}

/** Pick one or more options; each tick applies right away, and at least one stays selected. */
@Composable
private fun MultiChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    val isSelected = value in selected
                    ChoiceDialogRow(
                        label = label,
                        onClick = {
                            val next = selected.toMutableSet()
                            if (isSelected && next.size > 1) next.remove(value) else next.add(value)
                            onChanged(next)
                        }
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        }
    )
}

@Composable
private fun ChoiceDialogRow(label: String, onClick: () -> Unit, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        control()
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Green check or amber warning for each requirement automatic alerts depend on. */
@Composable
private fun AlertHealth(lines: List<Pair<Boolean, String>>) {
    val colors = MaterialTheme.spaceColors
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(Res.string.alert_health),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        lines.forEach { (healthy, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (healthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (healthy) colors.success else colors.warning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
