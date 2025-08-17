package com.restart.spacestationtracker.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.Astronaut
import com.restart.spacestationtracker.util.TopCrop

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.recyclerview.widget.RecyclerView
import androidx.core.net.toUri

class PeopleInSpaceAdapter(
    private val activity: AppCompatActivity,
    private var listItems: List<Any>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ad_native_layout, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.people_row, parent, false)
            PeopleInSpaceAdapterViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_ITEM -> {
                val itemHolder = holder as PeopleInSpaceAdapterViewHolder
                itemHolder.bind(listItems[position] as Astronaut)
            }
            VIEW_TYPE_AD -> {
                val adHolder = holder as AdViewHolder
                adHolder.bind(listItems[position] as NativeAd)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (listItems[position] is Astronaut) VIEW_TYPE_ITEM else VIEW_TYPE_AD
    }

    override fun getItemCount(): Int = listItems.size

    fun findPositionOf(astronaut: Astronaut): Int {
        return listItems.indexOf(astronaut)
    }

    private fun startCustomTab(url: String) {
        val intent = CustomTabsIntent.Builder().apply {
            setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary))
                    .build()
            )
            setUrlBarHidingEnabled(true)
            setShowTitle(true)
        }.build()
        intent.launchUrl(activity, url.toUri())
    }

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

    inner class PeopleInSpaceAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val profileImage: ImageView = view.findViewById(R.id.img)
        private val astronautPictureProgress: ProgressBar =
            view.findViewById(R.id.astronaut_picture_progress)
        private val countryFlag: ImageView = view.findViewById(R.id.countryFlag)
        private val astronautTwitter: ImageView = view.findViewById(R.id.astronautTwitter)
        private val astronautFacebook: ImageView = view.findViewById(R.id.astronautFacebook)
        private val astronautInstagram: ImageView = view.findViewById(R.id.astronautInstagram)
        private val astronautWiki: ImageView = view.findViewById(R.id.astronautWiki)
        private val astronautGoogle: ImageView = view.findViewById(R.id.astronautGoogle)
        private val name: TextView = view.findViewById(R.id.name)
        private val role: TextView = view.findViewById(R.id.role)
        private val date: TextView = view.findViewById(R.id.days_since_launch)
        private val bio: TextView = view.findViewById(R.id.bio)

        @SuppressLint("SetTextI18n")
        fun bind(astronaut: Astronaut) {
            val location = if (astronaut.isIss) "ISS" else "Tiangong"
            val firstName = astronaut.name.substringBefore(" ")

            setupSocialLink(
                astronautTwitter,
                astronaut.twitter,
                R.drawable.ic_action_twitter,
                R.drawable.ic_action_twitter_grey,
                "$firstName ${activity.getString(R.string.errorNoTwitter)}"
            )
            setupSocialLink(
                astronautInstagram,
                astronaut.instagram,
                R.drawable.ic_action_instagram,
                R.drawable.ic_action_instagram_grey,
                "$firstName ${activity.getString(R.string.errorNoInstagram)}"
            )
            setupSocialLink(
                astronautFacebook,
                astronaut.facebook,
                R.drawable.ic_action_facebook,
                R.drawable.ic_action_facebook_grey,
                "$firstName ${activity.getString(R.string.errorNoFacebook)}"
            )
            setupSocialLink(
                astronautWiki,
                astronaut.wiki,
                R.drawable.ic_wikipedia,
                R.drawable.ic_wikipedia_gray,
                "$firstName ${activity.getString(R.string.errorNoWiki)}"
            )

            astronautGoogle.setOnClickListener { startCustomTab("https://www.google.com/search?q=${astronaut.name}") }

            name.text = astronaut.name
            role.text = "${astronaut.role} ${activity.getString(R.string.midAt)} $location"
            bio.text = astronaut.bio

            try {
                date.text =
                    activity.getString(R.string.since) + dateFormat.format(Date(astronaut.launchDate * 1000L))
                date.visibility = View.VISIBLE
            } catch (e: Exception) {
                date.visibility = View.INVISIBLE
            }

            Glide.with(activity).load(astronaut.image).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    astronautPictureProgress.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    astronautPictureProgress.visibility = View.GONE
                    return false
                }
            }).transform(TopCrop()).error(R.drawable.ic_failure_profile).into(profileImage)

            Glide.with(activity).load("https://flagsapi.com/${astronaut.flagCode}/flat/64.png")
                .into(countryFlag)
        }

        private fun setupSocialLink(
            imageView: ImageView,
            url: String,
            @DrawableRes activeIconRes: Int,
            @DrawableRes inactiveIconRes: Int,
            errorToastString: String
        ) {
            val isActive = url.isNotEmpty()
            imageView.setImageResource(if (isActive) activeIconRes else inactiveIconRes)
            imageView.setOnClickListener {
                if (isActive) {
                    startCustomTab(url)
                } else {
                    Toast.makeText(activity, errorToastString, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_AD = 1
    }
}