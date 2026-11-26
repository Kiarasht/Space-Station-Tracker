package com.restart.spacestationtracker.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.shared.ui.HostedPages
import com.restart.spacestationtracker.shared.ui.SharedAboutScreen
import com.restart.spacestationtracker.util.AppRatingManager

@Composable
fun SharedAboutRoute(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val versionText = stringResource(
        R.string.version_details_format,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE
    )
    SharedAboutScreen(
        contentPadding = contentPadding,
        versionText = versionText,
        onContactSupport = {
            AppAnalytics.trackInteraction("contact_support", "about")
            openHostedPage(context, HostedPages.Support)
        },
        onRateApp = {
            AppAnalytics.trackInteraction("rate_app", "about")
            AppRatingManager(context).markRatedAndOpenStore()
        },
        onShareApp = {
            AppAnalytics.trackInteraction("share_app", "about")
            shareApp(context)
        },
        onPrivacyPolicy = {
            AppAnalytics.trackInteraction("open_privacy_policy", "about")
            openHostedPage(context, HostedPages.PrivacyPolicy)
        },
        onTermsOfUse = {
            AppAnalytics.trackInteraction("open_terms_of_use", "about")
            openHostedPage(context, HostedPages.TermsOfUse)
        }
    )
}

/** Our hosted pages (Terms, Privacy, Support) open in a Custom Tab over the app (Blueprint §14.9). */
private fun openHostedPage(context: Context, url: String) {
    val uri = url.toUri()
    val customTab = CustomTabsIntent.Builder().setShowTitle(true).build().intent.apply { data = uri }
    try {
        context.startActivity(customTab)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareApp(context: Context) {
    val text = context.getString(
        R.string.share_app_text,
        context.getString(R.string.msg_get_it_on_play_store_url)
    )
    try {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                context.getString(R.string.share_app)
            )
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_share_app_available, Toast.LENGTH_SHORT).show()
    }
}
