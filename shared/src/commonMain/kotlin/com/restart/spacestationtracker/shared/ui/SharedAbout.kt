package com.restart.spacestationtracker.shared.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SharedAboutScreen(
    contentPadding: PaddingValues,
    versionText: String,
    onContactSupport: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onPrivacyPolicy: (() -> Unit)?,
    onTermsOfUse: (() -> Unit)?,
    onLegalPageViewed: ((String) -> Unit)? = null
) {
    var legalPage by remember { mutableStateOf<LegalPage?>(null) }
    legalPage?.let { page ->
        SharedLegalScreen(
            contentPadding = contentPadding,
            title = stringResource(
                if (page == LegalPage.PRIVACY) {
                    Res.string.privacy_policy
                } else {
                    Res.string.terms_of_use
                }
            ),
            content = if (page == LegalPage.PRIVACY) {
                SharedLegalContent.privacyPolicy
            } else {
                SharedLegalContent.termsOfUse
            },
            onBack = {
                legalPage = null
                onLegalPageViewed?.invoke("ABOUT")
            }
        )
        return
    }

    val colors = MaterialTheme.spaceColors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { AboutHero(versionText) }
            item { IssStats() }
            item {
                SpaceSectionCard(title = stringResource(Res.string.support_title)) {
                    SettingsRow(
                        icon = Icons.Default.Email,
                        tint = colors.visibilityFaint,
                        title = stringResource(Res.string.contact_support),
                        subtitle = stringResource(Res.string.contact_support_description),
                        onClick = onContactSupport,
                        trailing = { RowArrow(opensExternally = true) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.StarRate,
                        tint = colors.visibilityVeryBright,
                        title = stringResource(Res.string.rate_app),
                        subtitle = stringResource(Res.string.rate_app_description),
                        onClick = onRateApp,
                        trailing = { RowArrow(opensExternally = true) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Share,
                        tint = colors.success,
                        title = stringResource(Res.string.share_app),
                        subtitle = stringResource(Res.string.share_app_description),
                        onClick = onShareApp,
                        trailing = { RowArrow(opensExternally = true) }
                    )
                }
            }
            item {
                SpaceSectionCard(title = stringResource(Res.string.legal_title)) {
                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        tint = colors.visibilityModerate,
                        title = stringResource(Res.string.privacy_policy),
                        subtitle = null,
                        onClick = onPrivacyPolicy ?: {
                            legalPage = LegalPage.PRIVACY
                            onLegalPageViewed?.invoke("PRIVACY_POLICY")
                            Unit
                        },
                        trailing = { RowArrow(opensExternally = onPrivacyPolicy != null) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Article,
                        tint = colors.visibilityVeryFaint,
                        title = stringResource(Res.string.terms_of_use),
                        subtitle = null,
                        onClick = onTermsOfUse ?: {
                            legalPage = LegalPage.TERMS
                            onLegalPageViewed?.invoke("TERMS_OF_USE")
                            Unit
                        },
                        trailing = { RowArrow(opensExternally = onTermsOfUse != null) }
                    )
                }
            }
        }
    }
}

private enum class LegalPage { PRIVACY, TERMS }

/** App name, version and description on the night sky, above a small animated orbit. */
@Composable
private fun AboutHero(versionText: String) {
    val heroText = MaterialTheme.spaceColors.onHero
    SpaceHeroCard(
        background = SkyNightBrush,
        decoration = { SkyPathCardSparkles(seed = 7L, modifier = Modifier.matchParentSize()) }
    ) {
        OrbitIllustration(modifier = Modifier.fillMaxWidth().height(150.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = heroText,
                textAlign = TextAlign.Center
            )
            SpacePill(
                text = versionText,
                color = MaterialTheme.spaceColors.gold,
                filled = true,
                contentColor = MaterialTheme.spaceColors.onGold
            )
            Text(
                stringResource(Res.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = heroText.copy(alpha = 0.85f)
            )
        }
    }
}

/** Earth with a tilted orbit; the ISS circles it once every 24 seconds, passing behind the planet. */
@Composable
private fun OrbitIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart)
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val earthRadius = size.height * 0.3f
        val orbitWidth = min(size.width / 2f - 20.dp.toPx(), size.height * 1.05f)
        val orbitHeight = orbitWidth * 0.3f
        val orbitTopLeft = Offset(center.x - orbitWidth, center.y - orbitHeight)
        val orbitSize = Size(orbitWidth * 2f, orbitHeight * 2f)
        val orbitStroke = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
        )
        // The far half of the orbit runs above the Earth's center, so the planet covers part of it.
        val issBehind = sin(angle) < 0f
        val iss = Offset(center.x + orbitWidth * cos(angle), center.y + orbitHeight * sin(angle))

        rotate(degrees = OrbitTilt, pivot = center) {
            drawArc(Color.White.copy(alpha = 0.25f), 180f, 180f, false, orbitTopLeft, orbitSize, style = orbitStroke)
            if (issBehind) drawOrbitingIss(iss)
        }
        drawEarth(center, earthRadius)
        rotate(degrees = OrbitTilt, pivot = center) {
            drawArc(Color.White.copy(alpha = 0.45f), 0f, 180f, false, orbitTopLeft, orbitSize, style = orbitStroke)
            if (!issBehind) drawOrbitingIss(iss)
        }
    }
}

private const val OrbitTilt = -14f

private fun DrawScope.drawEarth(center: Offset, radius: Float) {
    drawCircle(Color(0xFF6FA8FF).copy(alpha = 0.16f), radius = radius + 10.dp.toPx(), center = center)
    drawCircle(Color(0xFF6FA8FF).copy(alpha = 0.22f), radius = radius + 4.dp.toPx(), center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF5B9BFF), Color(0xFF1D4ED8), Color(0xFF0B1E5B)),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 1.6f
        ),
        radius = radius,
        center = center
    )
    // A couple of soft land masses so it reads as a planet rather than a ball.
    drawCircle(Color(0xFF34D399).copy(alpha = 0.55f), radius = radius * 0.32f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.15f))
    drawCircle(Color(0xFF34D399).copy(alpha = 0.45f), radius = radius * 0.22f, center = Offset(center.x + radius * 0.35f, center.y + radius * 0.3f))
    drawCircle(Color(0xFF34D399).copy(alpha = 0.4f), radius = radius * 0.14f, center = Offset(center.x + radius * 0.2f, center.y - radius * 0.5f))
}

private fun DrawScope.drawOrbitingIss(position: Offset) {
    drawCircle(Color(0xFFFFD84D).copy(alpha = 0.22f), radius = 14.dp.toPx(), center = position)
    drawIssSatellite(center = position, bodyColor = Color(0xFFFFD84D), panelColor = Color(0xFF9FB4FF), scale = 1.1f)
}

/** Three quick facts about the station, each with its own accent color. */
@Composable
private fun IssStats() {
    val colors = MaterialTheme.spaceColors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("92", stringResource(Res.string.about_stat_orbit), colors.visibilityModerate, Modifier.weight(1f))
        StatTile("16", stringResource(Res.string.about_stat_sunrises), colors.visibilityVeryBright, Modifier.weight(1f))
        StatTile(yearsCrewed().toString(), stringResource(Res.string.about_stat_crewed), colors.live, Modifier.weight(1f))
    }
}

/** Whole years since Expedition 1 arrived on November 2, 2000; the ISS has had people aboard ever since. */
private fun yearsCrewed(): Int {
    val elapsedSeconds = kotlin.time.Clock.System.now().epochSeconds - EXPEDITION_ONE_ARRIVAL_EPOCH_SECONDS
    return (elapsedSeconds / SECONDS_PER_YEAR).toInt()
}

private const val EXPEDITION_ONE_ARRIVAL_EPOCH_SECONDS = 973_123_200L
private const val SECONDS_PER_YEAR = 31_556_952.0

@Composable
private fun StatTile(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RowArrow(opensExternally: Boolean) {
    Icon(
        if (opensExternally) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun SharedLegalScreen(
    contentPadding: PaddingValues,
    title: String,
    content: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilledTonalButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(Res.string.back))
                }
            }
            item {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            item {
                SpaceSectionCard(title = null) {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }
    }
}
