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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.ui.ads.NativeAdCard
import com.restart.spacestationtracker.util.NotificationScheduler
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun IssPassesScreen(
    viewModel: IssPassesViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findActivity()
    val windowSizeClass = calculateWindowSizeClass(activity)
    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        bottom = contentPadding.calculateTopPadding() + 16.dp
    )

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
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        if (!uiState.permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            uiState.error != null -> Text(
                text = "Error: ${uiState.error}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )

            else -> {
                val annotatedString = buildAnnotatedString {
                    append("${uiState.location?.name} ")
                    appendInlineContent("infoIcon")
                }

                val inlineContent = mapOf(
                    "infoIcon" to InlineTextContent(
                        Placeholder(
                            width = 24.sp,
                            height = 24.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info"
                            )
                        }
                    }
                )

                val onNotificationClick: (IssPass) -> Unit = { pass ->
                    passForNotification = pass
                }

                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        LazyColumn(
                            contentPadding = screenPadding,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.headlineSmall,
                                    inlineContent = inlineContent
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
                                    is FeedItem.PassItem -> IssPassCard(
                                        pass = item.pass,
                                        onNotificationClick = onNotificationClick
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
                            contentPadding = screenPadding,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalItemSpacing = 16.dp
                        ) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.headlineSmall,
                                    inlineContent = inlineContent
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
                                    is FeedItem.PassItem -> IssPassCard(
                                        pass = item.pass,
                                        onNotificationClick = onNotificationClick
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
fun NotificationSchedulerDialog(pass: IssPass, onDismiss: () -> Unit) {
    val context = LocalContext.current
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                Toast.makeText(
                    context,
                    "Permission granted! You can now schedule notifications.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    val notificationOptions = remember {
        listOf(
            "At time of event",
            "10 minutes before",
            "1 hour before",
            "12 hours before",
            "1 day before",
            "1 week before"
        )
    }

    val selectedOptions = remember { mutableStateListOf<String>() }

    if (!canScheduleExactAlarms) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permission Required") },
            text = { Text("To schedule notifications accurately, please grant the 'Alarms & reminders' permission in your phone's settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            ).also {
                                context.startActivity(it)
                            }
                        }
                        onDismiss()
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Schedule Notification") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            "Schedule a notification for the ISS pass on ${dateFormat.format(pass.startTime)}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        notificationOptions.forEach { option ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = option in selectedOptions,
                                        onValueChange = {
                                            if (it) selectedOptions.add(option) else selectedOptions.remove(
                                                option
                                            )
                                        },
                                        role = Role.Checkbox
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = option in selectedOptions,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(option)
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
                            Toast.makeText(context, "Notification scheduled!", Toast.LENGTH_SHORT)
                                .show()
                            onDismiss()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    enabled = selectedOptions.isNotEmpty()
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sky Path") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("The chart visualizes the ISS pass across the sky from your perspective:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        Text("\t• The straight line is the horizon")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\t• The arc represents the sky")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\t• The ISS icon shows the highest point the station will reach")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\t• If the icon is at the very top of the arc, it means the ISS will pass almost directly overhead.")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\t• If the icon is lower on the arc, closer to the horizon line, it means the ISS will appear lower in the sky and won't climb as high during its pass.")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\t• The labels (e.g., NW, SE) show the start and end directions of the pass")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun IssPassCard(pass: IssPass, onNotificationClick: (IssPass) -> Unit) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(
                top = 16.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
        ) {
            Row {
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = dateFormat.format(pass.startTime).uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onNotificationClick(pass) }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Schedule Notification"
                    )
                }
                IconButton(onClick = { addPassToCalendar(context, pass) }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Add to Calendar"
                    )
                }
                IconButton(onClick = { sharePassDetails(context, pass) }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn("Starts", timeFormat.format(pass.startTime))
                InfoColumn(
                    "Duration",
                    "${pass.durationInSeconds / 60} min ${pass.durationInSeconds % 60} sec"
                )
                InfoColumn("Visibility", getBrightnessRating(pass.magnitude))
            }
            Spacer(modifier = Modifier.height(16.dp))
            SkyPathComposable(
                startCompass = pass.startAzimuthCompass,
                endCompass = pass.endAzimuthCompass,
                maxElevation = pass.maxElevation.toFloat()
            )
        }
    }
}

private fun sharePassDetails(context: Context, pass: IssPass) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d 'at' h:mm a", Locale.getDefault())
    val shareText = """
        Check out this ISS pass!
        Date: ${dateFormat.format(pass.startTime)}
        Duration: ${pass.durationInSeconds / 60} min ${pass.durationInSeconds % 60} sec
        Visibility: ${getBrightnessRating(pass.magnitude)}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share ISS Pass"))
}

private fun addPassToCalendar(context: Context, pass: IssPass) {
    val beginTimeMillis = pass.startTime.time
    val endTimeMillis = beginTimeMillis + (pass.durationInSeconds * 1000L)

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "ISS Pass Overhead")
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            "An ISS pass with a visibility of ${getBrightnessRating(pass.magnitude)} will be visible."
        )
    }
    context.startActivity(intent)
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SkyPathComposable(startCompass: String, endCompass: String, maxElevation: Float) {
    val context = LocalContext.current
    val exoFont = ResourcesCompat.getFont(context, R.font.exo_variable)
    val satellitePainter = painterResource(id = R.drawable.ic_iss)
    val userPainter = painterResource(id = R.drawable.ic_man)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 56.sp.value
        color = onSurface.toArgb()
        typeface = exoFont
    }
    val youTextPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 56.sp.value
        color = onSurface.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = exoFont
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .aspectRatio(2f)
    ) {
        val arcRadius = (min(size.width, size.height) - 2)
        val arcCenter = Offset(center.x, size.height)

        drawLine(
            color = Color.Gray,
            start = Offset(arcCenter.x - arcRadius, arcCenter.y),
            end = Offset(arcCenter.x + arcRadius, arcCenter.y),
            strokeWidth = 2.dp.toPx()
        )

        drawArc(
            color = onSurface,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 2.dp.toPx())
        )

        drawIntoCanvas {
            textPaint.textAlign = android.graphics.Paint.Align.LEFT
            it.nativeCanvas.drawText(
                startCompass,
                arcCenter.x - arcRadius,
                arcCenter.y + 24.dp.toPx(),
                textPaint
            )
            textPaint.textAlign = android.graphics.Paint.Align.RIGHT
            it.nativeCanvas.drawText(
                endCompass,
                arcCenter.x + arcRadius,
                arcCenter.y + 24.dp.toPx(),
                textPaint
            )
        }

        val angleInRadians = Math.toRadians(180.0 + maxElevation)
        val indicatorX = arcCenter.x + (arcRadius * cos(angleInRadians)).toFloat()
        val indicatorY = arcCenter.y + (arcRadius * sin(angleInRadians)).toFloat()
        val satelliteSize = 48.dp.toPx()
        val userIconSize = 48.dp.toPx()
        val userIconX = center.x - userIconSize / 2
        val userIconY = arcCenter.y - userIconSize - 4.dp.toPx()

        drawIntoCanvas {
            it.nativeCanvas.drawText(
                "You",
                center.x,
                userIconY - 8.dp.toPx(),
                youTextPaint
            )
        }
        translate(left = userIconX, top = userIconY) {
            with(userPainter) {
                draw(
                    size = Size(userIconSize, userIconSize)
                )
            }
        }

        translate(left = indicatorX - satelliteSize / 2, top = indicatorY - satelliteSize / 2) {
            with(satellitePainter) {
                draw(
                    size = Size(satelliteSize, satelliteSize)
                )
            }
        }
    }
}

fun getBrightnessRating(magnitude: Double): String {
    return when {
        magnitude < -2.0 -> "Very Bright"
        magnitude < -1.5 -> "Bright"
        magnitude < -1.0 -> "Moderate"
        magnitude < 0.0 -> "Faint"
        else -> "Very Faint"
    }
}

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("no activity")
}
