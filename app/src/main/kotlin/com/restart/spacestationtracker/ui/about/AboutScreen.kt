package com.restart.spacestationtracker.ui.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import com.restart.spacestationtracker.R

@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    onNavigateToLegal: (titleResId: Int, contentResId: Int) -> Unit
) {
    val context = LocalContext.current
    val versionName = getVersionName(context)
    val softwareList = stringArrayResource(id = R.array.software_list)
    val softwareUrlList = stringArrayResource(id = R.array.software_url_list)

    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        bottom = contentPadding.calculateTopPadding() + 16.dp
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            contentPadding = screenPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AboutAppHeader(versionName = versionName)
            }
            item {
                AboutFeatureHighlights()
            }
            item {
                AboutSection(title = stringResource(id = R.string.legal_title)) {
                    AboutNavRow(
                        icon = Icons.Default.PrivacyTip,
                        title = stringResource(id = R.string.privacy_policy),
                        onClick = {
                            onNavigateToLegal(
                                R.string.privacy_policy,
                                R.string.privacy_policy_content
                            )
                        }
                    )
                    HorizontalDivider()
                    AboutNavRow(
                        icon = Icons.AutoMirrored.Filled.Article,
                        title = stringResource(id = R.string.terms_of_use),
                        onClick = {
                            onNavigateToLegal(
                                R.string.terms_of_use,
                                R.string.terms_of_use_content
                            )
                        }
                    )
                }
            }
            item {
                AboutSection(title = stringResource(id = R.string.license)) {
                    softwareList.forEachIndexed { index, software ->
                        LicenseRow(
                            software = software,
                            url = softwareUrlList[index]
                        )
                        if (index != softwareList.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(108.dp))
            }
        }
    }
}

@Composable
fun AboutAppHeader(versionName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = stringResource(id = R.string.desc_about_app),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.msg_about_library_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AboutFeatureHighlights() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureHighlight(
                    icon = Icons.Default.Public,
                    title = "Live map",
                    modifier = Modifier.weight(1f)
                )
                FeatureHighlight(
                    icon = Icons.Default.Route,
                    title = "Sky paths",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureHighlight(
                    icon = Icons.Default.NotificationsActive,
                    title = "Pass alerts",
                    modifier = Modifier.weight(1f)
                )
                FeatureHighlight(
                    icon = Icons.Default.LiveTv,
                    title = "Live streams",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureHighlight(
                    icon = Icons.Default.Groups,
                    title = "Crew info",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AboutSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun LicenseRow(software: String, url: String) {
    val context = LocalContext.current
    ListItem(
        modifier = Modifier
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = software,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun AboutNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

private fun getVersionName(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: ""
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
}
