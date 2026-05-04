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
import com.restart.spacestationtracker.util.ForegroundLocationProvider
import com.restart.spacestationtracker.util.IssPassVisibility
import com.restart.spacestationtracker.util.isIgnoringBatteryOptimizations
import com.restart.spacestationtracker.util.openAppSettings
import com.restart.spacestationtracker.util.openBatteryOptimizationSettings
import kotlinx.coroutines.launch
import java.util.Locale

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
                Toast.makeText(context, "Automatic ISS pass alerts enabled.", Toast.LENGTH_SHORT).show()
            }

            AutomaticPassAlertLocationAction.Update -> {
                viewModel.updateAutomaticPassAlertLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    locationName = location.name
                )
                Toast.makeText(context, "Alert location updated.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun captureAlertLocation(action: AutomaticPassAlertLocationAction) {
        if (isAlertLocationLookupInProgress) return

        isAlertLocationLookupInProgress = true
        Toast.makeText(context, "Getting your current location...", Toast.LENGTH_SHORT).show()

        coroutineScope.launch {
            try {
                val location = fetchCurrentAlertLocation(context)
                if (location != null) {
                    applyAlertLocation(action, location)
                } else {
                    Toast.makeText(
                        context,
                        "Could not get your current location. Please try again.",
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
                Toast.makeText(context, "Enable location in app settings to use ISS pass alerts.", Toast.LENGTH_LONG)
                    .show()
                context.openAppSettings()
            } else if (!isGranted) {
                Toast.makeText(
                    context,
                    "Location permission is required for ISS pass alerts.",
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
                Toast.makeText(context, "Enable location in app settings to use ISS pass alerts.", Toast.LENGTH_LONG)
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
                Toast.makeText(context, "Enable notifications in app settings to use ISS pass alerts.", Toast.LENGTH_LONG)
                    .show()
                context.openAppSettings()
            } else if (!isGranted) {
                Toast.makeText(
                    context,
                    "Notification permission is required for ISS pass alerts.",
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
                Toast.makeText(context, "Enable notifications in app settings to use ISS pass alerts.", Toast.LENGTH_LONG)
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
            SettingsHeader(title = "ISS Pass Alerts")
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
                        "Open Battery, then set background usage to Unrestricted.",
                        Toast.LENGTH_LONG
                    ).show()
                    context.openBatteryOptimizationSettings()
                }
            )
        }
        item {
            SettingsHeader(title = "Map Settings")
        }
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(vertical = 8.dp),
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
            SwitchSetting(
                icon = Icons.Default.Route,
                title = "Show orbit path",
                subtitle = "Show the predicted ISS path on the map",
                checked = settings.showOrbit,
                onCheckedChange = viewModel::onShowOrbitChanged
            )
        }
        item {
            SettingsHeader(title = "General Settings")
        }
/*        item {
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
        }*/
        item {
            SegmentedControlSetting(
                modifier = Modifier.padding(vertical = 8.dp),
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
        if (isPrivacyOptionsRequired) {
            item {
                SettingsHeader(title = "Privacy")
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
    val alertSubtitle = when {
        enabled && minVisibility == IssPassVisibility.VERY_BRIGHT ->
            "Watching for Very Bright ISS passes only"
        enabled ->
            "Watching for $minVisibility or better ISS passes"
        isLocationLookupInProgress ->
            "Getting your current location"
        else ->
            "Notify when a good ISS pass is coming"
    }
    var showVisibilityInfoDialog by rememberSaveable { mutableStateOf(false) }

    Column {
        if (showVisibilityInfoDialog) {
            VisibilityInfoDialog(
                onDismiss = { showVisibilityInfoDialog = false }
            )
        }

        SwitchSetting(
            icon = Icons.Default.NotificationsActive,
            title = "Automatic good pass alerts",
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
                title = "Minimum visibility",
                options = IssPassVisibility.options,
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
                            contentDescription = "Minimum visibility info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            MultiSelectSetting(
                title = "Alert times",
                options = automaticPassAlertNotificationOptions,
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
                        contentDescription = "Alert location",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                headlineContent = {
                    Text(
                        text = "Alert location",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                },
                supportingContent = {
                    Text(
                        text = locationName ?: "No location saved",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                },
                trailingContent = {
                    OutlinedButton(onClick = onUpdateLocation) {
                        Text("Update")
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
            Text("Minimum visibility")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VisibilityInfoLine(
                    label = "Very Bright",
                    description = "The easiest passes to notice, often brighter than planets."
                )
                VisibilityInfoLine(
                    label = "Bright",
                    description = "Strong passes that should stand out in the sky."
                )
                VisibilityInfoLine(
                    label = "Moderate",
                    description = "Visible, but easier to miss in city light or haze."
                )
                VisibilityInfoLine(
                    label = "Faint",
                    description = "Possible to see, but best with darker skies and clear weather."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
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
                contentDescription = "Alert health",
                tint = statusColor
            )
        },
        headlineContent = {
            Text(
                text = "Alert health",
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
                        "Notifications allowed"
                    } else {
                        "Notifications need permission"
                    }
                )
                HealthStatusLine(
                    isHealthy = hasSavedAlertLocation,
                    text = if (hasSavedAlertLocation) {
                        "Alert location saved"
                    } else {
                        "Alert location needs update"
                    }
                )
                HealthStatusLine(
                    isHealthy = isIgnoringBatteryOptimizations,
                    text = if (isIgnoringBatteryOptimizations) {
                        "Battery unrestricted"
                    } else {
                        "Battery may pause alerts"
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
                contentDescription = "Notification reliability",
                tint = statusColor
            )
        },
        headlineContent = {
            Text(
                text = "Improve notification reliability",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        },
        supportingContent = {
            Text(
                text = if (isIgnoringBatteryOptimizations) {
                    "Battery optimization is off for this app."
                } else {
                    "Android may pause alerts after days of inactivity. Open Battery and choose Unrestricted."
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
                    Text("Open")
                }
            }
        }
    )
}

@Composable
fun MultiSelectSetting(
    title: String,
    options: List<String>,
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
                    val checked = option in selectedOptions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { isChecked ->
                                    val nextSelection = selectedOptions.toMutableSet()
                                    if (isChecked) {
                                        nextSelection.add(option)
                                    } else if (selectedOptions.size > 1) {
                                        nextSelection.remove(option)
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
                        Text(option)
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
                contentDescription = "Privacy choices",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = "Privacy choices",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        },
        supportingContent = {
            Text(
                text = "Manage ad consent preferences",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        trailingContent = {
            OutlinedButton(onClick = onClick) {
                Text("Manage")
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
    options: List<String>,
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

private val automaticPassAlertNotificationOptions = listOf(
    "At time of event",
    "10 minutes before",
    "1 hour before",
    "12 hours before",
    "1 day before",
    "1 week before"
)

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
        name = String.format(
            Locale.getDefault(),
            "Current Location (%.2f, %.2f)",
            latitude,
            longitude
        )
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
