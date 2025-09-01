package com.restart.spacestationtracker.ui.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.restart.spacestationtracker.R

@Composable
fun NativeAdCard(modifier: Modifier = Modifier, nativeAd: NativeAd) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium.copy(topEnd = CornerSize(0.dp))
    ) {
        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val bodyTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        val ctaButtonColor = MaterialTheme.colorScheme.primary.toArgb()
        val ctaButtonTextColor = MaterialTheme.colorScheme.onPrimary.toArgb()

        AndroidView(
            factory = {
                LayoutInflater.from(it).inflate(R.layout.native_ad_layout, null) as NativeAdView
            },
            update = { adView ->
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.mediaView = adView.findViewById(R.id.ad_media)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)
                adView.priceView = adView.findViewById(R.id.ad_price)
                adView.storeView = adView.findViewById(R.id.ad_store)
                adView.starRatingView = adView.findViewById(R.id.ad_stars)
                adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

                (adView.headlineView as? TextView)?.let {
                    if (nativeAd.headline.isNullOrEmpty()) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.text = nativeAd.headline
                        it.setTextColor(textColor)
                    }
                }

                adView.mediaView?.let {
                    if (nativeAd.mediaContent == null) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.mediaContent = nativeAd.mediaContent
                    }
                }

                (adView.bodyView as? TextView)?.let {
                    if (nativeAd.body.isNullOrEmpty()) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.text = nativeAd.body
                        it.setTextColor(bodyTextColor)
                    }
                }

                (adView.callToActionView as? Button)?.let {
                    if (nativeAd.callToAction.isNullOrEmpty()) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.text = nativeAd.callToAction
                        it.setBackgroundColor(ctaButtonColor)
                        it.setTextColor(ctaButtonTextColor)
                    }
                }

                (adView.iconView as? ImageView)?.let {
                    if (nativeAd.icon == null) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.setImageDrawable(nativeAd.icon?.drawable)
                    }
                }

                (adView.priceView as? TextView)?.let {
                    if (nativeAd.price.isNullOrEmpty()) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.text = nativeAd.price
                        it.setTextColor(bodyTextColor)
                    }
                }

                (adView.storeView as? TextView)?.let {
                    if (nativeAd.store.isNullOrEmpty()) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.text = nativeAd.store
                        it.setTextColor(bodyTextColor)
                    }
                }

                (adView.starRatingView as? RatingBar)?.let {
                    if (nativeAd.starRating == null || nativeAd.starRating!! <= 0) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.rating = nativeAd.starRating?.toFloat() ?: 0f
                    }
                }

                (adView.advertiserView as? TextView)?.let {
                    if (nativeAd.advertiser.isNullOrEmpty()) {
                        it.visibility = View.GONE
                    } else {
                        it.visibility = View.VISIBLE
                        it.text = nativeAd.advertiser
                        it.setTextColor(textColor)
                    }
                }

                adView.setNativeAd(nativeAd)
            }
        )
    }
}