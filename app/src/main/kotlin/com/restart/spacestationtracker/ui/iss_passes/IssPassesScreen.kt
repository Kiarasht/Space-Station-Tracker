package com.restart.spacestationtracker.ui.iss_passes

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.ui.ads.NativeAdCard
import com.restart.spacestationtracker.shared.ui.SharedIssPassCard
import com.restart.spacestationtracker.shared.ui.SkyPathVisualGuide
import com.restart.spacestationtracker.util.IssPassVisibility
import com.restart.spacestationtracker.util.NotificationScheduler
import com.restart.spacestationtracker.util.openAppSettings
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private data class NotificationTimeOption(
    val value: String,
    val label: String
)

@Composable
private fun notificationTimeOptions(): List<NotificationTimeOption> {
    return listOf(
        NotificationTimeOption(ALERT_TIME_AT_EVENT, stringResource(id = R.string.alert_time_at_event)),
        NotificationTimeOption(ALERT_TIME_10_MINUTES_BEFORE, stringResource(id = R.string.alert_time_10_minutes_before)),
        NotificationTimeOption(ALERT_TIME_1_HOUR_BEFORE, stringResource(id = R.string.alert_time_1_hour_before)),
        NotificationTimeOption(ALERT_TIME_12_HOURS_BEFORE, stringResource(id = R.string.alert_time_12_hours_before)),
        NotificationTimeOption(ALERT_TIME_1_DAY_BEFORE, stringResource(id = R.string.alert_time_1_day_before)),
        NotificationTimeOption(ALERT_TIME_1_WEEK_BEFORE, stringResource(id = R.string.alert_time_1_week_before))
    )
}

private const val ALERT_TIME_AT_EVENT = "At time of event"
private const val ALERT_TIME_10_MINUTES_BEFORE = "10 minutes before"
private const val ALERT_TIME_1_HOUR_BEFORE = "1 hour before"
private const val ALERT_TIME_12_HOURS_BEFORE = "12 hours before"
private const val ALERT_TIME_1_DAY_BEFORE = "1 day before"
private const val ALERT_TIME_1_WEEK_BEFORE = "1 week before"

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun IssPassesScreen(
    viewModel: IssPassesViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findActivity()
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { configuration.locales[0] }
    val layoutDirection = LocalLayoutDirection.current
    val windowSizeClass = calculateWindowSizeClass(activity)
    val passDateFormat = remember(locale) { SimpleDateFormat("EEEE, MMMM d", locale) }
    val passTimeFormat = remember(locale) { SimpleDateFormat("h:mm a", locale) }
    val viewportPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        end = contentPadding.calculateEndPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateTopPadding()
    )
    val listPadding = PaddingValues(16.dp)

    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var passForNotification by remember { mutableStateOf<IssPass?>(null) }

    if (showInfoDialog) {
        InfoDialog(onDismiss = { showInfoDialog = false })
    }

    passForNotification?.let { pass ->
        NotificationSchedulerDialog(
            pass = pass,
            onDismiss = { passForNotification = null }
        )
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            AppAnalytics.trackInteraction(
                if (isGranted) "location_permission_granted" else "location_permission_denied",
                "sky_path"
            )
            viewModel.onPermissionResult(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        if (!uiState.permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(viewportPadding)
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            uiState.error != null -> {
                val isLocationError = uiState.error?.contains("location", ignoreCase = true) == true
                SkyPathStateMessage(
                    title = if (isLocationError) {
                        stringResource(id = R.string.sky_path_location_needed)
                    } else {
                        stringResource(id = R.string.sky_path_unable_to_load_passes)
                    },
                    message = if (isLocationError) {
                        stringResource(id = R.string.sky_path_location_needed_message)
                    } else {
                        stringResource(id = R.string.sky_path_unable_to_load_passes_message)
                    },
                    icon = if (isLocationError) Icons.Default.LocationOff else Icons.Default.VisibilityOff,
                    primaryActionText = stringResource(id = R.string.try_again),
                    onPrimaryActionClick = viewModel::retryLocationAndPasses,
                    secondaryActionText = if (isLocationError) {
                        stringResource(id = R.string.open_settings_lowercase)
                    } else {
                        null
                    },
                    onSecondaryActionClick = if (isLocationError) {
                        { activity.openAppSettings() }
                    } else {
                        null
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                if (uiState.feedItems.isEmpty()) {
                    SkyPathStateMessage(
                        title = stringResource(id = R.string.sky_path_no_visible_passes),
                        message = stringResource(id = R.string.sky_path_no_visible_passes_message),
                        icon = Icons.Default.VisibilityOff,
                        primaryActionText = stringResource(id = R.string.refresh),
                        onPrimaryActionClick = viewModel::retryLocationAndPasses,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    return@Box
                }

                val onNotificationClick: (IssPass) -> Unit = { pass ->
                    AppAnalytics.trackInteraction("open_pass_alert_scheduler", "sky_path")
                    passForNotification = pass
                }

                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = listPadding,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                SkyPathHeader(
                                    locationName = uiState.location?.name.orEmpty(),
                                    onInfoClick = { showInfoDialog = true }
                                )
                            }
                            items(
                                items = uiState.feedItems,
                                contentType = { item ->
                                    when (item) {
                                        is FeedItem.PassItem -> "pass"
                                        is FeedItem.AdItem -> "ad"
                                    }
                                }
                            ) { item ->
                                when (item) {
                                    is FeedItem.PassItem -> SharedIssPassCard(
                                        pass = item.pass,
                                        onScheduleNotification = onNotificationClick,
                                        onAddToCalendar = {
                                            AppAnalytics.trackInteraction("add_pass_to_calendar", "sky_path")
                                            addPassToCalendar(activity, it)
                                        },
                                        onShare = {
                                            AppAnalytics.trackInteraction("share_pass", "sky_path")
                                            sharePassDetails(activity, it)
                                        },
                                        dateLabel = passDateFormat
                                            .format(Date(item.pass.startTimeMillis))
                                            .uppercase(locale),
                                        timeLabel = passTimeFormat.format(Date(item.pass.startTimeMillis))
                                    )

                                    is FeedItem.AdItem -> NativeAdCard(nativeAd = item.ad)
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }

                    else -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = listPadding,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalItemSpacing = 16.dp
                        ) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SkyPathHeader(
                                    locationName = uiState.location?.name.orEmpty(),
                                    onInfoClick = { showInfoDialog = true }
                                )
                            }
                            items(
                                items = uiState.feedItems,
                                contentType = { item ->
                                    when (item) {
                                        is FeedItem.PassItem -> "pass"
                                        is FeedItem.AdItem -> "ad"
                                    }
                                }
                            ) { item ->
                                when (item) {
                                    is FeedItem.PassItem -> SharedIssPassCard(
                                        pass = item.pass,
                                        onScheduleNotification = onNotificationClick,
                                        onAddToCalendar = {
                                            AppAnalytics.trackInteraction("add_pass_to_calendar", "sky_path")
                                            addPassToCalendar(activity, it)
                                        },
                                        onShare = {
                                            AppAnalytics.trackInteraction("share_pass", "sky_path")
                                            sharePassDetails(activity, it)
                                        },
                                        dateLabel = passDateFormat
                                            .format(Date(item.pass.startTimeMillis))
                                            .uppercase(locale),
                                        timeLabel = passTimeFormat.format(Date(item.pass.startTimeMillis))
                                    )

                                    is FeedItem.AdItem -> NativeAdCard(nativeAd = item.ad)
                                }
                            }
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyPathHeader(
    locationName: String,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = locationName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall
        )
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(id = R.string.info)
            )
        }
    }
}

@Composable
private fun SkyPathStateMessage(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryActionText: String,
    onPrimaryActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPrimaryActionClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(primaryActionText)
            }
            if (secondaryActionText != null && onSecondaryActionClick != null) {
                OutlinedButton(onClick = onSecondaryActionClick) {
                    Text(secondaryActionText)
                }
            }
        }
    }
}

@Composable
fun NotificationSchedulerDialog(pass: IssPass, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val dateFormat = remember { SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault()) }
    val notificationScheduler = remember { NotificationScheduler(context) }
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }
    var notificationPermissionDeniedCount by rememberSaveable { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                Toast.makeText(
                    context,
                    R.string.permission_granted_schedule_notifications,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                notificationPermissionDeniedCount += 1
            }

            if (!isGranted && notificationPermissionDeniedCount >= 2) {
                Toast.makeText(
                    context,
                    R.string.enable_notifications_for_pass_alerts,
                    Toast.LENGTH_LONG
                ).show()
                context.openAppSettings()
            }
        }
    )

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            canScheduleExactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
        }
    )

    fun shouldOpenSettingsForNotificationPermission(): Boolean {
        return notificationPermissionDeniedCount > 0 &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
    }

    fun requestNotificationPermissionOrOpenSettings() {
        if (shouldOpenSettingsForNotificationPermission()) {
            Toast.makeText(
                context,
                R.string.enable_notifications_for_pass_alerts,
                Toast.LENGTH_LONG
            ).show()
            context.openAppSettings()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val notificationOptions = notificationTimeOptions()

    val selectedOptions = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.schedule_notification_title)) },
        text = {
            LazyColumn {
                item {
                    Text(
                        stringResource(
                            id = R.string.schedule_notification_message_format,
                            dateFormat.format(Date(pass.startTimeMillis))
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!canScheduleExactAlarms) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.exact_alarm_permission_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    exactAlarmSettingsLauncher.launch(
                                        Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(stringResource(id = R.string.improve_notification_reliability))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    notificationOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = option.value in selectedOptions,
                                    onValueChange = {
                                        if (it) selectedOptions.add(option.value) else selectedOptions.remove(
                                            option.value
                                        )
                                    },
                                    role = Role.Checkbox
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = option.value in selectedOptions,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (hasNotificationPermission) {
                        notificationScheduler.scheduleNotifications(pass, selectedOptions)
                        Toast.makeText(context, R.string.notification_scheduled, Toast.LENGTH_SHORT)
                            .show()
                        onDismiss()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionOrOpenSettings()
                    }
                },
                enabled = selectedOptions.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.schedule))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}


@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.nav_sky_path)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text(stringResource(id = R.string.sky_path_info_intro))
                    Spacer(modifier = Modifier.height(16.dp))
                    SkyPathVisualGuide(
                        youLabel = stringResource(id = R.string.you),
                        horizonLabel = stringResource(id = R.string.sky_path_info_horizon),
                        skyArcLabel = stringResource(id = R.string.sky_path_info_arc),
                        highestPointLabel = stringResource(id = R.string.sky_path_info_iss_icon),
                        directionsLabel = stringResource(id = R.string.sky_path_info_labels),
                        overheadExplanation = stringResource(id = R.string.sky_path_info_overhead),
                        lowerExplanation = stringResource(id = R.string.sky_path_info_lower)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.got_it))
            }
        }
    )
}

private fun sharePassDetails(context: Context, pass: IssPass) {
    val dateFormat = DateFormat.getDateTimeInstance(
        DateFormat.FULL,
        DateFormat.SHORT,
        Locale.getDefault()
    )
    val visibility = context.getString(IssPassVisibility.labelResForMagnitude(pass.magnitude))
    val shareText = context.getString(
        R.string.iss_pass_share_text_format,
        dateFormat.format(Date(pass.startTimeMillis)),
        pass.durationInSeconds / 60,
        pass.durationInSeconds % 60,
        visibility
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.iss_pass_share_chooser)))
}

private fun addPassToCalendar(context: Context, pass: IssPass) {
    val beginTimeMillis = pass.startTimeMillis
    val endTimeMillis = beginTimeMillis + (pass.durationInSeconds * 1000L)

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, context.getString(R.string.iss_pass_calendar_title))
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            context.getString(
                R.string.iss_pass_calendar_description_format,
                context.getString(IssPassVisibility.labelResForMagnitude(pass.magnitude))
            )
        )
    }
    context.startActivity(intent)
}

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("no activity")
}
