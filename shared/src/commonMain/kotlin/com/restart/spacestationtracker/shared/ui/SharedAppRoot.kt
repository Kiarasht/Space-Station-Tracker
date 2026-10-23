package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.setSingletonImageLoaderFactory
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.shared.presentation.SharedAppController
import com.restart.spacestationtracker.shared.presentation.SharedAppState
import com.restart.spacestationtracker.shared.network.spaceStationJson
import com.restart.spacestationtracker.shared.passes.PassAlertPolicy
import com.restart.spacestationtracker.shared.passes.PassVisibility
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.*
import com.restart.spacestationtracker.shared.ui.platform.IssMapPoint
import com.restart.spacestationtracker.shared.ui.platform.PlatformIssMap
import com.restart.spacestationtracker.shared.ui.platform.SatelliteMapTrack
import com.restart.spacestationtracker.shared.satellites.SatelliteSnapshot
import com.restart.spacestationtracker.shared.satellites.TrackedSatellite
import com.restart.spacestationtracker.shared.ui.platform.PlatformBannerAd
import com.restart.spacestationtracker.shared.ui.platform.PlatformNativeAd
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Instant

private enum class SharedTab {
    MAP,
    PASSES,
    CREW,
    SETTINGS,
    ABOUT
}

object SharedAppActions {
    const val SCREEN_VIEW = "screen_view"
    const val OPEN_URL = "open_url"
    const val REQUEST_LOCATION = "request_location"
    const val MEANINGFUL_INTERACTION = "meaningful_interaction"
    const val SCHEDULE_PASS_NOTIFICATION = "schedule_pass_notification"
    const val ADD_PASS_TO_CALENDAR = "add_pass_to_calendar"
    const val SHARE_PASS = "share_pass"
    const val ENABLE_LIVE_EVENT_ALERTS = "enable_live_event_alerts"
    const val DISABLE_LIVE_EVENT_ALERTS = "disable_live_event_alerts"
    const val ENABLE_AUTOMATIC_PASS_ALERTS = "enable_automatic_pass_alerts"
    const val DISABLE_AUTOMATIC_PASS_ALERTS = "disable_automatic_pass_alerts"
    const val OPEN_BACKGROUND_SETTINGS = "open_background_settings"
    const val PRIVACY_CHOICES = "privacy_choices"
    const val REFRESH_AUTOMATIC_PASS_ALERTS = "refresh_automatic_pass_alerts"
    const val PURCHASE_AD_REMOVAL = "purchase_ad_removal"
    const val RESTORE_AD_REMOVAL = "restore_ad_removal"
    const val OPEN_AD_REMOVAL = "open_ad_removal"
    const val CONTACT_SUPPORT = "contact_support"
    const val RATE_APP = "rate_app"
    const val SHARE_APP = "share_app"
    const val MAP_TYPE_CHANGED = "map_type_changed"
    const val SHOW_ORBIT_CHANGED = "show_orbit_changed"
    const val THEME_CHANGED = "theme_changed"
    const val RETRY_CREW = "retry_crew"
}

@Composable
fun SharedAppRoot(
    controller: SharedAppController,
    versionText: String,
    settingsPlatformState: SharedSettingsPlatformState = SharedSettingsPlatformState(),
    onAction: (String, String?) -> Unit
) {
    setSingletonImageLoaderFactory { context -> spaceImageLoader(context) }
    val state by controller.state.collectAsState()
    val requestedTab by controller.requestedTab.collectAsState()
    var selectedTab by remember { mutableStateOf(SharedTab.MAP) }
    var chromeHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    LaunchedEffect(requestedTab) {
        val tabName = requestedTab ?: return@LaunchedEffect
        SharedTab.entries.firstOrNull { it.name == tabName }?.let { selectedTab = it }
        controller.consumeRequestedTab()
    }

    LaunchedEffect(Unit) {
        controller.start()
    }
    LaunchedEffect(selectedTab) {
        onAction(SharedAppActions.SCREEN_VIEW, selectedTab.name)
        controller.setMapVisible(selectedTab == SharedTab.MAP)
    }
    DisposableEffect(controller) {
        onDispose(controller::close)
    }

    val useDarkTheme = when (state.settings.theme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    SpaceStationTheme(darkTheme = useDarkTheme) {
        val navItems = SharedTab.entries.map { tab ->
            SpaceNavItem(
                key = tab.name,
                label = tab.localizedLabel(),
                icon = when (tab) {
                    SharedTab.MAP -> rememberVectorPainter(Icons.Default.Map)
                    SharedTab.PASSES -> painterResource(Res.drawable.ic_passes)
                    SharedTab.CREW -> painterResource(Res.drawable.ic_astronaut)
                    SharedTab.SETTINGS -> rememberVectorPainter(Icons.Default.Settings)
                    SharedTab.ABOUT -> rememberVectorPainter(Icons.Default.Info)
                }
            )
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { scaffoldPadding ->
            // Screens draw under the floating bar; its height becomes their bottom padding.
            val padding = PaddingValues(
                top = scaffoldPadding.calculateTopPadding(),
                bottom = chromeHeight
            )
            Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                SharedTab.MAP -> SharedIssMapScreen(
                    currentLocation = state.currentIssLocation,
                    futureLocations = state.futureIssLocations,
                    mapType = state.settings.mapType,
                    units = state.settings.units,
                    showOrbit = state.settings.showOrbit,
                    isLoading = state.isMapLoading,
                    error = state.mapError,
                    liveStreams = state.liveStreams,
                    contentPadding = padding,
                    satellites = state.satellites,
                    onOpenStream = { stream ->
                        onAction(
                            SharedAppActions.OPEN_URL,
                            "https://www.youtube.com/watch?v=${stream.videoId}"
                        )
                    },
                    topEndContent = {
                        if (settingsPlatformState.adsAvailable &&
                            !settingsPlatformState.isAdFree
                        ) {
                            SharedRemoveAdsButton(
                                purchasePriceText = settingsPlatformState.purchasePriceText,
                                isPurchaseInProgress =
                                    settingsPlatformState.isPurchaseInProgress,
                                isPurchaseAvailable = settingsPlatformState.isPurchaseAvailable,
                                purchaseStatusCode =
                                    settingsPlatformState.purchaseStatusCode,
                                onOpen = {
                                    onAction(SharedAppActions.OPEN_AD_REMOVAL, null)
                                },
                                onPurchase = {
                                    onAction(SharedAppActions.PURCHASE_AD_REMOVAL, null)
                                },
                                onRestore = {
                                    onAction(SharedAppActions.RESTORE_AD_REMOVAL, null)
                                }
                            )
                        }
                    }
                )
                SharedTab.PASSES -> SharedPassesScreen(
                    state = state,
                    padding = padding,
                    showAds = settingsPlatformState.adsAvailable &&
                        !settingsPlatformState.isAdFree,
                    onAction = onAction
                )
                SharedTab.CREW -> SharedCrewScreen(
                    items = buildList {
                        state.expedition?.let { add(SharedCrewItem.ExpeditionItem(it)) }
                        var adSlot = 0
                        state.astronauts.forEachIndexed { index, astronaut ->
                            add(SharedCrewItem.AstronautItem(astronaut))
                            val astronautCount = index + 1
                            if (settingsPlatformState.adsAvailable &&
                                !settingsPlatformState.isAdFree &&
                                (astronautCount == 2 ||
                                    (astronautCount > 2 && (astronautCount - 2) % 3 == 0))
                            ) {
                                adSlot += 1
                                add(SharedCrewItem.PlatformSlot("crew-$adSlot"))
                            }
                        }
                    },
                    isLoading = state.isCrewLoading,
                    error = state.crewError,
                    contentPadding = padding,
                    onRetry = {
                        onAction(SharedAppActions.RETRY_CREW, null)
                        controller.retryCrew()
                    },
                    onOpenUrl = { onAction(SharedAppActions.OPEN_URL, it) },
                    platformSlot = { slotId ->
                        PlatformNativeAd(
                            slotId = slotId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                        )
                    }
                )
                SharedTab.ABOUT -> SharedAboutScreen(
                    contentPadding = padding,
                    versionText = versionText,
                    onContactSupport = { onAction(SharedAppActions.CONTACT_SUPPORT, versionText) },
                    onRateApp = { onAction(SharedAppActions.RATE_APP, null) },
                    onShareApp = { onAction(SharedAppActions.SHARE_APP, null) },
                    onPrivacyPolicy = null,
                    onTermsOfUse = null,
                    onLegalPageViewed = { page ->
                        onAction(SharedAppActions.SCREEN_VIEW, page)
                    }
                )
                SharedTab.SETTINGS -> SharedFullSettingsScreen(
                    settings = state.settings,
                    platformState = settingsPlatformState,
                    contentPadding = padding,
                    onAutomaticAlertsChanged = { enabled ->
                        controller.setAutomaticPassAlertsEnabled(enabled)
                        onAction(
                            if (enabled) SharedAppActions.ENABLE_AUTOMATIC_PASS_ALERTS
                            else SharedAppActions.DISABLE_AUTOMATIC_PASS_ALERTS,
                            null
                        )
                    },
                    onLiveEventAlertsChanged = { enabled ->
                        controller.setLiveEventAlertsEnabled(enabled)
                        onAction(
                            if (enabled) SharedAppActions.ENABLE_LIVE_EVENT_ALERTS
                            else SharedAppActions.DISABLE_LIVE_EVENT_ALERTS,
                            null
                        )
                    },
                    onMinVisibilityChanged = { value ->
                        controller.setAutomaticPassAlertMinVisibility(value)
                        onAction(SharedAppActions.REFRESH_AUTOMATIC_PASS_ALERTS, null)
                    },
                    onNotificationTimesChanged = { value ->
                        controller.setAutomaticPassAlertNotificationTimes(value)
                        onAction(SharedAppActions.REFRESH_AUTOMATIC_PASS_ALERTS, null)
                    },
                    onUpdateLocation = { onAction(SharedAppActions.ENABLE_AUTOMATIC_PASS_ALERTS, null) },
                    onOpenBackgroundSettings = {
                        onAction(SharedAppActions.OPEN_BACKGROUND_SETTINGS, null)
                    },
                    onMapTypeChanged = { value ->
                        controller.setMapType(value)
                        onAction(SharedAppActions.MAP_TYPE_CHANGED, value)
                    },
                    onShowOrbitChanged = { value ->
                        controller.setShowOrbit(value)
                        onAction(SharedAppActions.SHOW_ORBIT_CHANGED, value.toString())
                    },
                    onThemeChanged = { value ->
                        controller.setTheme(value)
                        onAction(SharedAppActions.THEME_CHANGED, value)
                    },
                    onPrivacyChoices = {
                        onAction(SharedAppActions.PRIVACY_CHOICES, null)
                    }
                )
            }
            SpaceBottomChrome(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { chromeHeight = with(density) { it.height.toDp() } },
                banner = if (settingsPlatformState.adsAvailable && !settingsPlatformState.isAdFree) {
                    {
                        PlatformBannerAd(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                } else {
                    null
                }
            ) {
                SpaceFloatingNavBar(
                    items = navItems,
                    selectedKey = selectedTab.name,
                    onSelect = { key ->
                        val tab = SharedTab.valueOf(key)
                        if (tab != selectedTab) {
                            onAction(SharedAppActions.MEANINGFUL_INTERACTION, tab.name)
                        }
                        selectedTab = tab
                    }
                )
            }
            }
        }
    }
}

@Composable
fun SharedRemoveAdsButton(
    purchasePriceText: String,
    isPurchaseInProgress: Boolean,
    isPurchaseAvailable: Boolean,
    purchaseStatusCode: String?,
    onOpen: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.remove_ads)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(
                            Res.string.remove_ads_lifetime_description,
                            purchasePriceText
                        )
                    )
                    purchaseStatusCode?.let { statusCode ->
                        Text(
                            localizedPurchaseStatus(statusCode),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onPurchase,
                    enabled = isPurchaseAvailable && !isPurchaseInProgress
                ) {
                    Text(stringResource(Res.string.buy_for_price, purchasePriceText))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = onRestore,
                        enabled = !isPurchaseInProgress
                    ) {
                        Text(stringResource(Res.string.restore_purchase))
                    }
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        )
    }

    if (isPurchaseInProgress) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Color.White
        )
    } else {
        val gold = MaterialTheme.spaceColors.gold
        val onGold = MaterialTheme.spaceColors.onGold
        Surface(
            onClick = {
                onOpen()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(percent = 50),
            color = gold,
            contentColor = onGold,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.remove_ads),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

object SharedPurchaseStatus {
    const val UNAVAILABLE = "unavailable"
    const val ALREADY_REMOVED = "already_removed"
    const val CONNECTING = "connecting"
    const val CANCELED = "canceled"
    const val FAILED = "failed"
    const val NOT_CONFIGURED = "not_configured"
    const val CHECKING = "checking"
    const val RESTORE_NOT_FOUND = "restore_not_found"
    const val PENDING = "pending"
    const val RESTORED = "restored"
    const val REMOVED = "removed"
}

@Composable
private fun localizedPurchaseStatus(statusCode: String): String {
    val resource = when (statusCode) {
        SharedPurchaseStatus.UNAVAILABLE -> Res.string.purchase_status_unavailable
        SharedPurchaseStatus.ALREADY_REMOVED -> Res.string.purchase_status_already_removed
        SharedPurchaseStatus.CONNECTING -> Res.string.purchase_status_connecting
        SharedPurchaseStatus.CANCELED -> Res.string.purchase_status_canceled
        SharedPurchaseStatus.NOT_CONFIGURED -> Res.string.purchase_status_not_configured
        SharedPurchaseStatus.CHECKING -> Res.string.purchase_status_checking
        SharedPurchaseStatus.RESTORE_NOT_FOUND -> Res.string.purchase_status_restore_not_found
        SharedPurchaseStatus.PENDING -> Res.string.purchase_status_pending
        SharedPurchaseStatus.RESTORED -> Res.string.purchase_status_restored
        SharedPurchaseStatus.REMOVED -> Res.string.purchase_status_removed
        else -> Res.string.purchase_status_failed
    }
    return stringResource(resource)
}

@Composable
fun SharedIssMapScreen(
    currentLocation: IssLocation?,
    futureLocations: List<IssLocation>,
    mapType: String,
    units: String,
    showOrbit: Boolean,
    isLoading: Boolean,
    error: String?,
    liveStreams: List<LiveStream>,
    contentPadding: PaddingValues,
    onOpenStream: (LiveStream) -> Unit,
    topEndContent: @Composable ColumnScope.() -> Unit = {},
    satellites: List<SatelliteSnapshot> = emptyList()
) {
    var showStreamPicker by remember { mutableStateOf(false) }
    var selectedSatelliteId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSatellite = TrackedSatellite.fromId(selectedSatelliteId)
    val selectedSnapshot = satellites.firstOrNull { it.satellite == selectedSatellite }
    // One path at a time: the ISS's by default (following the orbit setting), or the selected spacecraft's.
    // Every path is passed with a visible flag so the map can fade between them.
    val issOrbitVisible = selectedSatellite == null || selectedSatellite == TrackedSatellite.ISS
    val tracks = satellites
        // The ISS's own orbit line stands in for its computed path unless the orbit setting is off.
        .filter { it.satellite != TrackedSatellite.ISS || !showOrbit }
        .map { snapshot ->
            SatelliteMapTrack(
                id = snapshot.satellite.id,
                points = snapshot.track,
                colorArgb = snapshot.satellite.trackColorArgb(),
                visible = snapshot.satellite == selectedSatellite
            )
        }

    if (showStreamPicker) {
        AlertDialog(
            onDismissRequest = { showStreamPicker = false },
            title = { Text(stringResource(Res.string.select_stream)) },
            text = {
                Column {
                    liveStreams.forEach { stream ->
                        TextButton(
                            onClick = {
                                showStreamPicker = false
                                onOpenStream(stream)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stream.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreamPicker = false }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PlatformIssMap(
            currentLocation = currentLocation,
            // Lead with the current position so the orbit line starts at the station icon.
            orbit = if (showOrbit) {
                (listOfNotNull(currentLocation) + futureLocations).map {
                    IssMapPoint(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        timestamp = it.timestamp
                    )
                }
            } else {
                emptyList()
            },
            mapType = mapType,
            modifier = Modifier.fillMaxSize(),
            bottomInset = contentPadding.calculateBottomPadding(),
            satellites = satelliteMarkers(satellites, currentLocation, selectedSatellite),
            tracks = tracks,
            orbitVisible = issOrbitVisible,
            onSatelliteClick = { id -> selectedSatelliteId = id }
        )

        // The map runs edge to edge under the status bar and the floating bar; overlays stay clear of both.
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        when {
            isLoading && currentLocation == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null && currentLocation == null -> {
                StateMessage(
                    title = stringResource(Res.string.unable_to_track_iss),
                    message = stringResource(Res.string.unable_to_track_iss_message),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // The card follows the selected satellite; with nothing (or the ISS) selected it shows the live ISS.
        val telemetry = when {
            selectedSnapshot != null && selectedSatellite != TrackedSatellite.ISS -> SatelliteTelemetry(
                latitude = selectedSnapshot.latitude,
                longitude = selectedSnapshot.longitude,
                altitudeKm = selectedSnapshot.altitudeKm,
                speedKmh = selectedSnapshot.speedKmh,
                inShadow = selectedSnapshot.isInEarthShadow,
                isIss = false
            )
            currentLocation != null -> SatelliteTelemetry(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                altitudeKm = currentLocation.altitude,
                speedKmh = currentLocation.velocity,
                inShadow = currentLocation.visibility.equals("eclipsed", ignoreCase = true),
                isIss = true
            )
            else -> null
        }
        telemetry?.let {
            TelemetryCard(
                telemetry = it,
                units = units,
                accent = selectedSatellite?.takeIf { satellite -> satellite != TrackedSatellite.ISS }?.accent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(IntrinsicSize.Max)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topEndContent()
            if (liveStreams.isNotEmpty()) {
                val pulse by rememberInfiniteTransition().animateFloat(
                    initialValue = 1f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
                )
                Surface(
                    onClick = {
                        if (liveStreams.size == 1) {
                            onOpenStream(liveStreams.first())
                        } else {
                            showStreamPicker = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.spaceColors.live,
                    contentColor = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color.White.copy(alpha = pulse))
                        )
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(stringResource(Res.string.live_stream), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

private class SatelliteTelemetry(
    val latitude: Double,
    val longitude: Double,
    val altitudeKm: Double,
    val speedKmh: Double,
    val inShadow: Boolean,
    val isIss: Boolean
)

/** Position, altitude, speed and sunlight for one satellite; [accent] marks a satellite other than the ISS. */
@Composable
private fun TelemetryCard(
    telemetry: SatelliteTelemetry,
    units: String,
    accent: Color?,
    modifier: Modifier = Modifier
) {
    val altitude = if (units == "Imperial") {
        "${(telemetry.altitudeKm * 0.621371).roundToInt()} mi"
    } else {
        "${telemetry.altitudeKm.roundToInt()} km"
    }
    val velocity = if (units == "Imperial") {
        "${(telemetry.speedKmh * 0.621371).roundToInt()} mph"
    } else {
        "${telemetry.speedKmh.roundToInt()} km/h"
    }
    val colors = MaterialTheme.spaceColors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(
            if (accent != null) 1.5.dp else 1.dp,
            accent ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IssTelemetryRow(
                icon = Icons.Default.LocationOn,
                tint = accent ?: MaterialTheme.colorScheme.primary,
                value = "${telemetry.latitude.format(2)}°, ${telemetry.longitude.format(2)}°"
            )
            IssTelemetryRow(
                icon = Icons.Default.Height,
                tint = MaterialTheme.colorScheme.tertiary,
                value = altitude
            )
            IssTelemetryRow(
                icon = Icons.Default.Speed,
                tint = colors.live,
                value = velocity
            )
            IssTelemetryRow(
                icon = if (telemetry.inShadow) Icons.Default.DarkMode else Icons.Default.WbSunny,
                tint = if (telemetry.inShadow) MaterialTheme.colorScheme.secondary else colors.visibilityVeryBright,
                value = stringResource(
                    when {
                        telemetry.inShadow -> Res.string.iss_in_earth_shadow
                        telemetry.isIss -> Res.string.iss_is_sunlit
                        else -> Res.string.satellite_sunlit
                    }
                )
            )
        }
    }
}

@Composable
private fun IssTelemetryRow(
    icon: ImageVector,
    tint: Color,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SpaceIconTile(icon = icon, tint = tint, size = 28.dp)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SharedPassesScreen(
    state: SharedAppState,
    padding: PaddingValues,
    showAds: Boolean,
    onAction: (String, String?) -> Unit
) {
    var passForNotification by remember { mutableStateOf<IssPass?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedNotificationTimes by remember {
        mutableStateOf(setOf(PassAlertPolicy.TEN_MINUTES_BEFORE))
    }

    if (showInfoDialog) {
        SharedSkyPathInfoDialog(onDismiss = { showInfoDialog = false })
    }

    passForNotification?.let { pass ->
        val options = listOf(
            PassAlertPolicy.AT_EVENT to stringResource(Res.string.alert_time_at_event),
            PassAlertPolicy.TEN_MINUTES_BEFORE to
                stringResource(Res.string.alert_time_10_minutes_before),
            PassAlertPolicy.ONE_HOUR_BEFORE to
                stringResource(Res.string.alert_time_1_hour_before),
            PassAlertPolicy.TWELVE_HOURS_BEFORE to
                stringResource(Res.string.alert_time_12_hours_before),
            PassAlertPolicy.ONE_DAY_BEFORE to
                stringResource(Res.string.alert_time_1_day_before),
            PassAlertPolicy.ONE_WEEK_BEFORE to
                stringResource(Res.string.alert_time_1_week_before)
        )
        AlertDialog(
            onDismissRequest = { passForNotification = null },
            title = { Text(stringResource(Res.string.schedule_notification)) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedNotificationTimes =
                                        selectedNotificationTimes.toMutableSet().apply {
                                            if (value in this && size > 1) remove(value) else add(value)
                                        }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = value in selectedNotificationTimes,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(
                            SharedAppActions.SCHEDULE_PASS_NOTIFICATION,
                            pass.toPlatformPayload(selectedNotificationTimes)
                        )
                        passForNotification = null
                    }
                ) {
                    Text(stringResource(Res.string.schedule_notification))
                }
            },
            dismissButton = {
                TextButton(onClick = { passForNotification = null }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    // Passes drop off once they end, so a forecast left open for hours never offers a past pass as next.
    val nowMillis = rememberPassClock()
    val upcomingPasses = remember(state.passes, nowMillis) { state.passes.filterNot { it.hasEnded(nowMillis) } }

    when {
        state.isPassesLoading -> LoadingScreen(padding)
        state.passLocationName == null && state.passesError == null -> SkyPathStateMessage(
            padding = padding,
            icon = Icons.Default.LocationOff,
            title = stringResource(Res.string.sky_path_location_needed),
            message = stringResource(Res.string.sky_path_location_needed_message),
            actionLabel = stringResource(Res.string.use_my_location),
            onAction = { onAction(SharedAppActions.REQUEST_LOCATION, null) }
        )
        state.passLocationName == null -> SkyPathStateMessage(
            padding = padding,
            icon = Icons.Default.LocationOff,
            title = stringResource(Res.string.sky_path_location_unavailable),
            message = stringResource(Res.string.sky_path_location_unavailable_message),
            actionLabel = stringResource(Res.string.try_again),
            onAction = { onAction(SharedAppActions.REQUEST_LOCATION, null) }
        )
        state.passesError != null -> SkyPathStateMessage(
            padding = padding,
            icon = Icons.Default.VisibilityOff,
            title = stringResource(Res.string.sky_path_unable_to_load_passes),
            message = stringResource(Res.string.sky_path_unable_to_load_passes_message),
            actionLabel = stringResource(Res.string.try_again),
            onAction = { onAction(SharedAppActions.REQUEST_LOCATION, null) }
        )
        upcomingPasses.isEmpty() -> SkyPathStateMessage(
            padding = padding,
            icon = Icons.Default.VisibilityOff,
            title = stringResource(Res.string.sky_path_no_visible_passes),
            message = stringResource(Res.string.sky_path_no_visible_passes_message),
            actionLabel = stringResource(Res.string.refresh),
            onAction = { onAction(SharedAppActions.REQUEST_LOCATION, null) }
        )
        else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Edge to edge: content starts below the status bar and scrolls under it and the floating bar.
            val listPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp + padding.calculateTopPadding(),
                end = 16.dp,
                bottom = 16.dp + padding.calculateBottomPadding()
            )
            val onScheduleNotification: (IssPass) -> Unit = { pass ->
                selectedNotificationTimes =
                    state.settings.automaticPassAlertNotificationTimes.ifEmpty {
                        setOf(PassAlertPolicy.TEN_MINUTES_BEFORE)
                    }
                passForNotification = pass
            }
            val onAddToCalendar: (IssPass) -> Unit = { pass ->
                onAction(
                    SharedAppActions.ADD_PASS_TO_CALENDAR,
                    pass.toPlatformPayload()
                )
            }
            val onShare: (IssPass) -> Unit = { pass ->
                onAction(
                    SharedAppActions.SHARE_PASS,
                    pass.toPlatformPayload()
                )
            }

            if (maxWidth < 600.dp) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SharedSkyPathHeader(
                            locationName = state.passLocationName.orEmpty(),
                            onInfoClick = { showInfoDialog = true }
                        )
                    }
                    upcomingPasses.forEachIndexed { index, pass ->
                        if (index == 0) {
                            item(key = pass.startTimeMillis) {
                                SharedNextPassHero(
                                    pass = pass,
                                    onScheduleNotification = onScheduleNotification,
                                    onAddToCalendar = onAddToCalendar,
                                    onShare = onShare
                                )
                            }
                            if (upcomingPasses.size > 1) {
                                item(key = "later-passes") { SkyPathSectionTitle() }
                            }
                        } else item(key = pass.startTimeMillis) {
                            SharedIssPassCard(
                                pass = pass,
                                onScheduleNotification = onScheduleNotification,
                                onAddToCalendar = onAddToCalendar,
                                onShare = onShare
                            )
                        }
                        if (showAds && index >= 1 && (index - 1) % 3 == 0) {
                            val slotId = "passes-${((index - 1) / 3) + 1}"
                            item(key = slotId) {
                                PlatformNativeAd(
                                    slotId = slotId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(360.dp)
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalItemSpacing = 16.dp
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SharedSkyPathHeader(
                            locationName = state.passLocationName.orEmpty(),
                            onInfoClick = { showInfoDialog = true }
                        )
                    }
                    upcomingPasses.forEachIndexed { index, pass ->
                        if (index == 0) {
                            item(key = pass.startTimeMillis, span = StaggeredGridItemSpan.FullLine) {
                                SharedNextPassHero(
                                    pass = pass,
                                    onScheduleNotification = onScheduleNotification,
                                    onAddToCalendar = onAddToCalendar,
                                    onShare = onShare
                                )
                            }
                            if (upcomingPasses.size > 1) {
                                item(key = "later-passes", span = StaggeredGridItemSpan.FullLine) {
                                    SkyPathSectionTitle()
                                }
                            }
                        } else item(key = pass.startTimeMillis) {
                            SharedIssPassCard(
                                pass = pass,
                                onScheduleNotification = onScheduleNotification,
                                onAddToCalendar = onAddToCalendar,
                                onShare = onShare
                            )
                        }
                        if (showAds && index >= 1 && (index - 1) % 3 == 0) {
                            val slotId = "passes-${((index - 1) / 3) + 1}"
                            item(key = slotId) {
                                PlatformNativeAd(
                                    slotId = slotId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(360.dp)
                                )
                            }
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedSkyPathHeader(
    locationName: String,
    onInfoClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            locationName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(Res.string.info)
            )
        }
    }
}

@Composable
private fun SharedSkyPathInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nav_sky_path)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text(stringResource(Res.string.sky_path_info_intro))
                    Spacer(modifier = Modifier.height(16.dp))
                    SkyPathVisualGuide(
                        youLabel = stringResource(Res.string.you),
                        horizonLabel = stringResource(Res.string.sky_path_info_horizon),
                        skyArcLabel = stringResource(Res.string.sky_path_info_arc),
                        highestPointLabel = stringResource(Res.string.sky_path_info_iss_icon),
                        directionsLabel = stringResource(Res.string.sky_path_info_labels),
                        overheadExplanation = stringResource(Res.string.sky_path_info_overhead),
                        lowerExplanation = stringResource(Res.string.sky_path_info_lower)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.got_it))
            }
        }
    )
}

@Composable
private fun SkyPathStateMessage(
    padding: PaddingValues,
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(message)
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Serializable
private data class PassPlatformPayload(
    val startTimeMillis: Long,
    val durationInSeconds: Int,
    val magnitude: Double,
    val maxElevation: Double,
    val startAzimuthCompass: String,
    val endAzimuthCompass: String,
    val notificationTimes: Set<String> = emptySet()
)

private fun IssPass.toPlatformPayload(
    notificationTimes: Set<String> = emptySet()
): String {
    return spaceStationJson.encodeToString(
        PassPlatformPayload(
            startTimeMillis = startTimeMillis,
            durationInSeconds = durationInSeconds,
            magnitude = magnitude,
            maxElevation = maxElevation,
            startAzimuthCompass = startAzimuthCompass,
            endAzimuthCompass = endAzimuthCompass,
            notificationTimes = notificationTimes
        )
    )
}

@Composable
internal fun LoadingScreen(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun StateMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.padding(24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message)
        }
    }
}

private fun Double.format(decimals: Int): String {
    val scale = when (decimals) {
        1 -> 10.0
        2 -> 100.0
        else -> 1.0
    }
    return (this * scale).roundToInt().toDouble().div(scale).toString()
}

internal fun formatEpochSeconds(seconds: Long): String {
    return formatDateTime(Instant.fromEpochSeconds(seconds))
}

private fun formatEpochDate(milliseconds: Long): String = formatPassDate(milliseconds)

private fun formatEpochTime(milliseconds: Long): String = formatPassTime(milliseconds)

private fun formatDateTime(instant: Instant, includeTime: Boolean = false): String {
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "${dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }} " +
        "${dateTime.day}, ${dateTime.year}"
    if (!includeTime) return date
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, '0')
    val period = if (dateTime.hour < 12) "AM" else "PM"
    return "$date · $hour:$minute $period"
}

@Composable
private fun SharedTab.localizedLabel(): String {
    val resource = when (this) {
        SharedTab.MAP -> Res.string.nav_map
        SharedTab.PASSES -> Res.string.nav_sky_path
        SharedTab.CREW -> Res.string.nav_on_duty
        SharedTab.SETTINGS -> Res.string.nav_settings
        SharedTab.ABOUT -> Res.string.nav_about
    }
    return stringResource(resource)
}

@Composable
internal fun visibilityLabel(magnitude: Double): String {
    val resource: StringResource = when (PassVisibility.fromMagnitude(magnitude)) {
        PassVisibility.VERY_BRIGHT -> Res.string.visibility_very_bright
        PassVisibility.BRIGHT -> Res.string.visibility_bright
        PassVisibility.MODERATE -> Res.string.visibility_moderate
        PassVisibility.FAINT -> Res.string.visibility_faint
        PassVisibility.VERY_FAINT -> Res.string.visibility_very_faint
    }
    return stringResource(resource)
}
