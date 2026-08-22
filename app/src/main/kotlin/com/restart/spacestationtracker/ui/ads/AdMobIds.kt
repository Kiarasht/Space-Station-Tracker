package com.restart.spacestationtracker.ui.ads

import android.content.Context
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.R

object AdMobIds {
    private const val TEST_BANNER_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_NATIVE_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/9257395921"

    private val usesTestAds: Boolean
        get() = BuildConfig.DEBUG || BuildConfig.USE_ADMOB_TEST_ADS

    fun banner(context: Context): String {
        return if (usesTestAds) {
            TEST_BANNER_AD_UNIT_ID
        } else {
            context.getString(R.string.banner_ad_unit_id)
        }
    }

    fun nativeForPasses(context: Context): String {
        return if (usesTestAds) {
            TEST_NATIVE_AD_UNIT_ID
        } else {
            context.getString(R.string.locations_native_ad_unit_id)
        }
    }

    fun nativeForCrew(context: Context): String {
        return if (usesTestAds) {
            TEST_NATIVE_AD_UNIT_ID
        } else {
            context.getString(R.string.on_duty_native_ad_unit_id)
        }
    }

    fun appOpen(context: Context): String {
        return if (usesTestAds) {
            TEST_APP_OPEN_AD_UNIT_ID
        } else {
            context.getString(R.string.app_open_ad_unit_id)
        }
    }

}
