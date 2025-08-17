package com.restart.spacestationtracker.adapter

import android.app.Activity
import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.SightSee
import com.restart.spacestationtracker.ui.SkyPathView

class LocationAdapter(
    private val activity: Activity,
    private var listItems: List<Any>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ad_native_layout, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.location_row, parent, false)
            LocationAdapterViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_ITEM -> {
                val locationHolder = holder as LocationAdapterViewHolder
                locationHolder.bind(listItems[position] as SightSee)
            }
            VIEW_TYPE_AD -> {
                val adHolder = holder as AdViewHolder
                adHolder.bind(listItems[position] as NativeAd)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (listItems[position] is NativeAd) VIEW_TYPE_AD else VIEW_TYPE_ITEM
    }

    override fun getItemCount(): Int = listItems.size

    inner class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val adView: NativeAdView = view.findViewById(R.id.native_ad)
        private val adHeadline: TextView = view.findViewById(R.id.ad_headline)
        private val adAdvertiser: TextView = view.findViewById(R.id.ad_advertiser)
        private val adBody: TextView = view.findViewById(R.id.ad_body)
        private val adCallToAction: Button = view.findViewById(R.id.ad_call_to_action)
        private val adAppIcon: ImageView = view.findViewById(R.id.ad_app_icon)
        private val adMedia: MediaView = view.findViewById(R.id.ad_media)

        init {
            adView.headlineView = adHeadline
            adView.bodyView = adBody
            adView.callToActionView = adCallToAction
            adView.iconView = adAppIcon
            adView.advertiserView = adAdvertiser
            adView.mediaView = adMedia
        }

        fun bind(nativeAd: NativeAd) {
            adHeadline.text = nativeAd.headline
            adBody.text = nativeAd.body
            adCallToAction.text = nativeAd.callToAction
            adAdvertiser.text = nativeAd.advertiser

            nativeAd.icon?.let {
                adAppIcon.setImageDrawable(it.drawable)
                adAppIcon.visibility = View.VISIBLE
            } ?: run {
                adAppIcon.visibility = View.GONE
            }

            adView.setNativeAd(nativeAd)
        }
    }

    inner class LocationAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateHeader: TextView = view.findViewById(R.id.dateHeader)
        private val startTime: TextView = view.findViewById(R.id.startTime)
        private val duration: TextView = view.findViewById(R.id.duration)
        private val brightness: TextView = view.findViewById(R.id.brightness)
        private val skyPathView: SkyPathView = view.findViewById(R.id.skyPathView)
        private val calendarButton: ImageButton = view.findViewById(R.id.calendarButton)
        private val shareButton: ImageButton = view.findViewById(R.id.shareButton)

        fun bind(sightSee: SightSee) {
            dateHeader.text = sightSee.formattedDateHeader
            startTime.text = activity.getString(R.string.starts, sightSee.formattedRiseTime)
            duration.text = activity.getString(R.string.duration, sightSee.formattedDuration)

            val brightnessRating = getBrightnessRating(sightSee.magnitude)
            brightness.text = brightnessRating.displayName
            brightness.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                brightnessRating.drawableRes,
                0
            )

            skyPathView.setPath(
                sightSee.startAzimuthCompass,
                sightSee.endAzimuthCompass,
                sightSee.maxElevation.toFloat()
            )

            calendarButton.setOnClickListener { handleCalendarClick(sightSee) }
            shareButton.setOnClickListener { handleShareClick(sightSee) }
        }

        private fun handleCalendarClick(currentSightSee: SightSee) {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = Events.CONTENT_URI
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentSightSee.riseTimeDate.time)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, currentSightSee.setTimeDate.time)
                putExtra(Events.TITLE, activity.getString(R.string.iss_sighting_share_title))
                putExtra(
                    Events.DESCRIPTION,
                    activity.getString(R.string.iss_sighting_share_description)
                )
                putExtra(Events.EVENT_LOCATION, SightSee.location)
                putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
            }
            activity.startActivity(intent)
        }

        private fun handleShareClick(currentSightSee: SightSee) {
            val shareText = "${activity.getString(R.string.iss_sighting_share_body)} " +
                    "${SightSee.location} ${activity.getString(R.string.on)} " +
                    "${currentSightSee.formattedRiseTime}.\n\n" +
                    activity.getString(R.string.msg_get_it_on_play_store_url)

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            activity.startActivity(
                Intent.createChooser(
                    sendIntent,
                    activity.getString(R.string.iss_sighting_share_chooser)
                )
            )
        }
    }

    private fun getBrightnessRating(magnitude: Double): BrightnessRating {
        return when {
            magnitude < -3.5 -> BRIGHTNESS_LEVELS.getValue("Very Bright")
            magnitude < -2.0 -> BRIGHTNESS_LEVELS.getValue("Bright")
            magnitude < -0.5 -> BRIGHTNESS_LEVELS.getValue("Moderate")
            magnitude < 1.5 -> BRIGHTNESS_LEVELS.getValue("Faint")
            else -> BRIGHTNESS_LEVELS.getValue("Very Faint")
        }
    }

    data class BrightnessRating(val displayName: String, @DrawableRes val drawableRes: Int)

    companion object {
        private val BRIGHTNESS_LEVELS = mapOf(
            "Very Bright" to BrightnessRating("Very Bright", R.drawable.ic_star_rating_5),
            "Bright" to BrightnessRating("Bright", R.drawable.ic_star_rating_4),
            "Moderate" to BrightnessRating("Moderate", R.drawable.ic_star_rating_3),
            "Faint" to BrightnessRating("Faint", R.drawable.ic_star_rating_2),
            "Very Faint" to BrightnessRating("Very Faint", R.drawable.ic_star_rating_1)
        )

        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_AD = 1
    }
}