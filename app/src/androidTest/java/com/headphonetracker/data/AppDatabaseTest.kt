package com.headphonetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseIsCreated() {
        assertNotNull(database)
    }

    @Test
    fun daoIsNotNull() {
        val dao = database.headphoneUsageDao()
        assertNotNull(dao)
    }

    @Test
    fun getDatabaseReturnsSameInstance() {
        val instance1 = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
        val instance2 = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())

        // Should return the same instance (singleton pattern)
        assertSame(instance1, instance2)
    }
}
