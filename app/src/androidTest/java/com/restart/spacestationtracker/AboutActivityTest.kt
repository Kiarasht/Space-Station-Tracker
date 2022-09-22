package com.restart.spacestationtracker

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutActivityTest {

    @Test
    fun testEvent() {
        launchActivity<AboutActivity>().use {
            onView(withId(R.id.about_title)).check(matches(withText(R.string.app_name)))
        }
    }
}