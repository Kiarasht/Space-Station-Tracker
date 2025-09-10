package com.restart.spacestationtracker.ui.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AboutAppCard(versionName = versionName)
            }
            item {
                LegalJibberJabberCard(onNavigateToLegal = onNavigateToLegal)
            }
            item {
                LicensesCard(softwareList = softwareList, softwareUrlList = softwareUrlList)
            }
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun AboutAppCard(versionName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = stringResource(id = R.string.desc_about_app),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = versionName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.msg_about_library_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LicensesCard(softwareList: Array<String>, softwareUrlList: Array<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(id = R.string.license),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            softwareList.forEachIndexed { index, software ->
                ClickableLink(
                    text = software,
                    url = softwareUrlList[index]
                )
                Spacer(modifier = Modifier.height(4.dp))
                ClickableLink(
                    text = stringResource(id = R.string.apache_open_source_url),
                    url = stringResource(id = R.string.apache_open_source_url)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LegalJibberJabberCard(onNavigateToLegal: (titleResId: Int, contentResId: Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(id = R.string.legal_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            ClickableNavText(
                text = stringResource(id = R.string.privacy_policy),
                onClick = {
                    onNavigateToLegal(
                        R.string.privacy_policy,
                        R.string.privacy_policy_content
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClickableNavText(
                text = stringResource(id = R.string.terms_of_use),
                onClick = {
                    onNavigateToLegal(
                        R.string.terms_of_use,
                        R.string.terms_of_use_content
                    )
                }
            )
        }
    }
}

@Composable
private fun ClickableLink(text: String, url: String) {
    val context = LocalContext.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            }
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun ClickableNavText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
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
