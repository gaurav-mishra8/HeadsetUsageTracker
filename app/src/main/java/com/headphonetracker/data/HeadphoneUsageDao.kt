package com.headphonetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadphoneUsageDao {

    @Query("SELECT * FROM headphone_usage WHERE date = :date")
    fun getUsageByDate(date: String): Flow<List<HeadphoneUsage>>

    @Query(
        """
        SELECT packageName, appName, SUM(duration) as totalDuration 
        FROM headphone_usage 
        WHERE date = :date 
        GROUP BY packageName, appName
    """
    )
    suspend fun getUsageByAppForDate(date: String): List<AppUsageSummary>

    @Query(
        """
        SELECT date, SUM(duration) as totalDuration 
        FROM headphone_usage 
        WHERE date >= date('now', '-6 days')
        GROUP BY date 
        ORDER BY date DESC
    """
    )
    suspend fun getLast7DaysUsage(): List<DailyUsageSummary>

    @Query(
        """
        SELECT date, packageName, appName, SUM(duration) as totalDuration 
        FROM headphone_usage 
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY date, packageName, appName
        ORDER BY date DESC, totalDuration DESC
    """
    )
    suspend fun getUsageForDateRange(startDate: String, endDate: String): List<DetailedUsageSummary>

    @Query("SELECT SUM(duration) FROM headphone_usage WHERE date = :date")
    suspend fun getTotalUsageForDate(date: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: HeadphoneUsage)

    @Query("DELETE FROM headphone_usage WHERE date < :date")
    suspend fun deleteOldData(date: String)

    @Query("SELECT * FROM headphone_usage ORDER BY date DESC")
    suspend fun getAllUsage(): List<HeadphoneUsage>

    @Query("DELETE FROM headphone_usage")
    suspend fun deleteAllUsage()

    @Query("SELECT SUM(duration) FROM headphone_usage")
    suspend fun getTotalUsageAllTime(): Long?

    @Query(
        """
        SELECT date, SUM(duration) as totalDuration
        FROM headphone_usage
        WHERE packageName = :packageName
        GROUP BY date
        ORDER BY date DESC
        LIMIT 30
    """
    )
    suspend fun getLast30DaysUsageForApp(packageName: String): List<DailyUsageSummary>

    @Query("SELECT SUM(duration) FROM headphone_usage WHERE packageName = :packageName")
    suspend fun getTotalDurationForApp(packageName: String): Long?

    @Query(
        """
        SELECT SUM(duration) FROM headphone_usage
        WHERE packageName = :packageName AND date >= :sinceDate
    """
    )
    suspend fun getDurationForAppSince(packageName: String, sinceDate: String): Long?

    @Query("SELECT COUNT(DISTINCT date) FROM headphone_usage")
    suspend fun getTotalDaysWithUsage(): Int

    /**
     * Volume-weighted exposure in minutes for [date].
     * Formula: (duration_ms / 60000) × (volumePercent / 0.8)²
     * Unknown volume → treated as 0.7 (factor ≈ 0.766).
     * Safe daily budget = 480 weighted-minutes (WHO 80 dB / 8 h reference).
     */
    @Query("""
        SELECT SUM(
            (duration / 60000.0) * CASE
                WHEN volumePercent IS NULL   THEN 0.766
                WHEN volumePercent <= 0.4    THEN 0.1
                ELSE (volumePercent * volumePercent) / 0.64
            END
        ) FROM headphone_usage WHERE date = :date
    """)
    suspend fun getWeightedExposureMinutes(date: String): Float?

    @Query("""
        SELECT date,
            SUM((duration / 60000.0) * CASE
                WHEN volumePercent IS NULL   THEN 0.766
                WHEN volumePercent <= 0.4    THEN 0.1
                ELSE (volumePercent * volumePercent) / 0.64
            END) as totalDuration
        FROM headphone_usage
        WHERE date >= date('now', '-6 days')
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getLast7DaysWeightedExposure(): List<DailyUsageSummary>
}
