package com.headphonetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadphoneUsageDao {
    
    @Query("SELECT * FROM headphone_usage WHERE date = :date")
    fun getUsageByDate(date: String): Flow<List<HeadphoneUsage>>
    
    @Query("""
        SELECT packageName, appName, SUM(duration) as totalDuration 
        FROM headphone_usage 
        WHERE date = :date 
        GROUP BY packageName, appName
    """)
    suspend fun getUsageByAppForDate(date: String): List<AppUsageSummary>
    
    @Query("""
        SELECT date, SUM(duration) as totalDuration 
        FROM headphone_usage 
        GROUP BY date 
        ORDER BY date DESC 
        LIMIT 7
    """)
    suspend fun getLast7DaysUsage(): List<DailyUsageSummary>
    
    @Query("SELECT SUM(duration) FROM headphone_usage WHERE date = :date")
    suspend fun getTotalUsageForDate(date: String): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: HeadphoneUsage)
    
    @Query("DELETE FROM headphone_usage WHERE date < :date")
    suspend fun deleteOldData(date: String)
}

