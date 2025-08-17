package com.restart.spacestationtracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.appbar.AppBarLayout
import com.restart.spacestationtracker.adapter.LocationAdapter
import com.restart.spacestationtracker.data.SightSee
import com.restart.spacestationtracker.util.FLYBYS_URL

import org.json.JSONException

import java.io.IOException
import java.util.Date

class Locations : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var adapter: LocationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var requestQueue: RequestQueue
    private lateinit var longitude: String
    private lateinit var latitude: String
    private lateinit var elevation: String
    private lateinit var loading: ProgressBar
    private lateinit var adLoader: AdLoader

    private val loadedAds = mutableListOf<NativeAd>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
        setContentView(R.layout.locations_layout)

        val container = findViewById<View>(R.id.container)
        imageView = findViewById(R.id.image)
        recyclerView = findViewById(R.id.recycler)
        loading = findViewById(R.id.location_loading)
        val appBarLayout = findViewById<AppBarLayout>(R.id.app_bar)

        ViewCompat.setOnApplyWindowInsetsListener(container) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(insets.left, 0, insets.right, 0)
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.isNestedScrollingEnabled = true
        adapter = LocationAdapter(this, emptyList())
        recyclerView.adapter = adapter
        recyclerView.visibility = View.GONE

        requestQueue = Volley.newRequestQueue(this)
        findLocationAndFetchData()
    }

    override fun onPause() {
        super.onPause()
        requestQueue.cancelAll(TAG)
    }

    private fun findLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, R.string.errorPermissionLocation, Toast.LENGTH_LONG).show()
            return
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location: Location? =
            locationManager.getProviders(true).asReversed().firstNotNullOfOrNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }

        if (location != null) {
            latitude = location.latitude.toString()
            longitude = location.longitude.toString()
            elevation = location.altitude.toString()

            val url = getStaticMapUrl()

            try {
                val matches =
                    Geocoder(this).getFromLocation(location.latitude, location.longitude, 1)
                val bestMatch = matches?.firstOrNull()
                if (bestMatch != null) {
                    SightSee.location = formatAddress(bestMatch)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            Glide.with(this).load(url).into(imageView)
            imageView.imageAlpha = 150
            displayPasses(null, null, null)
        } else {
            Toast.makeText(this, R.string.errorLocation, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAndInsertAds(originalList: List<SightSee>) {
        if (originalList.isEmpty()) {
            setupRecyclerView(originalList)
            return
        }

        val adsToRequest = originalList.size / ITEMS_PER_AD
        if (adsToRequest <= 0) {
            setupRecyclerView(originalList)
            return
        }

        loadedAds.clear()

        adLoader = AdLoader.Builder(this, getString(R.string.locations_native_ad_unit_id))
            .forNativeAd { nativeAd ->
                loadedAds.add(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.e(TAG, "Native ad failed to load: ${loadAdError.message}")
                    if (!adLoader.isLoading) {
                        insertAdsInList(originalList)
                    }
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    if (!adLoader.isLoading) {
                        insertAdsInList(originalList)
                    }
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d(TAG, "Native ad onAdImpression")
                }
            })
            .build()

        adLoader.loadAds(AdRequest.Builder().build(), adsToRequest)
    }

    private fun insertAdsInList(originalList: List<SightSee>) {
        if (loadedAds.isEmpty()) {
            setupRecyclerView(originalList)
            return
        }
        val combinedList = mutableListOf<Any>()
        var adIndex = 0
        originalList.forEachIndexed { index, sightSee ->
            combinedList.add(sightSee)
            if ((index + 1) % ITEMS_PER_AD == 0 && adIndex < loadedAds.size) {
                combinedList.add(loadedAds[adIndex])
                adIndex++
            }
        }
        setupRecyclerView(combinedList)
    }

    private fun setupRecyclerView(list: List<Any>) {
        adapter = LocationAdapter(this, list)
        recyclerView.adapter = adapter
        loading.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun formatAddress(address: Address): String = buildString {
        address.locality?.let { append("$it, ") }
        address.adminArea?.let { append("$it ") }
        address.countryCode?.let { append("$it ") }
        address.postalCode?.let { append(it) }
    }.trim()

    private fun getStaticMapUrl(): String {
        var url = buildString {
            append("https://maps.googleapis.com/maps/api/staticmap?")
            append("center=LAT,LNG&")
            append("zoom=11&")
            append("scale=1&")
            append("size=640x640&")
            append("maptype=terrain&")
            append("style=feature:road|visibility:off&")
            append("style=feature:poi|visibility:off&")
            append("style:feature:landscape|visibility:off&")
            append("style:feature:transit|visibility:off&")
            append("style:feature:administrative.province|visibility:off&")
            append("style:feature:administrative.neighborhood|visibility:off&")
            append("markers=color:red%7C$latitude,$longitude&markers=size:tiny&")
            append("key=AIzaSyAtpWPhzhbtqTgofnQhAHjiG12MmrY2AAE")
        }

        url = url.replace("LAT", latitude)
        url = url.replace("LNG", longitude)
        return url
    }

    fun displayPasses(lat: String?, lon: String?, applicationContext: Context?): MutableList<Date> {
        val passes = mutableListOf<Date>()
        val url = getPassesApiUrl(lat, lon)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val passCount = response.getJSONObject("info").getInt("passescount")
                    if (passCount > 0) {
                        val results = response.getJSONArray("passes")
                        val sightSees = mutableListOf<SightSee>()

                        for (i in 0 until results.length()) {
                            val aPass = results.getJSONObject(i)
                            passes.add(Date(aPass.getLong("startUTC") * 1000L))

                            val aSightSee = SightSee(
                                startUTC = aPass.getLong("startUTC"),
                                endUTC = aPass.getLong("endUTC"),
                                durationSeconds = aPass.getInt("duration"),
                                magnitude = aPass.getDouble("mag"),
                                maxElevation = aPass.getDouble("maxEl"),
                                startAzimuthCompass = aPass.getString("startAzCompass"),
                                endAzimuthCompass = aPass.getString("endAzCompass"),
                                location = SightSee.location
                            )
                            sightSees.add(aSightSee)
                        }

                        loadAndInsertAds(sightSees)

                    } else {
                        Toast.makeText(
                            this@Locations,
                            R.string.nothing_available,
                            Toast.LENGTH_LONG
                        ).show()
                        loading.visibility = View.GONE
                        recyclerView.visibility = View.GONE
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this@Locations, R.string.unable_to_get_passes, Toast.LENGTH_LONG)
                        .show()
                    loading.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                }
            },
            {
                if (lat == null && lon == null) {
                    Toast.makeText(this@Locations, R.string.unable_to_get_passes, Toast.LENGTH_LONG)
                        .show()
                    loading.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                }
            })

        if (applicationContext != null) {
            Volley.newRequestQueue(applicationContext).add(jsonObjectRequest)
        } else {
            jsonObjectRequest.tag = TAG
            requestQueue.add(jsonObjectRequest)
        }

        return passes
    }

    private fun getPassesApiUrl(lat: String?, lon: String?): String {
        return if (lat == null && lon == null) {
            "$FLYBYS_URL$ISS_NORAD_ID/$latitude/$longitude/$elevation/$ISS_RESULT_DAYS/$ISS_MIN_VISIBILITY/&apiKey=$ISS_TRACKER_API"
        } else {
            "http://api.open-notify.org/iss-pass.json?lat=$lat&lon=$lon"
        }
    }

    companion object {
        private const val TAG = ".Locations"
        private const val ISS_NORAD_ID = 25544
        private const val ISS_RESULT_DAYS = 10
        private const val ISS_MIN_VISIBILITY = 300
        private const val ISS_TRACKER_API = "HPD8AL-KBDGWE-JS2M48-4XH2"
        private const val ITEMS_PER_AD = 4
    }
}