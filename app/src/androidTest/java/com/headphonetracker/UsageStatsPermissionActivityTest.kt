package com.headphonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class UsageStatsPermissionActivityTest {
    
    @Test
    fun permissionActivityLaunches() {
        val scenario = ActivityScenario.launch(UsageStatsPermissionActivity::class.java)
        
        onView(withId(R.id.btnGrantPermission))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
        
        scenario.close()
    }
    
    @Test
    fun grantPermissionButtonIsVisible() {
        val scenario = ActivityScenario.launch(UsageStatsPermissionActivity::class.java)
        
        onView(withId(R.id.btnGrantPermission))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}

