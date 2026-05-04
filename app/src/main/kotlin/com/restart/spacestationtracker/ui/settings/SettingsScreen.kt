package com.restart.spacestationtracker.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.util.ForegroundLocationProvider
import com.restart.spacestationtracker.util.IssPassVisibility
import com.restart.spacestationtracker.util.isIgnoringBatteryOptimizations
import com.restart.spacestationtracker.util.openAppSettings
import com.restart.spacestationtracker.util.openBatteryOptimizationSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    contentPadding: PaddingValues,
    isPrivacyOptionsRequired: Boolean = false,
    onPrivacyOptionsClick: () -> Unit = {}
) {
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    var pendingLocationAction by remember { mutableStateOf<AutomaticPassAlertLocationAction?>(null) }
    var isAlertLocationLookupInProgress by remember { mutableStateOf(false) }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations())
    }
    var locationPermissionDeniedCount by rememberSaveable { mutableStateOf(0) }
    var notificationPermissionDeniedCount by rememberSaveable { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(): Boolean {
        return ForegroundLocationProvider.hasLocationPermission(context)
    }

    fun applyAlertLocation(action: AutomaticPassAlertLocationAction, location: AlertLocation) {
        when (action) {
            AutomaticPassAlertLocationAction.Enable -> {
                viewModel.enableAutomaticPassAlerts(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    locationName = location.name
                )
                Toast.makeText(context, R.string.automatic_alerts_enabled_toast, Toast.LENGTH_SHORT).show()
            }

            AutomaticPassAlertLocationAction.Update -> {
                viewModel.updateAutomaticPassAlertLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    locationName = location.name
                )
                Toast.makeText(context, R.string.alert_location_updated_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun captureAlertLocation(action: AutomaticPassAlertLocationAction) {
        if (isAlertLocationLookupInProgress) return

        isAlertLocationLookupInProgress = true
        Toast.makeText(context, R.string.getting_current_location_toast, Toast.LENGTH_SHORT).show()

        coroutineScope.launch {
            try {
                val location = fetchCurrentAlertLocation(context)
                if (location != null) {
                    applyAlertLocation(action, location)
                } else {
                    Toast.makeText(
                        context,
                        R.string.could_not_get_current_location_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isAlertLocationLookupInProgress = false
            }
        }
    }

    fun shouldOpenSettingsForLocationPermission(): Boolean {
        return locationPermissionDeniedCount > 0 &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
    }

    lateinit var locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val action = pendingLocationAction
            pendingLocationAction = null
            if (isGranted && action != null) {
                captureAlertLocation(action)
            } else {
                locationPermissionDeniedCount += 1
            }

            if (!isGranted && locationPermissionDeniedCount >= 2) {
                Toast.makeText(context, R.string.enable_location_in_settings_toast, Toast.LENGTH_LONG)
                    .show()
                context.openAppSettings()
            } else if (!isGranted) {
                Toast.makeText(
                    context,
                    R.string.location_permission_required_toast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    fun requestLocationForAction(action: AutomaticPassAlertLocationAction) {
        if (hasLocationPermission()) {
            captureAlertLocation(action)
        } else {
            pendingLocationAction = action
            if (shouldOpenSettingsForLocationPermission()) {
                Toast.makeText(context, R.string.enable_location_in_settings_toast, Toast.LENGTH_LONG)
                    .show()
                context.openAppSettings()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                requestLocationForAction(AutomaticPassAlertLocationAction.Enable)
            } else {
                notificationPermissionDeniedCount += 1
            }

            if (!isGranted && notificationPermissionDeniedCount >= 2) {
                Toast.makeText(context, R.string.enable_notifications_in_settings_toast, Toast.LENGTH_LONG)
                    .show()
                context.openAppSettings()
            } else if (!isGranted) {
                Toast.makeText(
                    context,
                    R.string.notification_permission_required_toast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    fun startEnableAutomaticAlerts() {
        if (hasNotificationPermission()) {
            requestLocationForAction(AutomaticPassAlertLocationAction.Enable)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationPermissionDeniedCount > 0 &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                Toast.makeText(context, R.string.enable_notifications_in_settings_toast, Toast.LENGTH_LONG)
                    .show()
                context.openAppSettings()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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
            SettingsHeader(title = stringResource(id = R.string.settings_iss_pass_alerts))
        }
        item {
            AutomaticPassAlertsSetting(
                enabled = settings.automaticPassAlertsEnabled,
                minVisibility = settings.automaticPassAlertMinVisibility,
                notificationTimes = settings.automaticPassAlertNotificationTimes,
                locationName = settings.automaticPassAlertLocationName,
                hasNotificationPermission = hasNotificationPermission(),
                hasSavedAlertLocation = settings.automaticPassAlertLatitude != null &&
                    settings.automaticPassAlertLongitude != null,
                isLocationLookupInProgress = isAlertLocationLookupInProgress,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onEnabledChange = { enabled ->
                    if (enabled) {
                        startEnableAutomaticAlerts()
                    } else {
                        viewModel.disableAutomaticPassAlerts()
                    }
                },
                onMinVisibilityChanged = viewModel::onAutomaticPassAlertMinVisibilityChanged,
                onNotificationTimesChanged = viewModel::onAutomaticPassAlertNotificationTimesChanged,
                onUpdateLocation = {
                    requestLocationForAction(AutomaticPassAlertLocationAction.Update)
                },
                onOpenBatterySettings = {
                    Toast.makeText(
                        context,
                        R.string.battery_unrestricted_guidance_toast,
                        Toast.LENGTH_LONG
                    ).show()
                    context.openBatteryOptimizationSettings()
                }
            )
        }
        item {
            SettingsHeader(title = stringResource(id = R.string.settings_map_settings))
        }
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(vertical = 8.dp),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = stringResource(id = R.string.map_type),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = stringResource(id = R.string.map_type),
                options = listOf(
                    SettingOption("Normal", stringResource(id = R.string.map_type_normal)),
                    SettingOption("Satellite", stringResource(id = R.string.map_type_satellite)),
                    SettingOption("Hybrid", stringResource(id = R.string.map_type_hybrid)),
                    SettingOption("Terrain", stringResource(id = R.string.map_type_terrain))
                ),
                selectedOption = settings.mapType,
                onOptionSelected = viewModel::onMapTypeChanged
            )
        }
        item {
            SwitchSetting(
                icon = Icons.Default.Route,
                title = stringResource(id = R.string.show_orbit_path),
                subtitle = stringResource(id = R.string.show_orbit_path_description),
                checked = settings.showOrbit,
                onCheckedChange = viewModel::onShowOrbitChanged
            )
        }
        item {
            SettingsHeader(title = stringResource(id = R.string.settings_general_settings))
        }
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(vertical = 8.dp),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = stringResource(id = R.string.theme),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = stringResource(id = R.string.theme),
                options = listOf(
                    SettingOption("Follow System", stringResource(id = R.string.theme_follow_system)),
                    SettingOption("Light", stringResource(id = R.string.theme_light)),
                    SettingOption("Dark", stringResource(id = R.string.theme_dark))
                ),
                selectedOption = settings.theme,
                onOptionSelected = viewModel::onThemeChanged,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (isPrivacyOptionsRequired) {
            item {
                SettingsHeader(title = stringResource(id = R.string.settings_privacy))
            }
            item {
                PrivacyOptionsSetting(onClick = onPrivacyOptionsClick)
            }
        }
    }
}

@Composable
fun AutomaticPassAlertsSetting(
    enabled: Boolean,
    minVisibility: String,
    notificationTimes: Set<String>,
    locationName: String?,
    hasNotificationPermission: Boolean,
    hasSavedAlertLocation: Boolean,
    isLocationLookupInProgress: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onMinVisibilityChanged: (String) -> Unit,
    onNotificationTimesChanged: (Set<String>) -> Unit,
    onUpdateLocation: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val selectedVisibilityLabel = stringResource(
        id = IssPassVisibility.labelResForVisibility(minVisibility)
    )
    val alertSubtitle = when {
        enabled && minVisibility == IssPassVisibility.VERY_BRIGHT ->
            stringResource(id = R.string.watching_very_bright_passes_only)
        enabled ->
            stringResource(id = R.string.watching_visibility_or_better_format, selectedVisibilityLabel)
        isLocationLookupInProgress ->
            stringResource(id = R.string.getting_your_current_location)
        else ->
            stringResource(id = R.string.automatic_good_pass_alerts_description)
    }
    val visibilityOptions = listOf(
        SettingOption(IssPassVisibility.FAINT, stringResource(id = R.string.visibility_faint)),
        SettingOption(IssPassVisibility.MODERATE, stringResource(id = R.string.visibility_moderate)),
        SettingOption(IssPassVisibility.BRIGHT, stringResource(id = R.string.visibility_bright)),
        SettingOption(IssPassVisibility.VERY_BRIGHT, stringResource(id = R.string.visibility_very_bright))
    )
    val alertTimeOptions = automaticPassAlertNotificationOptions()
    var showVisibilityInfoDialog by rememberSaveable { mutableStateOf(false) }

    Column {
        if (showVisibilityInfoDialog) {
            VisibilityInfoDialog(
                onDismiss = { showVisibilityInfoDialog = false }
            )
        }

        SwitchSetting(
            icon = Icons.Default.NotificationsActive,
            title = stringResource(id = R.string.automatic_good_pass_alerts),
            subtitle = alertSubtitle,
            checked = enabled,
            enabled = !isLocationLookupInProgress,
            onCheckedChange = onEnabledChange
        )

        if (enabled) {
            AlertHealthSetting(
                hasNotificationPermission = hasNotificationPermission,
                hasSavedAlertLocation = hasSavedAlertLocation,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations
            )

            SegmentedControlSetting(
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                leadingContent = null,
                title = stringResource(id = R.string.minimum_visibility),
                options = visibilityOptions,
                selectedOption = minVisibility,
                onOptionSelected = onMinVisibilityChanged,
                compact = true,
                titleAction = {
                    IconButton(
                        onClick = { showVisibilityInfoDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(id = R.string.minimum_visibility_info),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            MultiSelectSetting(
                title = stringResource(id = R.string.alert_times),
                options = alertTimeOptions,
                selectedOptions = notificationTimes,
                onSelectionChanged = onNotificationTimesChanged,
                compact = true
            )

            NotificationReliabilitySetting(
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onOpenBatterySettings = onOpenBatterySettings
            )

            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(id = R.string.alert_location),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.alert_location),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                },
                supportingContent = {
                    Text(
                        text = locationName ?: stringResource(id = R.string.no_location_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                },
                trailingContent = {
                    OutlinedButton(onClick = onUpdateLocation) {
                        Text(stringResource(id = R.string.update))
                    }
                }
            )
        }
    }
}

@Composable
fun VisibilityInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = R.string.minimum_visibility))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VisibilityInfoLine(
                    label = stringResource(id = R.string.visibility_very_bright),
                    description = stringResource(id = R.string.visibility_info_very_bright_description)
                )
                VisibilityInfoLine(
                    label = stringResource(id = R.string.visibility_bright),
                    description = stringResource(id = R.string.visibility_info_bright_description)
                )
                VisibilityInfoLine(
                    label = stringResource(id = R.string.visibility_moderate),
                    description = stringResource(id = R.string.visibility_info_moderate_description)
                )
                VisibilityInfoLine(
                    label = stringResource(id = R.string.visibility_faint),
                    description = stringResource(id = R.string.visibility_info_faint_description)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.got_it))
            }
        }
    )
}

@Composable
private fun VisibilityInfoLine(
    label: String,
    description: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun AlertHealthSetting(
    hasNotificationPermission: Boolean,
    hasSavedAlertLocation: Boolean,
    isIgnoringBatteryOptimizations: Boolean
) {
    val isHealthy = hasNotificationPermission && hasSavedAlertLocation && isIgnoringBatteryOptimizations
    val statusIcon = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning
    val statusColor = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    ListItem(
        leadingContent = {
            Icon(
                imageVector = statusIcon,
                contentDescription = stringResource(id = R.string.alert_health),
                tint = statusColor
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.alert_health),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                HealthStatusLine(
                    isHealthy = hasNotificationPermission,
                    text = if (hasNotificationPermission) {
                        stringResource(id = R.string.notifications_allowed)
                    } else {
                        stringResource(id = R.string.notifications_need_permission)
                    }
                )
                HealthStatusLine(
                    isHealthy = hasSavedAlertLocation,
                    text = if (hasSavedAlertLocation) {
                        stringResource(id = R.string.alert_location_saved)
                    } else {
                        stringResource(id = R.string.alert_location_needs_update)
                    }
                )
                HealthStatusLine(
                    isHealthy = isIgnoringBatteryOptimizations,
                    text = if (isIgnoringBatteryOptimizations) {
                        stringResource(id = R.string.battery_unrestricted)
                    } else {
                        stringResource(id = R.string.battery_may_pause_alerts)
                    }
                )
            }
        }
    )
}

@Composable
private fun HealthStatusLine(
    isHealthy: Boolean,
    text: String
) {
    val color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NotificationReliabilitySetting(
    isIgnoringBatteryOptimizations: Boolean,
    onOpenBatterySettings: () -> Unit
) {
    val statusIcon = if (isIgnoringBatteryOptimizations) {
        Icons.Default.CheckCircle
    } else {
        Icons.Default.Warning
    }
    val statusColor = if (isIgnoringBatteryOptimizations) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    ListItem(
        leadingContent = {
            Icon(
                imageVector = statusIcon,
                contentDescription = stringResource(id = R.string.notification_reliability),
                tint = statusColor
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.improve_notification_reliability),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        },
        supportingContent = {
            Text(
                text = if (isIgnoringBatteryOptimizations) {
                    stringResource(id = R.string.battery_optimization_off)
                } else {
                    stringResource(id = R.string.battery_may_pause_alerts_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        },
        trailingContent = if (isIgnoringBatteryOptimizations) {
            null
        } else {
            {
                OutlinedButton(onClick = onOpenBatterySettings) {
                    Text(stringResource(id = R.string.open))
                }
            }
        }
    )
}

@Composable
fun MultiSelectSetting(
    title: String,
    options: List<SettingOption>,
    selectedOptions: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    compact: Boolean = false
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                modifier = Modifier.padding(top = if (compact) 0.dp else 8.dp)
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier.padding(
                    top = if (compact) 4.dp else 8.dp,
                    bottom = if (compact) 4.dp else 8.dp
                )
            ) {
                options.forEach { option ->
                    val checked = option.value in selectedOptions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { isChecked ->
                                    val nextSelection = selectedOptions.toMutableSet()
                                    if (isChecked) {
                                        nextSelection.add(option.value)
                                    } else if (selectedOptions.size > 1) {
                                        nextSelection.remove(option.value)
                                    }
                                    onSelectionChanged(nextSelection)
                                }
                            )
                            .padding(vertical = if (compact) 2.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.label)
                    }
                }
            }
        }
    )
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

@Composable
fun PrivacyOptionsSetting(onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.PrivacyTip,
                contentDescription = stringResource(id = R.string.privacy_choices),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.privacy_choices),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        },
        supportingContent = {
            Text(
                text = stringResource(id = R.string.privacy_choices_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        trailingContent = {
            OutlinedButton(onClick = onClick) {
                Text(stringResource(id = R.string.manage))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlSetting(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    title: String,
    options: List<SettingOption>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    compact: Boolean = false,
    titleAction: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier.padding(top = if (compact) 4.dp else 16.dp),
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
                titleAction?.let {
                    Spacer(modifier = Modifier.width(4.dp))
                    it()
                }
            }
        },
        supportingContent = {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == option.value
                    OutlinedButton(
                        onClick = { onOptionSelected(option.value) },
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
                            text = option.label,
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
    enabled: Boolean = true,
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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
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
                    steps = steps,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    )
}

data class SettingOption(
    val value: String,
    val label: String
)

@Composable
private fun automaticPassAlertNotificationOptions(): List<SettingOption> {
    return listOf(
        SettingOption(ALERT_TIME_AT_EVENT, stringResource(id = R.string.alert_time_at_event)),
        SettingOption(ALERT_TIME_10_MINUTES_BEFORE, stringResource(id = R.string.alert_time_10_minutes_before)),
        SettingOption(ALERT_TIME_1_HOUR_BEFORE, stringResource(id = R.string.alert_time_1_hour_before)),
        SettingOption(ALERT_TIME_12_HOURS_BEFORE, stringResource(id = R.string.alert_time_12_hours_before)),
        SettingOption(ALERT_TIME_1_DAY_BEFORE, stringResource(id = R.string.alert_time_1_day_before)),
        SettingOption(ALERT_TIME_1_WEEK_BEFORE, stringResource(id = R.string.alert_time_1_week_before))
    )
}

private const val ALERT_TIME_AT_EVENT = "At time of event"
private const val ALERT_TIME_10_MINUTES_BEFORE = "10 minutes before"
private const val ALERT_TIME_1_HOUR_BEFORE = "1 hour before"
private const val ALERT_TIME_12_HOURS_BEFORE = "12 hours before"
private const val ALERT_TIME_1_DAY_BEFORE = "1 day before"
private const val ALERT_TIME_1_WEEK_BEFORE = "1 week before"

private enum class AutomaticPassAlertLocationAction {
    Enable,
    Update
}

private data class AlertLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val name: String
)

private suspend fun fetchCurrentAlertLocation(context: Context): AlertLocation? {
    val location = ForegroundLocationProvider.getBestLocation(context) ?: return null
    val latitude = location.latitude
    val longitude = location.longitude

    return AlertLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = location.altitude,
        name = context.getString(R.string.current_location_format, latitude, longitude)
    )
}

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("no activity")
}
