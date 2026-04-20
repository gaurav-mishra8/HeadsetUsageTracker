package com.headphonetracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "headphone_usage",
    indices = [
        Index(value = ["date"]),
        Index(value = ["date", "packageName"])
    ]
)
data class HeadphoneUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long, // in milliseconds
    val date: String, // YYYY-MM-DD format
    val volumePercent: Float? = null // 0.0–1.0, null = unknown (treated as 70%)
)
