package com.restart.spacestationtracker.ui.iss_live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import androidx.core.net.toUri
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    contentPadding: PaddingValues,
    canRequestAds: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        bottom = contentPadding.calculateBottomPadding()
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
            }
            var hasCentered by remember { mutableStateOf(false) }

            LaunchedEffect(uiState.issLocation) {
                if (!hasCentered && uiState.issLocation != null) {
                    val loc = uiState.issLocation!!
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(LatLng(loc.latitude, loc.longitude), 2f)
                        )
                    )
                    hasCentered = true
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = uiState.mapType),
                contentPadding = PaddingValues(bottom = 50.dp),
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                uiState.issLocation?.let { location ->
                    val issLatLng = LatLng(location.latitude, location.longitude)
                    val markerState = rememberUpdatedMarkerState(position = issLatLng)

                    Marker(
                        state = markerState,
                        title = stringResource(id = R.string.iss_marker_title),
                        snippet = stringResource(id = R.string.iss_marker_snippet),
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.iss_2011),
                        anchor = Offset(0.5f, 0.5f)
                    )
                }

                if (uiState.showOrbit && uiState.futureIssLocations.isNotEmpty()) {
                    val points = uiState.futureIssLocations.map {
                        LatLng(it.latitude, it.longitude)
                    }

                    if (points.isNotEmpty()) {
                        Polyline(
                            points = points,
                            color = Color.Red,
                            width = 5f
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.issLocation == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = stringResource(id = R.string.map_error_format, uiState.error ?: ""),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            uiState.issLocation?.let { location ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(screenPadding)
                ) {
                    IssDataCard(location = location)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(screenPadding)
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (!uiState.isAdFree && canRequestAds) {
                    RemoveAdsButton(
                        modifier = Modifier.fillMaxWidth(),
                        onRewardEarned = {
                            viewModel.grantAdFreeAccess()
                        }
                    )
                }

                uiState.liveStreams?.let { streams ->
                    LiveIndicator(
                        liveStreams = streams,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun RemoveAdsButton(
    modifier: Modifier = Modifier,
    onRewardEarned: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(R.string.remove_ads)) },
            text = { Text(text = stringResource(R.string.remove_ads_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        isLoadingAd = true
                        loadAndShowRewardedAd(context, onRewardEarned, onAdFailed = {
                            isLoadingAd = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_to_load_ad), Toast.LENGTH_SHORT
                            ).show()
                        })
                    }
                ) {
                    Text(stringResource(R.string.watch_video))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (isLoadingAd) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { showDialog = true }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.remove_ads),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

private fun loadAndShowRewardedAd(
    context: Context,
    onRewardEarned: () -> Unit,
    onAdFailed: () -> Unit
) {
    val activity = context as? Activity ?: return
    val adRequest = AdRequest.Builder().build()

    RewardedAd.load(
        context,
        context.getString(R.string.rewarded_ad_unit_id),
        adRequest,
        object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                onAdFailed()
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                rewardedAd.show(activity) { _ ->
                    onRewardEarned()
                }
            }
        }
    )
}


@Composable
fun LiveIndicator(
    liveStreams: List<LiveStream>,
    modifier: Modifier = Modifier
) {
    val isLive = liveStreams.isNotEmpty()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val indicatorColor = if (isLive) Color.Red else Color.Gray
    val labelText =
        if (isLive) stringResource(R.string.live_stream) else stringResource(R.string.stream_offline)

    val infiniteTransition = rememberInfiniteTransition(label = "live_indicator_pulse")
    val alpha by if (isLive) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha_animation"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(id = R.string.select_stream)) },
            text = {
                Column {
                    liveStreams.forEach { stream ->
                        TextButton(
                            onClick = {
                                openYoutubeVideo(context, stream.videoId)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stream.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = isLive) {
                if (liveStreams.size == 1) {
                    openYoutubeVideo(context, liveStreams[0].videoId)
                } else {
                    showDialog = true
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Text(
            text = labelText,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

private fun openYoutubeVideo(context: Context, videoId: String) {
    val intent = Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=$videoId".toUri())
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
