package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.*
import org.jetbrains.compose.resources.stringResource

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
                            stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            versionText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(Res.string.about_description),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                AboutSection(stringResource(Res.string.support_title)) {
                    AboutRow(
                        Icons.Default.Email,
                        stringResource(Res.string.contact_support),
                        stringResource(Res.string.contact_support_description),
                        onContactSupport
                    )
                    HorizontalDivider()
                    AboutRow(
                        Icons.Default.StarRate,
                        stringResource(Res.string.rate_app),
                        stringResource(Res.string.rate_app_description),
                        onRateApp
                    )
                    HorizontalDivider()
                    AboutRow(
                        Icons.Default.Share,
                        stringResource(Res.string.share_app),
                        stringResource(Res.string.share_app_description),
                        onShareApp
                    )
                }
            }
            item {
                AboutSection(stringResource(Res.string.legal_title)) {
                    AboutRow(
                        Icons.Default.PrivacyTip,
                        stringResource(Res.string.privacy_policy),
                        null,
                        onPrivacyPolicy ?: {
                            legalPage = LegalPage.PRIVACY
                            onLegalPageViewed?.invoke("PRIVACY_POLICY")
                            Unit
                        },
                        opensExternally = onPrivacyPolicy != null
                    )
                    HorizontalDivider()
                    AboutRow(
                        Icons.AutoMirrored.Filled.Article,
                        stringResource(Res.string.terms_of_use),
                        null,
                        onTermsOfUse ?: {
                            legalPage = LegalPage.TERMS
                            onLegalPageViewed?.invoke("TERMS_OF_USE")
                            Unit
                        },
                        opensExternally = onTermsOfUse != null
                    )
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

private enum class LegalPage { PRIVACY, TERMS }

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
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("‹ " + stringResource(Res.string.back))
                }
            }
            item {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AboutSection(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    supportingText: String?,
    onClick: () -> Unit,
    opensExternally: Boolean = true
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Icon(
                if (opensExternally) {
                    Icons.AutoMirrored.Filled.OpenInNew
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}
