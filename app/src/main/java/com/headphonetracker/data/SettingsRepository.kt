package com.headphonetracker.data

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions", "detekt.TooManyFunctions")
class SettingsRepository @Inject constructor(
    private val prefs: SharedPreferences
) {

    // Onboarding
    fun isOnboardingCompleted(): Boolean = prefs.getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(value: Boolean) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    // Break reminders
    fun isBreakRemindersEnabled(): Boolean = prefs.getBoolean("break_reminders_enabled", false)
    fun setBreakRemindersEnabled(value: Boolean) = prefs.edit().putBoolean("break_reminders_enabled", value).apply()
    fun getBreakIntervalMinutes(): Int = prefs.getInt("break_interval_minutes", 60)
    fun setBreakIntervalMinutes(value: Int) = prefs.edit().putInt("break_interval_minutes", value).apply()

    // Daily limit
    fun getDailyLimitMinutes(): Int = prefs.getInt("daily_limit_minutes", 0)
    fun setDailyLimitMinutes(value: Int) = prefs.edit().putInt("daily_limit_minutes", value).apply()

    // Notifications / summary / milestones
    fun isDailySummaryEnabled(): Boolean = prefs.getBoolean("daily_summary_enabled", false)
    fun setDailySummaryEnabled(value: Boolean) = prefs.edit().putBoolean("daily_summary_enabled", value).apply()
    fun isMilestonesEnabled(): Boolean = prefs.getBoolean("milestones_enabled", true)
    fun setMilestonesEnabled(value: Boolean) = prefs.edit().putBoolean("milestones_enabled", value).apply()

    // Theme / accent
    fun getAppTheme(): String = prefs.getString("app_theme", "dark") ?: "dark"
    fun setAppTheme(value: String) = prefs.edit().putString("app_theme", value).apply()
    fun getAccentColor(): String = prefs.getString("accent_color", "cyan") ?: "cyan"
    fun setAccentColor(value: String) = prefs.edit().putString("accent_color", value).apply()

    // Auto-start
    fun isAutoStartEnabled(): Boolean = prefs.getBoolean("auto_start_enabled", false)
    fun setAutoStartEnabled(value: Boolean) = prefs.edit().putBoolean("auto_start_enabled", value).apply()

    // Excluded apps
    fun getExcludedApps(): Set<String> = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet()
    fun setExcludedApps(set: Set<String>) = prefs.edit().putStringSet("excluded_apps", set).apply()

    // Tracking state (transient)
    fun isTracking(): Boolean = prefs.getBoolean("is_tracking", false)
    fun setTracking(value: Boolean) = prefs.edit().putBoolean("is_tracking", value).apply()
}
