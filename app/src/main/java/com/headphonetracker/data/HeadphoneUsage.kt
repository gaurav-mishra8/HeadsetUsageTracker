package com.headphonetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "headphone_usage")
data class HeadphoneUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long, // in milliseconds
    val date: String // YYYY-MM-DD format
)
