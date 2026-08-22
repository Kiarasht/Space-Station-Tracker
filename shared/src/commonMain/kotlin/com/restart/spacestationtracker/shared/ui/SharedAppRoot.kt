package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.shared.network.createPlatformHttpClient
import com.restart.spacestationtracker.shared.presentation.SharedAppController
import com.restart.spacestationtracker.shared.presentation.SharedAppState
import com.restart.spacestationtracker.shared.network.spaceStationJson
import com.restart.spacestationtracker.shared.passes.PassAlertPolicy
import com.restart.spacestationtracker.shared.passes.PassVisibility
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.*
import com.restart.spacestationtracker.shared.ui.platform.IssMapPoint
import com.restart.spacestationtracker.shared.ui.platform.PlatformIssMap
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
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { createPlatformHttpClient() }))
            }
            .build()
    }
    val state by controller.state.collectAsState()
    var selectedTab by remember { mutableStateOf(SharedTab.MAP) }

    LaunchedEffect(Unit) {
        controller.start()
    }
    LaunchedEffect(selectedTab) {
        onAction(SharedAppActions.SCREEN_VIEW, selectedTab.name)
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
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (settingsPlatformState.adsAvailable &&
                        !settingsPlatformState.isAdFree
                    ) {
                        PlatformBannerAd(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        SharedTab.entries.forEach { tab ->
                            val label = tab.localizedLabel()
                            NavigationBarItem(
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.background
                                ),
                                selected = selectedTab == tab,
                                onClick = {
                                    selectedTab = tab
                                    onAction(SharedAppActions.MEANINGFUL_INTERACTION, tab.name)
                                },
                                icon = {
                                    val painter = when (tab) {
                                        SharedTab.PASSES -> painterResource(Res.drawable.ic_passes)
                                        SharedTab.CREW -> painterResource(Res.drawable.ic_astronaut)
                                        else -> rememberVectorPainter(
                                            image = when (tab) {
                                                SharedTab.MAP -> Icons.Default.Map
                                                SharedTab.SETTINGS -> Icons.Default.Settings
                                                SharedTab.ABOUT -> Icons.Default.Info
                                                SharedTab.PASSES,
                                                SharedTab.CREW -> error("Custom navigation icon expected")
                                            }
                                        )
                                    }
                                    Icon(
                                        painter = painter,
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable {
                    onOpen()
                    showDialog = true
                }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = stringResource(Res.string.remove_ads),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
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
    topEndContent: @Composable ColumnScope.() -> Unit = {}
) {
    var showStreamPicker by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        PlatformIssMap(
            currentLocation = currentLocation,
            orbit = if (showOrbit) {
                futureLocations.map {
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
            modifier = Modifier.fillMaxSize()
        )

        when {
            isLoading && currentLocation == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null && currentLocation == null -> {
                StateMessage(
                    title = stringResource(Res.string.unable_to_track_iss),
                    message = error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        currentLocation?.let { location ->
            IssLocationCard(
                location = location,
                units = units,
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (liveStreams.size == 1) {
                                onOpenStream(liveStreams.first())
                            } else {
                                showStreamPicker = true
                            }
                        },
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Text(stringResource(Res.string.live_stream), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IssLocationCard(
    location: IssLocation,
    units: String,
    modifier: Modifier = Modifier
) {
    val altitude = if (units == "Imperial") {
        "${(location.altitude * 0.621371).roundToInt()} mi"
    } else {
        "${location.altitude.roundToInt()} km"
    }
    val velocity = if (units == "Imperial") {
        "${(location.velocity * 0.621371).roundToInt()} mph"
    } else {
        "${location.velocity.roundToInt()} km/h"
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IssTelemetryRow(
                icon = Icons.Default.LocationOn,
                value = "${location.latitude.format(2)}°, ${location.longitude.format(2)}°"
            )
            IssTelemetryRow(
                icon = Icons.Default.Height,
                value = altitude
            )
            IssTelemetryRow(
                icon = Icons.Default.Speed,
                value = velocity
            )
            IssTelemetryRow(
                icon = if (location.visibility.equals("eclipsed", ignoreCase = true)) {
                    Icons.Default.DarkMode
                } else {
                    Icons.Default.WbSunny
                },
                value = stringResource(
                    if (location.visibility.equals("eclipsed", ignoreCase = true)) {
                        Res.string.iss_in_earth_shadow
                    } else {
                        Res.string.iss_is_sunlit
                    }
                )
            )
        }
    }
}

@Composable
private fun IssTelemetryRow(
    icon: ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
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
        state.passes.isEmpty() -> SkyPathStateMessage(
            padding = padding,
            icon = Icons.Default.VisibilityOff,
            title = stringResource(Res.string.sky_path_no_visible_passes),
            message = stringResource(Res.string.sky_path_no_visible_passes_message),
            actionLabel = stringResource(Res.string.refresh),
            onAction = { onAction(SharedAppActions.REQUEST_LOCATION, null) }
        )
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.passLocationName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(Res.string.info)
                        )
                    }
                }
            }
            state.passes.forEachIndexed { index, pass ->
                item(key = pass.startTimeMillis) {
                    SharedIssPassCard(
                        pass = pass,
                        onScheduleNotification = {
                            selectedNotificationTimes =
                                state.settings.automaticPassAlertNotificationTimes.ifEmpty {
                                    setOf(PassAlertPolicy.TEN_MINUTES_BEFORE)
                                }
                            passForNotification = it
                        },
                        onAddToCalendar = {
                            onAction(
                                SharedAppActions.ADD_PASS_TO_CALENDAR,
                                it.toPlatformPayload()
                            )
                        },
                        onShare = {
                            onAction(
                                SharedAppActions.SHARE_PASS,
                                it.toPlatformPayload()
                            )
                        }
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

@Composable
fun SharedIssPassCard(
    pass: IssPass,
    onScheduleNotification: (IssPass) -> Unit,
    onAddToCalendar: (IssPass) -> Unit,
    onShare: (IssPass) -> Unit,
    dateLabel: String = formatEpochDate(pass.startTimeMillis),
    timeLabel: String = formatEpochTime(pass.startTimeMillis),
    modifier: Modifier = Modifier
) {
    val notifyLabel = stringResource(Res.string.schedule_notification)
    val calendarLabel = stringResource(Res.string.add_to_calendar)
    val shareLabel = stringResource(Res.string.share)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SkyPathCardSparkles(
                seed = pass.startTimeMillis,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onScheduleNotification(pass) }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = notifyLabel)
                    }
                    IconButton(
                        onClick = { onAddToCalendar(pass) }
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = calendarLabel)
                    }
                    IconButton(
                        onClick = { onShare(pass) }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = shareLabel)
                    }
                }
                SkyPathChart(
                    startCompass = pass.startAzimuthCompass,
                    endCompass = pass.endAzimuthCompass,
                    maxElevation = pass.maxElevation,
                    youLabel = stringResource(Res.string.you)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PassMetric(
                        stringResource(Res.string.starts_label),
                        timeLabel
                    )
                    PassMetric(
                        stringResource(Res.string.duration_label),
                        stringResource(
                            Res.string.duration_min_sec_format,
                            pass.durationInSeconds / 60,
                            pass.durationInSeconds % 60
                        )
                    )
                    PassMetric(
                        stringResource(Res.string.visibility),
                        visibilityLabel(pass.magnitude)
                    )
                }
            }
        }
    }
}

@Composable
private fun PassMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

private fun formatEpochDate(milliseconds: Long): String {
    return formatDateTime(Instant.fromEpochMilliseconds(milliseconds))
}

private fun formatEpochTime(milliseconds: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(milliseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, '0')
    val period = if (dateTime.hour < 12) "AM" else "PM"
    return "$hour:$minute $period"
}

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
private fun visibilityLabel(magnitude: Double): String {
    val resource: StringResource = when (PassVisibility.fromMagnitude(magnitude)) {
        PassVisibility.VERY_BRIGHT -> Res.string.visibility_very_bright
        PassVisibility.BRIGHT -> Res.string.visibility_bright
        PassVisibility.MODERATE -> Res.string.visibility_moderate
        PassVisibility.FAINT -> Res.string.visibility_faint
        PassVisibility.VERY_FAINT -> Res.string.visibility_very_faint
    }
    return stringResource(resource)
}
