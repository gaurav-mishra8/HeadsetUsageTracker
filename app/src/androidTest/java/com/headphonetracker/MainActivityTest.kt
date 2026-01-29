package com.headphonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @Test
    fun mainActivityLaunches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Check if the main elements are visible
        onView(withId(R.id.tvTotalTime))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnToggleTracking))
            .check(matches(isDisplayed()))

        onView(withId(R.id.pieChart))
            .check(matches(isDisplayed()))

        onView(withId(R.id.barChart))
            .check(matches(isDisplayed()))

        onView(withId(R.id.rvAppList))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun toggleButtonIsClickable() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnToggleTracking))
            .check(matches(isClickable()))

        scenario.close()
    }

    @Test
    fun totalTimeTextViewIsDisplayed() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.tvTotalTime))
            .check(matches(isDisplayed()))

        scenario.close()
    }
}
