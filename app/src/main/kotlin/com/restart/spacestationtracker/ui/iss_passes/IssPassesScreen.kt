package com.restart.spacestationtracker.ui.iss_passes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.ui.ads.NativeAdCard
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
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        LazyColumn(
                            contentPadding = screenPadding,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Showing passes for ${uiState.location?.name}",
                                    style = MaterialTheme.typography.headlineSmall
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
                                    is FeedItem.PassItem -> IssPassCard(pass = item.pass)
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
                                    text = "Showing passes for ${uiState.location?.name}",
                                    style = MaterialTheme.typography.headlineSmall
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
                                    is FeedItem.PassItem -> IssPassCard(pass = item.pass)
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
fun IssPassCard(pass: IssPass) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = dateFormat.format(pass.startTime).uppercase(),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
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