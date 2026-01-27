package com.headphonetracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import com.headphonetracker.data.HeadphoneUsageDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class DaoExtendedTest {
    
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
    fun testGetAllUsage() = runBlocking {
        val usage1 = HeadphoneUsage(
            date = "2024-01-01",
            packageName = "com.app1",
            appName = "App 1",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        val usage2 = HeadphoneUsage(
            date = "2024-01-02",
            packageName = "com.app2",
            appName = "App 2",
            duration = 7200000L,
            startTime = 2000L,
            endTime = 7202000L
        )
        
        dao.insertUsage(usage1)
        dao.insertUsage(usage2)
        
        val allUsage = dao.getAllUsage()
        
        assertEquals(2, allUsage.size)
        assertTrue(allUsage.any { it.packageName == "com.app1" })
        assertTrue(allUsage.any { it.packageName == "com.app2" })
    }
    
    @Test
    fun testGetAllUsageReturnsEmptyList() = runBlocking {
        val allUsage = dao.getAllUsage()
        
        assertEquals(0, allUsage.size)
    }
    
    @Test
    fun testDeleteAllUsage() = runBlocking {
        val usage1 = HeadphoneUsage(
            date = "2024-01-01",
            packageName = "com.app1",
            appName = "App 1",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        val usage2 = HeadphoneUsage(
            date = "2024-01-02",
            packageName = "com.app2",
            appName = "App 2",
            duration = 7200000L,
            startTime = 2000L,
            endTime = 7202000L
        )
        
        dao.insertUsage(usage1)
        dao.insertUsage(usage2)
        
        assertEquals(2, dao.getAllUsage().size)
        
        dao.deleteAllUsage()
        
        assertEquals(0, dao.getAllUsage().size)
    }
    
    @Test
    fun testGetAllUsageBetweenDates() = runBlocking {
        val usage1 = HeadphoneUsage(
            date = "2024-01-01",
            packageName = "com.app1",
            appName = "App 1",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        val usage2 = HeadphoneUsage(
            date = "2024-01-05",
            packageName = "com.app2",
            appName = "App 2",
            duration = 7200000L,
            startTime = 2000L,
            endTime = 7202000L
        )
        val usage3 = HeadphoneUsage(
            date = "2024-01-10",
            packageName = "com.app3",
            appName = "App 3",
            duration = 1800000L,
            startTime = 3000L,
            endTime = 1803000L
        )
        
        dao.insertUsage(usage1)
        dao.insertUsage(usage2)
        dao.insertUsage(usage3)
        
        val rangeUsage = dao.getUsageForDateRange("2024-01-01", "2024-01-07")
        
        assertEquals(2, rangeUsage.size)
        assertTrue(rangeUsage.any { it.date == "2024-01-01" })
        assertTrue(rangeUsage.any { it.date == "2024-01-05" })
        assertFalse(rangeUsage.any { it.date == "2024-01-10" })
    }
    
    @Test
    fun testGetUsageForDateRangeWithNoMatches() = runBlocking {
        val usage = HeadphoneUsage(
            date = "2024-01-05",
            packageName = "com.app1",
            appName = "App 1",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        
        dao.insertUsage(usage)
        
        val rangeUsage = dao.getUsageForDateRange("2024-01-10", "2024-01-15")
        
        assertEquals(0, rangeUsage.size)
    }
}


