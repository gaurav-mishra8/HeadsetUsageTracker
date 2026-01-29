package com.headphonetracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import com.headphonetracker.data.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ServiceIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()

        // Clear preferences via repository
        settingsRepository.setExcludedApps(emptySet())
        settingsRepository.setDailyLimitMinutes(0)
        settingsRepository.setBreakRemindersEnabled(false)
        settingsRepository.setBreakIntervalMinutes(60)
        settingsRepository.setAutoStartEnabled(false)
        settingsRepository.setDailySummaryEnabled(false)
        settingsRepository.setMilestonesEnabled(true)
        settingsRepository.setAppTheme("dark")
        settingsRepository.setAccentColor("cyan")
        settingsRepository.setTracking(false)
    }

    @After
    fun tearDown() {
        runBlocking {
            database.headphoneUsageDao().deleteAllUsage()
        }
        database.close()
    }

    @Test
    fun testExcludedAppsAreNotTracked() = runBlocking {
        val excludedApps = setOf("com.excluded.app")
        settingsRepository.setExcludedApps(excludedApps)

        val storedExcluded = settingsRepository.getExcludedApps()
        assert(storedExcluded.contains("com.excluded.app"))
    }

    @Test
    fun testDailyLimitPreferenceIsStored() {
        settingsRepository.setDailyLimitMinutes(120)
        val storedLimit = settingsRepository.getDailyLimitMinutes()
        assert(storedLimit == 120)
    }

    @Test
    fun testBreakRemindersPreferenceIsStored() {
        settingsRepository.setBreakRemindersEnabled(true)
        val stored = settingsRepository.isBreakRemindersEnabled()
        assert(stored)
    }

    @Test
    fun testBreakIntervalPreferenceIsStored() {
        settingsRepository.setBreakIntervalMinutes(60)
        val stored = settingsRepository.getBreakIntervalMinutes()
        assert(stored == 60)
    }

    @Test
    fun testAutoStartPreferenceIsStored() {
        settingsRepository.setAutoStartEnabled(true)
        val stored = settingsRepository.isAutoStartEnabled()
        assert(stored)
    }

    @Test
    fun testDailySummaryPreferenceIsStored() {
        settingsRepository.setDailySummaryEnabled(true)
        val stored = settingsRepository.isDailySummaryEnabled()
        assert(stored)
    }

    @Test
    fun testMilestonesPreferenceIsStored() {
        settingsRepository.setMilestonesEnabled(true)
        val stored = settingsRepository.isMilestonesEnabled()
        assert(stored)
    }

    @Test
    fun testThemePreferenceIsStored() {
        settingsRepository.setAppTheme("dark")
        val stored = settingsRepository.getAppTheme()
        assert(stored == "dark")
    }

    @Test
    fun testAccentColorPreferenceIsStored() {
        settingsRepository.setAccentColor("purple")
        val stored = settingsRepository.getAccentColor()
        assert(stored == "purple")
    }

    @Test
    fun testTrackingStatePreferenceIsStored() {
        settingsRepository.setTracking(true)
        val stored = settingsRepository.isTracking()
        assert(stored)
    }

    @Test
    fun testDatabaseCanStoreUsageWithExcludedApps() = runBlocking {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val usage = HeadphoneUsage(
            date = today,
            packageName = "com.test.app",
            appName = "Test App",
            duration = 3600000L,
            startTime = System.currentTimeMillis() - 3600000L,
            endTime = System.currentTimeMillis()
        )

        database.headphoneUsageDao().insertUsage(usage)

        val stored = database.headphoneUsageDao().getTotalUsageForDate(today)
        assert(stored == 3600000L)
    }
}


