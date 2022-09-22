package com.restart.spacestationtracker

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapsActivityTest {
    @get:Rule
    val rule = activityScenarioRule<MapsActivity>()

    @Test
    fun testMapsActivityActionBar() {
        onView(withId(R.id.title_text)).check(matches(withText(R.string.map_activity)))
    }
}