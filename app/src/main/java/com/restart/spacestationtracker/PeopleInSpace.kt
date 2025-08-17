package com.restart.spacestationtracker

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.airbnb.lottie.LottieAnimationView
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.adapter.PeopleInSpaceAdapter
import com.restart.spacestationtracker.data.Astronaut
import com.restart.spacestationtracker.util.ON_DUTY_URL
import com.restart.spacestationtracker.util.WIKI_BIO_URL

import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

import java.nio.charset.Charset

class PeopleInSpace : AppCompatActivity() {
    private lateinit var adapter: PeopleInSpaceAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var requestQueue: RequestQueue
    private lateinit var animation: LottieAnimationView
    private lateinit var errors: TextView
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
        setContentView(R.layout.people_in_space_layout)

        val mainLayout = findViewById<View>(R.id.main_layout)
        recyclerView = findViewById(R.id.recycler)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                insets.top,
                recyclerView.paddingRight,
                insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        recyclerView.clipToPadding = false

        requestQueue = Volley.newRequestQueue(this)
        displayPeople()

        animation = findViewById(R.id.animation_view)
        errors = findViewById(R.id.errors)
    }

    private fun onSuccessResult(peopleInSpace: MutableList<Astronaut>) {
        loadAndInsertAds(peopleInSpace)
        requestWiki(peopleInSpace)
    }

    private fun loadAndInsertAds(originalList: List<Astronaut>) {
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

        adLoader = AdLoader.Builder(this, getString(R.string.on_duty_native_ad_unit_id))
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
            })
            .build()

        adLoader.loadAds(AdRequest.Builder().build(), adsToRequest)
    }

    private fun insertAdsInList(originalList: List<Astronaut>) {
        if (loadedAds.isEmpty()) {
            setupRecyclerView(originalList)
            return
        }
        val combinedList = mutableListOf<Any>()
        var adIndex = 0
        originalList.forEachIndexed { index, astronaut ->
            combinedList.add(astronaut)
            if ((index + 1) % ITEMS_PER_AD == 0 && adIndex < loadedAds.size) {
                combinedList.add(loadedAds[adIndex])
                adIndex++
            }
        }
        setupRecyclerView(combinedList)
    }

    private fun setupRecyclerView(list: List<Any>) {
        adapter = PeopleInSpaceAdapter(this, list)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.adapter = adapter
        animation.visibility = View.GONE
        errors.visibility = View.GONE
    }

    private fun onFailure(peopleInSpace: MutableList<Astronaut>) {
        if (peopleInSpace.isEmpty()) {
            animation.visibility = View.VISIBLE
            errors.visibility = View.VISIBLE
        }
    }

    private fun displayPeople() {
        val peopleInSpace = mutableListOf<Astronaut>()

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, ON_DUTY_URL, null,
            {
                onSuccessResult(peopleInSpace)
            },
            {
                onFailure(peopleInSpace)
            }
        ) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
                return try {
                    val jsonString = String(
                        response.data,
                        Charset.forName(
                            HttpHeaderParser.parseCharset(
                                response.headers,
                                PROTOCOL_CHARSET
                            )
                        )
                    )
                    val astronautJson = JSONObject(jsonString)
                    val astronauts = astronautJson.getJSONArray("people")

                    for (i in 0 until astronauts.length()) {
                        val anAstronaut = astronauts.getJSONObject(i)
                        val astronaut = Astronaut(
                            name = anAstronaut.getString("name"),
                            image = anAstronaut.getString("image"),
                            isIss = anAstronaut.getBoolean("iss"),
                            flagCode = anAstronaut.getString("flag_code").uppercase(),
                            launchDate = anAstronaut.getInt("launched"),
                            role = anAstronaut.getString("position"),
                            location = anAstronaut.getString("spacecraft"),
                            wiki = anAstronaut.getString("url"),
                            twitter = anAstronaut.getString("twitter"),
                            facebook = anAstronaut.getString("facebook"),
                            instagram = anAstronaut.getString("instagram"),
                            bio = ""
                        )
                        peopleInSpace.add(astronaut)
                    }
                    Response.success(
                        JSONObject(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response)
                    )
                } catch (e: Exception) {
                    when (e) {
                        is UnsupportedEncodingException, is JSONException -> Response.error(
                            ParseError(e)
                        )

                        else -> throw e
                    }
                }
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

    private fun requestWiki(astronauts: MutableList<Astronaut>) {
        astronauts.forEach { astronaut ->
            val wikiPage = astronaut.wiki.split("/")
            val bioUrl = "$WIKI_BIO_URL${wikiPage.last()}"

            val wikiRequest = object : JsonObjectRequest(
                Method.GET, bioUrl, null,
                { response ->
                    try {
                        val pages = response.getJSONObject("query").getJSONObject("pages")
                        val innerObject = pages.getJSONObject(pages.keys().next())
                        var bio = innerObject.getString("extract")
                        while (bio.endsWith("\n")) {
                            bio = bio.substring(0, bio.length - 2)
                        }
                        astronaut.bio = bio
                        if (::adapter.isInitialized) {
                            val position = adapter.findPositionOf(astronaut)
                            if (position != -1) {
                                adapter.notifyItemChanged(position)
                            }
                        }
                    } catch (e: JSONException) {
                        astronaut.bio = ""
                    }
                },
                { }
            ) {
                override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
                    return try {
                        val bioJson = String(
                            response.data,
                            Charset.forName(
                                HttpHeaderParser.parseCharset(
                                    response.headers,
                                    PROTOCOL_CHARSET
                                )
                            )
                        )
                        Response.success(
                            JSONObject(bioJson),
                            HttpHeaderParser.parseCacheHeaders(response)
                        )
                    } catch (e: Exception) {
                        when (e) {
                            is UnsupportedEncodingException, is JSONException -> Response.error(
                                ParseError(e)
                            )

                            else -> throw e
                        }
                    }
                }
            }
            requestQueue.add(wikiRequest)
        }
    }

    companion object {
        private const val TAG = "PeopleInSpace"
        private const val ITEMS_PER_AD = 4
    }
}