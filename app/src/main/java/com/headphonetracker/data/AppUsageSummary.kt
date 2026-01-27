package com.headphonetracker.data

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalDuration: Long
)

data class DetailedUsageSummary(
    val date: String,
    val packageName: String,
    val appName: String,
    val totalDuration: Long
)