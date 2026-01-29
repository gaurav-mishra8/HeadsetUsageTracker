package com.headphonetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class HeadphoneUsageDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: HeadphoneUsageDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.headphoneUsageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUsageAndRetrieve() = runBlocking {
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )

        dao.insertUsage(usage)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val usages = dao.getUsageByDate("2024-01-01").first()

        assertEquals(1, usages.size)
        assertEquals("com.example.app", usages[0].packageName)
        assertEquals("Example App", usages[0].appName)
        assertEquals(4000L, usages[0].duration)
    }

    @Test
    fun insertMultipleUsagesAndGetTotal() = runBlocking {
        val date = "2024-01-01"
        val usage1 = HeadphoneUsage(
            packageName = "com.app1",
            appName = "App 1",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = date
        )
        val usage2 = HeadphoneUsage(
            packageName = "com.app2",
            appName = "App 2",
            startTime = 6000L,
            endTime = 10000L,
            duration = 4000L,
            date = date
        )

        dao.insertUsage(usage1)
        dao.insertUsage(usage2)

        val total = dao.getTotalUsageForDate(date)
        assertEquals(8000L, total)
    }

    @Test
    fun getUsageByAppForDate() = runBlocking {
        val date = "2024-01-01"
        val usage1 = HeadphoneUsage(
            packageName = "com.app1",
            appName = "App 1",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = date
        )
        val usage2 = HeadphoneUsage(
            packageName = "com.app1",
            appName = "App 1",
            startTime = 6000L,
            endTime = 10000L,
            duration = 4000L,
            date = date
        )
        val usage3 = HeadphoneUsage(
            packageName = "com.app2",
            appName = "App 2",
            startTime = 11000L,
            endTime = 15000L,
            duration = 4000L,
            date = date
        )

        dao.insertUsage(usage1)
        dao.insertUsage(usage2)
        dao.insertUsage(usage3)

        val summaries = dao.getUsageByAppForDate(date)

        assertEquals(2, summaries.size)
        val app1Summary = summaries.find { it.packageName == "com.app1" }
        assertNotNull(app1Summary)
        assertEquals(8000L, app1Summary?.totalDuration)

        val app2Summary = summaries.find { it.packageName == "com.app2" }
        assertNotNull(app2Summary)
        assertEquals(4000L, app2Summary?.totalDuration)
    }

    @Test
    fun getLast7DaysUsage() = runBlocking {
        val dates = listOf("2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04")

        dates.forEachIndexed { index, date ->
            val usage = HeadphoneUsage(
                packageName = "com.app1",
                appName = "App 1",
                startTime = 1000L,
                endTime = 5000L,
                duration = (index + 1) * 1000L,
                date = date
            )
            dao.insertUsage(usage)
        }

        val last7Days = dao.getLast7DaysUsage()

        assertTrue(last7Days.size >= 4)
        // Should be ordered by date DESC
        assertEquals("2024-01-04", last7Days[0].date)
    }

    @Test
    fun getTotalUsageForDateWithNoData() = runBlocking {
        val total = dao.getTotalUsageForDate("2024-01-01")
        assertNull(total)
    }

    @Test
    fun deleteOldData() = runBlocking {
        val oldDate = "2023-12-01"
        val newDate = "2024-01-01"

        val oldUsage = HeadphoneUsage(
            packageName = "com.app1",
            appName = "App 1",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = oldDate
        )
        val newUsage = HeadphoneUsage(
            packageName = "com.app2",
            appName = "App 2",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = newDate
        )

        dao.insertUsage(oldUsage)
        dao.insertUsage(newUsage)

        dao.deleteOldData("2024-01-01")

        val oldUsages = dao.getUsageByDate(oldDate).first()
        val newUsages = dao.getUsageByDate(newDate).first()

        assertEquals(0, oldUsages.size)
        assertEquals(1, newUsages.size)
    }

    @Test
    fun insertUsageWithSameIdReplaces() = runBlocking {
        val usage1 = HeadphoneUsage(
            id = 1L,
            packageName = "com.app1",
            appName = "App 1",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )
        val usage2 = HeadphoneUsage(
            id = 1L,
            packageName = "com.app2",
            appName = "App 2",
            startTime = 1000L,
            endTime = 5000L,
            duration = 5000L,
            date = "2024-01-01"
        )

        dao.insertUsage(usage1)
        dao.insertUsage(usage2)

        val usages = dao.getUsageByDate("2024-01-01").first()
        assertEquals(1, usages.size)
        assertEquals("com.app2", usages[0].packageName)
        assertEquals(5000L, usages[0].duration)
    }
}
