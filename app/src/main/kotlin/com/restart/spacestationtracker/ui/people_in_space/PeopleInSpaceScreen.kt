package com.restart.spacestationtracker.ui.people_in_space

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.ui.ads.NativeAdCard
import com.restart.spacestationtracker.util.TopCropTransformation
import com.restart.spacestationtracker.utils.DateUtils
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PeopleInSpaceScreen(
    viewModel: PeopleInSpaceViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val activity = LocalContext.current.findActivity()
    val windowSizeClass = calculateWindowSizeClass(activity)
    val uiState by viewModel.uiState.collectAsState()
    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        bottom = contentPadding.calculateTopPadding() + 16.dp
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    LazyColumn(
                        contentPadding = screenPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.feedItems,
                            key = { item ->
                                when (item) {
                                    is FeedItem.AstronautItem -> item.astronaut.name
                                    is FeedItem.AdItem -> "ad-${item.ad.hashCode()}"
                                    is FeedItem.ExpeditionItem -> item.expedition.number
                                }
                            },
                            contentType = { item ->
                                when (item) {
                                    is FeedItem.AstronautItem -> "astronaut"
                                    is FeedItem.AdItem -> "ad"
                                    is FeedItem.ExpeditionItem -> "expedition"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is FeedItem.AstronautItem -> AstronautCard(astronaut = item.astronaut)
                                is FeedItem.AdItem -> NativeAdCard(nativeAd = item.ad)
                                is FeedItem.ExpeditionItem -> ExpeditionCard(expedition = item.expedition)
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
                        items(
                            items = uiState.feedItems,
                            key = { item ->
                                when (item) {
                                    is FeedItem.AstronautItem -> item.astronaut.name
                                    is FeedItem.AdItem -> "ad-${item.ad.hashCode()}"
                                    is FeedItem.ExpeditionItem -> item.expedition.number
                                }
                            },
                            contentType = { item ->
                                when (item) {
                                    is FeedItem.AstronautItem -> "astronaut"
                                    is FeedItem.AdItem -> "ad"
                                    is FeedItem.ExpeditionItem -> "expedition"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is FeedItem.AstronautItem -> AstronautCard(astronaut = item.astronaut)
                                is FeedItem.AdItem -> NativeAdCard(nativeAd = item.ad)
                                is FeedItem.ExpeditionItem -> ExpeditionCard(expedition = item.expedition)
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

@Composable
fun ExpeditionCard(expedition: Expedition) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, expedition.url.toUri()))
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(expedition.imageUrl)
                        .transformations(TopCropTransformation())
                        .build()
                ),
                contentDescription = "Expedition ${expedition.number}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(model = expedition.patchUrl),
                        contentDescription = "Expedition Patch",
                        modifier = Modifier
                            .size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Expedition ${expedition.number}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Launched on ${DateUtils.formatLaunchDate(expedition.startDate)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Return on ${DateUtils.formatLaunchDate(expedition.endDate)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = expedition.bio, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SocialIcon(
                        iconRes = R.drawable.ic_social_nasa,
                        modifier = Modifier
                            .size(height = 32.dp, width = 104.dp)
                            .clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://www.nasa.gov/mission/expedition-${expedition.number}/".toUri()
                                    )
                                )
                            }
                    )
                    if (expedition.url.isNotBlank()) {
                        SocialIcon(
                            iconRes = R.drawable.ic_wikipedia_social_icon,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            expedition.url.toUri()
                                        )
                                    )
                                }
                        )
                    }
                    SocialIcon(
                        iconRes = R.drawable.ic_google_social_icon,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                val searchQuery = "Expedition ${expedition.number}"
                                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                                val searchUrl = "https://www.google.com/search?q=$encodedQuery"
                                val intent = Intent(Intent.ACTION_VIEW, searchUrl.toUri())
                                context.startActivity(intent)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun AstronautCard(astronaut: Astronaut) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(astronaut.profileImageUrl)
                        .transformations(TopCropTransformation())
                        .build()
                ),
                contentDescription = astronaut.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(model = "https://flagcdn.com/w320/${astronaut.flagCode.lowercase()}.png"),
                        contentDescription = "Flag",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = astronaut.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${astronaut.role} on ${astronaut.craft}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Launched on ${DateUtils.formatLaunchDate(astronaut.launchDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = astronaut.bio, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!astronaut.instagramUrl.isNullOrBlank()) {
                        SocialIcon(
                            iconRes = R.drawable.ic_instagram_social_icon,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            astronaut.instagramUrl.toUri()
                                        )
                                    )
                                }
                        )
                    }
                    if (!astronaut.facebookUrl.isNullOrBlank()) {
                        SocialIcon(
                            iconRes = R.drawable.ic_facebook_social_icon,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            astronaut.facebookUrl.toUri()
                                        )
                                    )
                                }
                        )
                    }
                    SocialIcon(
                        iconRes = R.drawable.ic_google_social_icon,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                val searchQuery = astronaut.name
                                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                                val searchUrl = "https://www.google.com/search?q=$encodedQuery"
                                val intent = Intent(Intent.ACTION_VIEW, searchUrl.toUri())
                                context.startActivity(intent)
                            }
                    )
                    if (!astronaut.twitterUrl.isNullOrBlank()) {
                        SocialIcon(
                            iconRes = R.drawable.ic_x_social_icon,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            astronaut.twitterUrl.toUri()
                                        )
                                    )
                                }
                        )
                    }
                    if (astronaut.bioUrl.isNotBlank()) {
                        SocialIcon(
                            iconRes = R.drawable.ic_wikipedia_social_icon,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            astronaut.bioUrl.toUri()
                                        )
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialIcon(
    modifier: Modifier,
    iconRes: Int,
) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = modifier,
        tint = Color.Unspecified
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